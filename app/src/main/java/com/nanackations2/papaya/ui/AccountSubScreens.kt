package com.nanackations2.papaya.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanackations2.papaya.ui.theme.DividerColor
import com.nanackations2.papaya.ui.theme.Info
import com.nanackations2.papaya.ui.theme.InfoLight
import com.nanackations2.papaya.ui.theme.Primary
import com.nanackations2.papaya.ui.theme.PrimaryDark
import com.nanackations2.papaya.ui.theme.PrimaryLight
import com.nanackations2.papaya.ui.theme.Secondary
import com.nanackations2.papaya.ui.theme.SecondaryLight
import com.nanackations2.papaya.ui.theme.ShadowColor
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.nanackations2.papaya.R

// region ---- Navigation ----

enum class AccountSubScreen {
    None,
    ProfileInfo,
    NotificationSettings,
    PrivacySecurity,
    DeviceManagement,
    EmergencyContacts,
    AboutUs,
    DeveloperOptions,
    RokidDemo,
}

// endregion

// region ---- 个人资料 ----

@Composable
fun ProfileInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("user_profile", Context.MODE_PRIVATE) }
    var nickname by remember { mutableStateOf(prefs.getString("nickname", "小糯用户") ?: "小糯用户") }
    var email by remember { mutableStateOf(prefs.getString("email", "xiaonuo@example.com") ?: "xiaonuo@example.com") }
    var phone by remember { mutableStateOf(prefs.getString("phone", "138****8888") ?: "138****8888") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editField by remember { mutableStateOf("") }
    var editValue by remember { mutableStateOf("") }

    BackHandler(onBack = onBack)

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = when (editField) {
                        "nickname" -> "修改昵称"
                        "email" -> "修改邮箱"
                        "phone" -> "修改手机号"
                        else -> "编辑"
                    },
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = when (editField) {
                                "nickname" -> "请输入昵称"
                                "email" -> "请输入邮箱"
                                "phone" -> "请输入手机号"
                                else -> ""
                            }
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = DividerColor,
                        cursorColor = Primary,
                    ),
                    shape = RoundedCornerShape(14.dp),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editValue.isNotBlank()) {
                            when (editField) {
                                "nickname" -> { nickname = editValue; prefs.edit().putString("nickname", editValue).apply() }
                                "email" -> { email = editValue; prefs.edit().putString("email", editValue).apply() }
                                "phone" -> { phone = editValue; prefs.edit().putString("phone", editValue).apply() }
                            }
                            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                        }
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("保存", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }

    AccountSubScreenTemplate(
        title = "个人资料",
        subtitle = "编辑姓名、头像等信息",
        onBack = onBack,
    ) {
        SubSection {
            SubClickItem(
                icon = Icons.Default.Person,
                iconTint = Primary,
                iconBackground = PrimaryLight,
                title = "昵称",
                subtitle = nickname,
                onClick = {
                    editField = "nickname"
                    editValue = nickname
                    showEditDialog = true
                },
            )
            SubDivider()
            SubClickItem(
                icon = Icons.Default.Email,
                iconTint = Info,
                iconBackground = InfoLight,
                title = "邮箱",
                subtitle = email,
                onClick = {
                    editField = "email"
                    editValue = email
                    showEditDialog = true
                },
            )
            SubDivider()
            SubClickItem(
                icon = Icons.Default.Phone,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "手机号",
                subtitle = phone,
                onClick = {
                    editField = "phone"
                    editValue = phone
                    showEditDialog = true
                },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SubSection {
            SubClickItem(
                icon = Icons.Default.Edit,
                iconTint = Color(0xFF9B72CF),
                iconBackground = Color(0xFFF3EEF8),
                title = "修改头像",
                onClick = {
                    Toast.makeText(context, "暂不支持修改头像，敬请期待", Toast.LENGTH_SHORT).show()
                },
            )
            SubDivider()
            SubClickItem(
                icon = Icons.Default.CheckCircle,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "实名认证",
                subtitle = "已完成认证",
                onClick = {
                    Toast.makeText(context, "您已完成实名认证", Toast.LENGTH_SHORT).show()
                },
            )
        }
    }
}

// endregion

// region ---- 通知设置 ----

@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE) }
    var medicationReminder by remember { mutableStateOf(prefs.getBoolean("medication_reminder", true)) }
    var monitorAlert by remember { mutableStateOf(prefs.getBoolean("monitor_alert", true)) }
    var systemNotice by remember { mutableStateOf(prefs.getBoolean("system_notice", true)) }
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound_enabled", true)) }
    var vibrationEnabled by remember { mutableStateOf(prefs.getBoolean("vibration_enabled", true)) }

    BackHandler(onBack = onBack)

    AccountSubScreenTemplate(
        title = "通知设置",
        subtitle = "待办提醒、监控报警等",
        onBack = onBack,
    ) {
        SubSection(title = "通知类型") {
            SubToggleItem(
                icon = Icons.Default.Notifications,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "待办提醒",
                subtitle = "到时间提醒待办事项",
                checked = medicationReminder,
                onCheckedChange = { medicationReminder = it; prefs.edit().putBoolean("medication_reminder", it).apply() },
            )
            SubDivider()
            SubToggleItem(
                icon = Icons.Default.Videocam,
                iconTint = Color(0xFF9B72CF),
                iconBackground = Color(0xFFF3EEF8),
                title = "监控报警",
                subtitle = "设备异常或检测到移动",
                checked = monitorAlert,
                onCheckedChange = { monitorAlert = it; prefs.edit().putBoolean("monitor_alert", it).apply() },
            )
            SubDivider()
            SubToggleItem(
                icon = Icons.Default.Info,
                iconTint = Info,
                iconBackground = InfoLight,
                title = "系统通知",
                subtitle = "版本更新、系统公告等",
                checked = systemNotice,
                onCheckedChange = { systemNotice = it; prefs.edit().putBoolean("system_notice", it).apply() },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SubSection(title = "提醒方式") {
            SubToggleItem(
                icon = Icons.Default.Notifications,
                iconTint = Primary,
                iconBackground = PrimaryLight,
                title = "声音",
                subtitle = "通知时播放提示音",
                checked = soundEnabled,
                onCheckedChange = { soundEnabled = it; prefs.edit().putBoolean("sound_enabled", it).apply() },
            )
            SubDivider()
            SubToggleItem(
                icon = Icons.Default.NotificationsOff,
                iconTint = Color(0xFFEF4444),
                iconBackground = Color(0xFFFEF2F2),
                title = "震动",
                subtitle = "通知时震动手机",
                checked = vibrationEnabled,
                onCheckedChange = { vibrationEnabled = it; prefs.edit().putBoolean("vibration_enabled", it).apply() },
            )
        }
    }
}

// endregion

// region ---- 隐私与安全 ----

@Composable
fun PrivacySecurityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("privacy_settings", Context.MODE_PRIVATE) }
    var biometricEnabled by remember { mutableStateOf(prefs.getBoolean("biometric_enabled", false)) }
    var dataEncryption by remember { mutableStateOf(prefs.getBoolean("data_encryption", true)) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    BackHandler(onBack = onBack)

    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("修改密码", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        singleLine = true,
                        placeholder = { Text("请输入旧密码") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = Primary,
                        ),
                        shape = RoundedCornerShape(14.dp),
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        singleLine = true,
                        placeholder = { Text("请输入新密码") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = Primary,
                        ),
                        shape = RoundedCornerShape(14.dp),
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        singleLine = true,
                        placeholder = { Text("请确认新密码") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = Primary,
                        ),
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.isNotBlank() && newPassword == confirmPassword) {
                            Toast.makeText(context, "密码修改成功", Toast.LENGTH_SHORT).show()
                        } else if (newPassword != confirmPassword) {
                            Toast.makeText(context, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        oldPassword = ""; newPassword = ""; confirmPassword = ""
                        showChangePasswordDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("确认修改", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { oldPassword = ""; newPassword = ""; confirmPassword = ""; showChangePasswordDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }

    AccountSubScreenTemplate(
        title = "隐私与安全",
        subtitle = "密码、数据加密等",
        onBack = onBack,
    ) {
        SubSection(title = "账户安全") {
            SubClickItem(
                icon = Icons.Default.Lock,
                iconTint = Info,
                iconBackground = InfoLight,
                title = "修改密码",
                subtitle = "建议定期更换密码",
                onClick = { showChangePasswordDialog = true },
            )
            SubDivider()
            SubToggleItem(
                icon = Icons.Default.Fingerprint,
                iconTint = Color(0xFF9B72CF),
                iconBackground = Color(0xFFF3EEF8),
                title = "生物识别",
                subtitle = "使用指纹或面部识别登录",
                checked = biometricEnabled,
                onCheckedChange = { biometricEnabled = it; prefs.edit().putBoolean("biometric_enabled", it).apply() },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SubSection(title = "数据隐私") {
            SubToggleItem(
                icon = Icons.Default.Security,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "数据加密",
                subtitle = "本地数据加密存储",
                checked = dataEncryption,
                onCheckedChange = { dataEncryption = it; prefs.edit().putBoolean("data_encryption", it).apply() },
            )
            SubDivider()
            SubClickItem(
                icon = Icons.Default.Visibility,
                iconTint = Primary,
                iconBackground = PrimaryLight,
                title = "隐私政策",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/emmettlu/xiaonuotuan"))
                    context.startActivity(intent)
                },
            )
            SubDivider()
            SubClickItem(
                icon = Icons.Default.Description,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                title = "用户协议",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/emmettlu/xiaonuotuan"))
                    context.startActivity(intent)
                },
            )
        }
    }
}

// endregion

// region ---- 监控设备管理 ----

@Composable
fun DeviceManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var showDeviceDetailDialog by remember { mutableStateOf<String?>(null) }

    BackHandler(onBack = onBack)

    if (showDeviceDetailDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeviceDetailDialog = null },
            title = { Text(showDeviceDetailDialog ?: "", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text("设备状态：在线", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("IP 地址：192.168.1.${(10..99).random()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("固件版本：v2.1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeviceDetailDialog = null }) {
                    Text("关闭", color = Primary)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }

    AccountSubScreenTemplate(
        title = "监控设备管理",
        subtitle = "添加、删除监控设备",
        onBack = onBack,
    ) {
        SubSection(title = "已绑定设备") {
            SubClickItem(
                icon = Icons.Default.CameraAlt,
                iconTint = Info,
                iconBackground = InfoLight,
                title = "客厅摄像头",
                subtitle = "在线 · 最后上线: 刚刚",
                onClick = { showDeviceDetailDialog = "客厅摄像头" },
            )
            SubDivider()
            SubClickItem(
                icon = Icons.Default.Videocam,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "门口摄像头",
                subtitle = "在线 · 最后上线: 5分钟前",
                onClick = { showDeviceDetailDialog = "门口摄像头" },
            )
            SubDivider()
            SubClickItem(
                icon = Icons.Default.Sensors,
                iconTint = Color(0xFF9B72CF),
                iconBackground = Color(0xFFF3EEF8),
                title = "门窗传感器",
                subtitle = "在线 · 电池电量 80%",
                onClick = { showDeviceDetailDialog = "门窗传感器" },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SubSection {
            SubClickItem(
                icon = Icons.Default.Add,
                iconTint = Primary,
                iconBackground = PrimaryLight,
                title = "添加新设备",
                subtitle = "扫码或手动添加设备",
                onClick = {
                    Toast.makeText(context, "请确保新设备已开启并处于配对模式", Toast.LENGTH_SHORT).show()
                },
            )
        }
    }
}

// endregion

// region ---- 紧急联系人 ----

private data class EmergencyContact(
    val name: String,
    val phone: String,
    val relation: String,
    val iconTint: Color,
    val iconBackground: Color,
)

@Composable
fun EmergencyContactsScreen(onBack: () -> Unit) {
    var contacts by remember {
        mutableStateOf(
            listOf(
                EmergencyContact("李明", "138****0001", "父亲", Color(0xFFEF4444), Color(0xFFFEF2F2)),
                EmergencyContact("张华", "139****0002", "母亲", Info, InfoLight),
                EmergencyContact("王芳", "137****0003", "姐姐", Secondary, SecondaryLight),
            )
        )
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(-1) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newRelation by remember { mutableStateOf("") }

    val iconColors = listOf(
        Color(0xFFEF4444) to Color(0xFFFEF2F2),
        Info to InfoLight,
        Secondary to SecondaryLight,
        Primary to PrimaryLight,
        Color(0xFF9B72CF) to Color(0xFFF3EEF8),
    )

    BackHandler(onBack = onBack)

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text("添加联系人", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        placeholder = { Text("请输入姓名") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = Primary,
                        ),
                        shape = RoundedCornerShape(14.dp),
                    )
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        singleLine = true,
                        placeholder = { Text("请输入手机号") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = Primary,
                        ),
                        shape = RoundedCornerShape(14.dp),
                    )
                    OutlinedTextField(
                        value = newRelation,
                        onValueChange = { newRelation = it },
                        singleLine = true,
                        placeholder = { Text("请输入关系（如：父亲、母亲）") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = Primary,
                        ),
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newPhone.isNotBlank() && newRelation.isNotBlank()) {
                            val colorPair = iconColors[contacts.size % iconColors.size]
                            contacts = contacts + EmergencyContact(
                                name = newName, phone = newPhone, relation = newRelation,
                                iconTint = colorPair.first, iconBackground = colorPair.second,
                            )
                        }
                        newName = ""; newPhone = ""; newRelation = ""; showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("添加", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { newName = ""; newPhone = ""; newRelation = ""; showAddDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }

    if (showDeleteDialog >= 0 && showDeleteDialog < contacts.size) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = -1 },
            title = { Text("删除联系人", style = MaterialTheme.typography.titleLarge) },
            text = { Text("确定要删除联系人「${contacts[showDeleteDialog].name}」吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        contacts = contacts.toMutableList().apply { removeAt(showDeleteDialog) }
                        showDeleteDialog = -1
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("删除", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = -1 }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }

    AccountSubScreenTemplate(
        title = "紧急联系人",
        subtitle = "设置紧急联系人信息",
        onBack = onBack,
    ) {
        SubSection(title = "联系人列表") {
            contacts.forEachIndexed { index, contact ->
                if (index > 0) SubDivider()
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
                            .background(contact.iconBackground),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = contact.iconTint,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${contact.phone} · ${contact.relation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = index }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SubSection {
            SubClickItem(
                icon = Icons.Default.Add,
                iconTint = Primary,
                iconBackground = PrimaryLight,
                title = "添加联系人",
                onClick = { showAddDialog = true },
            )
        }
    }
}

// endregion

// region ---- 关于我们 ----

@Composable
fun AboutUsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var showChangelogDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    if (showChangelogDialog) {
        AlertDialog(
            onDismissRequest = { showChangelogDialog = false },
            title = { Text("更新日志", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text("v1.0.0 (2025-05)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• 首次发布", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• 支持待办管理与提醒", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• 支持实时监控（WebRTC）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• 支持 Rokid AR 眼镜连接", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• 紧急联系人管理", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showChangelogDialog = false }) {
                    Text("关闭", color = Primary)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }

    AccountSubScreenTemplate(
        title = "关于我们",
        subtitle = "版本 v1.0.0",
        onBack = onBack,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Primary, PrimaryDark)
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "糯",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "小糯智能家庭中心",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "版本 v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        SubSection {
            SubClickItem(
                icon = Icons.Default.Star,
                iconTint = Primary,
                iconBackground = PrimaryLight,
                title = "给我们评分",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/emmettlu/xiaonuotuan"))
                    context.startActivity(intent)
                },
            )
            SubDivider()
            SubClickItem(
                icon = Icons.Default.Info,
                iconTint = Info,
                iconBackground = InfoLight,
                title = "开源许可",
                subtitle = "MIT License",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/emmettlu/xiaonuotuan/blob/main/LICENSE"))
                    context.startActivity(intent)
                },
            )
            SubDivider()
            SubClickItem(
                icon = Icons.Default.Description,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "更新日志",
                subtitle = "查看版本更新内容",
                onClick = { showChangelogDialog = true },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SubSection(title = "微信群") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Papaya 用户交流群",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "扫码进群，交流 Rokid / X4 / App 调试进展",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                )
                Image(
                    painter = painterResource(id = R.drawable.wechat_group),
                    contentDescription = "Papaya 微信群二维码",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(930f / 1446f)
                        .clip(RoundedCornerShape(18.dp)),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Copyright © 2025 小糯团队\n保留所有权利",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// endregion

// region ---- 通用子页面组件 ----

@Composable
private fun AccountSubScreenTemplate(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
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
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )

        content()
    }
}

@Composable
private fun SubSection(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(horizontal = 20.dp)) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
            )
        }
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
private fun SubClickItem(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {},
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
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun SubToggleItem(
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
private fun SubDivider() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 68.dp),
        thickness = 1.dp,
        color = DividerColor.copy(alpha = 0.6f),
    )
}

// endregion
