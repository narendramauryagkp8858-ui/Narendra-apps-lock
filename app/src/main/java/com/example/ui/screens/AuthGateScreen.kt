package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.NarendraAppLockApplication
import com.example.R
import com.example.data.model.LockMethod
import com.example.data.model.SecuritySettingsState
import com.example.ui.components.PasswordRecoverySheet
import com.example.ui.components.PatternLockCanvas
import com.example.ui.components.PinPadView
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.AppLockGradients
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.CyanShield
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AuthGateScreen(
    settings: SecuritySettingsState,
    authError: String?,
    issuedRecoveryCode: String?,
    onVerifyPin: (String, () -> Unit) -> Unit,
    onVerifyPattern: (List<Int>, () -> Unit) -> Unit,
    onIntruderPhotoCaptured: (String) -> Unit,
    onSendRecoveryEmail: () -> Unit,
    onResetPinWithCode: (String, String, (Boolean) -> Unit) -> Unit,
    onResetPinWithAnswer: (String, String, (Boolean) -> Unit) -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val app = context.applicationContext as? NarendraAppLockApplication

    var activeMethod by remember(settings.primaryLockMethod, settings.hasPatternSet) {
        mutableStateOf(
            if (settings.primaryLockMethod == LockMethod.PATTERN && settings.hasPatternSet) {
                LockMethod.PATTERN
            } else {
                LockMethod.PIN
            }
        )
    }
    var enteredPin by remember { mutableStateOf("") }
    var showRecoverySheet by remember { mutableStateOf(false) }

    val triggerIntruderCapture = remember(settings.intruderSelfieEnabled, app, lifecycleOwner) {
        {
            if (settings.intruderSelfieEnabled && app != null) {
                app.intruderCameraHelper.captureIntruderPhoto(
                    lifecycleOwner = lifecycleOwner,
                    onCaptured = onIntruderPhotoCaptured,
                    onError = { /* logged cleanly even without camera */ }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(2.dp, BorderGlow, CircleShape)
                        .background(ElevatedNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_lock_icon_1794978319143),
                        contentDescription = "Narendra AppLock Shield",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Narendra AppLock",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enter master credentials to access vault & controls",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                if (settings.isDefaultDemoPin) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(RoyalPurple.copy(alpha = 0.22f))
                            .border(1.dp, RoyalPurple.copy(alpha = 0.6f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Default Demo PIN: 8546",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.labelLarge,
                            color = CyanShield
                        )
                    }
                }

                if (settings.hasPatternSet) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = activeMethod == LockMethod.PIN,
                            onClick = { activeMethod = LockMethod.PIN; onClearError() },
                            label = { Text("PIN") },
                            leadingIcon = { Icon(Icons.Rounded.Pin, contentDescription = null) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalPurple,
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("auth_mode_pin")
                        )
                        FilterChip(
                            selected = activeMethod == LockMethod.PATTERN,
                            onClick = { activeMethod = LockMethod.PATTERN; onClearError() },
                            label = { Text("Pattern") },
                            leadingIcon = { Icon(Icons.Rounded.GridOn, contentDescription = null) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalPurple,
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("auth_mode_pattern")
                        )
                    }
                }
            }

            if (activeMethod == LockMethod.PIN) {
                PinPadView(
                    enteredPin = enteredPin,
                    errorMessage = authError,
                    onDigitClick = { digit ->
                        onClearError()
                        if (enteredPin.length < 4) {
                            val updated = enteredPin + digit
                            enteredPin = updated
                            if (updated.length == 4) {
                                onVerifyPin(updated, triggerIntruderCapture)
                                enteredPin = ""
                            }
                        }
                    },
                    onBackspace = {
                        if (enteredPin.isNotEmpty()) {
                            enteredPin = enteredPin.dropLast(1)
                        }
                    }
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (authError != null) {
                        Text(
                            text = authError,
                            color = AlertCrimson,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    PatternLockCanvas(
                        isError = authError != null,
                        onPatternComplete = { nodes ->
                            onVerifyPattern(nodes, triggerIntruderCapture)
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = { showRecoverySheet = true },
                    modifier = Modifier.testTag("forgot_pin_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Forgot PIN? Recover via Email", color = TextSecondary)
                }
            }
        }
    }

    if (showRecoverySheet) {
        PasswordRecoverySheet(
            recoveryEmail = settings.recoveryEmail,
            securityQuestion = settings.securityQuestion,
            hasSecurityAnswer = settings.hasSecurityAnswer,
            issuedRecoveryCode = issuedRecoveryCode,
            onSendEmailCode = onSendRecoveryEmail,
            onVerifyCodeAndReset = onResetPinWithCode,
            onVerifyAnswerAndReset = onResetPinWithAnswer,
            onDismiss = { showRecoverySheet = false }
        )
    }
}
