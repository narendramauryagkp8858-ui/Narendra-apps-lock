package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.NarendraAppLockApplication
import com.example.R
import com.example.data.repository.AppSessionManager
import com.example.ui.lock.LockGateActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppLockMonitorService : Service() {

    companion object {
        private const val CHANNEL_ID = "applock_protection_guard"
        private const val NOTIFICATION_ID = 8546

        fun start(context: Context) {
            val intent = Intent(context, AppLockMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppLockMonitorService::class.java))
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                AppSessionManager.onScreenTurnedOff()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenOffReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenOffReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startMonitoringLoop()
        return START_STICKY
    }

    private fun startMonitoringLoop() {
        if (monitorJob?.isActive == true) return
        val app = applicationContext as NarendraAppLockApplication

        monitorJob = serviceScope.launch {
            var lastCount = -1
            while (isActive) {
                val settings = app.preferencesRepository.settingsFlow.first()
                if (!settings.protectionEnabled) {
                    stopSelf()
                    break
                }

                val lockedPackages = app.database.lockedAppDao().getLockedNow()
                    .filter { it.isLocked }
                    .map { it.packageName }
                    .toSet()

                if (lockedPackages.size != lastCount) {
                    lastCount = lockedPackages.size
                    val manager = getSystemService(NotificationManager::class.java)
                    manager?.notify(NOTIFICATION_ID, buildNotification(lockedPackages.size))
                }

                if (lockedPackages.isNotEmpty() &&
                    PermissionStatusHelper.hasUsageStatsPermission(this@AppLockMonitorService)
                ) {
                    val topPkg = queryForegroundPackage()
                    if (!topPkg.isNullOrBlank() && topPkg != packageName) {
                        AppSessionManager.onForegroundAppChanged(topPkg, settings.autoLockPolicy)
                        if (lockedPackages.contains(topPkg) &&
                            !AppSessionManager.isAppTemporarilyUnlocked(topPkg, settings.autoLockPolicy) &&
                            !AppSessionManager.isLockGateShowing
                        ) {
                            launchLockGate(topPkg)
                        }
                    }
                }
                delay(650L)
            }
        }
    }

    private fun queryForegroundPackage(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null
        val end = System.currentTimeMillis()
        val begin = end - 4_000L
        val events = usageStatsManager.queryEvents(begin, end)
        val event = UsageEvents.Event()
        var lastForegroundPkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastForegroundPkg = event.packageName
            }
        }
        return lastForegroundPkg
    }

    private fun launchLockGate(targetPackage: String) {
        AppSessionManager.isLockGateShowing = true
        val intent = LockGateActivity.createIntent(
            context = this,
            targetPackage = targetPackage,
            isPreview = false
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        runCatching { startActivity(intent) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_protection_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_protection_desc)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(protectedCount: Int): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_protection_title))
            .setContentText(getString(R.string.notification_protection_text, protectedCount))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenOffReceiver) }
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
