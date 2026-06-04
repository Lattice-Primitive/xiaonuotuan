# Insta360 X4 Linux Preview

Minimal Linux preview path for Insta360 X4:

1. Connect Linux Wi-Fi to the X4 hotspot.
2. Use CameraSDK to start live preview.
3. Enable X4 in-camera stitching.
4. Pipe the encoded preview stream to `ffplay`.

```bash
nmcli dev wifi connect "X4 9XEPGJ.OSC" password "782WKBVS"
./tools/insta360_x4_preview/build.sh
./tools/insta360_x4_preview/run.sh
```

On the current Linux CameraSDK package, `DeviceDiscovery::GetAvailableDevices()` only discovers USB devices. The preview tool therefore falls back to a direct Wi-Fi socket connection:

```text
192.168.42.1:6666
```

You can override it if needed:

```bash
./tools/insta360_x4_preview/run.sh --ip 192.168.42.1 --port 6666
```

To bridge the wireless preview into the React web console:

```bash
cd web
bun run x4:bridge
```

Then open the web UI. The live monitor panel loads:

```text
http://localhost:8787/x4.mjpeg
```

The bridge also exposes the newest decoded JPEG frame for Android/LLM sampling:

```text
http://localhost:8787/latest.jpg
```

If discovery fails, confirm that the computer is connected to the X4 Wi-Fi and that the camera screen accepted the connection authorization prompt.

You can verify the Wi-Fi layer independently:

```bash
curl http://192.168.42.1/osc/info
```

If this returns X4 camera info but the official CameraSDK demo says no device found, the Wi-Fi/OSC layer is working and the failure is in CameraSDK device discovery. The local preview tool works around this by directly connecting to the X4 CameraSDK socket port.

For host-side high-quality stitching, use `libMediaSDK/example/realtime_stitcher_demo.cc`. That path requires the MediaSDK runtime and OpenCV display dependencies.
