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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DisguiseMode
import com.example.data.model.InstalledAppInfo
import com.example.data.model.PermissionHealth
import com.example.ui.components.DrawableAppIcon
import com.example.ui.components.GlassSecurityCard
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanShield
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SecureEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun AppHiderScreen(
    disguiseMode: DisguiseMode,
    apps: List<InstalledAppInfo>,
    permissions: PermissionHealth,
    onSelectDisguise: (DisguiseMode) -> Unit,
    onToggleHiddenShortcut: (InstalledAppInfo, Boolean) -> Unit,
    onLaunchHiddenApp: (String) -> Unit
) {
    val stealthApps = remember(apps) { apps.filter { it.isHiddenShortcut } }
    val userApps = remember(apps) { apps.filter { !it.isSystemApp } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                Text(
                    text = "App Hider & Disguise",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Camouflage launcher identity & manage Stealth App Vault",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyanShield
                )
            }
        }

        // Section 1: Real Supported Launcher Icon Disguise
        item {
            GlassSecurityCard(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()
            ) {
                Text(
                    text = "Disguise Narendra AppLock Icon",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Uses Android component aliases to camouflage this app's home screen icon & title.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DisguiseMode.entries.forEach { mode ->
                        val selected = mode == disguiseMode
                        val icon = when (mode) {
                            DisguiseMode.DEFAULT -> Icons.Rounded.Shield
                            DisguiseMode.CALCULATOR -> Icons.Rounded.Calculate
                            DisguiseMode.NOTES -> Icons.Rounded.EditNote
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) RoyalPurple.copy(alpha = 0.25f) else ElevatedNavy.copy(alpha = 0.65f))
                                .border(
                                    1.dp,
                                    if (selected) RoyalPurple else BorderSubtle,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectDisguise(mode) }
                                .padding(12.dp)
                                .testTag("disguise_option_${mode.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(ElevatedNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = mode.label, tint = CyanShield)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mode.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(mode.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Active disguise",
                                    tint = SecureEmerald
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Android OS Compatibility Transparency Card
        item {
            GlassSecurityCard(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                borderColor = WarningAmber.copy(alpha = 0.65f)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Android compatibility information",
                        tint = WarningAmber
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Android OS App-Hiding Compatibility",
                            style = MaterialTheme.typography.titleMedium,
                            color = WarningAmber
                        )
                        Text(
                            text = "• Device Status: ${if (permissions.isDeviceOwner) "Managed Device Owner (Full OS hiding supported)" else "Standard User Device (Non-Root / Non-Device Owner)"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Standard Android security sandboxes prevent third-party apps from removing other apps' launcher icons without Device Owner / Enterprise MDM or Root access. To protect sensitive apps without root, Narendra AppLock locks them behind PIN/Biometric AND lets you keep private quick-launch shortcuts inside this Stealth Vault.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Stealth Vault Active Shortcuts
        if (stealthApps.isNotEmpty()) {
            item {
                Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                    Text(
                        text = "Stealth Vault Shortcuts (${stealthApps.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }
            }

            items(stealthApps, key = { "stealth_${it.packageName}" }) { app ->
                GlassSecurityCard(
                    modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                    borderColor = CyanShield.copy(alpha = 0.65f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DrawableAppIcon(drawable = app.icon, contentDescription = app.appName, size = 42.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.appName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Protected inside Stealth Vault", style = MaterialTheme.typography.labelSmall, color = CyanShield)
                        }
                        IconButton(
                            onClick = { onLaunchHiddenApp(app.packageName) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = "Launch ${app.appName}",
                                tint = SecureEmerald
                            )
                        }
                        IconButton(
                            onClick = { onToggleHiddenShortcut(app, false) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Visibility,
                                contentDescription = "Remove ${app.appName} from stealth vault",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Toggle Apps into Stealth Vault
        item {
            Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                Text(
                    text = "Manage Stealth Apps",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Apps toggled below are automatically locked and pinned inside your Stealth Vault.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        items(userApps, key = { "manage_${it.packageName}" }) { app ->
            GlassSecurityCard(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DrawableAppIcon(drawable = app.icon, contentDescription = app.appName, size = 40.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = if (app.isHiddenShortcut) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = if (app.isHiddenShortcut) CyanShield else TextSecondary
                    )
                    Switch(
                        checked = app.isHiddenShortcut,
                        onCheckedChange = { hidden -> onToggleHiddenShortcut(app, hidden) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = RoyalPurple
                        ),
                        modifier = Modifier.testTag("stealth_switch_${app.packageName}")
                    )
                }
            }
        }
    }
}
