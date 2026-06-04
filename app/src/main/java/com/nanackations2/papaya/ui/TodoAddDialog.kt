package com.nanackations2.papaya.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanackations2.papaya.ui.theme.ChildColor
import com.nanackations2.papaya.ui.theme.ChildColorLight
import com.nanackations2.papaya.ui.theme.ElderlyColor
import com.nanackations2.papaya.ui.theme.ElderlyColorLight
import com.nanackations2.papaya.ui.theme.PetColor
import com.nanackations2.papaya.ui.theme.PetColorLight

data class PersonOption(
    val id: String,
    val label: String,
    val icon: String,
    val borderColor: Color,
    val lightColor: Color,
)

val personOptions = listOf(
    PersonOption("room_elderly", "老人", "👴", ElderlyColor, ElderlyColorLight),
    PersonOption("room_child", "儿童", "👧", ChildColor, ChildColorLight),
    PersonOption("room_pet", "宠物", "🐱", PetColor, PetColorLight),
)

@Composable
fun TodoAddDialog(
    initialPersonId: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (personId: String, title: String, hour: Int, minute: Int) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var selectedPersonId by remember { mutableStateOf(initialPersonId ?: personOptions.first().id) }
    var hour by remember { mutableIntStateOf(8) }
    var minute by remember { mutableIntStateOf(0) }
    var showHourPicker by remember { mutableStateOf(false) }
    var showMinutePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "添加待办",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "选择成员",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    personOptions.forEach { person ->
                        val isSelected = person.id == selectedPersonId
                        val bgColor = if (isSelected) person.lightColor else MaterialTheme.colorScheme.surfaceVariant
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(bgColor)
                                .clickable { selectedPersonId = person.id }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) person.borderColor.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surface
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = person.icon, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = person.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) person.borderColor
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 30) title = it },
                    label = { Text("待办内容") },
                    placeholder = { Text("如：吃降压药、散步") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = RoundedCornerShape(16.dp),
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "提醒时间",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TimePickerButton(
                        label = "时",
                        value = hour,
                        onClick = { showHourPicker = !showHourPicker },
                        modifier = Modifier.weight(1f),
                    )
                    TimePickerButton(
                        label = "分",
                        value = minute,
                        onClick = { showMinutePicker = !showMinutePicker },
                        modifier = Modifier.weight(1f),
                    )
                }

                if (showHourPicker) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TimeSelector(
                        range = 0..23,
                        selected = hour,
                        onSelect = { hour = it; showHourPicker = false },
                    )
                }

                if (showMinutePicker) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TimeSelector(
                        range = 0..59,
                        selected = minute,
                        onSelect = { minute = it; showMinutePicker = false },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(selectedPersonId, title.trim(), hour, minute)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                ),
                enabled = title.isNotBlank(),
            ) {
                Text("添加", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text("取消", fontWeight = FontWeight.Medium)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun TimePickerButton(
    label: String,
    value: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = String.format("%02d", value),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun TimeSelector(
    range: IntRange,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .verticalScroll(scrollState),
    ) {
        range.forEach { value ->
            Text(
                text = String.format("%02d", value),
                fontSize = 16.sp,
                fontWeight = if (value == selected) FontWeight.Bold else FontWeight.Normal,
                color = if (value == selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(value) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}
