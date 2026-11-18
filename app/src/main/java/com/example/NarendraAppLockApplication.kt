package com.example

import android.app.Application
import com.example.data.local.AppLockDatabase
import com.example.data.repository.InstalledAppsRepository
import com.example.data.repository.SecurityPreferencesRepository
import com.example.data.repository.VaultRepository
import com.example.security.CryptoManager
import com.example.security.IntruderCameraHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NarendraAppLockApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val cryptoManager: CryptoManager by lazy { CryptoManager() }
    val database: AppLockDatabase by lazy { AppLockDatabase.getInstance(this) }
    val preferencesRepository: SecurityPreferencesRepository by lazy {
        SecurityPreferencesRepository(this, cryptoManager)
    }
    val appsRepository: InstalledAppsRepository by lazy {
        InstalledAppsRepository(this, database.lockedAppDao())
    }
    val vaultRepository: VaultRepository by lazy {
        VaultRepository(this, database.vaultItemDao(), cryptoManager)
    }
    val intruderCameraHelper: IntruderCameraHelper by lazy {
        IntruderCameraHelper(this, cryptoManager)
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            preferencesRepository.ensureDefaultPinInitialized()
        }
    }
}
