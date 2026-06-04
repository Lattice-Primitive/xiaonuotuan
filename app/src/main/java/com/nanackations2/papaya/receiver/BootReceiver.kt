package com.nanackations2.papaya.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nanackations2.papaya.data.repository.TodoRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val repository = TodoRepository(context)
            repository.rescheduleAllAlarms()
        }
    }
}
