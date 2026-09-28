package com.yanis.objectif30.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yanis.objectif30.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = UserPreferences(context).settings.first()
                if (settings.reminderEnabled) {
                    ReminderScheduler.schedule(context, settings.reminderHour, settings.reminderMinute)
                }
            } finally {
                result.finish()
            }
        }
    }
}
