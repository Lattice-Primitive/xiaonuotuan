package com.nanackations2.papaya.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nanackations2.papaya.notification.NotificationHelper

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra("todo_id", -1L)
        val todoName = intent.getStringExtra("todo_name") ?: "未知待办"

        if (todoId > 0) {
            NotificationHelper.createNotificationChannel(context)
            NotificationHelper.showTodoNotification(context, todoId, todoName)
        }
    }
}
