package com.example.ui.screens

import android.Manifest
import android.os.Build
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PermissionHealth
import com.example.data.model.SecurityPosture
import com.example.data.model.SecuritySettingsState
import com.example.service.PermissionStatusHelper
import com.example.ui.components.GlassSecurityCard
import com.example.ui.components.PermissionConsentDialog
import com.example.ui.components.SecurityScoreGauge
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.AppLockGradients
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanShield
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SecureEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun DashboardScreen(
    posture: SecurityPosture,
    settings: SecuritySettingsState,
    permissions: PermissionHealth,
    onToggleProtection: (Boolean) -> Unit,
    onRefreshPermissions: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToIntruders: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLockAppNow: () -> Unit
) {
    val context = LocalContext.current
    var showUsageConsent by remember { mutableStateOf(false) }
    var showAccessibilityConsent by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { onRefreshPermissions() }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 720.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Narendra AppLock",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Zero-Upload On-Device Security",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyanShield
                        )
                    }
                    OutlinedButton(
                        onClick = onLockAppNow,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("lock_now_button")
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lock")
                    }
                }
            }
        }

        // Hero Security Score Card
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(AppLockGradients.HeroCardGradient)
                    .border(1.dp, BorderGlow.copy(alpha = 0.7f), RoundedCornerShape(26.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_security_vault_hero),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.22f)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    DeepNavy.copy(alpha = 0.88f),
                                    DeepNavy.copy(alpha = 0.55f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SecurityScoreGauge(score = posture.score)

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = posture.statusTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = posture.statusSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElevatedNavy.copy(alpha = 0.78f))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AdminPanelSettings,
                                contentDescription = null,
                                tint = if (settings.protectionEnabled) SecureEmerald else WarningAmber
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AppLock Protection Guard",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (settings.protectionEnabled) "Active background monitoring" else "Guard paused",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = settings.protectionEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissions.hasNotificationPermission) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                onToggleProtection(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = RoyalPurple
                            ),
                            modifier = Modifier.testTag("protection_master_switch")
                        )
                    }
                }
            }
        }

        // Default Demo PIN Advisory Banner
        if (posture.isUsingDefaultDemoPin) {
            item {
                GlassSecurityCard(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth()
                        .clickable { onNavigateToSettings() }
                        .testTag("default_pin_warning_card"),
                    borderColor = WarningAmber.copy(alpha = 0.7f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = "Default PIN warning",
                            tint = WarningAmber
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Default Demo PIN (8546) Active",
                                style = MaterialTheme.typography.titleMedium,
                                color = WarningAmber
                            )
                            Text(
                                text = "Tap to set your own private PIN or Pattern in Settings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Metrics Grid (Locked Apps, Intruder Alerts, Auto-Lock)
        item {
            Column(
                modifier = Modifier.widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatTile(
                        title = "Locked Apps",
                        value = posture.lockedAppsCount.toString(),
                        subtitle = "Protected",
                        icon = Icons.Rounded.Lock,
                        accent = RoyalPurple,
                        onClick = onNavigateToApps,
                        modifier = Modifier.weight(1f).testTag("tile_locked_apps")
                    )
                    DashboardStatTile(
                        title = "Intruder Alerts",
                        value = posture.intruderEventsCount.toString(),
                        subtitle = if (settings.intruderSelfieEnabled) "Selfie active" else "Logs only",
                        icon = Icons.Rounded.CameraAlt,
                        accent = if (posture.intruderEventsCount > 0) AlertCrimson else SecureEmerald,
                        onClick = onNavigateToIntruders,
                        modifier = Modifier.weight(1f).testTag("tile_intruder_alerts")
                    )
                }
                DashboardStatTile(
                    title = "Auto-Lock",
                    value = when (settings.autoLockPolicy) {
                        com.example.data.model.AutoLockPolicy.IMMEDIATE -> "Instant"
                        com.example.data.model.AutoLockPolicy.SECONDS_30 -> "30s"
                        com.example.data.model.AutoLockPolicy.MINUTE_1 -> "1m"
                        com.example.data.model.AutoLockPolicy.MINUTES_5 -> "5m"
                        com.example.data.model.AutoLockPolicy.SCREEN_OFF -> "Screen"
                    },
                    subtitle = settings.autoLockPolicy.label,
                    icon = Icons.Rounded.Timer,
                    accent = ElectricViolet,
                    onClick = onNavigateToSettings,
                    modifier = Modifier.fillMaxWidth().testTag("tile_auto_lock")
                )
            }
        }

        // Permission Health & Consent Card
        item {
            GlassSecurityCard(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "System Guard Permissions",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Grant only the system guard permissions you choose. Requested only with explicit consent.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                PermissionRow(
                    title = "Usage Access Guard",
                    subtitle = "Detects foreground app launches locally",
                    isGranted = permissions.hasUsageStatsAccess,
                    icon = Icons.Rounded.Security,
                    onGrantClick = { showUsageConsent = true },
                    testTag = "grant_usage_access_button"
                )
                Spacer(modifier = Modifier.height(10.dp))
                PermissionRow(
                    title = "Accessibility Instant Guard",
                    subtitle = "Instant lock overlay without reading screen content",
                    isGranted = permissions.hasAccessibilityEnabled,
                    icon = Icons.Rounded.AccessibilityNew,
                    onGrantClick = { showAccessibilityConsent = true },
                    testTag = "grant_accessibility_button"
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissions.hasNotificationPermission) {
                    Spacer(modifier = Modifier.height(10.dp))
                    PermissionRow(
                        title = "Guard Status Notification",
                        subtitle = "Keeps background protection reliable",
                        isGranted = false,
                        icon = Icons.Rounded.NotificationsActive,
                        onGrantClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        testTag = "grant_notifications_button"
                    )
                }
            }
        }

        // Quick Tools Row
        item {
            GlassSecurityCard(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Security Tools",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionPill(
                        label = "History Log",
                        icon = Icons.Rounded.History,
                        onClick = onNavigateToHistory,
                        modifier = Modifier.weight(1f).testTag("quick_history")
                    )
                    QuickActionPill(
                        label = "Settings",
                        icon = Icons.Rounded.Settings,
                        onClick = onNavigateToSettings,
                        modifier = Modifier.weight(1f).testTag("quick_settings")
                    )
                }
            }
        }
    }

    if (showUsageConsent) {
        PermissionConsentDialog(
            title = "Usage Access Permission",
            explanation = "Narendra AppLock uses Usage Access solely to detect which protected app has opened in the foreground so it can display your PIN, Pattern, or Fingerprint lock screen.",
            privacyPromise = "Privacy Guarantee: App usage history never leaves your device and is never collected or uploaded.",
            confirmLabel = "Open Usage Settings",
            onConfirm = {
                showUsageConsent = false
                PermissionStatusHelper.openUsageAccessSettings(context)
            },
            onDismiss = { showUsageConsent = false }
        )
    }

    if (showAccessibilityConsent) {
        PermissionConsentDialog(
            title = "Accessibility Service Disclosure",
            explanation = "Narendra AppLock offers an optional Accessibility Guard to detect window transitions instantaneously when a locked app opens.",
            privacyPromise = "Strict Zero-Read Policy: Window content retrieval is disabled in system XML. We never read text, passwords, messages, or personal screen contents.",
            confirmLabel = "Open Accessibility Settings",
            onConfirm = {
                showAccessibilityConsent = false
                PermissionStatusHelper.openAccessibilitySettings(context)
            },
            onDismiss = { showAccessibilityConsent = false }
        )
    }
}

@Composable
private fun DashboardStatTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSecurityCard(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(20.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    icon: ImageVector,
    onGrantClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ElevatedNavy.copy(alpha = 0.65f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGranted) SecureEmerald else CyanShield
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        if (isGranted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Granted",
                    tint = SecureEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Active", color = SecureEmerald, style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.minimumInteractiveComponentSize().testTag(testTag)
            ) {
                Text("Enable")
            }
        }
    }
}

@Composable
private fun QuickActionPill(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ElevatedNavy.copy(alpha = 0.72f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = CyanShield, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
    }
}
