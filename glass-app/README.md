# Papaya Glass App

运行在 Rokid AR 眼镜上的配套 App，与手机端 Papaya App 通过 CXR 自定义通道进行双向通信。

## 功能

- **消息接收与展示**：接收手机端推送的文本、通知、AI 结果
- **状态上报**：定期向手机端报告电量、亮度、音量等
- **远程控制响应**：处理来自手机的亮度/音量等轻量控制
- **心跳机制**：响应手机端 Ping，返回设备状态 JSON

## 通信协议

参见 `protocol/GlassProtocol.kt` 中的完整文档。

## 依赖

- `com.rokid.cxr:cxr-service-bridge:1.0` — Rokid 眼镜端 CXR 通信桥接库
- Jetpack Compose — UI 框架
- Gson — JSON 序列化

## 构建

```bash
./gradlew :glass-app:assembleDebug
```

生成的 APK 通过 ADB 安装到 Rokid 眼镜：

```bash
adb install glass-app/build/outputs/apk/debug/glass-app-debug.apk
```

## 架构

```
glass-app/
├── GlassMainActivity.kt     # 主界面（480x640 竖屏，显示视觉讲解/通知）
├── service/
│   └── PapayaGlassService.kt  # 后台通信服务
└── protocol/
    └── GlassProtocol.kt       # 协议文档与常量
```

## 注意事项

1. 眼镜端通信使用 `com.rokid.cxr.CXRServiceBridge`，区别于手机端的 `com.rokid.cxr.client.extend.CxrApi`
2. YodaOS-Sprite 基于 Android Go，眼镜端只保留通信与展示，重计算、网络请求和图片处理放在手机端
3. Rokid Glasses 设计尺寸为 480x640，Activity 固定竖屏并按小屏布局
4. 系统保留的触控板、返回、拍照、录像和唤醒词交互不在裸机 App 中重写
5. `com.rokid.cxr:glass-m` 未在当前公开 Maven 仓库中发布，已改用公开可解析的 `cxr-service-bridge`
