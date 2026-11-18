package com.example.data.model

import android.graphics.drawable.Drawable

enum class LockMethod(val label: String) {
    PIN("PIN Lock"),
    PATTERN("Pattern Lock")
}

enum class AutoLockPolicy(val label: String, val timeoutMs: Long) {
    IMMEDIATE("Immediately on exit", 0L),
    SECONDS_30("After 30 seconds", 30_000L),
    MINUTE_1("After 1 minute", 60_000L),
    MINUTES_5("After 5 minutes", 300_000L),
    SCREEN_OFF("Until screen turns off", Long.MAX_VALUE)
}

enum class DisguiseMode(val label: String, val subtitle: String, val aliasClass: String) {
    DEFAULT("Narendra AppLock", "Default Security Shield", "com.example.AliasDefault"),
    CALCULATOR("Vault Calc", "Disguised as Calculator", "com.example.AliasCalculator"),
    NOTES("Quick Notes", "Disguised as Notes App", "com.example.AliasNotes")
}

enum class VaultMediaType {
    PHOTO,
    VIDEO
}

enum class SecurityEventType(val label: String) {
    APP_LOCKED("App Locked"),
    APP_UNLOCKED("App Unlocked"),
    AUTH_SUCCESS("Verified Access"),
    AUTH_FAILED("Failed Attempt"),
    INTRUDER_ALERT("Intruder Detected"),
    VAULT_ADDED("Vault Encrypted"),
    VAULT_EXPORTED("Vault Exported"),
    RECOVERY_RESET("PIN Reset"),
    SETTINGS_CHANGED("Policy Updated")
}

enum class AppCategory(val label: String) {
    SOCIAL("Social"),
    FINANCE("Finance"),
    MEDIA("Media"),
    PRODUCTIVITY("Productivity"),
    SYSTEM("System"),
    OTHER("Other")
}

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val category: AppCategory,
    val isLocked: Boolean,
    val isHiddenShortcut: Boolean,
    val lockCount: Int = 0,
    val icon: Drawable? = null
)

data class PermissionHealth(
    val hasUsageStatsAccess: Boolean,
    val hasAccessibilityEnabled: Boolean,
    val hasOverlayPermission: Boolean,
    val hasCameraPermission: Boolean,
    val hasNotificationPermission: Boolean,
    val isDeviceOwner: Boolean
)

data class SecurityPosture(
    val score: Int,
    val statusTitle: String,
    val statusSubtitle: String,
    val isUsingDefaultDemoPin: Boolean,
    val hasPatternConfigured: Boolean,
    val protectionActive: Boolean,
    val lockedAppsCount: Int,
    val vaultItemsCount: Int,
    val intruderEventsCount: Int
)

data class SecuritySettingsState(
    val primaryLockMethod: LockMethod = LockMethod.PIN,
    val hasPatternSet: Boolean = false,
    val isDefaultDemoPin: Boolean = true,
    val protectionEnabled: Boolean = true,
    val autoLockPolicy: AutoLockPolicy = AutoLockPolicy.IMMEDIATE,
    val intruderSelfieEnabled: Boolean = true,
    val intruderThreshold: Int = 2,
    val recoveryEmail: String = "narendramauryagkp@gmail.com",
    val securityQuestion: String = "What is your favorite security phrase?",
    val hasSecurityAnswer: Boolean = false,
    val disguiseMode: DisguiseMode = DisguiseMode.DEFAULT,
    val showSystemApps: Boolean = false
)
