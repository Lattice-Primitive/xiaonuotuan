import React from 'react';
import {
  Activity,
  AlarmClock,
  ArrowUpRight,
  Bell,
  BluetoothConnected,
  Camera,
  CameraOff,
  CheckCircle2,
  ChevronDown,
  CircleDot,
  Command,
  FlipHorizontal,
  HeartPulse,
  Home,
  Mic,
  MicOff,
  Pill,
  Radar,
  Radio,
  RefreshCw,
  ScanEye,
  Search,
  Settings2,
  ShieldCheck,
  Sparkles,
  UserRound,
  UsersRound,
  Video,
  VideoOff,
  Wifi,
  WifiOff,
} from 'lucide-react';
import { useWebRTC, ConnectionState, StreamMode } from './useWebRTC';

const navItems = [
  { label: '实时画面', icon: Camera, eyebrow: 'Live Monitor', title: '实时画面' },
  { label: '用药提醒', icon: Bell, eyebrow: 'Medication', title: '用药提醒' },
  { label: 'Rokid AR', icon: ScanEye, eyebrow: 'Rokid AR', title: 'AR 设备协同' },
  { label: '我的', icon: UserRound, eyebrow: 'Account', title: '家庭组与个人中心' },
];

/**
 * 监控标签 —— 对齐 Android MonitorScreen 的 MONITOR_TABS。
 * roomId 与 Android 端完全一致，保证信令能互通。
 */
const monitorTabs = [
  {
    name: '老人',
    scene: '客厅 - 爷爷',
    roomId: 'room_elderly',
    status: '稳定',
    color: '#ff7a45',
    location: 'Living Room',
    insight: 'Vitals Normal',
    quality: '1080p · 30fps',
    latency: '42 ms',
  },
  {
    name: '儿童',
    scene: '儿童房 - 小美',
    roomId: 'room_child',
    status: '活跃',
    color: '#2f80ed',
    location: 'Kids Room',
    insight: 'Motion Active',
    quality: '720p · 30fps',
    latency: '58 ms',
  },
  {
    name: '宠物',
    scene: '阳台 - 咪咪',
    roomId: 'room_pet',
    status: '休息',
    color: '#8b5cf6',
    location: 'Pet Corner',
    insight: 'Resting Mode',
    quality: '1080p · 24fps',
    latency: '64 ms',
  },
];

/** 流模式选项 —— 对齐 Android MonitorScreen 的 StreamMode enum */
const streamModes = [
  { key: StreamMode.VIEW, label: '观看' },
  { key: StreamMode.PUSH, label: '推流' },
  { key: StreamMode.BIDI, label: '双向' },
];

const meds = [
  { time: '08:00', name: '降压药', dose: '1 片', state: '已服用' },
  { time: '13:30', name: '维生素 D', dose: '2 粒', state: '待提醒' },
  { time: '20:00', name: '睡前药', dose: '1 片', state: '待提醒' },
];

const devices = [
  { name: 'Rokid Max Pro', state: '已连接', value: '92%', icon: ScanEye },
  { name: '客厅摄像头', state: '直播中', value: '1080p', icon: Video },
  { name: '用药提醒器', state: '在线', value: '3 个计划', icon: Pill },
];

/** 默认信令地址，可在页面上修改 */
const DEFAULT_SIGNAL_URL = 'wss://signal.papaya.example.com';

/** WebRTC 视频播放组件 —— 对齐 Android WebRtcVideoView */
function WebRtcVideo({ stream, mirror = false, muted = true, className = '' }) {
  const videoRef = React.useRef(null);

  React.useEffect(() => {
    if (videoRef.current && stream) {
      videoRef.current.srcObject = stream;
    }
    return () => {
      if (videoRef.current) {
        videoRef.current.srcObject = null;
      }
    };
  }, [stream]);

  return (
    <video
      ref={videoRef}
      autoPlay
      playsInline
      muted={muted}
      className={className}
      style={{
        width: '100%',
        height: '100%',
        objectFit: 'cover',
        transform: mirror ? 'scaleX(-1)' : 'none',
      }}
    />
  );
}

/** 连接状态 badge —— 对齐 Android ConnectionBadge */
function ConnectionBadge({ state }) {
  const map = {
    [ConnectionState.CONNECTED]: { text: '已连接', color: '#10b981' },
    [ConnectionState.CONNECTING]: { text: '连接中', color: '#ff7a45' },
    [ConnectionState.FAILED]: { text: '未连接', color: '#999' },
    [ConnectionState.DISCONNECTED]: { text: '已断开', color: '#999' },
    [ConnectionState.IDLE]: { text: '待机', color: '#999' },
  };
  const { text, color } = map[state] || map[ConnectionState.IDLE];

  return (
    <span className="conn-badge" style={{ '--badge-color': color }}>
      <span className="conn-dot" />
      {text}
    </span>
  );
}

/** 流模式切换器 —— 对齐 Android StreamModeSelector */
function StreamModeSelector({ current, onChange }) {
  return (
    <div className="stream-mode-selector">
      {streamModes.map((m) => (
        <button
          key={m.key}
          className={m.key === current ? 'selected' : ''}
          onClick={() => onChange(m.key)}
          type="button"
        >
          {m.label}
        </button>
      ))}
    </div>
  );
}

function App() {
  const [activeNav, setActiveNav] = React.useState(0);
  const [activeMonitor, setActiveMonitor] = React.useState(0);
  const [streamMode, setStreamMode] = React.useState(StreamMode.VIEW);
  const [signalUrl, setSignalUrl] = React.useState(DEFAULT_SIGNAL_URL);
  const [isMicOn, setIsMicOn] = React.useState(true);
  const currentNav = navItems[activeNav];
  const currentMonitor = monitorTabs[activeMonitor];

  // WebRTC hook —— 核心连接逻辑
  const {
    connState,
    remoteStream,
    localStream,
    error: webrtcError,
    reconnect,
    disconnect,
  } = useWebRTC({
    signalUrl,
    roomId: currentMonitor.roomId,
    mode: streamMode,
    active: activeNav === 0,
  });

  // 麦克风开关
  React.useEffect(() => {
    if (localStream) {
      localStream.getAudioTracks().forEach((t) => {
        t.enabled = isMicOn;
      });
    }
  }, [isMicOn, localStream]);

  const isLive = remoteStream && connState === ConnectionState.CONNECTED;
  const isPushing = localStream && streamMode !== StreamMode.VIEW;

  const transitionTo = React.useCallback((update) => {
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (!document.startViewTransition || reduceMotion) {
      update();
      return;
    }
    document.startViewTransition(update);
  }, []);

  const handleNavChange = (index) => {
    if (index === activeNav) return;
    transitionTo(() => setActiveNav(index));
  };

  const handleMonitorChange = (index) => {
    if (index === activeMonitor) return;
    transitionTo(() => {
      setActiveMonitor(index);
    });
  };

  const handleStreamModeChange = (mode) => {
    if (mode === streamMode) return;
    transitionTo(() => {
      setStreamMode(mode);
    });
  };

  return (
    <main className="app-shell min-h-screen text-papaya-ink">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">
            <Sparkles size={20} />
          </div>
          <div>
            <strong>Papaya</strong>
            <span>Family AR Care</span>
          </div>
        </div>

        <nav className="nav-list" aria-label="主导航">
          {navItems.map((item, index) => {
            const Icon = item.icon;
            return (
              <button
                className={index === activeNav ? 'nav-item active' : 'nav-item'}
                key={item.label}
                onClick={() => handleNavChange(index)}
                type="button"
              >
                <Icon size={19} />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>

        {/* 信令面板 —— 显示 WebRTC 连接状态 */}
        <div className="signal-panel">
          <div className="signal-ring">
            <Radar size={28} />
          </div>
          <span>WebRTC</span>
          <strong>{currentMonitor.latency}</strong>
          <ConnectionBadge state={connState} />
          <p>
            {streamMode === StreamMode.VIEW
              ? 'viewer'
              : streamMode === StreamMode.PUSH
                ? 'publisher'
                : 'bidirectional'}
          </p>
        </div>

        {/* 信令 URL 配置 */}
        <div className="signal-url-config">
          <label>
            <span>信令地址</span>
            <input
              value={signalUrl}
              onChange={(e) => setSignalUrl(e.target.value)}
              placeholder="wss://signal.papaya.example.com"
            />
          </label>
        </div>
      </aside>

      <section className="workspace" data-page={activeNav}>
        <header className="topbar" key={`topbar-${activeNav}`}>
          <div>
            <p className="eyebrow">{currentNav.eyebrow}</p>
            <h1 className="bg-gradient-to-r from-papaya-ink via-papaya-red to-blue-600 bg-clip-text text-transparent">
              {currentNav.title}
            </h1>
          </div>
          <div className="topbar-actions">
            <label className="search-box">
              <Search size={18} />
              <input placeholder="搜索设备、房间、用药计划" />
            </label>
            <button className="icon-button" aria-label="设置">
              <Settings2 size={20} />
            </button>
            <button className="primary-button" onClick={reconnect}>
              <Radio size={18} />
              <span>开始直播</span>
            </button>
          </div>
        </header>

        {activeNav === 0 && (
          <section className="hero-grid" key="live-page">
            <section className="monitor-stage">
              <div className="stage-toolbar">
                {/* 监控标签 —— 对齐 Android MonitorViewTab */}
                <div className="segmented">
                  {monitorTabs.map((tab, index) => (
                    <button
                      className={index === activeMonitor ? 'selected' : ''}
                      key={tab.name}
                      onClick={() => handleMonitorChange(index)}
                      type="button"
                    >
                      <span style={{ '--tab-color': tab.color }} />
                      {tab.name}
                    </button>
                  ))}
                </div>

                {/* 流模式切换 —— 对齐 Android StreamModeSelector */}
                <StreamModeSelector current={streamMode} onChange={handleStreamModeChange} />

                <div className="stage-toolbar-right">
                  <ConnectionBadge state={connState} />
                  {(isLive || isPushing) && (
                    <div className="live-badge">
                      <CircleDot size={14} />
                      LIVE
                    </div>
                  )}
                </div>
              </div>

              {/* 视频区域 —— 对齐 Android MonitorFrame 的 Box(280dp) */}
              <div
                className={`video-frame ${isLive || isPushing ? 'stream-active' : ''}`}
                key={`video-${activeMonitor}-${streamMode}`}
              >
                {/* 远端视频流 */}
                {isLive && (
                  <WebRtcVideo
                    stream={remoteStream}
                    className="webrtc-video"
                    muted={false}
                  />
                )}

                {/* 本地推流预览（推流或双向模式） */}
                {isPushing && !isLive && (
                  <WebRtcVideo
                    stream={localStream}
                    className="webrtc-video"
                    mirror={true}
                    muted={true}
                  />
                )}

                {/* 双向模式下同时显示远端和本地（画中画） */}
                {isPushing && isLive && streamMode === StreamMode.BIDI && (
                  <div className="pip-container">
                    <WebRtcVideo
                      stream={localStream}
                      className="pip-video"
                      mirror={true}
                      muted={true}
                    />
                  </div>
                )}

                {/* 未连接时的占位 */}
                {!isLive && !isPushing && (
                  <>
                    <div className="video-grid" />
                    <div className="scan-line" />
                    <div className="video-placeholder">
                      <span className="placeholder-icon">
                        {connState === ConnectionState.CONNECTING
                          ? '⏳'
                          : activeMonitor === 0
                            ? '👤'
                            : activeMonitor === 1
                              ? '👧'
                              : '🐱'}
                      </span>
                      <span className="placeholder-text">
                        {connState === ConnectionState.CONNECTING
                          ? '连接中...'
                          : connState === ConnectionState.FAILED
                            ? '连接失败'
                            : '监控画面'}
                      </span>
                      {webrtcError && (
                        <span className="placeholder-error">{webrtcError}</span>
                      )}
                    </div>
                  </>
                )}

                {/* HUD 覆盖层 */}
                <div className="hud hud-left">
                  <span>{currentMonitor.scene}</span>
                  <strong>{currentMonitor.quality}</strong>
                </div>
                <div className="hud hud-right">
                  {isPushing ? (
                    isMicOn ? <Mic size={16} /> : <MicOff size={16} />
                  ) : (
                    <Mic size={16} />
                  )}
                  <Wifi size={16} />
                  <BluetoothConnected size={16} />
                </div>
              </div>

              {/* 控制栏 —— 对齐 Android CameraControlButton */}
              <div className="control-strip">
                <button onClick={() => {
                  if (streamMode === StreamMode.VIEW) {
                    handleStreamModeChange(StreamMode.PUSH);
                  } else {
                    handleStreamModeChange(StreamMode.VIEW);
                  }
                }}>
                  {streamMode !== StreamMode.VIEW ? <CameraOff size={18} /> : <Camera size={18} />}
                  {streamMode !== StreamMode.VIEW ? '停止推流' : '摄像头'}
                </button>
                <button onClick={() => setIsMicOn(!isMicOn)}>
                  {isMicOn ? <Mic size={18} /> : <MicOff size={18} />}
                  {isMicOn ? '麦克风开' : '麦克风关'}
                </button>
                <button>
                  <ShieldCheck size={18} />
                  隐私遮罩
                </button>
                <button onClick={() => handleStreamModeChange(StreamMode.BIDI)}>
                  <Command size={18} />
                  双向模式
                </button>
              </div>
            </section>

            <section className="status-column">
              <div className="metric-panel attention">
                <div>
                  <span>今日提醒</span>
                  <strong>3</strong>
                </div>
                <AlarmClock size={28} />
              </div>
              <div className="metric-panel cool">
                <div>
                  <span>{currentMonitor.name}状态</span>
                  <strong>{currentMonitor.status}</strong>
                </div>
                <Activity size={28} />
              </div>
              <div className="ar-panel">
                <div className="ar-visual">
                  <div className="lens lens-left" />
                  <div className="lens lens-right" />
                  <div className="bridge" />
                </div>
                <p>Rokid AR</p>
                <strong>已连接 · 低延迟</strong>
                <button>
                  设备详情
                  <ArrowUpRight size={16} />
                </button>
              </div>
            </section>
          </section>
        )}

        <section
          className={activeNav === 0 ? 'content-grid' : 'content-grid active-page-grid'}
          key={`content-${activeNav}`}
        >
          {(activeNav === 0 || activeNav === 1) && (
            <section className="panel medication-panel">
              <div className="panel-title">
                <div>
                  <p className="eyebrow">Medication</p>
                  <h2>用药提醒</h2>
                </div>
                <button className="ghost-button">
                  今天
                  <ChevronDown size={16} />
                </button>
              </div>
              <div className="timeline">
                {meds.map((med) => (
                  <div className="timeline-row" key={med.time}>
                    <time>{med.time}</time>
                    <div className="timeline-dot" />
                    <div className="timeline-content">
                      <strong>{med.name}</strong>
                      <span>{med.dose}</span>
                    </div>
                    <span className={med.state === '已服用' ? 'tag done' : 'tag'}>{med.state}</span>
                  </div>
                ))}
              </div>
            </section>
          )}

          {(activeNav === 0 || activeNav === 2) && (
            <section className="panel device-panel">
              <div className="panel-title">
                <div>
                  <p className="eyebrow">Devices</p>
                  <h2>设备矩阵</h2>
                </div>
                <button className="icon-button small" aria-label="设备设置">
                  <Settings2 size={17} />
                </button>
              </div>
              <div className="device-list">
                {devices.map((device) => {
                  const Icon = device.icon;
                  return (
                    <div className="device-row" key={device.name}>
                      <div className="device-icon">
                        <Icon size={20} />
                      </div>
                      <div>
                        <strong>{device.name}</strong>
                        <span>{device.state}</span>
                      </div>
                      <em>{device.value}</em>
                    </div>
                  );
                })}
              </div>
            </section>
          )}

          {(activeNav === 0 || activeNav === 3) && (
            <section className="panel account-panel">
              <div className="profile">
                <div className="avatar">
                  <UsersRound size={28} />
                </div>
                <div>
                  <h2>小糯团家庭组</h2>
                  <p>4 位成员 · 2 个监控房间</p>
                </div>
                <CheckCircle2 className="verified" size={22} />
              </div>
              <div className="quick-stats">
                <div>
                  <strong>98%</strong>
                  <span>提醒准时率</span>
                </div>
                <div>
                  <strong>12h</strong>
                  <span>今日监控</span>
                </div>
                <div>
                  <strong>4</strong>
                  <span>紧急联系人</span>
                </div>
              </div>
            </section>
          )}

          {activeNav === 1 && (
            <section className="panel focus-panel medication-focus">
              <div>
                <p className="eyebrow">Next Dose</p>
                <h2>13:30 · 维生素 D</h2>
                <span>距离下次提醒还有 2 小时 14 分钟</span>
              </div>
              <div className="focus-orbit">
                <Pill size={46} />
              </div>
            </section>
          )}

          {activeNav === 2 && (
            <section className="panel focus-panel ar-focus">
              <div>
                <p className="eyebrow">Rokid Glasses</p>
                <h2>低延迟协同已就绪</h2>
                <span>蓝牙连接稳定，音视频通道可用，当前电量 92%</span>
              </div>
              <div className="focus-orbit">
                <ScanEye size={48} />
              </div>
            </section>
          )}

          {activeNav === 3 && (
            <section className="panel focus-panel account-focus">
              <div>
                <p className="eyebrow">Family</p>
                <h2>小糯团家庭组</h2>
                <span>4 位成员在线协同，紧急联系人与隐私设置已同步</span>
              </div>
              <div className="focus-orbit">
                <UsersRound size={48} />
              </div>
            </section>
          )}
        </section>
      </section>
    </main>
  );
}

export default App;
