package com.nanackations2.papaya.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nanackations2.papaya.ui.viewmodel.TodoViewModel

@Composable
fun ProfileHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Primary, PrimaryDark)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "糯",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "小糯用户",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SecondaryLight,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Secondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "已认证",
                    fontSize = 12.sp,
                    color = Secondary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun StatsRow(modifier: Modifier = Modifier, todoViewModel: TodoViewModel = viewModel()) {
    val todos by todoViewModel.todos.collectAsState()
    val pendingCount = todos.count { !it.isDone && it.enabled }
    val monitorDeviceCount = 3 // 客厅摄像头、门口摄像头、门窗传感器
    val familyMemberCount = personOptions.size

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = ShadowColor),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem(icon = "📋", value = "$pendingCount", label = "待办计划", modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier.width(1.dp).height(40.dp).background(DividerColor),
            )
            StatItem(icon = "👁", value = "$monitorDeviceCount", label = "监控设备", modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier.width(1.dp).height(40.dp).background(DividerColor),
            )
            StatItem(icon = "👨‍👩‍👧‍👦", value = "$familyMemberCount", label = "家庭成员", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StatItem(
    icon: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = icon, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun MenuSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
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
fun MenuItem(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    showArrow: Boolean = true,
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
            contentAlignment = Alignment.Center
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
                )
            }
        }

        if (showArrow) {
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
fun AccountScreen(
    modifier: Modifier = Modifier,
    currentSubScreen: AccountSubScreen = AccountSubScreen.None,
    onSubScreenChange: (AccountSubScreen) -> Unit = {},
    onLogout: () -> Unit = {},
    onKeepScreenOnChange: (Boolean) -> Unit = {},
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录", style = MaterialTheme.typography.titleLarge, color = Color(0xFFEF4444)) },
            text = { Text("确定要退出登录吗？退出后将清除本地登录状态。") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    // 清除本地用户数据
                    context.getSharedPreferences("user_profile", Context.MODE_PRIVATE).edit().clear().apply()
                    Toast.makeText(context, "已退出登录", Toast.LENGTH_SHORT).show()
                    onLogout()
                    // 关闭 Activity
                    (context as? ComponentActivity)?.finish()
                }) {
                    Text("确认退出", color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }

    if (currentSubScreen != AccountSubScreen.None) {
        when (currentSubScreen) {
            AccountSubScreen.ProfileInfo -> ProfileInfoScreen(onBack = { onSubScreenChange(AccountSubScreen.None) })
            AccountSubScreen.NotificationSettings -> NotificationSettingsScreen(onBack = { onSubScreenChange(AccountSubScreen.None) })
            AccountSubScreen.PrivacySecurity -> PrivacySecurityScreen(onBack = { onSubScreenChange(AccountSubScreen.None) })
            AccountSubScreen.DeviceManagement -> DeviceManagementScreen(onBack = { onSubScreenChange(AccountSubScreen.None) })
            AccountSubScreen.EmergencyContacts -> EmergencyContactsScreen(onBack = { onSubScreenChange(AccountSubScreen.None) })
            AccountSubScreen.AboutUs -> AboutUsScreen(onBack = { onSubScreenChange(AccountSubScreen.None) })
            AccountSubScreen.DeveloperOptions -> DeveloperOptionsScreen(
                onBack = { onSubScreenChange(AccountSubScreen.None) },
                onKeepScreenOnChange = onKeepScreenOnChange,
            )
            AccountSubScreen.RokidDemo -> RokidDemoScreen(onBack = { onSubScreenChange(AccountSubScreen.None) })
            AccountSubScreen.None -> {}
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "我的",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "管理个人信息和设置",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        ProfileHeader()

        Spacer(modifier = Modifier.height(16.dp))

        StatsRow()

        Spacer(modifier = Modifier.height(28.dp))

        MenuSection(title = "个人设置") {
            MenuItem(
                icon = Icons.Default.Person,
                iconTint = Primary,
                iconBackground = PrimaryLight,
                title = "个人资料",
                subtitle = "编辑姓名、头像等信息",
                onClick = { onSubScreenChange(AccountSubScreen.ProfileInfo) },
            )
            MenuDivider()
            MenuItem(
                icon = Icons.Default.Notifications,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "通知设置",
                subtitle = "待办提醒、监控报警等",
                onClick = { onSubScreenChange(AccountSubScreen.NotificationSettings) },
            )
            MenuDivider()
            MenuItem(
                icon = Icons.Default.Shield,
                iconTint = Info,
                iconBackground = InfoLight,
                title = "隐私与安全",
                subtitle = "密码、数据加密等",
                onClick = { onSubScreenChange(AccountSubScreen.PrivacySecurity) },
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        MenuSection(title = "设备与服务") {
            MenuItem(
                icon = Icons.Default.Settings,
                iconTint = Color(0xFF9B72CF),
                iconBackground = Color(0xFFF3EEF8),
                title = "监控设备管理",
                subtitle = "添加、删除监控设备",
                onClick = { onSubScreenChange(AccountSubScreen.DeviceManagement) },
            )
            MenuDivider()
            MenuItem(
                icon = Icons.Default.Devices,
                iconTint = Secondary,
                iconBackground = SecondaryLight,
                title = "Rokid AR 眼镜",
                subtitle = "连接和管理 Rokid 智能眼镜",
                onClick = { onSubScreenChange(AccountSubScreen.RokidDemo) },
            )
            MenuDivider()
            MenuItem(
                icon = Icons.Default.Favorite,
                iconTint = Color(0xFFEF4444),
                iconBackground = Color(0xFFFEF2F2),
                title = "紧急联系人",
                subtitle = "设置紧急联系人信息",
                onClick = { onSubScreenChange(AccountSubScreen.EmergencyContacts) },
            )
            MenuDivider()
            MenuItem(
                icon = Icons.Default.Build,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                title = "开发者选项",
                subtitle = "调试工具、服务器配置等",
                onClick = { onSubScreenChange(AccountSubScreen.DeveloperOptions) },
            )
            MenuDivider()
            MenuItem(
                icon = Icons.Default.Info,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                title = "关于我们",
                subtitle = "版本 v1.0.0",
                onClick = { onSubScreenChange(AccountSubScreen.AboutUs) },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(18.dp), spotColor = ShadowColor)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = { showLogoutDialog = true }),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "退出登录",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFEF4444),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "小糯智能家庭中心 v1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 68.dp)
            .background(DividerColor.copy(alpha = 0.6f)),
    )
}
