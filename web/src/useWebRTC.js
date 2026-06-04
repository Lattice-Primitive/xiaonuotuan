/**
 * WebRTC 实时监控 Hook —— 对齐 Android 端 WebRTCManager + SignalingClient 实现。
 *
 * 支持三种模式：
 * - VIEW:  纯观看（recvonly），等待对端 offer 或主动 createOffer(recvonly)
 * - PUSH:  推流（sendonly），获取本地摄像头后 createOffer
 * - BIDI:  双向（sendrecv），获取本地摄像头 + 接收远端流
 *
 * 信令协议与 Android SignalingClient 完全一致（WebSocket JSON）：
 *   { type: "join"|"leave"|"offer"|"answer"|"candidate", roomId, ... }
 */

import { useCallback, useEffect, useRef, useState } from 'react';

const ICE_SERVERS = [
  { urls: 'stun:stun.l.google.com:19302' },
  { urls: 'stun:stun1.l.google.com:19302' },
];

/** 连接状态，与 Android WebRTCManager.State 一一对应 */
export const ConnectionState = {
  IDLE: 'IDLE',
  CONNECTING: 'CONNECTING',
  CONNECTED: 'CONNECTED',
  DISCONNECTED: 'DISCONNECTED',
  FAILED: 'FAILED',
};

/** 流模式，与 Android StreamMode / WebRTCManager.Mode 对应 */
export const StreamMode = {
  VIEW: 'VIEW',
  PUSH: 'PUSH',
  BIDI: 'BIDI',
};

/**
 * @param {object} options
 * @param {string} options.signalUrl - WebSocket 信令地址
 * @param {string} options.roomId    - 房间 ID
 * @param {string} options.mode      - StreamMode.*
 * @param {boolean} options.active   - 是否激活连接
 */
export function useWebRTC({ signalUrl, roomId, mode = StreamMode.VIEW, active = true }) {
  const [connState, setConnState] = useState(ConnectionState.IDLE);
  const [remoteStream, setRemoteStream] = useState(null);
  const [localStream, setLocalStream] = useState(null);
  const [error, setError] = useState(null);

  const pcRef = useRef(null);
  const wsRef = useRef(null);
  const localStreamRef = useRef(null);
  const remoteDescriptionSet = useRef(false);
  const pendingCandidates = useRef([]);
  const reconnectTimer = useRef(null);
  const shouldReconnect = useRef(false);

  const cleanup = useCallback(() => {
    shouldReconnect.current = false;
    clearTimeout(reconnectTimer.current);

    if (wsRef.current) {
      try {
        wsRef.current.close(1000, 'cleanup');
      } catch (_) {}
      wsRef.current = null;
    }

    if (pcRef.current) {
      try {
        pcRef.current.close();
      } catch (_) {}
      pcRef.current = null;
    }

    if (localStreamRef.current) {
      localStreamRef.current.getTracks().forEach((t) => t.stop());
      localStreamRef.current = null;
    }

    pendingCandidates.current = [];
    remoteDescriptionSet.current = false;
    setRemoteStream(null);
    setLocalStream(null);
    setConnState(ConnectionState.IDLE);
    setError(null);
  }, []);

  const connect = useCallback(async () => {
    if (!signalUrl || !roomId) return;

    cleanup();
    shouldReconnect.current = true;
    setConnState(ConnectionState.CONNECTING);

    // 1. 如果需要发送本地媒体，先获取摄像头/麦克风
    let localMediaStream = null;
    if (mode === StreamMode.PUSH || mode === StreamMode.BIDI) {
      try {
        localMediaStream = await navigator.mediaDevices.getUserMedia({
          video: { width: { ideal: 1280 }, height: { ideal: 720 } },
          audio: true,
        });
        localStreamRef.current = localMediaStream;
        setLocalStream(localMediaStream);
      } catch (e) {
        setError(`获取摄像头失败: ${e.message}`);
        setConnState(ConnectionState.FAILED);
        return;
      }
    }

    // 2. 创建 PeerConnection
    const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS });
    pcRef.current = pc;

    // 添加本地 tracks
    if (localMediaStream) {
      localMediaStream.getTracks().forEach((track) => {
        pc.addTrack(track, localMediaStream);
      });
    }

    // 纯观看模式添加 recvonly transceiver
    if (mode === StreamMode.VIEW) {
      pc.addTransceiver('video', { direction: 'recvonly' });
      pc.addTransceiver('audio', { direction: 'recvonly' });
    }

    // 远端 track 回调
    pc.ontrack = (event) => {
      if (event.streams && event.streams[0]) {
        setRemoteStream(event.streams[0]);
        setConnState(ConnectionState.CONNECTED);
      }
    };

    pc.onconnectionstatechange = () => {
      switch (pc.connectionState) {
        case 'connected':
          setConnState(ConnectionState.CONNECTED);
          break;
        case 'disconnected':
          setConnState(ConnectionState.DISCONNECTED);
          break;
        case 'failed':
          setConnState(ConnectionState.FAILED);
          setError('P2P 连接失败');
          break;
        case 'closed':
          setConnState(ConnectionState.IDLE);
          break;
      }
    };

    // 3. 建立 WebSocket 信令
    let ws;
    try {
      ws = new WebSocket(signalUrl);
    } catch (e) {
      setError(`WebSocket 连接失败: ${e.message}`);
      setConnState(ConnectionState.FAILED);
      return;
    }
    wsRef.current = ws;

    const sendJson = (obj) => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(obj));
      }
    };

    // ICE candidate → 发送给信令
    pc.onicecandidate = (event) => {
      if (event.candidate) {
        sendJson({
          type: 'candidate',
          candidate: event.candidate.candidate,
          sdpMid: event.candidate.sdpMid,
          sdpMLineIndex: event.candidate.sdpMLineIndex,
          roomId,
        });
      }
    };

    ws.onopen = () => {
      // 加入房间
      sendJson({ type: 'join', roomId });

      // 如果是推流或双向模式，主动 createOffer
      if (mode === StreamMode.PUSH || mode === StreamMode.BIDI) {
        createAndSendOffer(pc, sendJson, roomId);
      }
    };

    ws.onmessage = (event) => {
      handleSignalingMessage(event.data, pc, sendJson, roomId, mode);
    };

    ws.onclose = () => {
      if (shouldReconnect.current) {
        reconnectTimer.current = setTimeout(() => {
          if (shouldReconnect.current) connect();
        }, 3000);
      }
    };

    ws.onerror = () => {
      setError('WebSocket 连接错误');
      if (shouldReconnect.current) {
        reconnectTimer.current = setTimeout(() => {
          if (shouldReconnect.current) connect();
        }, 3000);
      }
    };
  }, [signalUrl, roomId, mode, cleanup]);

  // 处理信令消息
  function handleSignalingMessage(data, pc, sendJson, roomId, mode) {
    let msg;
    try {
      msg = JSON.parse(data);
    } catch {
      return;
    }

    switch (msg.type) {
      case 'offer': {
        const desc = new RTCSessionDescription({ type: 'offer', sdp: msg.sdp });
        pc.setRemoteDescription(desc)
          .then(() => {
            remoteDescriptionSet.current = true;
            pendingCandidates.current.forEach((c) => pc.addIceCandidate(c));
            pendingCandidates.current = [];

            // 观看或双向模式下收到 offer，回复 answer
            if (mode === StreamMode.VIEW || mode === StreamMode.BIDI) {
              return pc.createAnswer();
            }
          })
          .then((answer) => {
            if (answer) {
              return pc.setLocalDescription(answer).then(() => {
                sendJson({ type: 'answer', sdp: answer.sdp, roomId });
              });
            }
          })
          .catch((e) => setError(`处理 offer 失败: ${e.message}`));
        break;
      }
      case 'answer': {
        const desc = new RTCSessionDescription({ type: 'answer', sdp: msg.sdp });
        pc.setRemoteDescription(desc)
          .then(() => {
            remoteDescriptionSet.current = true;
            pendingCandidates.current.forEach((c) => pc.addIceCandidate(c));
            pendingCandidates.current = [];
            setConnState(ConnectionState.CONNECTED);
          })
          .catch((e) => setError(`处理 answer 失败: ${e.message}`));
        break;
      }
      case 'candidate': {
        const candidate = new RTCIceCandidate({
          candidate: msg.candidate,
          sdpMid: msg.sdpMid,
          sdpMLineIndex: msg.sdpMLineIndex,
        });
        if (remoteDescriptionSet.current) {
          pc.addIceCandidate(candidate).catch(() => {});
        } else {
          pendingCandidates.current.push(candidate);
        }
        break;
      }
      case 'joined':
      case 'left':
      case 'error':
        if (msg.type === 'error') {
          setError(`信令错误: ${msg.message || 'unknown'}`);
        }
        break;
    }
  }

  async function createAndSendOffer(pc, sendJson, roomId) {
    try {
      const offer = await pc.createOffer({
        offerToReceiveVideo: true,
        offerToReceiveAudio: true,
      });
      await pc.setLocalDescription(offer);
      sendJson({ type: 'offer', sdp: offer.sdp, roomId });
    } catch (e) {
      setError(`createOffer 失败: ${e.message}`);
    }
  }

  // 激活/停用
  useEffect(() => {
    if (active) {
      connect();
    } else {
      cleanup();
    }
    return cleanup;
  }, [active, connect, cleanup]);

  return {
    connState,
    remoteStream,
    localStream,
    error,
    reconnect: connect,
    disconnect: cleanup,
  };
}
