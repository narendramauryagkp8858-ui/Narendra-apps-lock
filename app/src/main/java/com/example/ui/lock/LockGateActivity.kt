package com.example.ui.lock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.NarendraAppLockApplication
import com.example.data.local.SecurityEventEntity
import com.example.data.model.LockMethod
import com.example.data.model.SecurityEventType
import com.example.data.model.SecuritySettingsState
import com.example.data.repository.AppSessionManager
import com.example.ui.components.DrawableAppIcon
import com.example.ui.components.PatternLockCanvas
import com.example.ui.components.PinPadView
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.AppLockGradients
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.CyanShield
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.NarendraAppLockTheme
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

class LockGateActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_TARGET_PACKAGE = "extra_target_package"
        private const val EXTRA_IS_PREVIEW = "extra_is_preview"

        fun createIntent(context: Context, targetPackage: String, isPreview: Boolean): Intent {
            return Intent(context, LockGateActivity::class.java).apply {
                putExtra(EXTRA_TARGET_PACKAGE, targetPackage)
                putExtra(EXTRA_IS_PREVIEW, isPreview)
            }
        }
    }

    @Volatile
    private var unlockedAndFinishing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: packageName
        val isPreview = intent.getBooleanExtra(EXTRA_IS_PREVIEW, false)
        val app = applicationContext as NarendraAppLockApplication
        val targetLabel = app.appsRepository.getAppLabel(targetPackage)
        val targetIcon = app.appsRepository.getAppIcon(targetPackage)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AppSessionManager.onLockGateHidden()
                if (isPreview) {
                    finish()
                } else {
                    val home = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(home)
                    finish()
                }
            }
        })

        setContent {
            NarendraAppLockTheme {
                val settings by app.preferencesRepository.settingsFlow
                    .collectAsStateWithLifecycle(initialValue = SecuritySettingsState())
                val scope = rememberCoroutineScope()

                var enteredPin by remember { mutableStateOf("") }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                var failedCount by remember { mutableIntStateOf(0) }
                var activeMethod by remember(settings.primaryLockMethod, settings.hasPatternSet) {
                    mutableStateOf(
                        if (settings.primaryLockMethod == LockMethod.PATTERN && settings.hasPatternSet) {
                            LockMethod.PATTERN
                        } else {
                            LockMethod.PIN
                        }
                    )
                }

                fun completeUnlock(methodLabel: String) {
                    unlockedAndFinishing = true
                    scope.launch {
                        AppSessionManager.markAppUnlocked(targetPackage, settings.autoLockPolicy)
                        AppSessionManager.onLockGateHidden()
                        app.database.lockedAppDao().updateLastUnlocked(targetPackage, System.currentTimeMillis())
                        app.database.lockedAppDao().incrementLockCount(targetPackage)
                        app.database.securityEventDao().insert(
                            SecurityEventEntity(
                                eventType = SecurityEventType.AUTH_SUCCESS,
                                title = "Unlocked $targetLabel",
                                detail = "Verified via $methodLabel",
                                targetPackage = targetPackage
                            )
                        )
                        finish()
                    }
                }

                fun handleFailedAttempt() {
                    val next = failedCount + 1
                    failedCount = next
                    errorMessage = "Incorrect credentials ($next attempt${if (next > 1) "s" else ""})"

                    scope.launch {
                        if (next >= settings.intruderThreshold) {
                            if (settings.intruderSelfieEnabled) {
                                app.intruderCameraHelper.captureIntruderPhoto(
                                    lifecycleOwner = this@LockGateActivity,
                                    onCaptured = { encryptedPath ->
                                        scope.launch {
                                            app.database.securityEventDao().insert(
                                                SecurityEventEntity(
                                                    eventType = SecurityEventType.INTRUDER_ALERT,
                                                    title = "Intruder Alert on $targetLabel",
                                                    detail = "$next failed attempts triggered camera capture",
                                                    targetPackage = targetPackage,
                                                    intruderPhotoPath = encryptedPath
                                                )
                                            )
                                        }
                                    },
                                    onError = {
                                        scope.launch {
                                            app.database.securityEventDao().insert(
                                                SecurityEventEntity(
                                                    eventType = SecurityEventType.INTRUDER_ALERT,
                                                    title = "Intruder Alert on $targetLabel",
                                                    detail = "$next failed attempts detected",
                                                    targetPackage = targetPackage
                                                )
                                            )
                                        }
                                    }
                                )
                            } else {
                                app.database.securityEventDao().insert(
                                    SecurityEventEntity(
                                        eventType = SecurityEventType.INTRUDER_ALERT,
                                        title = "Intruder Alert on $targetLabel",
                                        detail = "$next failed attempts detected",
                                        targetPackage = targetPackage
                                    )
                                )
                            }
                        } else {
                            app.database.securityEventDao().insert(
                                SecurityEventEntity(
                                    eventType = SecurityEventType.AUTH_FAILED,
                                    title = "Failed Unlock on $targetLabel",
                                    detail = "Invalid attempt #$next",
                                    targetPackage = targetPackage
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppLockGradients.ScreenBackground)
                        .safeDrawingPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(ElevatedNavy)
                                    .border(1.5.dp, BorderGlow, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                DrawableAppIcon(
                                    drawable = targetIcon,
                                    contentDescription = targetLabel,
                                    size = 56.dp
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = targetLabel,
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = CyanShield,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPreview) "AppLock Gate Preview" else "Protected by Narendra AppLock",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }

                            if (settings.hasPatternSet) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = activeMethod == LockMethod.PIN,
                                        onClick = { activeMethod = LockMethod.PIN; errorMessage = null },
                                        label = { Text("PIN") },
                                        leadingIcon = { Icon(Icons.Rounded.Pin, contentDescription = null) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = RoyalPurple,
                                            selectedLabelColor = TextPrimary
                                        )
                                    )
                                    FilterChip(
                                        selected = activeMethod == LockMethod.PATTERN,
                                        onClick = { activeMethod = LockMethod.PATTERN; errorMessage = null },
                                        label = { Text("Pattern") },
                                        leadingIcon = { Icon(Icons.Rounded.GridOn, contentDescription = null) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = RoyalPurple,
                                            selectedLabelColor = TextPrimary
                                        )
                                    )
                                }
                            }
                        }

                        if (activeMethod == LockMethod.PIN) {
                            PinPadView(
                                enteredPin = enteredPin,
                                errorMessage = errorMessage,
                                onDigitClick = { digit ->
                                    errorMessage = null
                                    if (enteredPin.length < 4) {
                                        val nextPin = enteredPin + digit
                                        enteredPin = nextPin
                                        if (nextPin.length == 4) {
                                            scope.launch {
                                                val ok = app.preferencesRepository.verifyPin(nextPin)
                                                enteredPin = ""
                                                if (ok) completeUnlock("PIN") else handleFailedAttempt()
                                            }
                                        }
                                    }
                                },
                                onBackspace = {
                                    if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                }
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (errorMessage != null) {
                                    Text(errorMessage!!, color = AlertCrimson)
                                }
                                PatternLockCanvas(
                                    isError = errorMessage != null,
                                    onPatternComplete = { nodes ->
                                        scope.launch {
                                            val ok = app.preferencesRepository.verifyPattern(nodes)
                                            if (ok) completeUnlock("Pattern") else handleFailedAttempt()
                                        }
                                    }
                                )
                            }
                        }

                        if (isPreview) {
                            TextButton(
                                onClick = {
                                    AppSessionManager.onLockGateHidden()
                                    finish()
                                }
                            ) {
                                Text("Exit Preview", color = TextSecondary)
                            }
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppSessionManager.onLockGateVisible()
    }

    override fun onStop() {
        super.onStop()
        AppSessionManager.onLockGateHidden()
        if (!unlockedAndFinishing && !isChangingConfigurations && !isFinishing) {
            finish()
        }
    }

    override fun onDestroy() {
        AppSessionManager.onLockGateHidden()
        super.onDestroy()
    }
}
