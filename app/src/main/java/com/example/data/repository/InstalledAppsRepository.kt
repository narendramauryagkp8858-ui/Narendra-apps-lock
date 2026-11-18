package com.example.data.repository

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.example.data.local.LockedAppDao
import com.example.data.local.LockedAppEntity
import com.example.data.model.AppCategory
import com.example.data.model.DisguiseMode
import com.example.data.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class InstalledAppsRepository(
    private val context: Context,
    private val lockedAppDao: LockedAppDao
) {
    private val rawAppsFlow = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val iconCache = ConcurrentHashMap<String, Drawable>()

    val appsFlow: Flow<List<InstalledAppInfo>> = combine(
        rawAppsFlow,
        lockedAppDao.observeAll()
    ) { installed, lockEntities ->
        val entityMap = lockEntities.associateBy { it.packageName }
        installed.map { app ->
            val entity = entityMap[app.packageName]
            app.copy(
                isLocked = entity?.isLocked == true,
                isHiddenShortcut = entity?.isHiddenShortcut == true,
                lockCount = entity?.lockCount ?: 0
            )
        }
    }

    suspend fun refreshInstalledApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                launchIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launchIntent, 0)
        }

        val ownPackage = context.packageName
        val uniquePackages = resolved
            .mapNotNull { it.activityInfo?.packageName }
            .distinct()
            .filter { it != ownPackage }

        val appList = uniquePackages.mapNotNull { pkg ->
            runCatching {
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(pkg, 0)
                }
                val label = pm.getApplicationLabel(appInfo).toString()
                val icon = iconCache.getOrPut(pkg) { pm.getApplicationIcon(appInfo) }
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

                InstalledAppInfo(
                    packageName = pkg,
                    appName = label,
                    isSystemApp = isSystem,
                    category = categorizeApp(pkg, label, isSystem, appInfo),
                    isLocked = false,
                    isHiddenShortcut = false,
                    icon = icon
                )
            }.getOrNull()
        }.sortedBy { it.appName.lowercase() }

        rawAppsFlow.value = appList
    }

    fun getAppIcon(packageName: String): Drawable? {
        return iconCache[packageName] ?: runCatching {
            context.packageManager.getApplicationIcon(packageName).also {
                iconCache[packageName] = it
            }
        }.getOrNull()
    }

    fun getAppLabel(packageName: String): String {
        return runCatching {
            val pm = context.packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }

    suspend fun setAppLocked(app: InstalledAppInfo, locked: Boolean) {
        val existing = lockedAppDao.getByPackage(app.packageName)
        if (existing != null) {
            lockedAppDao.upsert(existing.copy(appName = app.appName, isLocked = locked))
        } else {
            lockedAppDao.upsert(
                LockedAppEntity(
                    packageName = app.packageName,
                    appName = app.appName,
                    isLocked = locked,
                    isHiddenShortcut = false
                )
            )
        }
    }

    suspend fun setAllAppsLocked(apps: List<InstalledAppInfo>, locked: Boolean) {
        val currentMap = lockedAppDao.getLockedNow().associateBy { it.packageName }
        val entities = apps.map { app ->
            val existing = currentMap[app.packageName]
            LockedAppEntity(
                packageName = app.packageName,
                appName = app.appName,
                isLocked = locked,
                isHiddenShortcut = existing?.isHiddenShortcut ?: app.isHiddenShortcut,
                lockCount = existing?.lockCount ?: app.lockCount,
                lastUnlockedAt = existing?.lastUnlockedAt ?: 0L
            )
        }
        lockedAppDao.upsertAll(entities)
    }

    suspend fun setHiddenShortcut(app: InstalledAppInfo, hidden: Boolean) {
        val existing = lockedAppDao.getByPackage(app.packageName)
        if (existing != null) {
            lockedAppDao.upsert(existing.copy(appName = app.appName, isHiddenShortcut = hidden))
        } else {
            lockedAppDao.upsert(
                LockedAppEntity(
                    packageName = app.packageName,
                    appName = app.appName,
                    isLocked = true,
                    isHiddenShortcut = hidden
                )
            )
        }
    }

    fun applyAppIconDisguise(mode: DisguiseMode) {
        val pm = context.packageManager
        DisguiseMode.entries.forEach { entry ->
            val component = ComponentName(context, entry.aliasClass)
            val state = if (entry == mode) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            runCatching {
                pm.setComponentEnabledSetting(
                    component,
                    state,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }

    fun isDeviceOwner(): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        return dpm?.isDeviceOwnerApp(context.packageName) == true
    }

    fun launchApp(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    private fun categorizeApp(
        pkg: String,
        label: String,
        isSystem: Boolean,
        info: ApplicationInfo
    ): AppCategory {
        val lower = "$pkg $label".lowercase()
        return when {
            isSystem -> AppCategory.SYSTEM
            lower.containsAny("whatsapp", "telegram", "instagram", "facebook", "snap", "chat", "discord", "x.", "twitter", "threads", "signal", "messenger") -> AppCategory.SOCIAL
            lower.containsAny("bank", "pay", "wallet", "upi", "finance", "gpay", "phonepe", "paypal", "crypto", "stock") -> AppCategory.FINANCE
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && (info.category == ApplicationInfo.CATEGORY_VIDEO || info.category == ApplicationInfo.CATEGORY_AUDIO || info.category == ApplicationInfo.CATEGORY_IMAGE) -> AppCategory.MEDIA
            lower.containsAny("photo", "gallery", "video", "youtube", "spotify", "music", "netflix", "camera") -> AppCategory.MEDIA
            lower.containsAny("mail", "gmail", "docs", "drive", "notes", "calendar", "slack", "teams", "zoom") -> AppCategory.PRODUCTIVITY
            else -> AppCategory.OTHER
        }
    }

    private fun String.containsAny(vararg tokens: String): Boolean = tokens.any { this.contains(it) }
}
