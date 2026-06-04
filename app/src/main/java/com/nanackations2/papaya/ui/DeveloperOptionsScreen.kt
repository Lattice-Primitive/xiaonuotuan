package com.nanackations2.papaya.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanackations2.papaya.api.llm.LlmClientFactory
import com.nanackations2.papaya.api.llm.LlmConfigStore
import com.nanackations2.papaya.api.llm.LlmProvider
import com.nanackations2.papaya.data.db.AppDatabase
import com.nanackations2.papaya.ui.theme.DividerColor
import com.nanackations2.papaya.ui.theme.Info
import com.nanackations2.papaya.ui.theme.InfoLight
import com.nanackations2.papaya.ui.theme.Primary
import com.nanackations2.papaya.ui.theme.PrimaryLight
import com.nanackations2.papaya.ui.theme.Secondary
import com.nanackations2.papaya.ui.theme.SecondaryLight
import com.nanackations2.papaya.ui.theme.ShadowColor
import com.nanackations2.papaya.webrtc.StreamProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val TAG = "DeveloperOptions"

@Composable
fun DeveloperOptionsScreen(
    onBack: () -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dev_options", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    var debugMode by remember { mutableStateOf(prefs.getBoolean("debug_mode", false)) }
    var keepScreenOn by remember { mutableStateOf(prefs.getBoolean("keep_screen_on", false)) }
    var apiUrl by remember { mutableStateOf(prefs.getString("api_url", "") ?: "") }
    var signalUrl by remember { mutableStateOf(prefs.getString("signal_url", "") ?: "") }
    var logLevel by remember { mutableStateOf(prefs.getString("log_level", "NONE") ?: "NONE") }
    var logLevelExpanded by remember { mutableStateOf(false) }

    var rokidClientId by remember { mutableStateOf(prefs.getString("rokid_client_id", "") ?: "") }
    var rokidClientSecret by remember { mutableStateOf(prefs.getString("rokid_client_secret", "") ?: "") }
    var rokidAccessKey by remember { mutableStateOf(prefs.getString("rokid_access_key", "") ?: "") }
    var fakeConnected by remember { mutableStateOf(prefs.getBoolean("fake_connected", false)) }

    // 推流协议
    var streamProtocol by remember {
        mutableStateOf(prefs.getString("stream_protocol", StreamProtocol.WEBSOCKET.name) ?: StreamProtocol.WEBSOCKET.name)
    }
    var streamProtocolExpanded by remember { mutableStateOf(false) }
    var whipEndpoint by remember { mutableStateOf(prefs.getString("whip_endpoint", "") ?: "") }
    var whepEndpoint by remember { mutableStateOf(prefs.getString("whep_endpoint", "") ?: "") }
    var streamToken by remember { mutableStateOf(prefs.getString("stream_token", "") ?: "") }
    var mjpegUrl by remember { mutableStateOf(prefs.getString("mjpeg_url", "http://localhost:8787/x4.mjpeg") ?: "http://localhost:8787/x4.mjpeg") }

    // LLM
    var llmProvider by remember {
        mutableStateOf(prefs.getString("llm_provider", LlmProvider.OPENAI.name) ?: LlmProvider.OPENAI.name)
    }
    var llmProviderExpanded by remember { mutableStateOf(false) }
    var llmApiKey by remember { mutableStateOf(prefs.getString("llm_api_key", "") ?: "") }
    var llmBaseUrl by remember { mutableStateOf(prefs.getString("llm_base_url", "") ?: "") }
    var llmModel by remember { mutableStateOf(prefs.getString("llm_model", "") ?: "") }
    var llmMaxTokens by remember { mutableFloatStateOf(prefs.getInt("llm_max_tokens", 800).toFloat()) }
    var llmTemperature by remember { mutableFloatStateOf(prefs.getFloat("llm_temperature", 0.7f)) }

    // LLM 连接测试状态
    var llmTestState by remember { mutableStateOf<LlmTestState>(LlmTestState.Idle) }

    // 记忆数据统计
    var memoryCount by remember { mutableIntStateOf(-1) }
    var todoCount by remember { mutableIntStateOf(-1) }

    // 重置确认弹窗
    var showResetDialog by remember { mutableStateOf(false) }

    val logLevels = listOf("NONE", "BASIC", "HEADERS", "BODY")
    val streamProtocolNames = StreamProtocol.entries.map { it.name }
    val llmProviderNames = LlmProvider.entries.map { it.name }

    // 加载数据统计
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                memoryCount = db.memoryDao().count()
                todoCount = db.todoDao().getAllTodosOnce().size
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load stats", e)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "开发者选项",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "调试、测试和高级配置",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )

        // ==================== 应用和设备信息 ====================
        DevSection(title = "应用 & 设备信息") {
            DevInfoRow(label = "应用版本", value = getAppVersion(context))
            DevDivider()
            DevInfoRow(label = "设备型号", value = "${Build.MANUFACTURER} ${Build.MODEL}")
            DevDivider()
            DevInfoRow(label = "Android 版本", value = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            DevDivider()
            DevInfoRow(label = "可用内存", value = getAvailableMemory(context))
            DevDivider()
            DevInfoRow(
                label = "数据统计",
                value = "记忆 ${if (memoryCount >= 0) memoryCount else "..."} 条 · 待办 ${if (todoCount >= 0) todoCount else "..."} 条",
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==================== 调试设置 ====================
        DevSection(title = "调试设置") {
            DevToggleItem(
                icon = Icons.Default.Build,
                iconTint = Primary,
                iconBackground = PrimaryLight,
                title = "调试模式",
                subtitle = "启用后输出详细的调试日志",
                checked = debugMode,
                onCheckedChange = { checked ->
                    debugMode = checked
                    prefs.edit().putBoolean("debug_mode", checked).apply()
                    Log.d(TAG, "Debug mode: $checked")
                },
            )
            DevDivider()
            DevToggleItem(
                icon = Icons.Default.PowerSettingsNew,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "保持屏幕唤醒",
                subtitle = "屏幕常亮，不会自动休眠",
                checked = keepScreenOn,
                onCheckedChange = { checked ->
                    keepScreenOn = checked
                    prefs.edit().putBoolean("keep_screen_on", checked).apply()
                    onKeepScreenOnChange(checked)
                },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==================== 网络配置 ====================
        DevSection(title = "网络配置") {
            DevInputItem(
                icon = Icons.Default.Language,
                iconTint = Info,
                iconBackground = InfoLight,
                title = "API 服务器地址",
                subtitle = "修改后端 API 地址",
                value = apiUrl,
                placeholder = "https://api.example.com",
                onValueChange = { newUrl ->
                    apiUrl = newUrl
                    prefs.edit().putString("api_url", newUrl).apply()
                },
            )
            DevDivider()
            DevInputItem(
                icon = Icons.Default.Wifi,
                iconTint = Color(0xFF9B72CF),
                iconBackground = Color(0xFFF3EEF8),
                title = "信令服务器地址",
                subtitle = "WebRTC 信令 WebSocket 地址",
                value = signalUrl,
                placeholder = "wss://signal.example.com",
                onValueChange = { newUrl ->
                    signalUrl = newUrl
                    prefs.edit().putString("signal_url", newUrl).apply()
                },
            )
            DevDivider()
            DevSelectItem(
                icon = Icons.Default.Storage,
                iconTint = Color(0xFFEF4444),
                iconBackground = Color(0xFFFEF2F2),
                title = "网络日志级别",
                subtitle = "当前: $logLevel",
                selectedText = logLevel,
                expanded = logLevelExpanded,
                options = logLevels,
                onExpandedChange = { logLevelExpanded = it },
                onOptionSelected = { level ->
                    logLevel = level
                    logLevelExpanded = false
                    prefs.edit().putString("log_level", level).apply()
                },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==================== 推流协议 ====================
        DevSection(title = "推流协议") {
            DevSelectItem(
                icon = Icons.Default.Wifi,
                iconTint = Info,
                iconBackground = InfoLight,
                title = "协议",
                subtitle = "WEBSOCKET 走自研信令；WHIP 推流 / WHEP 观看可直连 SRS、MediaMTX 等",
                selectedText = streamProtocol,
                expanded = streamProtocolExpanded,
                options = streamProtocolNames,
                onExpandedChange = { streamProtocolExpanded = it },
                onOptionSelected = { v ->
                    streamProtocol = v
                    streamProtocolExpanded = false
                    prefs.edit().putString("stream_protocol", v).apply()
                },
            )
            DevDivider()
            DevInputItem(
                icon = Icons.Default.Language,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "WHIP 推流地址",
                subtitle = "推流 endpoint，可用 {room} 作为占位符",
                value = whipEndpoint,
                placeholder = "http://host:1985/rtc/v1/whip/?app=live&stream={room}",
                onValueChange = { v ->
                    whipEndpoint = v
                    prefs.edit().putString("whip_endpoint", v).apply()
                },
            )
            DevDivider()
            DevInputItem(
                icon = Icons.Default.Language,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "WHEP 观看地址",
                subtitle = "观看 endpoint，可用 {room} 作为占位符",
                value = whepEndpoint,
                placeholder = "http://host:1985/rtc/v1/whep/?app=live&stream={room}",
                onValueChange = { v ->
                    whepEndpoint = v
                    prefs.edit().putString("whep_endpoint", v).apply()
                },
            )
            DevDivider()
            DevInputItem(
                icon = Icons.Default.Info,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "Bearer Token（可选）",
                subtitle = "WHIP / WHEP 请求的 Authorization",
                value = streamToken,
                placeholder = "留空则不带 Authorization",
                onValueChange = { v ->
                    streamToken = v
                    prefs.edit().putString("stream_token", v).apply()
                },
                isPassword = true,
            )
            DevDivider()
            DevInputItem(
                icon = Icons.Default.Language,
                iconTint = Color(0xFF10B981),
                iconBackground = Color(0xFFD1FAE5),
                title = "MJPEG 图传地址",
                subtitle = "摄像头 MJPEG 流地址，接上即可显示（对齐 Web 端）",
                value = mjpegUrl,
                placeholder = "http://192.168.1.100:8787/x4.mjpeg",
                onValueChange = { v ->
                    mjpegUrl = v
                    prefs.edit().putString("mjpeg_url", v).apply()
                },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==================== 大模型配置 ====================
        DevSection(title = "大模型（LLM）配置") {
            DevSelectItem(
                icon = Icons.Default.Psychology,
                iconTint = Primary,
                iconBackground = PrimaryLight,
                title = "Provider",
                subtitle = "支持 OpenAI 兼容 / Claude / Gemini / 通义 / 本地 Ollama",
                selectedText = llmProvider,
                expanded = llmProviderExpanded,
                options = llmProviderNames,
                onExpandedChange = { llmProviderExpanded = it },
                onOptionSelected = { v ->
                    llmProvider = v
                    llmProviderExpanded = false
                    prefs.edit().putString("llm_provider", v).apply()
                },
            )
            DevDivider()
            DevInputItem(
                icon = Icons.Default.Info,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "API Key",
                subtitle = "Ollama 可留空",
                value = llmApiKey,
                placeholder = "sk-...",
                onValueChange = { v ->
                    llmApiKey = v
                    prefs.edit().putString("llm_api_key", v).apply()
                },
                isPassword = true,
            )
            DevDivider()
            DevInputItem(
                icon = Icons.Default.Language,
                iconTint = Info,
                iconBackground = InfoLight,
                title = "Base URL（可选）",
                subtitle = "留空使用所选 Provider 的默认值",
                value = llmBaseUrl,
                placeholder = "https://api.openai.com/",
                onValueChange = { v ->
                    llmBaseUrl = v
                    prefs.edit().putString("llm_base_url", v).apply()
                },
            )
            DevDivider()
            DevInputItem(
                icon = Icons.Default.Info,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "模型（可选）",
                subtitle = "留空使用 Provider 默认模型",
                value = llmModel,
                placeholder = "gpt-4o-mini / claude-3-5-sonnet / gemini-2.0-flash / llava ...",
                onValueChange = { v ->
                    llmModel = v
                    prefs.edit().putString("llm_model", v).apply()
                },
            )
            DevDivider()
            DevSliderItem(
                icon = Icons.Default.Tune,
                iconTint = Color(0xFF8B5CF6),
                iconBackground = Color(0xFFF5F3FF),
                title = "Max Tokens",
                subtitle = "最大生成长度: ${llmMaxTokens.toInt()}",
                value = llmMaxTokens,
                valueRange = 64f..4096f,
                steps = 0,
                onValueChange = { v ->
                    llmMaxTokens = v
                },
                onValueChangeFinished = {
                    prefs.edit().putInt("llm_max_tokens", llmMaxTokens.toInt()).apply()
                },
            )
            DevDivider()
            DevSliderItem(
                icon = Icons.Default.Speed,
                iconTint = Color(0xFFF59E0B),
                iconBackground = Color(0xFFFEF3C7),
                title = "Temperature",
                subtitle = "创造性: %.2f（0=确定性, 1=创造性）".format(llmTemperature),
                value = llmTemperature,
                valueRange = 0f..1f,
                steps = 0,
                onValueChange = { v ->
                    llmTemperature = v
                },
                onValueChangeFinished = {
                    prefs.edit().putFloat("llm_temperature", llmTemperature).apply()
                },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==================== LLM 连接测试 ====================
        DevSection(title = "LLM 连接测试") {
            DevClickItem(
                icon = Icons.Default.NetworkCheck,
                iconTint = when (llmTestState) {
                    is LlmTestState.Success -> Color(0xFF10B981)
                    is LlmTestState.Failed -> Color(0xFFEF4444)
                    is LlmTestState.Testing -> Info
                    else -> Primary
                },
                iconBackground = when (llmTestState) {
                    is LlmTestState.Success -> Color(0xFFD1FAE5)
                    is LlmTestState.Failed -> Color(0xFFFEF2F2)
                    is LlmTestState.Testing -> InfoLight
                    else -> PrimaryLight
                },
                title = "测试 LLM 连接",
                subtitle = when (llmTestState) {
                    LlmTestState.Idle -> "点击发送测试请求验证 API 连通性"
                    LlmTestState.Testing -> "正在测试..."
                    is LlmTestState.Success -> "✓ 成功 (${(llmTestState as LlmTestState.Success).latencyMs}ms)"
                    is LlmTestState.Failed -> "✗ ${(llmTestState as LlmTestState.Failed).error}"
                },
                onClick = {
                    if (llmTestState != LlmTestState.Testing) {
                        llmTestState = LlmTestState.Testing
                        scope.launch {
                            llmTestState = testLlmConnection(context)
                        }
                    }
                },
                trailing = {
                    when (llmTestState) {
                        LlmTestState.Testing -> CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Primary,
                        )
                        is LlmTestState.Success -> Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp),
                        )
                        is LlmTestState.Failed -> Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp),
                        )
                        else -> {}
                    }
                },
            )
            // 测试结果详情
            AnimatedVisibility(
                visible = llmTestState is LlmTestState.Success,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                if (llmTestState is LlmTestState.Success) {
                    val result = (llmTestState as LlmTestState.Success).response
                    Text(
                        text = "响应: ${result.take(200)}${if (result.length > 200) "..." else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 68.dp, end = 16.dp, bottom = 12.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==================== Rokid 配置 ====================
        DevSection(title = "Rokid 配置") {
            DevToggleItem(
                icon = Icons.Default.PhoneAndroid,
                iconTint = Color(0xFFF59E0B),
                iconBackground = Color(0xFFFEF3C7),
                title = "假装已连接",
                subtitle = "跳过蓝牙连接，模拟眼镜已连接状态",
                checked = fakeConnected,
                onCheckedChange = { checked ->
                    fakeConnected = checked
                    prefs.edit().putBoolean("fake_connected", checked).apply()
                },
            )
            DevDivider()
            DevInputItem(
                icon = Icons.Default.Info,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "Client ID",
                subtitle = "Rokid 开发者 Client ID",
                value = rokidClientId,
                placeholder = "输入 Client ID",
                onValueChange = { newValue ->
                    rokidClientId = newValue
                    prefs.edit().putString("rokid_client_id", newValue).apply()
                },
            )
            DevDivider()
            DevInputItem(
                icon = Icons.Default.Info,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "Client Secret",
                subtitle = "Rokid 开发者 Client Secret",
                value = rokidClientSecret,
                placeholder = "输入 Client Secret",
                onValueChange = { newValue ->
                    rokidClientSecret = newValue
                    prefs.edit().putString("rokid_client_secret", newValue).apply()
                },
                isPassword = true,
            )
            DevDivider()
            DevInputItem(
                icon = Icons.Default.Info,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "Access Key",
                subtitle = "Rokid 开发者 Access Key",
                value = rokidAccessKey,
                placeholder = "输入 Access Key",
                onValueChange = { newValue ->
                    rokidAccessKey = newValue
                    prefs.edit().putString("rokid_access_key", newValue).apply()
                },
                isPassword = true,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==================== 数据管理 ====================
        DevSection(title = "数据管理") {
            DevClickItem(
                icon = Icons.Default.Info,
                iconTint = Info,
                iconBackground = InfoLight,
                title = "查看应用详情",
                subtitle = "打开系统设置中的应用信息",
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                },
            )
            DevDivider()
            DevClickItem(
                icon = Icons.Default.Memory,
                iconTint = Color(0xFF8B5CF6),
                iconBackground = Color(0xFFF5F3FF),
                title = "清除 LLM 记忆",
                subtitle = "删除所有长期记忆 (${if (memoryCount >= 0) "${memoryCount} 条" else "加载中..."})",
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                val db = AppDatabase.getDatabase(context)
                                db.memoryDao().clearAll()
                                memoryCount = 0
                                Log.d(TAG, "LLM memories cleared")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to clear memories", e)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "LLM 记忆已清除", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
            )
            DevDivider()
            DevClickItem(
                icon = Icons.Default.CleaningServices,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "导出数据库到日志",
                subtitle = "将数据库内容输出到 Logcat",
                onClick = {
                    dumpDatabaseToLogcat(context)
                },
            )
            DevDivider()
            DevClickItem(
                icon = Icons.Default.Delete,
                iconTint = Color(0xFFEF4444),
                iconBackground = Color(0xFFFEF2F2),
                title = "清除本地数据库",
                subtitle = "删除所有用药提醒数据",
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { clearDatabase(context) }
                        todoCount = 0
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "数据库已清除", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==================== 导出 / 导入 / 重置 ====================
        DevSection(title = "配置管理") {
            DevClickItem(
                icon = Icons.Default.ContentCopy,
                iconTint = Info,
                iconBackground = InfoLight,
                title = "导出配置到剪贴板",
                subtitle = "将所有开发者选项导出为 JSON",
                onClick = {
                    exportConfigToClipboard(context)
                    Toast.makeText(context, "配置已复制到剪贴板", Toast.LENGTH_SHORT).show()
                },
            )
            DevDivider()
            DevClickItem(
                icon = Icons.Default.ContentPaste,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "从剪贴板导入配置",
                subtitle = "从剪贴板读取 JSON 并覆盖当前配置",
                onClick = {
                    val imported = importConfigFromClipboard(context)
                    if (imported) {
                        Toast.makeText(context, "配置已导入，请返回重新进入页面", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "导入失败：剪贴板中无有效 JSON", Toast.LENGTH_SHORT).show()
                    }
                },
            )
            DevDivider()
            DevClickItem(
                icon = Icons.Default.RestartAlt,
                iconTint = Color(0xFFEF4444),
                iconBackground = Color(0xFFFEF2F2),
                title = "重置所有配置",
                subtitle = "恢复所有开发者选项为默认值",
                onClick = { showResetDialog = true },
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "注意：修改部分配置后需要重启应用才能生效",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // 重置确认弹窗
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("确认重置") },
            text = { Text("将清除所有开发者选项配置，恢复为默认值。此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        prefs.edit().clear().apply()
                        showResetDialog = false
                        Toast.makeText(context, "已重置，请返回重新进入", Toast.LENGTH_LONG).show()
                    },
                ) {
                    Text("确认重置", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}

// ==================== LLM 测试状态 ====================

private sealed class LlmTestState {
    data object Idle : LlmTestState()
    data object Testing : LlmTestState()
    data class Success(val response: String, val latencyMs: Long) : LlmTestState()
    data class Failed(val error: String) : LlmTestState()
}

private suspend fun testLlmConnection(context: Context): LlmTestState {
    return withContext(Dispatchers.IO) {
        try {
            val config = LlmConfigStore.read(context)
            val client = LlmClientFactory.create(config)
            val start = System.currentTimeMillis()
            val response = client.chat("Reply with exactly: 'Connection OK'. Nothing else.")
            val latency = System.currentTimeMillis() - start
            LlmTestState.Success(response, latency)
        } catch (e: Exception) {
            Log.e(TAG, "LLM test failed", e)
            LlmTestState.Failed(e.message?.take(100) ?: "未知错误")
        }
    }
}

// ==================== 工具函数 ====================

private fun getAppVersion(context: Context): String {
    return try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        "${pInfo.versionName} (${pInfo.longVersionCode})"
    } catch (e: Exception) {
        "未知"
    }
}

private fun getAvailableMemory(context: Context): String {
    val runtime = Runtime.getRuntime()
    val maxMem = runtime.maxMemory() / 1024 / 1024
    val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    return "${usedMem}MB / ${maxMem}MB"
}

private fun exportConfigToClipboard(context: Context) {
    val prefs = context.getSharedPreferences("dev_options", Context.MODE_PRIVATE)
    val json = JSONObject()
    prefs.all.forEach { (key, value) ->
        when (value) {
            is String -> json.put(key, value)
            is Boolean -> json.put(key, value)
            is Int -> json.put(key, value)
            is Float -> json.put(key, value.toDouble())
            is Long -> json.put(key, value)
        }
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("dev_options", json.toString(2)))
}

private fun importConfigFromClipboard(context: Context): Boolean {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = clipboard.primaryClip ?: return false
    if (clip.itemCount == 0) return false
    val text = clip.getItemAt(0).text?.toString() ?: return false
    return try {
        val json = JSONObject(text)
        val prefs = context.getSharedPreferences("dev_options", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        json.keys().forEach { key ->
            when (val v = json.get(key)) {
                is String -> editor.putString(key, v)
                is Boolean -> editor.putBoolean(key, v)
                is Int -> editor.putInt(key, v)
                is Long -> editor.putLong(key, v)
                is Double -> editor.putFloat(key, v.toFloat())
            }
        }
        editor.apply()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Import config failed", e)
        false
    }
}

// ==================== UI 组件 ====================

@Composable
private fun DevSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(horizontal = 20.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = ShadowColor),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun DevInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun DevToggleItem(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

@Composable
private fun DevInputItem(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 52.dp),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = DividerColor,
                cursorColor = Primary,
            ),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        )
    }
}

@Composable
private fun DevSelectItem(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    selectedText: String,
    expanded: Boolean,
    options: List<String>,
    onExpandedChange: (Boolean) -> Unit,
    onOptionSelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onExpandedChange(!expanded) },
                    shape = RoundedCornerShape(10.dp),
                    color = PrimaryLight,
                ) {
                    Text(
                        text = selectedText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    fontWeight = if (option == selectedText) FontWeight.Bold else FontWeight.Normal,
                                    color = if (option == selectedText) Primary else MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = { onOptionSelected(option) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DevSliderItem(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 52.dp),
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
                inactiveTrackColor = DividerColor,
            ),
        )
    }
}

@Composable
private fun DevClickItem(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun DevDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 68.dp)
            .background(DividerColor.copy(alpha = 0.6f)),
    )
}

// ==================== 数据操作 ====================

private suspend fun clearDatabase(context: Context) {
    try {
        val db = AppDatabase.getDatabase(context)
        val dao = db.todoDao()
        val todos = dao.getAllTodosOnce()
        todos.forEach { dao.delete(it) }
        Log.d(TAG, "Database cleared: ${todos.size} todos deleted")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to clear database", e)
    }
}

private fun dumpDatabaseToLogcat(context: Context) {
    try {
        val db = AppDatabase.getDatabase(context)
        val dao = db.todoDao()
        CoroutineScope(Dispatchers.IO).launch {
            val todos = dao.getAllTodosOnce()
            Log.d(TAG, "========== Database Dump ==========")
            Log.d(TAG, "Total todos: ${todos.size}")
            todos.forEach { todo ->
                Log.d(TAG, "  [${todo.id}] ${todo.personId} | ${todo.title} | ${todo.hour}:${todo.minute.toString().padStart(2, '0')} | done=${todo.isDone} | enabled=${todo.enabled}")
            }
            Log.d(TAG, "===================================")

            // 同时导出 memories
            val memoryDao = db.memoryDao()
            val memories = memoryDao.getAll()
            Log.d(TAG, "========== Memory Dump ==========")
            Log.d(TAG, "Total memories: ${memories.size}")
            memories.forEach { m ->
                Log.d(TAG, "  [${m.id}] tag=${m.tag} | importance=${m.importance} | ${m.content.take(80)}")
            }
            Log.d(TAG, "=================================")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to dump database", e)
    }
}
