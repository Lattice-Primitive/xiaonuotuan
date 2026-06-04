#include <atomic>
#include <chrono>
#include <csignal>
#include <cstdio>
#include <cstdlib>
#include <iostream>
#include <memory>
#include <stdexcept>
#include <string>
#include <thread>

#include <camera/camera.h>
#include <camera/device_discovery.h>
#include <camera/photography_settings.h>

namespace {

std::atomic_bool g_stop{false};

void HandleSignal(int) {
    g_stop = true;
}

class FfplayStreamDelegate final : public ins_camera::StreamDelegate {
public:
    explicit FfplayStreamDelegate(ins_camera::VideoEncodeType encode_type) {
        const char* codec = encode_type == ins_camera::VideoEncodeType::H265 ? "hevc" : "h264";
        std::string command =
            std::string("ffplay -hide_banner -loglevel warning ")
            + "-fflags nobuffer -flags low_delay -framedrop "
            + "-probesize 32 -analyzeduration 0 "
            + "-f " + codec + " -i -";

        ffplay_ = popen(command.c_str(), "w");
        if (!ffplay_) {
            throw std::runtime_error("failed to start ffplay");
        }
    }

    ~FfplayStreamDelegate() {
        if (ffplay_) {
            pclose(ffplay_);
            ffplay_ = nullptr;
        }
    }

    void OnAudioData(const uint8_t*, size_t, int64_t) override {}

    void OnVideoData(const uint8_t* data, size_t size, int64_t, uint8_t, int stream_index) override {
        if (!ffplay_ || stream_index != 0) {
            return;
        }

        fwrite(data, 1, size, ffplay_);
        fflush(ffplay_);
    }

    void OnGyroData(const std::vector<ins_camera::GyroData>&) override {}
    void OnExposureData(const ins_camera::ExposureData&) override {}

private:
    FILE* ffplay_ = nullptr;
};

class StdoutStreamDelegate final : public ins_camera::StreamDelegate {
public:
    StdoutStreamDelegate() {
        setvbuf(stdout, nullptr, _IONBF, 0);
    }

    void OnAudioData(const uint8_t*, size_t, int64_t) override {}

    void OnVideoData(const uint8_t* data, size_t size, int64_t, uint8_t, int stream_index) override {
        if (stream_index != 0) {
            return;
        }

        fwrite(data, 1, size, stdout);
    }

    void OnGyroData(const std::vector<ins_camera::GyroData>&) override {}
    void OnExposureData(const ins_camera::ExposureData&) override {}
};

// The Linux SDK headers expose DeviceConnectionInfo as an opaque pointer.
// libCameraSDK.so copies a std::string at offset 0 and a uint16_t port at offset 0x20
// when ConnectionType::Wifi is used.
struct SocketConnectionInfo {
    std::string ip;
    uint16_t port;
};

} // namespace

int main(int argc, char* argv[]) {
    std::signal(SIGINT, HandleSignal);
    std::signal(SIGTERM, HandleSignal);

    std::string camera_ip = "192.168.42.1";
    uint16_t camera_port = 6666;
    bool output_stdout = false;
    for (int i = 1; i < argc; ++i) {
        const std::string arg = argv[i];
        if (arg == "--ip" && i + 1 < argc) {
            camera_ip = argv[++i];
        } else if (arg == "--port" && i + 1 < argc) {
            camera_port = static_cast<uint16_t>(std::stoi(argv[++i]));
        } else if (arg == "--stdout") {
            output_stdout = true;
        }
    }

    auto& log = output_stdout ? std::cerr : std::cout;

    ins_camera::SetLogLevel(ins_camera::LogLevel::WARNING);

    ins_camera::DeviceDiscovery discovery;
    auto devices = discovery.GetAvailableDevices();
    ins_camera::CameraType camera_type = ins_camera::CameraType::Insta360X4;
    std::shared_ptr<ins_camera::Camera> cam;

    if (!devices.empty()) {
        for (const auto& device : devices) {
            log << "Found camera: " << device.camera_name
                      << " serial=" << device.serial_number
                      << " fw=" << device.fw_version << std::endl;
        }

        camera_type = devices[0].camera_type;
        cam = std::make_shared<ins_camera::Camera>(devices[0].info);
        discovery.FreeDeviceDescriptors(devices);
    } else {
        log << "DeviceDiscovery found no USB camera. Trying direct Wi-Fi socket "
                  << camera_ip << ":" << camera_port << " ..." << std::endl;

        SocketConnectionInfo socket_info{camera_ip, camera_port};
        ins_camera::DeviceConnectionInfo connection_info{
            ins_camera::ConnectionType::Wifi,
            &socket_info,
        };
        cam = std::make_shared<ins_camera::Camera>(connection_info);
    }

    if (!cam->Open()) {
        std::cerr << "Failed to open camera." << std::endl;
        return 1;
    }

    const auto resolution = ins_camera::VideoResolution::RES_3840_1920P30;

    if (camera_type >= ins_camera::CameraType::Insta360X4) {
        cam->EnableInCameraStitching(true);
        cam->SetVideoSubMode(ins_camera::SubVideoMode::VIDEO_LIVEVIEW);

        ins_camera::RecordParams record_params;
        record_params.resolution = resolution;
        record_params.bitrate = 0;
        cam->SetVideoCaptureParams(record_params, ins_camera::CameraFunctionMode::FUNCTION_MODE_LIVE_STREAM);
    }

    auto preview_param = cam->GetPreviewParam();
    std::shared_ptr<ins_camera::StreamDelegate> stream_delegate;
    if (output_stdout) {
        stream_delegate = std::make_shared<StdoutStreamDelegate>();
    } else {
        stream_delegate = std::make_shared<FfplayStreamDelegate>(preview_param.encode_type);
    }
    cam->SetStreamDelegate(stream_delegate);

    ins_camera::LiveStreamParam param;
    param.video_resolution = resolution;
    param.lrv_video_resulution = ins_camera::VideoResolution::RES_1440_720P30;
    param.video_bitrate = 1024 * 1024 * 4;
    param.enable_audio = false;
    param.enable_gyro = false;
    param.using_lrv = false;

    if (!cam->StartLiveStreaming(param)) {
        std::cerr << "Failed to start live preview." << std::endl;
        cam->Close();
        return 1;
    }

    log << "Live preview started. Press Ctrl+C to stop." << std::endl;
    while (!g_stop) {
        std::this_thread::sleep_for(std::chrono::milliseconds(200));
    }

    cam->StopLiveStreaming();
    cam->Close();
    log << "Stopped." << std::endl;
    return 0;
}
