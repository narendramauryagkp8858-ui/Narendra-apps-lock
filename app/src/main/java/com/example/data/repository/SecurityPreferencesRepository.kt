package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AutoLockPolicy
import com.example.data.model.DisguiseMode
import com.example.data.model.LockMethod
import com.example.data.model.SecuritySettingsState
import com.example.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "narendra_applock_secure_prefs"
)

class SecurityPreferencesRepository(
    private val context: Context,
    private val cryptoManager: CryptoManager
) {
    companion object {
        const val DEFAULT_DEMO_PIN = "8546"
        const val DEFAULT_DEVELOPER_EMAIL = "narendramauryagkp@gmail.com"
        private const val RECOVERY_CODE_VALIDITY_MS = 15 * 60 * 1000L

        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
        private val KEY_IS_DEFAULT_PIN = booleanPreferencesKey("is_default_pin")
        private val KEY_PATTERN_HASH = stringPreferencesKey("pattern_hash")
        private val KEY_PATTERN_SALT = stringPreferencesKey("pattern_salt")
        private val KEY_HAS_PATTERN = booleanPreferencesKey("has_pattern")
        private val KEY_LOCK_METHOD = stringPreferencesKey("primary_lock_method")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_PROTECTION_ENABLED = booleanPreferencesKey("protection_enabled")
        private val KEY_AUTO_LOCK_POLICY = stringPreferencesKey("auto_lock_policy")
        private val KEY_INTRUDER_SELFIE = booleanPreferencesKey("intruder_selfie")
        private val KEY_INTRUDER_THRESHOLD = intPreferencesKey("intruder_threshold")
        private val KEY_RECOVERY_EMAIL_ENC = stringPreferencesKey("recovery_email_enc")
        private val KEY_SECURITY_QUESTION = stringPreferencesKey("security_question")
        private val KEY_SECURITY_ANSWER_HASH = stringPreferencesKey("security_answer_hash")
        private val KEY_SECURITY_ANSWER_SALT = stringPreferencesKey("security_answer_salt")
        private val KEY_RECOVERY_CODE_HASH = stringPreferencesKey("recovery_code_hash")
        private val KEY_RECOVERY_CODE_SALT = stringPreferencesKey("recovery_code_salt")
        private val KEY_RECOVERY_CODE_EXPIRY = longPreferencesKey("recovery_code_expiry")
        private val KEY_DISGUISE_MODE = stringPreferencesKey("disguise_mode")
        private val KEY_SHOW_SYSTEM_APPS = booleanPreferencesKey("show_system_apps")
    }

    val settingsFlow: Flow<SecuritySettingsState> = context.securityDataStore.data.map { prefs ->
        val encryptedEmail = prefs[KEY_RECOVERY_EMAIL_ENC]
        val decodedEmail = if (!encryptedEmail.isNullOrBlank()) {
            runCatching { cryptoManager.decryptString(encryptedEmail) }.getOrDefault(DEFAULT_DEVELOPER_EMAIL)
        } else {
            DEFAULT_DEVELOPER_EMAIL
        }

        SecuritySettingsState(
            primaryLockMethod = prefs[KEY_LOCK_METHOD]?.let {
                runCatching { LockMethod.valueOf(it) }.getOrDefault(LockMethod.PIN)
            } ?: LockMethod.PIN,
            hasPatternSet = prefs[KEY_HAS_PATTERN] ?: false,
            isDefaultDemoPin = prefs[KEY_IS_DEFAULT_PIN] ?: true,
            biometricEnabled = prefs[KEY_BIOMETRIC_ENABLED] ?: true,
            protectionEnabled = prefs[KEY_PROTECTION_ENABLED] ?: true,
            autoLockPolicy = prefs[KEY_AUTO_LOCK_POLICY]?.let {
                runCatching { AutoLockPolicy.valueOf(it) }.getOrDefault(AutoLockPolicy.IMMEDIATE)
            } ?: AutoLockPolicy.IMMEDIATE,
            intruderSelfieEnabled = prefs[KEY_INTRUDER_SELFIE] ?: true,
            intruderThreshold = prefs[KEY_INTRUDER_THRESHOLD] ?: 2,
            recoveryEmail = decodedEmail,
            securityQuestion = prefs[KEY_SECURITY_QUESTION] ?: "What is your favorite security phrase?",
            hasSecurityAnswer = !prefs[KEY_SECURITY_ANSWER_HASH].isNullOrBlank(),
            disguiseMode = prefs[KEY_DISGUISE_MODE]?.let {
                runCatching { DisguiseMode.valueOf(it) }.getOrDefault(DisguiseMode.DEFAULT)
            } ?: DisguiseMode.DEFAULT,
            showSystemApps = prefs[KEY_SHOW_SYSTEM_APPS] ?: false
        )
    }

    suspend fun ensureDefaultPinInitialized() {
        val prefs = context.securityDataStore.data.first()
        if (prefs[KEY_PIN_HASH].isNullOrBlank()) {
            val (hash, salt) = cryptoManager.hashSecret(DEFAULT_DEMO_PIN)
            val encryptedEmail = cryptoManager.encryptString(DEFAULT_DEVELOPER_EMAIL)
            context.securityDataStore.edit { mutable ->
                mutable[KEY_PIN_HASH] = hash
                mutable[KEY_PIN_SALT] = salt
                mutable[KEY_IS_DEFAULT_PIN] = true
                mutable[KEY_BIOMETRIC_ENABLED] = true
                mutable[KEY_PROTECTION_ENABLED] = true
                mutable[KEY_INTRUDER_SELFIE] = true
                mutable[KEY_INTRUDER_THRESHOLD] = 2
                mutable[KEY_RECOVERY_EMAIL_ENC] = encryptedEmail
            }
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        ensureDefaultPinInitialized()
        val prefs = context.securityDataStore.data.first()
        val hash = prefs[KEY_PIN_HASH].orEmpty()
        val salt = prefs[KEY_PIN_SALT].orEmpty()
        return cryptoManager.verifySecret(pin, hash, salt)
    }

    suspend fun updatePin(newPin: String, isResetFromRecovery: Boolean = false) {
        val (hash, salt) = cryptoManager.hashSecret(newPin)
        context.securityDataStore.edit { prefs ->
            prefs[KEY_PIN_HASH] = hash
            prefs[KEY_PIN_SALT] = salt
            prefs[KEY_IS_DEFAULT_PIN] = (newPin == DEFAULT_DEMO_PIN && !isResetFromRecovery)
        }
    }

    suspend fun verifyPattern(patternNodes: List<Int>): Boolean {
        if (patternNodes.size < 4) return false
        val prefs = context.securityDataStore.data.first()
        val hash = prefs[KEY_PATTERN_HASH].orEmpty()
        val salt = prefs[KEY_PATTERN_SALT].orEmpty()
        val serialized = patternNodes.joinToString("-")
        return cryptoManager.verifySecret(serialized, hash, salt)
    }

    suspend fun savePattern(patternNodes: List<Int>) {
        val serialized = patternNodes.joinToString("-")
        val (hash, salt) = cryptoManager.hashSecret(serialized)
        context.securityDataStore.edit { prefs ->
            prefs[KEY_PATTERN_HASH] = hash
            prefs[KEY_PATTERN_SALT] = salt
            prefs[KEY_HAS_PATTERN] = true
        }
    }

    suspend fun setPrimaryLockMethod(method: LockMethod) {
        context.securityDataStore.edit { prefs ->
            prefs[KEY_LOCK_METHOD] = method.name
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.securityDataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        context.securityDataStore.edit { prefs ->
            prefs[KEY_PROTECTION_ENABLED] = enabled
        }
    }

    suspend fun setAutoLockPolicy(policy: AutoLockPolicy) {
        context.securityDataStore.edit { prefs ->
            prefs[KEY_AUTO_LOCK_POLICY] = policy.name
        }
    }

    suspend fun setIntruderSettings(selfieEnabled: Boolean, threshold: Int) {
        context.securityDataStore.edit { prefs ->
            prefs[KEY_INTRUDER_SELFIE] = selfieEnabled
            prefs[KEY_INTRUDER_THRESHOLD] = threshold.coerceIn(1, 5)
        }
    }

    suspend fun setDisguiseMode(mode: DisguiseMode) {
        context.securityDataStore.edit { prefs ->
            prefs[KEY_DISGUISE_MODE] = mode.name
        }
    }

    suspend fun setShowSystemApps(show: Boolean) {
        context.securityDataStore.edit { prefs ->
            prefs[KEY_SHOW_SYSTEM_APPS] = show
        }
    }

    suspend fun updateRecoveryConfig(email: String, question: String, answer: String?) {
        val encryptedEmail = cryptoManager.encryptString(email.trim())
        context.securityDataStore.edit { prefs ->
            prefs[KEY_RECOVERY_EMAIL_ENC] = encryptedEmail
            prefs[KEY_SECURITY_QUESTION] = question.trim()
            if (!answer.isNullOrBlank()) {
                val (hash, salt) = cryptoManager.hashSecret(answer.trim().lowercase())
                prefs[KEY_SECURITY_ANSWER_HASH] = hash
                prefs[KEY_SECURITY_ANSWER_SALT] = salt
            }
        }
    }

    suspend fun issueLocalRecoveryCode(): String {
        val code = cryptoManager.generateRecoveryCode()
        val (hash, salt) = cryptoManager.hashSecret(code)
        val expiry = System.currentTimeMillis() + RECOVERY_CODE_VALIDITY_MS
        context.securityDataStore.edit { prefs ->
            prefs[KEY_RECOVERY_CODE_HASH] = hash
            prefs[KEY_RECOVERY_CODE_SALT] = salt
            prefs[KEY_RECOVERY_CODE_EXPIRY] = expiry
        }
        return code
    }

    suspend fun verifyRecoveryCodeAndResetPin(code: String, newPin: String): Boolean {
        val prefs = context.securityDataStore.data.first()
        val hash = prefs[KEY_RECOVERY_CODE_HASH].orEmpty()
        val salt = prefs[KEY_RECOVERY_CODE_SALT].orEmpty()
        val expiry = prefs[KEY_RECOVERY_CODE_EXPIRY] ?: 0L
        if (System.currentTimeMillis() > expiry) return false
        val valid = cryptoManager.verifySecret(code.trim(), hash, salt)
        if (valid) {
            updatePin(newPin, isResetFromRecovery = true)
            context.securityDataStore.edit { mutable ->
                mutable.remove(KEY_RECOVERY_CODE_HASH)
                mutable.remove(KEY_RECOVERY_CODE_SALT)
                mutable.remove(KEY_RECOVERY_CODE_EXPIRY)
            }
        }
        return valid
    }

    suspend fun verifySecurityAnswerAndResetPin(answer: String, newPin: String): Boolean {
        val prefs = context.securityDataStore.data.first()
        val hash = prefs[KEY_SECURITY_ANSWER_HASH].orEmpty()
        val salt = prefs[KEY_SECURITY_ANSWER_SALT].orEmpty()
        val valid = cryptoManager.verifySecret(answer.trim().lowercase(), hash, salt)
        if (valid) {
            updatePin(newPin, isResetFromRecovery = true)
        }
        return valid
    }

    suspend fun getSettingsSnapshot(): SecuritySettingsState = settingsFlow.first()
}
