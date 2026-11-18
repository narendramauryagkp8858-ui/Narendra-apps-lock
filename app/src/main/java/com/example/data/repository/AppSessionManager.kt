package com.example.data.repository

import com.example.data.model.AutoLockPolicy
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks temporary unlocked app packages according to AutoLockPolicy,
 * screen-off events, and active lock gate presentation.
 */
object AppSessionManager {
    private const val LAUNCH_PENDING_TIMEOUT_MS = 1_500L

    private val unlockedUntilMs = ConcurrentHashMap<String, Long>()

    @Volatile
    var activeForegroundPackage: String? = null

    @Volatile
    private var lockGateActive: Boolean = false

    @Volatile
    private var pendingLaunchUntilMs: Long = 0L

    var isLockGateShowing: Boolean
        get() = lockGateActive || System.currentTimeMillis() < pendingLaunchUntilMs
        set(value) {
            lockGateActive = value
            if (!value) {
                pendingLaunchUntilMs = 0L
            }
        }

    fun markLockGateLaunching() {
        pendingLaunchUntilMs = System.currentTimeMillis() + LAUNCH_PENDING_TIMEOUT_MS
    }

    fun onLockGateVisible() {
        lockGateActive = true
        pendingLaunchUntilMs = 0L
    }

    fun onLockGateHidden() {
        lockGateActive = false
        pendingLaunchUntilMs = 0L
    }

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

    fun onAppTaskRemoved(ownPackage: String) {
        onLockGateHidden()
        if (activeForegroundPackage == ownPackage) {
            activeForegroundPackage = null
        }
    }

    fun onScreenTurnedOff() {
        unlockedUntilMs.clear()
        activeForegroundPackage = null
        onLockGateHidden()
    }

    fun clearAllUnlocked() {
        unlockedUntilMs.clear()
    }
}
