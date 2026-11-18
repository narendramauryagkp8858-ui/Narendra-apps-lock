package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.NarendraAppLockApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? NarendraAppLockApplication
                val settings = app?.preferencesRepository?.getSettingsSnapshot()
                if (settings?.protectionEnabled == true &&
                    PermissionStatusHelper.hasUsageStatsPermission(context)
                ) {
                    AppLockMonitorService.start(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
