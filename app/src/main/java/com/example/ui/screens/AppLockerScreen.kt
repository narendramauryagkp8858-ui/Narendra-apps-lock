package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppCategory
import com.example.data.model.InstalledAppInfo
import com.example.data.model.PermissionHealth
import com.example.service.PermissionStatusHelper
import com.example.ui.components.DrawableAppIcon
import com.example.ui.components.GlassSecurityCard
import com.example.ui.components.PermissionConsentDialog
import com.example.ui.lock.LockGateActivity
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanShield
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SecureEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AppLockFilter

@Composable
fun AppLockerScreen(
    apps: List<InstalledAppInfo>,
    permissions: PermissionHealth,
    protectionEnabled: Boolean,
    searchQuery: String,
    selectedFilter: AppLockFilter,
    selectedCategory: AppCategory?,
    onSearchChange: (String) -> Unit,
    onFilterChange: (AppLockFilter) -> Unit,
    onCategoryChange: (AppCategory?) -> Unit,
    onToggleAppLock: (InstalledAppInfo, Boolean) -> Unit,
    onLockOrUnlockAll: (Boolean) -> Unit,
    onEnableProtection: () -> Unit,
    onRefreshApps: () -> Unit
) {
    val context = LocalContext.current
    val lockedCount = apps.count { it.isLocked }
    var showUsageConsent by remember { mutableStateOf(false) }
    var showOverlayConsent by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 720.dp)) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "App Locker",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "$lockedCount of ${apps.size} shown apps protected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyanShield
                    )
                }
                IconButton(
                    onClick = onRefreshApps,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("refresh_apps_button")
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh installed apps", tint = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!protectionEnabled || !permissions.hasUsageStatsAccess || !permissions.hasOverlayPermission || !permissions.isIgnoringBatteryOptimizations) {
                GlassSecurityCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("applocker_setup_banner"),
                    borderColor = WarningAmber.copy(alpha = 0.75f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = "Setup required",
                            tint = WarningAmber
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Guard Permissions to Lock Apps",
                                style = MaterialTheme.typography.titleMedium,
                                color = WarningAmber
                            )
                            Text(
                                text = "Grant Usage Access, Display Over Other Apps & Unrestricted Battery so locked apps stay locked reliably.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!protectionEnabled) {
                            Button(
                                onClick = onEnableProtection,
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("banner_enable_guard_button")
                            ) {
                                Text("Turn On Guard")
                            }
                        }
                        if (!permissions.hasUsageStatsAccess) {
                            Button(
                                onClick = { showUsageConsent = true },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("banner_grant_usage_button")
                            ) {
                                Text("Usage Access")
                            }
                        }
                        if (!permissions.hasOverlayPermission) {
                            Button(
                                onClick = { showOverlayConsent = true },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("banner_grant_overlay_button")
                            ) {
                                Text("Display Over Apps")
                            }
                        }
                        if (!permissions.isIgnoringBatteryOptimizations) {
                            Button(
                                onClick = { PermissionStatusHelper.openBatteryOptimizationSettings(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("banner_battery_unrestricted_button")
                            ) {
                                Text("Unrestricted Battery")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search installed apps or packages...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppLockFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalPurple,
                            selectedLabelColor = TextPrimary
                        ),
                        modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
                    )
                }
            }

            // Category Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategoryChange(null) },
                    label = { Text("All Categories") }
                )
                AppCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onCategoryChange(if (selectedCategory == cat) null else cat) },
                        label = { Text(cat.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Batch Lock/Unlock Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onLockOrUnlockAll(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("lock_all_apps_button")
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lock All")
                }
                OutlinedButton(
                    onClick = { onLockOrUnlockAll(false) },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("unlock_all_apps_button")
                ) {
                    Icon(Icons.Rounded.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Unlock All")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No matching applications found",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Try clearing your search or enabling System Apps in Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    GlassSecurityCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = if (app.isLocked) RoyalPurple.copy(alpha = 0.7f) else BorderSubtle
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DrawableAppIcon(
                                drawable = app.icon,
                                contentDescription = app.appName,
                                size = 46.dp
                            )

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
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ElevatedNavy)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = app.category.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyanShield
                                        )
                                    }
                                    if (app.isLocked) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(SecureEmerald.copy(alpha = 0.16f))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "LOCKED",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SecureEmerald
                                            )
                                        }
                                    }
                                }
                            }

                            if (app.isLocked) {
                                IconButton(
                                    onClick = {
                                        val intent = LockGateActivity.createIntent(
                                            context = context,
                                            targetPackage = app.packageName,
                                            isPreview = true
                                        )
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("preview_lock_${app.packageName}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Preview lock gate for ${app.appName}",
                                        tint = CyanShield
                                    )
                                }
                            }

                            Switch(
                                checked = app.isLocked,
                                onCheckedChange = { locked -> onToggleAppLock(app, locked) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = RoyalPurple
                                ),
                                modifier = Modifier.testTag("lock_switch_${app.packageName}")
                            )
                        }
                    }
                }
            }
        }
    }

    if (showUsageConsent) {
        PermissionConsentDialog(
            title = "Usage Access Permission",
            explanation = "Narendra AppLock uses Usage Access solely to detect which protected app has opened in the foreground so it can display your PIN or Pattern lock screen.",
            privacyPromise = "Privacy Guarantee: App usage history never leaves your device and is never collected or uploaded.",
            confirmLabel = "Open Usage Settings",
            onConfirm = {
                showUsageConsent = false
                PermissionStatusHelper.openUsageAccessSettings(context)
            },
            onDismiss = { showUsageConsent = false }
        )
    }

    if (showOverlayConsent) {
        PermissionConsentDialog(
            title = "Display Over Other Apps",
            explanation = "Android requires Display Over Other Apps permission so Narendra AppLock can display the PIN or Pattern lock overlay immediately whenever you open a protected app.",
            privacyPromise = "Privacy Guarantee: Used strictly to display your lock screen over protected apps.",
            confirmLabel = "Open Overlay Settings",
            onConfirm = {
                showOverlayConsent = false
                PermissionStatusHelper.openOverlaySettings(context)
            },
            onDismiss = { showOverlayConsent = false }
        )
    }
}
