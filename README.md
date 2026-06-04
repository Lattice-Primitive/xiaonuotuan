# Papaya

基于 Kotlin + Jetpack Compose + Material3 的 Android 原生应用，集成了 Rokid CXR-M SDK，用于构建手机端与 Rokid Glasses 的控制和协同应用。

## Web 端

仓库新增 `web/` React + Tailwind CSS 控制台原型，用于和 Android 端统一产品信息架构：实时画面、用药提醒、Rokid AR、我的。

```bash
cd web
bun install
bun run dev
```

生产构建：

```bash
cd web
bun run build
```

X4 无线实时画面桥接：

```bash
nmcli dev wifi connect "X4 9XEPGJ.OSC" password "782WKBVS"
cd web
bun run x4:bridge
```

Web 端实时画面会加载本机桥接流 `http://localhost:8787/x4.mjpeg`。

Android App 端 X4 预览与讲解：

1. Linux 连接 X4 热点并启动桥接：

```bash
nmcli dev wifi connect "X4 9XEPGJ.OSC" password "782WKBVS"
cd web
bun run x4:bridge
```

2. 手机进入 App → `实时画面` → 模式选择 `X4`。

默认桥接地址为 `http://192.168.42.3:8787/x4.mjpeg`，可在 App 输入框里修改。USB 调试时也可以执行 `adb reverse tcp:8787 tcp:8787`，然后把地址改成 `http://127.0.0.1:8787/x4.mjpeg`。

3. 连接 Rokid 眼镜后，点击 X4 预览下方的 `X4 讲解`。

App 会从同一桥接服务自动推导抽帧地址，例如 `http://192.168.42.3:8787/latest.jpg`，每 15 秒抓取一帧，复用 `我的 → 开发者选项` 里的 LLM 配置调用多模态接口，并把返回文字通过 `ai_result` 通道发回眼镜端 App 显示。

当前仓库内的 Insta360 CameraSDK / MediaSDK 是 x86-64 Linux 版本，手机端纯本地直连 X4 需要另接可在 Android/ARM 上运行的 Insta360 SDK 或实现等价的相机预览协议；现阶段手机端 X4 讲解依赖上述桥接服务。

## Rokid 视觉讲解

手机 App 的 Rokid AR 页面新增 `视觉讲解` 开关。开启后，手机端会每 15 秒请求 Rokid 眼镜拍一张图，复用 `我的 → 开发者选项` 里的 LLM 配置调用多模态接口，并把返回文字通过 `ai_result` 通道发回眼镜端 App 显示。

眼镜端运行在 YodaOS-Sprite / Android Go 环境中，只负责小屏展示和 CXR 通信。Rokid 系统保留的触控板、返回、拍照、录像和唤醒词交互不在裸机 App 中重写。

注意：Rokid 的 Client ID / Client Secret / Access Key 请只填在开发者选项或本地配置里，不要写进源码或提交记录。

## 版本历史

### v0.1.19-MEM (当前)

- **🧠 LLM 长记忆系统 — 智能检索 + 思维链联想**
  - 新增 `MemoryEntity` + `MemoryDao`（Room 持久化），存储记忆条目（内容、标签、重要性、时间戳）
  - `AppDatabase` 升级 v3，新增 `llm_memories` 表 + MIGRATION_2_3 迁移脚本
  - 新增 `LlmMemoryManager`：记忆增删改查、关键字搜索、按重要性排序、构建记忆上下文注入 prompt、从对话中自动提取新记忆
  - 新增 `MemoryAwareLlmClient`：LlmClient 装饰器，完整六步流程（检索→注入→对话→提取→联想→压缩）
  - `LlmClientFactory` 新增 `createWithMemory()` 便捷工厂方法
  - `ImageDescriber` 新增 `createWithMemory(context)` 和 `forClient()` 入口
- **🔍 基于问题的智能记忆检索**
  - 对话前根据用户问题自动提取关键词（中文滑窗分词 + 英文分词，内置停用词过滤）
  - 两步检索策略：关键词 DB LIKE 预筛选 → LLM 相关性精筛（支持直接/间接相关判断）
  - 只注入与当前问题相关的记忆，避免无关信息干扰
  - `retrieveRelevantMemories()` / `buildRelevantMemoryContext()` 公开 API
- **🔗 思维链联想（Chain-of-Thought Association）**
  - 对话后基于相关记忆 + 对话内容，让 LLM 进行链式推理
  - 自动发现记忆之间的逻辑关系、因果联系，推导隐含信息
  - 生成 `[inference]`/`[connection]`/`[prediction]` 三类推理记忆（重要性=4）
  - 至少 2 条相关记忆时触发，最多生成 5 条新推理记忆
- **📦 记忆容量增强**
  - 记忆上限 100 条，超 50 条自动 LLM 摘要压缩
  - 分层注入：摘要全文 + 最近 10 条补充，`MAX_CONTEXT_CHARS = 4000` 截断保护
- **⚙️ 可配置参数**
  - `useRelevanceRetrieval`（智能检索开关）、`useChainOfThought`（思维链联想开关）
- **📄 文件改动**
  - 新增：`data/db/MemoryEntity.kt`、`data/db/MemoryDao.kt`、`api/llm/LlmMemoryManager.kt`、`api/llm/MemoryAwareLlmClient.kt`
  - 修改：`data/db/AppDatabase.kt`、`api/llm/LlmClients.kt`、`service/ImageDescriber.kt`

### v0.1.14-AR

- **🥽 Rokid 双向通信协议与眼镜端 App**
  - 新增完整 Papaya 双向通信协议（`RokidConstants.BidiCmd`）：定义 8 个通道（papaya_ping/pong/status_sync/cmd/event/chat/nav/notify）+ 20+ 子命令，覆盖状态同步、远程控制、事件上报、聊天消息
  - **ConnectedDashboard 增强**：实时电量进度条（带充电指示）、亮度/音量/屏幕状态实时显示（StateFlow 订阅）、佩戴状态标签、运行中场景标签（AI/翻译/录音等）、可展开设备详情面板（SN/固件/设备ID/助手版本/SDK版本）
  - **RokidBidirectionalScreen 完整重写**：远程亮度/音量滑块（拖拽即时生效）、文本/通知/Toast/TTS/同步时间/状态请求/系统消息等快捷命令、聊天消息发送、心跳状态指示器（最后响应时间 + 延迟）、实时 TX/RX 通信日志
- **📱 眼镜端 App 模块 (`glass-app`)**
  - 新建 `glass-app` Gradle 模块，运行在 Rokid AR 眼镜上（480x640 竖屏 UI，黑底白字）
  - `GlassMainActivity`：连接状态显示、最近消息面板、通知列表面板
  - `PapayaGlassService`：后台通信服务，处理手机端轻量命令
    - Ping → Pong + 设备状态 JSON（电量/亮度/音量/设备型号）
    - 远程命令响应（SET_BRIGHTNESS/SET_VOLUME/SHOW_TOAST 等）
    - 每 30 秒主动上报电量到手机端
    - 接收聊天消息和通知推送并展示
  - `GlassProtocol`：完整协议文档（通道表、子命令表、JSON 格式规范）
  - 使用 `com.rokid.cxr:cxr-service-bridge:1.0` 眼镜端 CXR 通信桥接库
- **📄 文件改动**
  - 修改：`RokidConstants.kt`（新增 Channel.PAPAYA_* + BidiCmd）、`RokidTabScreen.kt`（ConnectedDashboard 增强）、`RokidBidirectionalScreen.kt`（完整重写）、`settings.gradle.kts`（include glass-app）
  - 新增：`glass-app/build.gradle.kts`、`glass-app/AndroidManifest.xml`、`GlassMainActivity.kt`、`PapayaGlassService.kt`、`GlassProtocol.kt`、`glass-app/README.md`

### v0.1.13-MR

- **🔧 "我的"页面全部功能真实可用化**
  - 消除所有纯展示/空 onClick 的 UI 元素，全部改为可交互且持久化
  - **个人资料**：昵称、邮箱、手机号修改后持久化到 SharedPreferences，重启保留
  - **通知设置**：待办提醒、监控报警、系统通知、声音、震动 5 个开关状态持久化
  - **隐私与安全**：修改密码弹窗（含确认校验）、生物识别/数据加密开关持久化、隐私政策/用户协议跳转浏览器
  - **退出登录**：点击弹出确认弹窗，确认后清除本地数据并关闭应用
  - **监控设备管理**：设备点击显示详情弹窗（状态/IP/固件版本），添加新设备有 Toast 提示
  - **紧急联系人**：支持添加/删除，列表交互完整
  - **关于我们**：给我们评分→打开 GitHub、开源许可→打开 LICENSE、更新日志→弹窗展示版本日志
- **🆘 紧急联系按钮功能化**
  - TodoScreen 底部紧急联系按钮从空操作改为弹出联系人列表，点击即可拨打电话（Intent.ACTION_DIAL）
- **📊 统计数据动态化**
  - AccountScreen 的 StatsRow 从硬编码改为实时读取：待办数量从数据库查询、监控设备数/家庭成员数动态计算
- **🥽 AR 提词器功能可用**
  - RokidTabScreen 的"提词器"功能按钮从无响应改为可点击切换开关，状态实时显示并记录日志
- **📄 文件改动**
  - `TodoScreen.kt`：紧急联系按钮 + 拨号弹窗
  - `AccountScreen.kt`：动态统计 + 退出确认弹窗
  - `AccountSubScreens.kt`：全部子页面设置持久化 + 交互可用
  - `RokidTabScreen.kt`：提词器 onClick + 状态联动

### v0.1.12-AR

- **🥽 Rokid CXR-M SDK 接入全面重写**
  - 通过反编译 `com.rokid.cxr:client-m:1.0.8` 的 `CxrApi`（100+ 公开签名）、`ValueUtil.CxrStatus/CxrSceneType/CxrStreamType/CxrBluetoothErrorCode/CxrMediaType`、`infos/GlassInfo`、`infos/SceneStatusInfo` 以及所有 `listeners/callbacks`，核对并重写 Papaya 侧 6 个 Rokid 文件，使之严格对齐 SDK 真实 API。
- **🐛 关键 Bug 修复**
  - `CxrSBridge` 原先用 `caps.at(i).string` 读取自定义通道数据 —— SDK 中实际只公开 `getString()/getInt()/getLong()/getBinary()` 方法，这段代码**运行时会 `NoSuchFieldError`**。已改为安全辅助 `capString/capInt/capLong/capBinary`。
- **📡 状态响应式化（StateFlow）**
  - `RokidApi` 新增 `connectionStateFlow / deviceInfoFlow / runtimeStatusFlow / sceneStatusFlow`，UI 可直接 `collectAsState()` 订阅，替代原先轮询式调用。
  - `RokidManager` 初始化时自动订阅 SDK 六大更新监听器：`BatteryLevelUpdateListener`（电量 + 充电）、`BrightnessUpdateListener`、`VolumeUpdateListener`、`ScreenStatusUpdateListener`、`GlassStatusUpdateListener`（穿戴 / 全局 TTS 状态）、`SceneStatusUpdateListener`（15 个场景开关实时同步）。
  - 新增 `RokidRuntimeStatus` / `RokidSceneStatus` 数据模型，连接成功时自动调用 `getGlassInfo()` 和 `refreshSceneStatus()`，UI 无需额外操作。
- **🔐 凭据管理重构（`RokidCredentials`）**
  - 新增 `RokidCredentials.kt` 统一管理 `clientSecret` / `snEncryptHex` / `lastSocketUuid` / `lastMacAddress`。
  - 优先级：用户手动输入（SharedPreferences）→ BuildConfig 兜底（`ROKID_CLIENT_ID` / `ROKID_CLIENT_SECRET` / `ROKID_SN_ENCRYPT_HEX`）→ 空。
  - `connect()` 自动记忆连接参数，新增 `reconnectLast()` 一键断线重连。
- **📚 SDK API 全量覆盖（`RokidApi` / `CxrSdkProxy`）**
  - 从原先 10+ 方法扩展到 60+ 方法，覆盖 SDK 所有关键能力：
    - **ASR**：`sendAsrContent / notifyAsrNone / notifyAsrEnd / notifyAsrError`
    - **TTS**：`sendTtsContent / notifyTtsAudioFinished(ok)`
    - **全局**：`sendGlobalToast / sendGlobalMessage / sendGlobalTts`（对应 SDK `sendGlobalToastContent / sendGlobalMsgContent / sendGlobalTtsContent`）
    - **AI**：`notifyAiStart / notifyAiError / notifyNoNetwork`
    - **提词器**：`configWordTipsText(fontSize, lineSpacing, color, bgAlpha, textAlign, maxLine, speed) / sendWordTipsContent`（对应 SDK `WORD_TIPS` 场景）
    - **翻译**：`setTranslationLanguage(src, dst, model) / sendTranslationContent(srcLang, dstLang, isAsr, isFinal, text)`
    - **相机**：`openGlassCamera / takeGlassPhoto / takeGlassPhotoGlobal / closeGlassCamera / openCameraVideo / setPhotoParams / setVideoParams / isGlassCameraInUse`
    - **音频**：`openAudioRecord(codec, mode, intent, denoiseMode) / closeAudioRecord / startPlayAudio / writeAudioStream`
    - **系统**：`setGlassBrightness / setGlassVolume / setScreenOffTimeout / setPowerOffTimeout / syncGlassTime / notifyGlassReboot / notifyGlassShutdown`
  - `CxrSdkProxy` 新增 `ValueUtil.CxrStatus.ok()` 扩展，统一把 `REQUEST_SUCCEED / REQUEST_WAITING / RESPONSE_SUCCEED` 视为成功。
  - `controlScene` 做了 Papaya 字符串 → `CxrSceneType` 枚举的完整映射（提词器 → `WORD_TIPS`、翻译 → `TRANSLATE`、AI → `AI_CHAT`、音/视频录制、导航）。
- **🛡️ 稳健性增强**
  - `RokidManager` 新增 `guarded{}` 护栏：未连接时所有 SDK 调用直接返回 `false`，且捕获 SDK 抛出的异常，避免应用崩溃。
  - BLE 扫描过滤器从"强 UUID 匹配"改为"UUID 过滤器 + 名称回退"（应对不同固件 Service UUID 差异），名称匹配放宽为 `rokid` / `glass` 任一关键字。
  - 新增 `RokidConstants.bluetoothErrorMessage(code)`，把 `CxrBluetoothErrorCode`（`PARAM_INVALID` / `BLE_CONNECT_FAILED` / `SOCKET_CONNECT_FAILED` / `SN_CHECK_FAILED`）翻译成中文用户提示。
- **🎨 业务级快捷方法（`CxrSBridge`）**
  - `glassToast / glassMessage / glassSpeak`：向眼镜弹 Toast / 消息 / TTS 朗读
  - `startTeleprompter / pushTeleprompterContent / stopTeleprompter`：提词器开场景 + 设置字体 + 推送文案
  - `startTranslation / stopTranslation / pushTranslation`：翻译场景
  - `pushAiTurn(userAsr, assistantTts)`：把一次完整 AI 对话（AI 开始 → ASR → TTS → 完成）下发给眼镜
  - `takePhoto` / `registerGlassesMessageListener`：常用快捷封装
- **📄 文件改动**
  - 重写：`RokidApi.kt` / `RokidManager.kt` / `CxrSdkProxy.kt` / `CxrSBridge.kt` / `RokidConstants.kt` / `RokidDevice.kt`
  - 新增：`RokidCredentials.kt`
  - UI 代码（`RokidDemoScreen.kt` / `RokidTabScreen.kt` / `RokidBidirectionalScreen.kt`）无需改动即可继续使用（接口向后兼容）。

### v0.1.11-AR


- **🥽 Rokid 集成代码审核与修复**
  - 根据 Rokid CXR-M SDK 官方文档（v1.0.8）全面审查 Rokid 相关代码，修复 6 项差异
  - **蓝牙权限修复**：`hasBluetoothPermissions()` 检查 `ACCESS_FINE_LOCATION`（原错误地使用了 `ACCESS_COARSE_LOCATION`），符合官方 BLE 扫描要求
  - **连接回调包装**：`CxrSdkProxy.connectBluetooth()` 补全 `RokidConnectionCallback` → `BluetoothStatusCallback` 桥接代码（两个重载）
  - **摄像头回调转发**：`openCamera()` 中 `MediaStreamListener` 事件正确转发到 `RokidCameraCallback`（`onCameraOpened` / `onCameraClosed` / `onCameraError` / `onCameraFrame`）
  - **音频 API 同步 SDK 1.0.8**：`openAudioRecord()` 新增 `intent` 和 `denoiseMode` 参数；录音前调用 `setAudioStreamListener()` 注册 `AudioStreamListener`
  - **蓝牙扫描权限标志**：`BLUETOOTH_SCAN` 添加 `android:usesPermissionFlags="neverForLocation"`，声明不用于获取位置信息
- **📄 文档同步更新**
  - README SDK 版本号 `1.0.1-20250812.080117-2` → `1.0.8`
  - Gradle 依赖版本号同步实际配置，SDK 标记为注释/模拟模式
  - 权限表格补充 `CAMERA` / `RECORD_AUDIO`、`neverForLocation` 和 `maxSdkVersion` 说明
  - SDK 导入路径修正为 `com.rokid.cxr.CxrApi`
  - 初始化代码示例补全 `onConnected()` / `onConnecting()` 回调
  - 项目结构表格新增 5 个 Rokid 核心文件说明

### v0.1.10-Dev

- **👤 "我的"页面二级菜单完善**
  - 为"个人设置"分组中的"个人资料"、"通知设置"、"隐私与安全"添加二级菜单页面
  - 为"设备与服务"分组中的"监控设备管理"、"紧急联系人"、"关于我们"添加二级菜单页面
  - 二级菜单统一使用返回按钮 + 页面标题 + 卡片式内容布局，与"开发者选项"页面风格一致
  - "通知设置"支持 Switch 开关交互（用药提醒、监控报警、系统通知等通知类型）
  - "隐私与安全"支持生物识别、数据加密等开关配置
  - 其余二级菜单均包含静态占位内容，点击事件预留（通过 `onClick = {}` 空函数），可由后续开发接入真实功能
- **📄 新增文件**: [AccountSubScreens.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/ui/AccountSubScreens.kt)
  - 定义 `AccountSubScreen` 枚举：`None` / `ProfileInfo` / `NotificationSettings` / `PrivacySecurity` / `DeviceManagement` / `EmergencyContacts` / `AboutUs`
  - 封装通用子页面模板组件 `AccountSubScreenTemplate`、`SubSection`、`SubClickItem`、`SubToggleItem`、`SubDivider`
  - `AccountScreen` 通过 `currentSubScreen` 状态变量控制页面导航，进入二级菜单时隐藏底栏菜单内容，返回时恢复

### v0.1.9-Dev

- **🔧 开发者选项功能 (`DeveloperOptionsScreen`)**
  - 在"我的"页面 → "设备与服务"菜单中新增"开发者选项"入口
  - **调试设置**：调试模式开关（SharedPreferences 持久化）、保持屏幕唤醒开关（Window Flag 控制）
  - **网络配置**：API 服务器地址输入、WebRTC 信令服务器地址输入、OkHttp 日志级别选择器（NONE / BASIC / HEADERS / BODY）
  - **数据管理**：查看应用详情（跳转系统设置）、导出数据库内容到 Logcat、清除本地 Room 数据库
  - 进入开发者选项时自动隐藏底部导航栏，返回时恢复
- **📄 版本记录更新**
  - README 新增 v0.1.9-Dev 版本历史

### v0.1.8-MR

- **🧭 底部导航栏重构**
  - 单页面布局升级为三页面底部导航结构：`实时画面` / `用药提醒` / `我的`
  - `MainActivity` 使用 Material3 `NavigationBar` + `NavigationBarItem` 实现底部切换
  - `Crossfade` 动画实现页面间平滑过渡切换
  - 新增 `material-icons-extended` 依赖提供 Material Icons 图标
- **📹 实时画面独立页面 (`MonitorScreen`)**
  - 从 `HomeMonitorScreen` 拆分为独立 `MonitorScreen`，专注监控功能
  - 移除底部笑脸动画组件，页面更简洁
  - 监控对象切换卡片重新设计：圆形头像图标 + 渐变色背景 + 底部指示条
  - 选中/未选中状态平滑动画过渡（文字颜色 + 指示条显隐）
- **🚀 独立 WebRTC 推流逻辑**
  - 为每个监控对象（老人/儿童/宠物）分配独立 `roomId`
  - 切换监控对象时自动断开当前连接并重连对应房间的 WebRTC 流
  - `MonitorTab` 数据类新增 `roomId` 字段
- **💊 用药提醒独立页面 (`MedicationScreen`)**
  - 用药提醒卡片从监控页面独立为新页面
  - 保留完整功能：时间线展示、添加/删除用药、切换已服用状态
  - 紧急联系按钮保留在用药提醒页面底部
- **👤 账户页面 (`AccountScreen`)** 🆕
  - 用户头像展示（渐变色圆形头像 + "已认证"徽章）
  - 统计信息卡片：用药计划 / 监控设备 / 家庭成员数量
  - 个人设置菜单：个人资料、通知设置、隐私与安全
  - 设备与服务菜单：监控设备管理、紧急联系人、关于我们
  - 退出登录入口
- **🗑️ 代码清理**
  - 旧 `HomeMonitorScreen` 标记为 `@Deprecated`，被新页面体系取代
  - 移除未使用的 import，修复编译错误

### v0.1.7-debug

- **💊 用药提醒功能完整实现**
  - 从静态占位升级为全功能用药提醒系统，支持添加/删除/切换状态
  - Room 数据库持久化用药计划，重启不丢失
  - `MedicationEntity` / `MedicationDao` / `AppDatabase` 三层数据架构
  - `MedicationRepository` 业务逻辑层：增删改查 + 闹钟调度与取消
  - `AlarmManager` 精确闹钟 + `AlarmReceiver` 广播接收，时间到达推送系统通知
  - `BootReceiver` 开机自启，自动重新注册所有闹钟
  - `NotificationHelper` 通知渠道创建与消息推送
  - `MedicationViewModel` 连接 UI 与数据层，Flow 实时监听数据变化
  - `MedicationManagerDialog` 添加用药对话框，含药品名称输入 + 时间选择器
  - `MedicationReminderCard` 重构：实时显示待服用数量、点击状态标签切换已服用/待服用、删除按钮
- **🔧 构建系统修复**
  - KSP 替代 KAPT 用于 Room 注解处理器（Kotlin 2.2.10 兼容）
  - 添加 `mavenCentral()` 仓库确保新版依赖可下载
  - 补充项目缺失的 Retrofit、OkHttp 依赖（`ChatApi`、`ImageDescriber` 所需）
  - 修复 `WebRTCManager` 缺少 `onIceConnectionReceivingChange` 方法导致的编译错误
  - 新增 `android.disallowKotlinSourceSets=false` 兼容 KSP 生成源码

### v0.1.7-rc1

- **🔧 构建系统修复与镜像源迁移（Release Candidate）**
  - Gradle 发行版下载地址从阿里云镜像迁移至腾讯云镜像，解决阿里云 DNS 解析失败问题
  - 移除不可达的 `maven.aliyun.com` 仓库，避免 Gradle 同步时因 DNS 解析失败而中断
  - `pluginManagement` 新增 `gradlePluginPortal()` 仓库，修复 `foojay-resolver-convention` 插件无法解析的问题
  - 保留清华镜像 (`mirrors.tuna.tsinghua.edu.cn`) 作为 Maven 依赖加速源

### v0.1.8-MR

- **📤 WebRTC 多模式推流（P2P Publisher / Viewer / Bidirectional）**
  - `WebRTCManager.Mode` 新增三种连接模式：`VIEWER`（观看）、`PUBLISHER`（推流）、`BIDIRECTIONAL`（双向）
  - 新增 `publish(videoTrack, audioTrack)` 方法，向 PeerConnection 添加本地音视频轨并主动发起 Offer
  - 新增 `unpublish()` 方法，移除所有已发送轨
  - `setRemoteDescription` 智能分发：Offer → 非 Publisher 模式创建 Answer；Answer → Publisher 模式视为连接成功
  - 新增 `createOffer()` 方法，支持 Publisher/Bidirectional 作为发起端
  - 暴露 `eglContext` 和 `factory` 属性，供 CameraManager 复用 EGL 上下文
- **📷 本地摄像头采集（CameraManager）**
  - 新增 `CameraManager` 类，封装 Camera2 / Camera1 双 API 自动降级
  - 支持前后摄像头枚举与 `switchCamera()` 动态翻转
  - 自动创建 `VideoTrack` / `AudioTrack`，通过 `onVideoTrackReady` 回调通知上层
  - 采集规格：1280x720 @ 30fps
  - 新增 `State.CONNECTING` 状态下自动发布本地流
- **🔌 WebSocket 信令实现（SignalingClient）**
  - 新增 `SignalingClient` 类，完整实现 `SignalingAdapter` 接口
  - 基于 OkHttp WebSocket，支持 `wss://` 安全连接
  - JSON 消息协议：`offer` / `answer` / `candidate` / `join` / `leave` / `error`
  - 自动重连机制（断线 3s 后重试）
- **🎮 UI 交互增强**
  - 新增 `StreamModeSelector` 组件：观看 / 推流 / 双向 三种模式一键切换
  - 推流模式下实时展示本地摄像头预览画面（前置摄像头自动镜像）
  - 新增摄像头控制栏：🔄 翻转摄像头、🎤/🔇 麦克风开关
  - 未授权时显示「📷 授予摄像头权限」按钮，点击触发运行时权限请求
  - 动态权限请求：`CAMERA` + `RECORD_AUDIO`，通过 `ActivityResultContracts.RequestMultiplePermissions` 处理回调
- **🔧 依赖与配置**
  - `AndroidManifest.xml` 新增 `CAMERA` / `RECORD_AUDIO` 权限声明
  - `build.gradle.kts` 显式添加 `com.squareup.okhttp3:okhttp:4.12.0` 依赖

### v0.1.6

- **📹 WebRTC 实时直播窗口**
  - 新增 `webrtc/` 包，实现完整的 WebRTC 视频直播能力
  - `SignalingAdapter` 接口 — 抽象信令层，支持任意后端实现（WebSocket / REST 等）
  - `WebRTCManager` — 核心管理类：PeerConnectionFactory 初始化、SDP 协商、ICE 候选处理、远端 VideoTrack 回调
  - `WebRtcVideoView` — Compose 视频渲染组件（`AndroidView` + `SurfaceViewRenderer` 硬件加速）
  - `MonitorFrame` 重构：监控画面区域从静态占位升级为 WebRTC 直播窗口
- **✨ 连接状态感知 UI**
  - 新增 `ConnectionBadge` 组件，实时显示 WebRTC 连接状态（待机 / 连接中 / 已连接 / 已断开 / 未连接）
  - Live 模式下右上角显示红色 `LIVE` 徽章
  - 连接中显示 ⏳ 动画 + "连接中..." 文字
  - 连接失败显示对应错误提示
  - 视频背景自动在深色网格（未连接）与纯黑（直播中）之间切换
- **🔧 依赖新增**
  - `io.getstream:stream-webrtc-android:1.1.1` — Google WebRTC 官方 Android 库
  - `AndroidManifest.xml` 新增 `MODIFY_AUDIO_SETTINGS` 权限（WebRTC 音频轨需要）

### v0.1.5

- **🎯 LLM API 抽象层**
  - 新增 `api/llm/` 包，解耦 LLM 服务调用，支持未来无缝切换不同大模型（OpenAI / Claude / Gemini / 本地模型等）
  - `LLMModels.kt` — 通用领域模型：`LLMConfig`、`LLMMessage`、`LLMMultimodalMessage`、`LLMContentPart`（sealed class：Text / ImageUrl）、`LLMResponse`、`LLMStreamChunk`、`LLMResult<T>`（Success / Error）
  - `LLMProvider` 接口 — 定义 4 个核心方法：`chat()` / `chatMultimodal()` / `chatStream()` / `chatMultimodalStream()`
  - `OpenAICompatProvider` — OpenAI 兼容实现，支持非流式 + SSE 流式两种模式，支持 `extraHeaders` / `extraBody` 扩展
  - `LLMClient` 门面类 — 便捷方法：`chat(prompt)` 快速文本对话、`describeImage(bitmap/url/bytes)` 图片描述、自动 Bitmap→base64 编码
  - `ImageDescriber` 内部重构为使用 `LLMClient`，对外 API 保持向后兼容
- **🐛 修复**
  - 修复 `HomeMonitorScreen.kt` 缺少 `togetherWith` 导入导致的编译错误

### v0.1.4

- **🔒 安全性增强**
  - 修复 `ImageDescriber` 中 HTTP Body 日志拦截器泄露 API Key 的问题：Release 构建完全禁用日志、Debug 构建仅记录 Headers
  - `local.properties` 注入改为白名单机制，仅允许 `api_key`/`base_url`/`model` 注入 `BuildConfig`，防止意外泄露其他配置项（如 SDK 路径、密码等）
  - 移除 ProGuard 过宽 `-keep class kotlin.**` 规则，提升 Release 构建的混淆和代码收缩效果
  - 新增 `-assumenosideeffects BuildConfig.DEBUG` 规则，Release 构建中彻底消除 DEBUG 分支
  - 添加 `-allowaccessmodification` 允许 R8 更激进的内联优化
  - 新增 Debug 专属网络安全配置，允许 `localhost`/模拟器明文流量方便开发，Release 保持严格锁定

### v0.1.3

- **🎨 UI 全面美化升级**
  - 优化配色方案：调整主色/辅助色饱和度，新增宠物紫色系、阴影色，提升视觉和谐度
  - 标题区域：增大字号（30sp → 32sp）、ExtraBold 字重、优化间距布局
  - 笑脸 IP：增加外圈光晕、内圈高光，添加呼吸缩放动画（~2s 周期）
- **✨ 动画与交互增强**
  - 监控标签切换：颜色/边框/文字颜色平滑过渡动画（300ms）
  - 监控画面切换：fadeIn/fadeOut 交互动画
  - 实时指示器：红色圆点闪烁动画
  - 紧急按钮：水平红-深红渐变背景 + 脉冲缩放动画 + 阴影光晕
- **💊 用药提醒卡片重构**
  - 添加卡片阴影（6dp）
  - 时间轴：圆点增大（12dp → 14dp），添加外圈光晕，连接线改用渐变色
  - 图标区域：圆角背景容器
  - 状态标签：添加细边框，增大圆角与间距
- **📐 监控卡片改进**
  - 添加卡片阴影（8dp）
  - 选中标签样式增强：圆角边框 + 高亮背景
  - 监控画面容器增大（180dp → 200dp），圆角增大
  - 场景标签：圆角与内边距优化
- **🔧 代码清理**
  - 移除未使用的 import（Icon、painterResource、offset 等）

### v0.1.1

- 新增 **Cloud AI 图片描述** 功能
- 集成 OpenAI 兼容的视觉 API，将照片自动转为文字描述
- 提供 `ImageDescriber` 服务类，支持 Bitmap / URL / byte[] 三种输入方式
- 内置 Compose UI：图片选择器 + 描述按钮 + 加载状态 + 结果展示

### v0.1.0

- 项目初始化
- Rokid CXR-M SDK 集成
- 基础蓝牙 & 网络权限配置

## 技术栈

| 组件 | 版本 |
|---|---|
| Gradle | 9.4.1 |
| Android Gradle Plugin | 9.2.0 |
| Kotlin | 2.2.10 |
| compileSdk / targetSdk | 36 |
| minSdk | 29 |
| Compose BOM | 2026.02.01 |
| Rokid CXR-M SDK | 1.0.8 |

## Cloud AI 图片描述 (v0.1.1)

调用 OpenAI 兼容的视觉 API，将图片描述为文字。支持任意兼容端点（OpenAI、Azure、硅基流动等）。

### 1. 配置 API 信息

在 `local.properties` 中配置（该文件已加入 `.gitignore`，不会被提交到版本控制）：

```properties
api.key=sk-your-api-key-here
api.base_url=https://api.openai.com
api.model=gpt-4o-mini
```

配置会自动注入为 `BuildConfig` 字段：
- `BuildConfig.api_key`
- `BuildConfig.api_base_url`
- `BuildConfig.api_model`

### 2. 核心 API

```kotlin
val describer = ImageDescriber(
    apiKey = BuildConfig.api_key,
    baseUrl = BuildConfig.api_base_url,
    model = BuildConfig.api_model   // 默认 gpt-4o-mini
)

// 从 Bitmap 描述
describer.describeBitmap(bitmap)

// 从图片 URL 描述（直接传链接，不传图片字节）
describer.describeUrl("https://example.com/photo.jpg")

// 从字节数组描述
describer.describeFileBytes(bytes)
```

### 3. 项目结构

| 文件 | 说明 |
|---|---|
| [api/ChatModels.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/api/ChatModels.kt) | API 请求 / 响应数据模型 |
| [api/ChatApi.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/api/ChatApi.kt) | Retrofit 接口，调用 `v1/chat/completions` |
| [api/llm/LLMModels.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/api/llm/LLMModels.kt) | 🆕 v0.1.5 通用 LLM 领域模型 |
| [api/llm/LLMProvider.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/api/llm/LLMProvider.kt) | 🆕 v0.1.5 LLM 核心接口，定义 chat / stream 协议 |
| [api/llm/OpenAICompatProvider.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/api/llm/OpenAICompatProvider.kt) | 🆕 v0.1.5 OpenAI 兼容实现，含 SSE 流式解析 |
| [api/llm/LLMClient.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/api/llm/LLMClient.kt) | 🆕 v0.1.5 LLM 门面类，封装便捷调用方法 |
| [service/ImageDescriber.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/service/ImageDescriber.kt) | 核心服务类，内部使用 `LLMClient`，封装编码、压缩、鉴权 |
| [webrtc/SignalingAdapter.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/webrtc/SignalingAdapter.kt) | 🆕 v0.1.6 信令适配器接口，抽象 SDP/ICE 交换协议 |
| [webrtc/WebRTCManager.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/webrtc/WebRTCManager.kt) | 🆕 v0.1.6 → 🔄 v0.1.8 WebRTC 核心管理，连接生命周期 + 视频轨回调 + 推流模式（Viewer / Publisher / Bidirectional） |
| [ui/component/WebRtcVideoView.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/ui/component/WebRtcVideoView.kt) | 🆕 v0.1.6 Compose WebRTC 视频渲染组件 |
| [webrtc/CameraManager.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/webrtc/CameraManager.kt) | 🆕 v0.1.8 本地摄像头采集管理（Camera2 / Camera1 双 API，前后翻转，音视频采集） |
| [webrtc/SignalingClient.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/webrtc/SignalingClient.kt) | 🆕 v0.1.8 WebSocket 信令客户端实现（自动重连，JSON 消息协议） |
| [MainActivity.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/MainActivity.kt) | 带图片选择和描述功能的 UI |
| [AccountSubScreens.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/ui/AccountSubScreens.kt) | 🆕 v0.1.10 "我的"页面二级菜单：个人资料、通知设置、隐私安全、设备管理、紧急联系人、关于我们 |
| [RokidApi.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/rokid/RokidApi.kt) | 🆕 Rokid SDK 抽象接口：连接、数据、摄像头、音频、AI 聊天 |
| [RokidManager.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/rokid/RokidManager.kt) | 🆕 Rokid 单例实现：BLE 扫描、配对、连接、Mock 设备 |
| [CxrSdkProxy.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/rokid/CxrSdkProxy.kt) | 🆕 CXR-M SDK 代理层（目前为 Mock 模式，注释了真实 SDK 调用） |
| [RokidConstants.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/rokid/RokidConstants.kt) | 🆕 Rokid 常量：BLE UUID、连接状态、眼镜类型、功能特性 |
| [RokidDevice.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/rokid/RokidDevice.kt) | 🆕 Rokid 设备数据模型 |

## API 接口说明

以下 API 接口已定义但暂未接入真实后端服务，当前为占位/桩实现，可直接在现有代码中对接：

### 账户相关（"我的"页面二级菜单）

| 接口 | 位置 | 说明 |
|---|---|---|
| 个人资料保存/更新 | [AccountSubScreens.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/ui/AccountSubScreens.kt) — `ProfileInfoScreen` | 昵称、邮箱、手机号、头像等字段的 `SubClickItem` 的 `onClick` 为空 |
| 通知设置持久化 | [AccountSubScreens.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/ui/AccountSubScreens.kt) — `NotificationSettingsScreen` | `medicationReminder` / `monitorAlert` / `systemNotice` / `soundEnabled` / `vibrationEnabled` 开关状态仅内存保存，需对接 SharedPreferences 或后端 |
| 密码修改 | [AccountSubScreens.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/ui/AccountSubScreens.kt) — `PrivacySecurityScreen` | "修改密码" `SubClickItem` 的 `onClick` 为空，需接入修改密码 API |
| 隐私政策/用户协议 | [AccountSubScreens.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/ui/AccountSubScreens.kt) — `PrivacySecurityScreen` | `onClick` 为空，需接入 WebView 或 URL 跳转 |
| 设备管理 | [AccountSubScreens.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/ui/AccountSubScreens.kt) — `DeviceManagementScreen` | 设备列表为静态数据，`SubClickItem` 的 `onClick` 均为空，需对接设备绑定/解绑/状态查询 API |
| 紧急联系人管理 | [AccountSubScreens.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/ui/AccountSubScreens.kt) — `EmergencyContactsScreen` | 联系人列表为静态数据，`SubClickItem` 的 `onClick` 均为空，需对接联系人 CRUD API |
| 关于我们 | [AccountSubScreens.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/ui/AccountSubScreens.kt) — `AboutUsScreen` | "给我们评分"、"开源许可"、"更新日志" 的 `onClick` 为空，需接入对应跳转逻辑 |

### 开发者选项

| 接口 | 位置 | 说明 |
|---|---|---|
| 开发者配置持久化 | [DeveloperOptionsScreen.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/ui/DeveloperOptionsScreen.kt) | 调试模式、API 地址、Rokid 凭证等已通过 `SharedPreferences` 持久化；`API 服务器地址` 和 `信令服务器地址` 字段已预留，可对接网络层使用 |
| Rokid 配置 | [DeveloperOptionsScreen.kt](file:///Users/levi/AndroidStudioProjects/papaya/app/src/main/java/com/nanackations2/papaya/ui/DeveloperOptionsScreen.kt) — "Rokid 配置" 区域 | Client ID / Client Secret / Access Key 已支持输入和持久化，可对接 Rokid SDK 初始化 |

> **设计细节**：图片自动压缩至最长边 1024px、JPEG 质量 80%，以控制 token 消耗。超时配置为连接 30s / 读取 120s / 写入 60s。

## LLM API 抽象层 (v0.1.5)

从 v0.1.5 开始，项目引入了统一的 LLM 抽象层，将 LLM 服务调用与具体实现解耦。

### 架构

```
调用方（UI / Service）
      │
      ▼
┌─────────────────┐
│   LLMClient      │  ← 门面：封装常用操作
└────────┬────────┘
         │ 委托
         ▼
┌─────────────────┐
│  LLMProvider     │  ← 接口：定义 LLM 调用协议
└────────┬────────┘
         │ 实现
         ▼
┌─────────────────────┐
│ OpenAICompatProvider │  ← OpenAI 兼容实现
│ (可替换为其他 Provider)│
└─────────────────────┘
```

### 新增 Provider 示例

实现 `LLMProvider` 接口即可接入任意大模型：

```kotlin
class ClaudeProvider(config: LLMConfig) : LLMProvider {
    override suspend fun chat(messages: List<LLMMessage>): LLMResult<LLMResponse> { ... }
    override suspend fun chatMultimodal(messages: List<LLMMultimodalMessage>): LLMResult<LLMResponse> { ... }
    override fun chatStream(messages: List<LLMMessage>): Flow<LLMStreamChunk> { ... }
    override fun chatMultimodalStream(messages: List<LLMMultimodalMessage>): Flow<LLMStreamChunk> { ... }
}

// 使用
val client = LLMClient(ClaudeProvider(config))
```

## Rokid CXR-M SDK 集成

### 1. 配置认证信息

在 `local.properties` 中配置 Rokid 平台凭证（该文件已加入 `.gitignore`，不会被提交到版本控制）：

```properties
rokid.clientId=YOUR_CLIENT_ID
rokid.clientSecret=YOUR_CLIENT_SECRET
rokid.accessKey=YOUR_ACCESS_KEY
```

前往 [Rokid 开发者平台](https://ar.rokid.com/sdk) 注册获取以上凭证。

### 2. Gradle 配置

**settings.gradle.kts** — 已添加 Rokid Maven 仓库：

```kotlin
maven { url = uri("https://maven.rokid.com/repository/maven-public/") }
```

**app/build.gradle.kts** — 已添加 SDK 依赖及配套库：

```kotlin
dependencies {
    // Rokid CXR-M SDK - 取消注释以启用真实硬件连接
    // implementation("com.rokid.cxr:client-m:1.0.8")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
```

认证信息通过 `local.properties` 自动注入为 `BuildConfig` 字段：
- `BuildConfig.rokid_clientId`
- `BuildConfig.rokid_clientSecret`
- `BuildConfig.rokid_accessKey`

### 3. 权限声明

`AndroidManifest.xml` 已添加以下权限：

| 权限 | 用途 |
|---|---|
| `BLUETOOTH` / `BLUETOOTH_ADMIN` | 经典蓝牙 |
| `BLUETOOTH_CONNECT` | Android 12+ 蓝牙连接 |
| `BLUETOOTH_SCAN` (带 `neverForLocation`) | Android 12+ 蓝牙扫描（不获取位置） |
| `ACCESS_FINE_LOCATION` (maxSdkVersion=30) | 蓝牙扫描（Android 11 及以下需要） |
| `ACCESS_COARSE_LOCATION` (maxSdkVersion=30) | 蓝牙扫描（Android 11 及以下备用） |
| `INTERNET` / `ACCESS_NETWORK_STATE` / `CHANGE_NETWORK_STATE` | 网络通信 |
| `ACCESS_WIFI_STATE` / `CHANGE_WIFI_STATE` | Wi-Fi 状态 |
| `CAMERA` | 摄像头采集（WebRTC 推流） |
| `RECORD_AUDIO` | 音频采集（WebRTC 推流 / 语音） |
| `FOREGROUND_SERVICE` | 前台服务 |
| `POST_NOTIFICATIONS` | 通知权限 |

### 4. 初始化 SDK

```kotlin
import com.rokid.cxr.CxrApi
import com.rokid.cxr.BluetoothStatusCallback

val bluetoothDevice = // 通过蓝牙扫描获取 Rokid Glasses 设备
CxrApi.getInstance().initBluetooth(context, bluetoothDevice, object : BluetoothStatusCallback {
    override fun onConnectionInfo(
        socketUuid: String?,
        macAddress: String?,
        rokidAccount: String?,
        glassesType: Int,
    ) {
        // 配对成功，保存 socketUuid 和 macAddress
    }

    override fun onConnected() {
        // 蓝牙连接已建立
    }

    override fun onConnecting() {
        // 正在连接
    }

    override fun onDisconnected() {
        // 断开连接
    }

    override fun onError(code: Int, message: String?) {
        // 连接失败
    }
})
```

> **注意**：SDK 依赖当前为注释状态，项目运行在**模拟模式**（不依赖真实 Rokid 硬件）。如需使用真实 Rokid Glasses，请取消 `build.gradle.kts` 中 Rokid SDK 依赖的注释，并在 `local.properties` 中配置开发者凭证。
>
> Android 12+ (API 31+) 需要在运行时动态申请 `BLUETOOTH_CONNECT` 和 `BLUETOOTH_SCAN` 权限。`BLUETOOTH_SCAN` 已添加 `neverForLocation` 标志，声明扫描蓝牙设备不用于获取位置信息。

## 构建

```bash
./gradlew assembleDebug
```
