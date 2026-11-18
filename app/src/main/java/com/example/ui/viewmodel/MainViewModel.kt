package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.NarendraAppLockApplication
import com.example.data.local.SecurityEventDao
import com.example.data.local.SecurityEventEntity
import com.example.data.model.AppCategory
import com.example.data.model.AutoLockPolicy
import com.example.data.model.DisguiseMode
import com.example.data.model.InstalledAppInfo
import com.example.data.model.LockMethod
import com.example.data.model.PermissionHealth
import com.example.data.model.SecurityEventType
import com.example.data.model.SecurityPosture
import com.example.data.model.SecuritySettingsState
import com.example.data.repository.AppSessionManager
import com.example.data.repository.InstalledAppsRepository
import com.example.data.repository.SecurityPreferencesRepository
import com.example.security.CryptoManager
import com.example.service.AppLockMonitorService
import com.example.service.PermissionStatusHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

enum class AppLockFilter(val label: String) {
    ALL("All"),
    LOCKED("Locked"),
    USER("User Apps"),
    SYSTEM("System")
}

class MainViewModel(
    private val appContext: Context,
    private val preferencesRepository: SecurityPreferencesRepository,
    private val appsRepository: InstalledAppsRepository,
    private val eventDao: SecurityEventDao,
    private val cryptoManager: CryptoManager,
    vaultCountFlow: kotlinx.coroutines.flow.Flow<Int>
) : ViewModel() {

    private val intruderPhotoCache = ConcurrentHashMap<String, Bitmap>()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(AppLockFilter.ALL)
    val selectedFilter: StateFlow<AppLockFilter> = _selectedFilter.asStateFlow()

    private val _selectedCategory = MutableStateFlow<AppCategory?>(null)
    val selectedCategory: StateFlow<AppCategory?> = _selectedCategory.asStateFlow()

    private val _permissionHealth = MutableStateFlow(PermissionStatusHelper.getPermissionHealth(appContext))
    val permissionHealth: StateFlow<PermissionHealth> = _permissionHealth.asStateFlow()

    private val _issuedRecoveryCode = MutableStateFlow<String?>(null)
    val issuedRecoveryCode: StateFlow<String?> = _issuedRecoveryCode.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val settingsState: StateFlow<SecuritySettingsState> = preferencesRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SecuritySettingsState())

    val allApps: StateFlow<List<InstalledAppInfo>> = appsRepository.appsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredApps: StateFlow<List<InstalledAppInfo>> = combine(
        allApps,
        _searchQuery,
        _selectedFilter,
        _selectedCategory,
        settingsState
    ) { apps, query, filter, category, settings ->
        apps.filter { app ->
            val matchesSystemVisibility = when (filter) {
                AppLockFilter.SYSTEM -> app.isSystemApp
                AppLockFilter.USER -> !app.isSystemApp
                else -> settings.showSystemApps || !app.isSystemApp || app.isLocked
            }
            val matchesFilter = when (filter) {
                AppLockFilter.ALL -> true
                AppLockFilter.LOCKED -> app.isLocked
                AppLockFilter.USER -> !app.isSystemApp
                AppLockFilter.SYSTEM -> app.isSystemApp
            }
            val matchesCategory = category == null || app.category == category
            val matchesQuery = query.isBlank() ||
                app.appName.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)

            matchesSystemVisibility && matchesFilter && matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val securityEvents: StateFlow<List<SecurityEventEntity>> = eventDao.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val intruderAlerts: StateFlow<List<SecurityEventEntity>> = eventDao.observeIntruderAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val securityPosture: StateFlow<SecurityPosture> = combine(
        settingsState,
        allApps,
        vaultCountFlow,
        intruderAlerts,
        _permissionHealth
    ) { settings, apps, vaultCount, intruders, perms ->
        val lockedCount = apps.count { it.isLocked }
        var score = 25
        if (!settings.isDefaultDemoPin) score += 25
        if (settings.hasPatternSet) score += 15
        if (settings.protectionEnabled) score += 10
        if (perms.hasUsageStatsAccess || perms.hasAccessibilityEnabled) score += 15
        if (lockedCount > 0) score += 10
        score = score.coerceIn(0, 100)

        val (title, subtitle) = when {
            score >= 85 -> "Fortress Shield Active" to "Encryption, guard services & master lock are hardened."
            score >= 60 -> "Strong Protection" to if (settings.isDefaultDemoPin) {
                "Change default demo PIN (8546) for maximum security."
            } else {
                "Enable system app-monitoring permissions for real-time lock."
            }
            else -> "Setup Recommended" to "Complete guard permissions and customize your master PIN."
        }

        SecurityPosture(
            score = score,
            statusTitle = title,
            statusSubtitle = subtitle,
            isUsingDefaultDemoPin = settings.isDefaultDemoPin,
            hasPatternConfigured = settings.hasPatternSet,
            protectionActive = settings.protectionEnabled,
            lockedAppsCount = lockedCount,
            vaultItemsCount = vaultCount,
            intruderEventsCount = intruders.size
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SecurityPosture(
            score = 35,
            statusTitle = "Initializing Shield",
            statusSubtitle = "Checking device security state...",
            isUsingDefaultDemoPin = true,
            hasPatternConfigured = false,
            protectionActive = true,
            lockedAppsCount = 0,
            vaultItemsCount = 0,
            intruderEventsCount = 0
        )
    )

    init {
        refreshApps()
        refreshPermissions()
    }

    fun refreshApps() {
        viewModelScope.launch {
            appsRepository.refreshInstalledApps()
        }
    }

    fun refreshPermissions() {
        _permissionHealth.value = PermissionStatusHelper.getPermissionHealth(appContext)
    }

    fun verifyPinAndUnlock(pin: String, onIntruderThresholdReached: () -> Unit = {}) {
        viewModelScope.launch {
            val valid = preferencesRepository.verifyPin(pin)
            if (valid) {
                onSuccessfulUnlock("Unlocked Narendra AppLock with PIN")
            } else {
                onFailedUnlockAttempt("Incorrect PIN entered on main vault", onIntruderThresholdReached)
            }
        }
    }

    fun verifyPatternAndUnlock(nodes: List<Int>, onIntruderThresholdReached: () -> Unit = {}) {
        viewModelScope.launch {
            val valid = preferencesRepository.verifyPattern(nodes)
            if (valid) {
                onSuccessfulUnlock("Unlocked Narendra AppLock with Pattern")
            } else {
                onFailedUnlockAttempt("Incorrect pattern drawn on main vault", onIntruderThresholdReached)
            }
        }
    }

    private suspend fun onSuccessfulUnlock(detail: String) {
        _authError.value = null
        _failedAttempts.value = 0
        _isUnlocked.value = true
        eventDao.insert(
            SecurityEventEntity(
                eventType = SecurityEventType.AUTH_SUCCESS,
                title = "Vault Access Granted",
                detail = detail
            )
        )
    }

    private suspend fun onFailedUnlockAttempt(detail: String, onIntruderThresholdReached: () -> Unit) {
        val nextCount = _failedAttempts.value + 1
        _failedAttempts.value = nextCount
        val settings = preferencesRepository.getSettingsSnapshot()
        _authError.value = "Invalid credentials ($nextCount failed attempt${if (nextCount > 1) "s" else ""})"

        if (nextCount >= settings.intruderThreshold) {
            eventDao.insert(
                SecurityEventEntity(
                    eventType = SecurityEventType.INTRUDER_ALERT,
                    title = "Intruder Alert ($nextCount failed attempts)",
                    detail = detail,
                    targetPackage = appContext.packageName
                )
            )
            onIntruderThresholdReached()
        } else {
            eventDao.insert(
                SecurityEventEntity(
                    eventType = SecurityEventType.AUTH_FAILED,
                    title = "Failed Unlock Attempt",
                    detail = detail
                )
            )
        }
    }

    fun recordIntruderSelfie(encryptedPhotoPath: String, targetPackage: String) {
        viewModelScope.launch {
            eventDao.insert(
                SecurityEventEntity(
                    eventType = SecurityEventType.INTRUDER_ALERT,
                    title = "Intruder Photo Captured",
                    detail = "Front camera captured unauthorized unlock attempt",
                    targetPackage = targetPackage,
                    intruderPhotoPath = encryptedPhotoPath
                )
            )
        }
    }

    fun lockNow() {
        _isUnlocked.value = false
        _authError.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: AppLockFilter) {
        _selectedFilter.value = filter
    }

    fun setCategory(category: AppCategory?) {
        _selectedCategory.value = category
    }

    fun toggleAppLock(app: InstalledAppInfo, locked: Boolean) {
        viewModelScope.launch {
            appsRepository.setAppLocked(app, locked)
            if (locked) {
                AppSessionManager.clearAllUnlocked()
                if (settingsState.value.protectionEnabled &&
                    PermissionStatusHelper.hasUsageStatsPermission(appContext)
                ) {
                    AppLockMonitorService.start(appContext)
                }
            }
            eventDao.insert(
                SecurityEventEntity(
                    eventType = if (locked) SecurityEventType.APP_LOCKED else SecurityEventType.APP_UNLOCKED,
                    title = "${app.appName} ${if (locked) "Protected" else "Unprotected"}",
                    detail = "Package: ${app.packageName}",
                    targetPackage = app.packageName
                )
            )
        }
    }

    fun lockOrUnlockAllVisible(lockAll: Boolean) {
        viewModelScope.launch {
            val list = filteredApps.value
            if (list.isEmpty()) return@launch
            appsRepository.setAllAppsLocked(list, lockAll)
            if (lockAll) {
                AppSessionManager.clearAllUnlocked()
                if (settingsState.value.protectionEnabled &&
                    PermissionStatusHelper.hasUsageStatsPermission(appContext)
                ) {
                    AppLockMonitorService.start(appContext)
                }
            }
            eventDao.insert(
                SecurityEventEntity(
                    eventType = if (lockAll) SecurityEventType.APP_LOCKED else SecurityEventType.APP_UNLOCKED,
                    title = if (lockAll) "Bulk Protected ${list.size} Apps" else "Unprotected ${list.size} Apps",
                    detail = "Applied batch lock action from App Locker"
                )
            )
            showToast(if (lockAll) "${list.size} apps locked" else "${list.size} apps unlocked")
        }
    }

    fun toggleHiddenShortcut(app: InstalledAppInfo, hidden: Boolean) {
        viewModelScope.launch {
            appsRepository.setHiddenShortcut(app, hidden)
            showToast(
                if (hidden) "${app.appName} added to Stealth Vault"
                else "${app.appName} removed from Stealth Vault"
            )
        }
    }

    fun launchHiddenApp(packageName: String) {
        if (!appsRepository.launchApp(packageName)) {
            showToast("Unable to launch $packageName on this device")
        }
    }

    fun applyDisguiseMode(mode: DisguiseMode) {
        viewModelScope.launch {
            appsRepository.applyAppIconDisguise(mode)
            preferencesRepository.setDisguiseMode(mode)
            eventDao.insert(
                SecurityEventEntity(
                    eventType = SecurityEventType.SETTINGS_CHANGED,
                    title = "Icon Disguise Changed",
                    detail = "Launcher identity switched to ${mode.label}"
                )
            )
            showToast("Launcher icon updated to ${mode.label}. Launcher may take a few seconds to refresh.")
        }
    }

    fun updatePin(newPin: String) {
        viewModelScope.launch {
            preferencesRepository.updatePin(newPin)
            eventDao.insert(
                SecurityEventEntity(
                    eventType = SecurityEventType.SETTINGS_CHANGED,
                    title = "Master PIN Updated",
                    detail = "New PBKDF2-SHA256 PIN hash stored securely"
                )
            )
            showToast("Master PIN updated")
        }
    }

    fun savePattern(nodes: List<Int>) {
        viewModelScope.launch {
            preferencesRepository.savePattern(nodes)
            eventDao.insert(
                SecurityEventEntity(
                    eventType = SecurityEventType.SETTINGS_CHANGED,
                    title = "Pattern Lock Configured",
                    detail = "Pattern sequence with ${nodes.size} nodes saved"
                )
            )
            showToast("Pattern lock saved")
        }
    }

    fun setPrimaryLockMethod(method: LockMethod) {
        viewModelScope.launch {
            if (method == LockMethod.PATTERN && !settingsState.value.hasPatternSet) {
                showToast("Please draw and save a pattern first")
                return@launch
            }
            preferencesRepository.setPrimaryLockMethod(method)
            showToast("Primary lock switched to ${method.label}")
        }
    }

    fun setProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setProtectionEnabled(enabled)
            if (enabled) {
                if (PermissionStatusHelper.hasUsageStatsPermission(appContext)) {
                    AppLockMonitorService.start(appContext)
                }
            } else {
                AppLockMonitorService.stop(appContext)
            }
            eventDao.insert(
                SecurityEventEntity(
                    eventType = SecurityEventType.SETTINGS_CHANGED,
                    title = "Protection Guard ${if (enabled) "Activated" else "Paused"}",
                    detail = "Background app lock guard state updated"
                )
            )
        }
    }

    fun setAutoLockPolicy(policy: AutoLockPolicy) {
        viewModelScope.launch {
            preferencesRepository.setAutoLockPolicy(policy)
            eventDao.insert(
                SecurityEventEntity(
                    eventType = SecurityEventType.SETTINGS_CHANGED,
                    title = "Auto-Lock Policy Updated",
                    detail = "Relock timing set to: ${policy.label}"
                )
            )
            showToast("Auto-lock: ${policy.label}")
        }
    }

    fun setIntruderSettings(selfieEnabled: Boolean, threshold: Int) {
        viewModelScope.launch {
            preferencesRepository.setIntruderSettings(selfieEnabled, threshold)
            showToast("Intruder threshold set to $threshold attempt(s)")
        }
    }

    fun setShowSystemApps(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowSystemApps(show)
        }
    }

    fun updateRecoveryConfig(email: String, question: String, answer: String?) {
        viewModelScope.launch {
            preferencesRepository.updateRecoveryConfig(email, question, answer)
            eventDao.insert(
                SecurityEventEntity(
                    eventType = SecurityEventType.SETTINGS_CHANGED,
                    title = "Recovery Email Updated",
                    detail = "Encrypted recovery email configured ($email)"
                )
            )
            showToast("Recovery details saved securely")
        }
    }

    fun generateAndSendRecoveryEmail(context: Context) {
        viewModelScope.launch {
            val code = preferencesRepository.issueLocalRecoveryCode()
            _issuedRecoveryCode.value = code
            val email = settingsState.value.recoveryEmail

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, "Narendra AppLock Recovery Code")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Your Narendra AppLock offline recovery verification code is: $code\n\n" +
                        "Valid for 15 minutes on your device. No data is uploaded to any server."
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }
    }

    fun resetPinWithRecoveryCode(code: String, newPin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = preferencesRepository.verifyRecoveryCodeAndResetPin(code, newPin)
            if (success) {
                _issuedRecoveryCode.value = null
                _failedAttempts.value = 0
                _authError.value = null
                eventDao.insert(
                    SecurityEventEntity(
                        eventType = SecurityEventType.RECOVERY_RESET,
                        title = "PIN Reset via Recovery Code",
                        detail = "Master PIN updated using registered email verification"
                    )
                )
                showToast("PIN successfully reset")
            }
            onResult(success)
        }
    }

    fun resetPinWithSecurityAnswer(answer: String, newPin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = preferencesRepository.verifySecurityAnswerAndResetPin(answer, newPin)
            if (success) {
                _failedAttempts.value = 0
                _authError.value = null
                eventDao.insert(
                    SecurityEventEntity(
                        eventType = SecurityEventType.RECOVERY_RESET,
                        title = "PIN Reset via Security Question",
                        detail = "Master PIN reset locally"
                    )
                )
                showToast("PIN successfully reset")
            }
            onResult(success)
        }
    }

    fun deleteSecurityEvent(id: Long) {
        viewModelScope.launch {
            eventDao.deleteById(id)
        }
    }

    fun clearSecurityHistory() {
        viewModelScope.launch {
            eventDao.clearAll()
            showToast("Security history cleared")
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun clearAuthError() {
        _authError.value = null
    }

    private fun showToast(message: String) {
        _toastMessage.value = message
    }

    suspend fun loadIntruderPhoto(encryptedPath: String): Bitmap? = withContext(Dispatchers.IO) {
        intruderPhotoCache[encryptedPath]?.let { return@withContext it }
        val file = File(encryptedPath)
        if (!file.exists()) return@withContext null
        runCatching {
            val plain = file.inputStream().use { cryptoManager.decryptStreamToBytes(it) }
            BitmapFactory.decodeByteArray(plain, 0, plain.size)?.also { bmp ->
                intruderPhotoCache[encryptedPath] = bmp
            }
        }.getOrNull()
    }

    class Factory(private val app: NarendraAppLockApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                appContext = app,
                preferencesRepository = app.preferencesRepository,
                appsRepository = app.appsRepository,
                eventDao = app.database.securityEventDao(),
                cryptoManager = app.cryptoManager,
                vaultCountFlow = app.vaultRepository.vaultCountFlow
            ) as T
        }
    }
}
