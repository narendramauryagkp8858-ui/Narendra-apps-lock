package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.NarendraAppLockApplication
import com.example.data.repository.AppSessionManager
import com.example.ui.lock.LockGateActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppLockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == "com.android.systemui") return

        val app = applicationContext as? NarendraAppLockApplication ?: return
        serviceScope.launch {
            val settings = app.preferencesRepository.settingsFlow.first()
            if (!settings.protectionEnabled) return@launch

            AppSessionManager.onForegroundAppChanged(pkg, settings.autoLockPolicy)

            val lockedEntity = app.database.lockedAppDao().getByPackage(pkg)
            if (lockedEntity?.isLocked == true &&
                !AppSessionManager.isAppTemporarilyUnlocked(pkg, settings.autoLockPolicy) &&
                !AppSessionManager.isLockGateShowing
            ) {
                AppSessionManager.isLockGateShowing = true
                val intent = LockGateActivity.createIntent(
                    context = this@AppLockAccessibilityService,
                    targetPackage = pkg,
                    isPreview = false
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                runCatching { startActivity(intent) }
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
