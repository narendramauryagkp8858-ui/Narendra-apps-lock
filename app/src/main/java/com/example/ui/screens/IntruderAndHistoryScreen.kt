package com.example.ui.screens

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.SecurityEventEntity
import com.example.data.model.PermissionHealth
import com.example.data.model.SecurityEventType
import com.example.data.model.SecuritySettingsState
import com.example.ui.components.GlassSecurityCard
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanShield
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SecureEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IntruderDetectionScreen(
    alerts: List<SecurityEventEntity>,
    settings: SecuritySettingsState,
    permissions: PermissionHealth,
    loadIntruderPhoto: suspend (String) -> Bitmap?,
    onUpdateIntruderSettings: (Boolean, Int) -> Unit,
    onRefreshPermissions: () -> Unit,
    onBack: () -> Unit
) {
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            onRefreshPermissions()
            onUpdateIntruderSettings(granted, settings.intruderThreshold)
        }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column {
                    Text("Intruder Detection", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text("Captures unauthorized unlock attempts locally", style = MaterialTheme.typography.bodyMedium, color = CyanShield)
                }
            }
        }

        item {
            GlassSecurityCard(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Intruder Selfie Capture", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            text = if (permissions.hasCameraPermission) {
                                "Front camera captures photo encrypted inside Vault"
                            } else {
                                "Requires Camera permission when enabled"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = settings.intruderSelfieEnabled && permissions.hasCameraPermission,
                        onCheckedChange = { enabled ->
                            if (enabled && !permissions.hasCameraPermission) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                onUpdateIntruderSettings(enabled, settings.intruderThreshold)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = RoyalPurple),
                        modifier = Modifier.testTag("intruder_selfie_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Trigger after ${settings.intruderThreshold} failed attempt(s)",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyanShield
                )
                Slider(
                    value = settings.intruderThreshold.toFloat(),
                    onValueChange = { value ->
                        onUpdateIntruderSettings(settings.intruderSelfieEnabled, value.toInt().coerceIn(1, 5))
                    },
                    valueRange = 1f..5f,
                    steps = 3
                )
            }
        }

        if (alerts.isEmpty()) {
            item {
                GlassSecurityCard(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = SecureEmerald, modifier = Modifier.size(44.dp))
                        Text("No Intruder Attempts Recorded", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            text = "Failed unlock attempts exceeding your threshold will appear here with timestamps and encrypted snapshots.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(alerts, key = { it.id }) { alert ->
                IntruderAlertCard(alert = alert, loadIntruderPhoto = loadIntruderPhoto)
            }
        }
    }
}

@Composable
private fun IntruderAlertCard(
    alert: SecurityEventEntity,
    loadIntruderPhoto: suspend (String) -> Bitmap?
) {
    var photo by remember(alert.intruderPhotoPath) { mutableStateOf<Bitmap?>(null) }
    val path = alert.intruderPhotoPath
    LaunchedEffect(path) {
        if (!path.isNullOrBlank()) {
            photo = loadIntruderPhoto(path)
        }
    }

    GlassSecurityCard(
        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
        borderColor = AlertCrimson.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (photo != null) {
                Image(
                    bitmap = photo!!.asImageBitmap(),
                    contentDescription = "Captured intruder photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AlertCrimson.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = AlertCrimson)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(alert.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(alert.detail, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(alert.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = WarningAmber
                )
            }
        }
    }
}

@Composable
fun HistoryLogScreen(
    events: List<SecurityEventEntity>,
    onClearHistory: () -> Unit,
    onBack: () -> Unit
) {
    var selectedType by remember { mutableStateOf<SecurityEventType?>(null) }
    val filtered = remember(events, selectedType) {
        if (selectedType == null) events else events.filter { it.eventType == selectedType }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Column {
                        Text("App Lock History", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                        Text("${events.size} security audit records", style = MaterialTheme.typography.bodyMedium, color = CyanShield)
                    }
                }
                if (events.isNotEmpty()) {
                    OutlinedButton(onClick = onClearHistory, modifier = Modifier.testTag("clear_history_button")) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { selectedType = null },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalPurple, selectedLabelColor = TextPrimary)
                )
                FilterChip(
                    selected = selectedType == SecurityEventType.INTRUDER_ALERT,
                    onClick = {
                        selectedType = if (selectedType == SecurityEventType.INTRUDER_ALERT) null else SecurityEventType.INTRUDER_ALERT
                    },
                    label = { Text("Intruders") }
                )
                FilterChip(
                    selected = selectedType == SecurityEventType.APP_LOCKED,
                    onClick = {
                        selectedType = if (selectedType == SecurityEventType.APP_LOCKED) null else SecurityEventType.APP_LOCKED
                    },
                    label = { Text("Locked") }
                )
            }
        }

        if (filtered.isEmpty()) {
            item {
                GlassSecurityCard(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(44.dp))
                        Text("No Audit Logs Yet", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { event ->
                val accent = when (event.eventType) {
                    SecurityEventType.INTRUDER_ALERT, SecurityEventType.AUTH_FAILED -> AlertCrimson
                    SecurityEventType.AUTH_SUCCESS, SecurityEventType.APP_LOCKED -> SecureEmerald
                    else -> CyanShield
                }
                GlassSecurityCard(
                    modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                    borderColor = BorderSubtle
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (event.eventType) {
                                    SecurityEventType.INTRUDER_ALERT -> Icons.Rounded.Warning
                                    else -> Icons.Rounded.Lock
                                },
                                contentDescription = event.eventType.label,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(event.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(event.detail, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${event.eventType.label} • ${formatTimestamp(event.timestamp)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(ms: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(ms))
}
