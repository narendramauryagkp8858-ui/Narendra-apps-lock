package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AutoLockPolicy
import com.example.data.model.LockMethod
import com.example.data.model.SecuritySettingsState
import com.example.ui.components.GlassSecurityCard
import com.example.ui.components.PasswordRecoverySheet
import com.example.ui.components.PatternLockCanvas
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanShield
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SecureEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    settings: SecuritySettingsState,
    issuedRecoveryCode: String?,
    onUpdatePin: (String) -> Unit,
    onSavePattern: (List<Int>) -> Unit,
    onSetPrimaryLockMethod: (LockMethod) -> Unit,
    onSetAutoLockPolicy: (AutoLockPolicy) -> Unit,
    onSetShowSystemApps: (Boolean) -> Unit,
    onUpdateRecoveryConfig: (String, String, String?) -> Unit,
    onSendRecoveryEmail: () -> Unit,
    onResetPinWithCode: (String, String, (Boolean) -> Unit) -> Unit,
    onResetPinWithAnswer: (String, String, (Boolean) -> Unit) -> Unit,
    onOpenIntruders: () -> Unit,
    onOpenHistory: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showPatternDialog by remember { mutableStateOf(false) }
    var showRecoveryConfigDialog by remember { mutableStateOf(false) }
    var showRecoveryResetSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                }
                Column {
                    Text("Security & Settings", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                    Text("Credentials, Auto-Lock, Recovery & Privacy", style = MaterialTheme.typography.bodyMedium, color = CyanShield)
                }
            }
        }

        // Security Modules Shortcuts
        item {
            Row(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenIntruders,
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("open_intruders_button")
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Intruders")
                }
                OutlinedButton(
                    onClick = onOpenHistory,
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("open_history_button")
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("History")
                }
            }
        }

        // Lock Credentials
        item {
            GlassSecurityCard(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                Text("Authentication", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showChangePinDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("change_pin_button")
                    ) {
                        Icon(Icons.Rounded.Pin, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change PIN")
                    }
                    OutlinedButton(
                        onClick = { showPatternDialog = true },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("setup_pattern_button")
                    ) {
                        Icon(Icons.Rounded.GridOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (settings.hasPatternSet) "Edit Pattern" else "Set Pattern")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Primary Lock Method", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                LockMethod.entries.forEach { method ->
                    val selected = settings.primaryLockMethod == method
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElevatedNavy.copy(alpha = 0.65f))
                            .clickable { onSetPrimaryLockMethod(method) }
                            .padding(12.dp)
                            .testTag("lock_method_${method.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(method.label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        Icon(
                            imageVector = if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = method.label,
                            tint = if (selected) CyanShield else TextSecondary
                        )
                    }
                }
            }
        }

        // Auto-Lock & Unlock Policies
        item {
            GlassSecurityCard(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                Text("Auto-Lock & Unlock Settings", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Control when unlocked apps re-engage the Narendra AppLock shield.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                AutoLockPolicy.entries.forEach { policy ->
                    val selected = settings.autoLockPolicy == policy
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) RoyalPurple.copy(alpha = 0.22f) else ElevatedNavy.copy(alpha = 0.6f))
                            .border(1.dp, if (selected) RoyalPurple else BorderSubtle, RoundedCornerShape(12.dp))
                            .clickable { onSetAutoLockPolicy(policy) }
                            .padding(12.dp)
                            .testTag("autolock_${policy.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(policy.label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        Icon(
                            imageVector = if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = policy.label,
                            tint = if (selected) CyanShield else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show System Apps in Locker", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Include pre-installed system apps in App Locker list", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    Switch(
                        checked = settings.showSystemApps,
                        onCheckedChange = onSetShowSystemApps,
                        colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = RoyalPurple),
                        modifier = Modifier.testTag("show_system_apps_switch")
                    )
                }
            }
        }

        // Email Password Recovery
        item {
            GlassSecurityCard(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Email, contentDescription = null, tint = CyanShield)
                    Column {
                        Text("Registered Email Recovery", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text(settings.recoveryEmail, style = MaterialTheme.typography.bodyMedium, color = CyanShield)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showRecoveryConfigDialog = true },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("edit_recovery_email_button")
                    ) {
                        Text("Configure Email")
                    }
                    Button(
                        onClick = { showRecoveryResetSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("trigger_recovery_button")
                    ) {
                        Text("Recover PIN")
                    }
                }
            }
        }

        // Developer & Privacy Attribution
        item {
            GlassSecurityCard(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                borderColor = SecureEmerald.copy(alpha = 0.5f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = SecureEmerald)
                    Text("Developer & Privacy Charter", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("App: Narendra AppLock", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Text("Developer: Narendra Maurya", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("Support Email: narendramauryagkp@gmail.com", style = MaterialTheme.typography.bodyMedium, color = CyanShield)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = SecureEmerald, modifier = Modifier.size(18.dp))
                    Text(
                        text = "All PINs, patterns, intruder photos, and vault media remain encrypted locally on your device. Zero personal data is collected or uploaded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecureEmerald
                    )
                }
            }
        }
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            onSave = { newPin ->
                onUpdatePin(newPin)
                showChangePinDialog = false
            },
            onDismiss = { showChangePinDialog = false }
        )
    }

    if (showPatternDialog) {
        SetupPatternDialog(
            onSave = { nodes ->
                onSavePattern(nodes)
                showPatternDialog = false
            },
            onDismiss = { showPatternDialog = false }
        )
    }

    if (showRecoveryConfigDialog) {
        ConfigureRecoveryDialog(
            currentEmail = settings.recoveryEmail,
            currentQuestion = settings.securityQuestion,
            onSave = { email, question, answer ->
                onUpdateRecoveryConfig(email, question, answer)
                showRecoveryConfigDialog = false
            },
            onDismiss = { showRecoveryConfigDialog = false }
        )
    }

    if (showRecoveryResetSheet) {
        PasswordRecoverySheet(
            recoveryEmail = settings.recoveryEmail,
            securityQuestion = settings.securityQuestion,
            hasSecurityAnswer = settings.hasSecurityAnswer,
            issuedRecoveryCode = issuedRecoveryCode,
            onSendEmailCode = onSendRecoveryEmail,
            onVerifyCodeAndReset = onResetPinWithCode,
            onVerifyAnswerAndReset = onResetPinWithAnswer,
            onDismiss = { showRecoveryResetSheet = false }
        )
    }
}

@Composable
private fun ChangePinDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightNavy,
        title = { Text("Set New 4-Digit PIN", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = pin1,
                    onValueChange = { if (it.length <= 4) pin1 = it.filter(Char::isDigit) },
                    label = { Text("New 4-digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_pin_field_1")
                )
                OutlinedTextField(
                    value = pin2,
                    onValueChange = { if (it.length <= 4) pin2 = it.filter(Char::isDigit) },
                    label = { Text("Confirm 4-digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_pin_field_2")
                )
                if (error != null) {
                    Text(error!!, color = AlertCrimson, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        pin1.length != 4 -> error = "PIN must be exactly 4 digits."
                        pin1 != pin2 -> error = "PINs do not match."
                        else -> onSave(pin1)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                modifier = Modifier.testTag("save_new_pin_button")
            ) {
                Text("Save PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SetupPatternDialog(
    onSave: (List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var firstPattern by remember { mutableStateOf<List<Int>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightNavy,
        title = {
            Text(
                text = if (firstPattern == null) "Draw New Pattern" else "Confirm Pattern",
                color = TextPrimary
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (firstPattern == null) {
                        "Connect at least 4 dots."
                    } else {
                        "Draw the same pattern again to confirm."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                PatternLockCanvas(
                    isError = error != null,
                    onPatternComplete = { nodes ->
                        if (nodes.size < 4) {
                            error = "Connect at least 4 nodes."
                        } else if (firstPattern == null) {
                            firstPattern = nodes
                            error = null
                        } else if (firstPattern == nodes) {
                            onSave(nodes)
                        } else {
                            error = "Patterns did not match. Try again."
                            firstPattern = null
                        }
                    }
                )
                if (error != null) {
                    Text(error!!, color = AlertCrimson, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ConfigureRecoveryDialog(
    currentEmail: String,
    currentQuestion: String,
    onSave: (String, String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf(currentEmail) }
    var question by remember { mutableStateOf(currentQuestion) }
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightNavy,
        title = { Text("Configure Recovery Email", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Registered Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Security Question") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Security Answer (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = AlertCrimson)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!email.contains("@") || !email.contains(".")) {
                        error = "Enter a valid email address."
                    } else {
                        onSave(email, question, answer.takeIf { it.isNotBlank() })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
