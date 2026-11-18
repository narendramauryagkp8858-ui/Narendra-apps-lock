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
import com.example.data.model.SecuritySettingsState
import com.example.data.repository.AppSessionManager
import com.example.ui.lock.LockGateActivity
import com.example.ui.lock.LockOverlayWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppLockMonitorService : Service() {

    companion object {
        private const val CHANNEL_ID = "applock_protection_guard"
        private const val NOTIFICATION_ID = 8546
        private const val ACTION_STOP = "com.example.service.action.STOP_MONITOR"

        fun start(context: Context) {
            val appContext = context.applicationContext
            ServiceRestartReceiver.cancelScheduledRestart(appContext)
            val intent = Intent(appContext, AppLockMonitorService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(intent)
                } else {
                    appContext.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            ServiceRestartReceiver.cancelScheduledRestart(appContext)
            val stopIntent = Intent(appContext, AppLockMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching { appContext.startService(stopIntent) }
            appContext.stopService(Intent(appContext, AppLockMonitorService::class.java))
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private var collectorsJob: Job? = null

    @Volatile
    private var stoppedExplicitly: Boolean = false

    @Volatile
    private var cachedSettings: SecuritySettingsState = SecuritySettingsState()

    @Volatile
    private var cachedLockedPackages: Set<String> = emptySet()

    private lateinit var lockOverlayWindow: LockOverlayWindow

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                lockOverlayWindow.dismiss()
                AppSessionManager.onScreenTurnedOff()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        lockOverlayWindow = LockOverlayWindow(this)
        createNotificationChannel()
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenOffReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenOffReceiver, filter)
        }
        startStateCollectors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stoppedExplicitly = true
            lockOverlayWindow.dismiss()
            ServiceRestartReceiver.cancelScheduledRestart(applicationContext)
            stopSelf()
            return START_NOT_STICKY
        }

        stoppedExplicitly = false
        val notification = buildNotification(cachedLockedPackages.size)
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

    private fun startStateCollectors() {
        if (collectorsJob?.isActive == true) return
        val app = applicationContext as NarendraAppLockApplication

        collectorsJob = serviceScope.launch {
            launch {
                app.preferencesRepository.settingsFlow.collect { settings ->
                    cachedSettings = settings
                    if (!settings.protectionEnabled) {
                        stoppedExplicitly = true
                        lockOverlayWindow.dismiss()
                        ServiceRestartReceiver.cancelScheduledRestart(applicationContext)
                        stopSelf()
                    }
                }
            }
            launch {
                var lastCount = -1
                app.database.lockedAppDao().observeLocked().collect { list ->
                    val pkgs = list.filter { it.isLocked }.map { it.packageName }.toSet()
                    cachedLockedPackages = pkgs
                    if (pkgs.size != lastCount) {
                        lastCount = pkgs.size
                        val manager = getSystemService(NotificationManager::class.java)
                        manager?.notify(NOTIFICATION_ID, buildNotification(pkgs.size))
                    }
                }
            }
        }
    }

    private fun startMonitoringLoop() {
        if (monitorJob?.isActive == true) return
        val app = applicationContext as NarendraAppLockApplication

        monitorJob = serviceScope.launch {
            // Prime initial state immediately so there is no startup race
            cachedSettings = app.preferencesRepository.getSettingsSnapshot()
            cachedLockedPackages = app.database.lockedAppDao().getLockedNow()
                .filter { it.isLocked }
                .map { it.packageName }
                .toSet()

            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, buildNotification(cachedLockedPackages.size))

            while (isActive) {
                val settings = cachedSettings
                if (!settings.protectionEnabled) {
                    stoppedExplicitly = true
                    lockOverlayWindow.dismiss()
                    stopSelf()
                    break
                }

                val lockedPackages = cachedLockedPackages
                if (lockedPackages.isNotEmpty() &&
                    PermissionStatusHelper.hasUsageStatsPermission(this@AppLockMonitorService)
                ) {
                    val topPkg = queryForegroundPackage()
                    if (!topPkg.isNullOrBlank()) {
                        if (lockOverlayWindow.isShowing) {
                            val overlayPkg = lockOverlayWindow.currentTargetPackage
                            if (overlayPkg != null && topPkg != overlayPkg && topPkg != packageName) {
                                lockOverlayWindow.dismiss()
                            }
                        }

                        if (topPkg == packageName) {
                            if (!AppSessionManager.isLockGateShowing) {
                                AppSessionManager.onForegroundAppChanged(topPkg, settings.autoLockPolicy)
                            }
                        } else {
                            AppSessionManager.onForegroundAppChanged(topPkg, settings.autoLockPolicy)
                            if (lockedPackages.contains(topPkg) &&
                                !AppSessionManager.isAppTemporarilyUnlocked(topPkg, settings.autoLockPolicy) &&
                                !AppSessionManager.isLockGateShowing
                            ) {
                                launchLockGate(topPkg)
                            }
                        }
                    }
                }
                delay(250L)
            }
        }
    }

    private fun queryForegroundPackage(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null
        val end = System.currentTimeMillis()
        val begin = end - 60_000L
        val events = usageStatsManager.queryEvents(begin, end)
        val event = UsageEvents.Event()
        var lastForegroundPkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            @Suppress("DEPRECATION")
            val isResumed = event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            val pkg = event.packageName
            if (isResumed && !pkg.isNullOrBlank() && pkg != "com.android.systemui") {
                lastForegroundPkg = pkg
            }
        }
        if (lastForegroundPkg != null) {
            return lastForegroundPkg
        }

        // Cold-start fallback when the app was already open before service start or permission grant
        if (AppSessionManager.activeForegroundPackage == null && !lockOverlayWindow.isShowing) {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                end - 120_000L,
                end
            )
            return stats
                ?.filter { !it.packageName.isNullOrBlank() && it.packageName != "com.android.systemui" }
                ?.maxByOrNull { it.lastTimeUsed }
                ?.packageName
        }

        return null
    }

    private fun launchLockGate(targetPackage: String) {
        if (PermissionStatusHelper.hasOverlayPermission(this)) {
            AppSessionManager.markLockGateLaunching()
            lockOverlayWindow.show(targetPackage)
            return
        }

        AppSessionManager.markLockGateLaunching()
        val intent = LockGateActivity.createIntent(
            context = this,
            targetPackage = targetPackage,
            isPreview = false
        ).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
        }
        try {
            startActivity(intent)
        } catch (_: Throwable) {
            AppSessionManager.onLockGateHidden()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        AppSessionManager.onAppTaskRemoved(packageName)
        if (!stoppedExplicitly &&
            cachedSettings.protectionEnabled &&
            PermissionStatusHelper.hasUsageStatsPermission(applicationContext)
        ) {
            ServiceRestartReceiver.scheduleRestart(applicationContext, delayMs = 350L)
            start(applicationContext)
        }
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
            .setSmallIcon(R.drawable.ic_stat_lock)
            .setContentTitle(getString(R.string.notification_protection_title))
            .setContentText(getString(R.string.notification_protection_text, protectedCount))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        runCatching { lockOverlayWindow.dismiss() }
        runCatching { unregisterReceiver(screenOffReceiver) }
        val shouldRestart = !stoppedExplicitly &&
            cachedSettings.protectionEnabled &&
            PermissionStatusHelper.hasUsageStatsPermission(applicationContext)
        collectorsJob?.cancel()
        monitorJob?.cancel()
        serviceScope.cancel()
        if (shouldRestart) {
            ServiceRestartReceiver.scheduleRestart(applicationContext, delayMs = 600L)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
