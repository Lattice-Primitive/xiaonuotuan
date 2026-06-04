package com.nanackations2.papaya.glass

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nanackations2.papaya.glass.service.PapayaGlassService

/**
 * Papaya 眼镜端主 Activity。
 *
 * 运行在 Rokid AR 眼镜上，通过 CXR Glass SDK 与手机端 Papaya App 进行双向通信。
 * YodaOS-Sprite 目标设计尺寸为 480x640，眼镜端只展示手机侧推送结果，
 * 不接管系统保留的按键、触控板或唤醒词交互。
 */
class GlassMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBluetoothConnectPermissionIfNeeded()

        // 启动后台通信服务
        val serviceIntent = Intent(this, PapayaGlassService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setContent {
            GlassMainScreen()
        }
    }

    private fun requestBluetoothConnectPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                1002,
            )
        }
    }
}

@Composable
fun GlassMainScreen() {
    val isChannelConnected by PapayaGlassService.sharedIsPhoneConnected.collectAsState()
    val isBluetoothConnected by PapayaGlassService.sharedIsNativeBluetoothConnected.collectAsState()
    val bluetoothLabel by PapayaGlassService.sharedNativeBluetoothLabel.collectAsState()
    val lastMessage by PapayaGlassService.sharedLastReceivedMessage.collectAsState()
    val notifications by PapayaGlassService.sharedNotificationList.collectAsState()
    val displayMessage = lastMessage.ifBlank {
        when {
            isChannelConnected -> "等待视觉讲解结果"
            isBluetoothConnected -> "蓝牙已连接，等待 Papaya 通道"
            else -> "等待手机端连接"
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val compact = maxHeight < 580.dp
        val horizontalPadding = if (compact) 18.dp else 24.dp
        val mainTextSize = if (compact) 22.sp else 25.sp
        val mainLineHeight = if (compact) 30.sp else 34.sp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isBluetoothConnected) Color(0xFF4CAF50) else Color(0xFFFF5252)),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBluetoothConnected) "蓝牙已连接" else "蓝牙未连接",
                        color = if (isBluetoothConnected) Color(0xFF4CAF50) else Color(0xFFFF5252),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Text(
                    text = "Papaya",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )

                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = if (isBluetoothConnected) Color(0xFF4CAF50) else Color.Gray,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.height(if (compact) 16.dp else 22.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF10131D),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = null,
                            tint = Color(0xFF7DD3FC),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "视觉讲解",
                            color = Color(0xFF7DD3FC),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(modifier = Modifier.height(if (compact) 18.dp else 24.dp))

                    Text(
                        text = displayMessage,
                        color = Color.White,
                        fontSize = mainTextSize,
                        lineHeight = mainLineHeight,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = if (compact) 8 else 10,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (notifications.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color(0xFFFFD166),
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "通知",
                                color = Color(0xFFFFD166),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        notifications.takeLast(2).forEach { notif ->
                            Text(
                                text = notif,
                                color = Color(0xFFE5E7EB),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 拍照识别按钮
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable {
                        PapayaGlassService.instance?.captureAndSendPhoto()
                    },
                shape = RoundedCornerShape(24.dp),
                color = if (isChannelConnected) Color(0xFF2563EB) else Color(0xFF374151),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "拍照识别",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "拍照识别",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    isChannelConnected -> "Papaya 通道已就绪，点击上方按钮拍照识别"
                    isBluetoothConnected -> "系统蓝牙：${bluetoothLabel.ifBlank { "已连接" }} · Papaya 通道等待中"
                    else -> "请在手机系统蓝牙中连接 Rokid 眼镜"
                },
                color = Color(0xFF8B949E),
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
