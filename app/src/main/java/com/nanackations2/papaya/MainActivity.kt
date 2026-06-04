package com.nanackations2.papaya

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nanackations2.papaya.notification.NotificationHelper
import com.nanackations2.papaya.ui.AccountScreen
import com.nanackations2.papaya.ui.AccountSubScreen
import com.nanackations2.papaya.ui.RokidTabScreen
import com.nanackations2.papaya.ui.TodoScreen
import com.nanackations2.papaya.ui.MonitorScreen
import com.nanackations2.papaya.ui.theme.CreamBackground
import com.nanackations2.papaya.ui.theme.CreamCard
import com.nanackations2.papaya.ui.theme.MintGreen
import com.nanackations2.papaya.ui.theme.PapayaTheme
import com.nanackations2.papaya.ui.theme.TextHint
import com.nanackations2.papaya.ui.theme.WarmOrange

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setKeepScreenOn(
            getSharedPreferences("dev_options", MODE_PRIVATE)
                .getBoolean("keep_screen_on", false),
        )
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)
        setContent {
            PapayaTheme {
                MainScreen(onKeepScreenOnChange = ::setKeepScreenOn)
            }
        }
    }

    private fun setKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

/** 导航栏项列表，提升为顶层常量避免每次重组都重新分配 */
private val NAV_ITEMS = listOf(
    NavItem(
        label = "实时画面",
        icon = Icons.Default.CameraAlt,
        selectedIcon = Icons.Default.CameraAlt,
    ),
    NavItem(
        label = "用药提醒",
        icon = Icons.Default.Notifications,
        selectedIcon = Icons.Default.Notifications,
    ),
    NavItem(
        label = "Rokid AR",
        icon = Icons.Default.Devices,
        selectedIcon = Icons.Default.Devices,
    ),
    NavItem(
        label = "我的",
        icon = Icons.Default.AccountCircle,
        selectedIcon = Icons.Default.AccountCircle,
    ),
)

@androidx.compose.runtime.Composable
fun MainScreen(onKeepScreenOnChange: (Boolean) -> Unit = {}) {
    var selectedTab by remember { mutableIntStateOf(0) }
    // 跟踪"我的"界面的二级子页面，用于隐藏底部导航栏
    var accountSubScreen by remember { mutableStateOf(AccountSubScreen.None) }

    val navItems = NAV_ITEMS

    // 是否处于全屏子页面（隐藏底部导航栏）
    val isInFullScreenSubPage = accountSubScreen != AccountSubScreen.None

    // 返回手势处理：
    // 1. 如果在"我的"的二级页面，返回到"我的"一级页面
    // 2. 如果在非首页Tab，返回到首页Tab
    // 3. 如果在首页Tab，交给系统处理（退出app）
    BackHandler(enabled = isInFullScreenSubPage || selectedTab != 0) {
        when {
            isInFullScreenSubPage -> accountSubScreen = AccountSubScreen.None
            selectedTab != 0 -> selectedTab = 0
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CreamBackground,
        bottomBar = {
            // 子页面全屏时隐藏底部导航栏
            AnimatedVisibility(
                visible = !isInFullScreenSubPage,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                NavigationBar(
                    containerColor = CreamCard,
                    tonalElevation = 8.dp,
                ) {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (index == selectedTab) item.selectedIcon else item.icon,
                                    contentDescription = item.label,
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontWeight = if (index == selectedTab) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            selected = index == selectedTab,
                            onClick = { selectedTab = index },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = WarmOrange,
                                selectedTextColor = WarmOrange,
                                unselectedIconColor = TextHint,
                                unselectedTextColor = TextHint,
                                indicatorColor = WarmOrange.copy(alpha = 0.1f),
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Crossfade(
            targetState = selectedTab,
            label = "main_screen_crossfade",
        ) { tab ->
            when (tab) {
                0 -> MonitorScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
                1 -> TodoScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
                2 -> RokidTabScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
                3 -> AccountScreen(
                    modifier = if (isInFullScreenSubPage) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    },
                    currentSubScreen = accountSubScreen,
                    onSubScreenChange = { accountSubScreen = it },
                    onKeepScreenOnChange = onKeepScreenOnChange,
                )
            }
        }
    }
}
