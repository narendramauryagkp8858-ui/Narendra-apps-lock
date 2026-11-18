
package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.AppLockMonitorService
import com.example.service.PermissionStatusHelper
import com.example.ui.navigation.NarendraAppLockRoot
import com.example.ui.screens.AuthGateScreen
import com.example.ui.theme.NarendraAppLockTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : FragmentActivity() {

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application as NarendraAppLockApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NarendraAppLockTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                val isUnlocked by mainViewModel.isUnlocked.collectAsStateWithLifecycle()
                val settings by mainViewModel.settingsState.collectAsStateWithLifecycle()
                val authError by mainViewModel.authError.collectAsStateWithLifecycle()
                val issuedRecoveryCode by mainViewModel.issuedRecoveryCode.collectAsStateWithLifecycle()

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            mainViewModel.refreshPermissions()
                            mainViewModel.refreshApps()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                LaunchedEffect(settings.protectionEnabled) {
                    if (settings.protectionEnabled &&
                        PermissionStatusHelper.hasUsageStatsPermission(this@MainActivity)
                    ) {
                        AppLockMonitorService.start(this@MainActivity)
                    }
                }

                Crossfade(
                    targetState = isUnlocked,
                    label = "authGateTransition"
                ) { unlocked ->
                    if (!unlocked) {
                        AuthGateScreen(
                            settings = settings,
                            authError = authError,
                            issuedRecoveryCode = issuedRecoveryCode,
                            onVerifyPin = mainViewModel::verifyPinAndUnlock,
                            onVerifyPattern = mainViewModel::verifyPatternAndUnlock,
                            onBiometricSuccess = mainViewModel::onBiometricUnlockSuccess,
                            onIntruderPhotoCaptured = { path ->
                                mainViewModel.recordIntruderSelfie(path, packageName)
                            },
                            onSendRecoveryEmail = {
                                mainViewModel.generateAndSendRecoveryEmail(this@MainActivity)
                            },
                            onResetPinWithCode = mainViewModel::resetPinWithRecoveryCode,
                            onResetPinWithAnswer = mainViewModel::resetPinWithSecurityAnswer,
                            onClearError = mainViewModel::clearAuthError
                        )
                    } else {
                        NarendraAppLockRoot(
                            mainViewModel = mainViewModel
                        )
                    }
                }
            }
        }
    }
}
