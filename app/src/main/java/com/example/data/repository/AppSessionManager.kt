package com.example.data.repository

import com.example.data.model.AutoLockPolicy
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks temporary unlocked app packages according to AutoLockPolicy,
 * screen-off events, and active lock gate presentation.
 */
object AppSessionManager {
    private val unlockedUntilMs = ConcurrentHashMap<String, Long>()

    @Volatile
    var activeForegroundPackage: String? = null

    @Volatile
    var isLockGateShowing: Boolean = false

    fun markAppUnlocked(packageName: String, policy: AutoLockPolicy) {
        val expiry = when (policy) {
            AutoLockPolicy.IMMEDIATE -> System.currentTimeMillis() + 5_000L // brief grace while app launches
            AutoLockPolicy.SCREEN_OFF -> Long.MAX_VALUE
            else -> System.currentTimeMillis() + policy.timeoutMs
        }
        unlockedUntilMs[packageName] = expiry
    }

    fun isAppTemporarilyUnlocked(packageName: String, policy: AutoLockPolicy): Boolean {
        val expiry = unlockedUntilMs[packageName] ?: return false
        val now = System.currentTimeMillis()
        if (policy == AutoLockPolicy.IMMEDIATE) {
            // If user already left the package or grace passed, lock again
            return activeForegroundPackage == packageName || now < expiry
        }
        if (now >= expiry) {
            unlockedUntilMs.remove(packageName)
            return false
        }
        return true
    }

    fun onForegroundAppChanged(newPackage: String, policy: AutoLockPolicy) {
        val previous = activeForegroundPackage
        activeForegroundPackage = newPackage
        if (policy == AutoLockPolicy.IMMEDIATE && previous != null && previous != newPackage) {
            // Re-lock previous app immediately when user leaves it
            unlockedUntilMs.remove(previous)
        }
    }

    fun onScreenTurnedOff() {
        unlockedUntilMs.clear()
        activeForegroundPackage = null
    }

    fun clearAllUnlocked() {
        unlockedUntilMs.clear()
    }
}
