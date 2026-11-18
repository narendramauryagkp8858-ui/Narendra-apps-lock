package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AppLockerScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryLogScreen
import com.example.ui.screens.IntruderDetectionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AppLockGradients
import com.example.ui.theme.CyanShield
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import kotlinx.serialization.Serializable

@Serializable
object DashboardRoute

@Serializable
object AppLockerRoute

@Serializable
object SettingsRoute

@Serializable
object IntrudersRoute

@Serializable
object HistoryRoute

private data class TopDestination(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topDestinations = listOf(
    TopDestination(DashboardRoute, "Dashboard", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
    TopDestination(AppLockerRoute, "App Lock", Icons.Rounded.Lock, Icons.Outlined.Lock, "nav_applock"),
    TopDestination(SettingsRoute, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun NarendraAppLockRoot(
    mainViewModel: MainViewModel
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }

    val posture by mainViewModel.securityPosture.collectAsStateWithLifecycle()
    val settings by mainViewModel.settingsState.collectAsStateWithLifecycle()
    val permissions by mainViewModel.permissionHealth.collectAsStateWithLifecycle()
    val allApps by mainViewModel.allApps.collectAsStateWithLifecycle()
    val filteredApps by mainViewModel.filteredApps.collectAsStateWithLifecycle()
    val searchQuery by mainViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by mainViewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedCategory by mainViewModel.selectedCategory.collectAsStateWithLifecycle()
    val securityEvents by mainViewModel.securityEvents.collectAsStateWithLifecycle()
    val intruderAlerts by mainViewModel.intruderAlerts.collectAsStateWithLifecycle()
    val issuedRecoveryCode by mainViewModel.issuedRecoveryCode.collectAsStateWithLifecycle()
    val toastMessage by mainViewModel.toastMessage.collectAsStateWithLifecycle()

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            mainViewModel.clearToast()
        }
    }

    fun navigateTop(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppLockGradients.ScreenBackground)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MidnightNavy.copy(alpha = 0.96f),
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        topDestinations.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTop(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TextPrimary,
                                    selectedTextColor = CyanShield,
                                    indicatorColor = RoyalPurple.copy(alpha = 0.35f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = DeepNavy.copy(alpha = 0.94f),
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        topDestinations.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTop(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = TextPrimary,
                                    selectedTextColor = CyanShield,
                                    indicatorColor = RoyalPurple.copy(alpha = 0.35f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = DashboardRoute
                    ) {
                        composable<DashboardRoute> {
                            DashboardScreen(
                                posture = posture,
                                settings = settings,
                                permissions = permissions,
                                onToggleProtection = mainViewModel::setProtectionEnabled,
                                onRefreshPermissions = mainViewModel::refreshPermissions,
                                onNavigateToApps = { navigateTop(AppLockerRoute) },
                                onNavigateToIntruders = { navController.navigate(IntrudersRoute) },
                                onNavigateToHistory = { navController.navigate(HistoryRoute) },
                                onNavigateToSettings = { navigateTop(SettingsRoute) },
                                onLockAppNow = mainViewModel::lockNow
                            )
                        }

                        composable<AppLockerRoute> {
                            AppLockerScreen(
                                apps = filteredApps,
                                permissions = permissions,
                                protectionEnabled = settings.protectionEnabled,
                                searchQuery = searchQuery,
                                selectedFilter = selectedFilter,
                                selectedCategory = selectedCategory,
                                onSearchChange = mainViewModel::setSearchQuery,
                                onFilterChange = mainViewModel::setFilter,
                                onCategoryChange = mainViewModel::setCategory,
                                onToggleAppLock = mainViewModel::toggleAppLock,
                                onLockOrUnlockAll = mainViewModel::lockOrUnlockAllVisible,
                                onEnableProtection = { mainViewModel.setProtectionEnabled(true) },
                                onRefreshApps = mainViewModel::refreshApps
                            )
                        }

                        composable<SettingsRoute> {
                            SettingsScreen(
                                settings = settings,
                                issuedRecoveryCode = issuedRecoveryCode,
                                onUpdatePin = mainViewModel::updatePin,
                                onSavePattern = mainViewModel::savePattern,
                                onSetPrimaryLockMethod = mainViewModel::setPrimaryLockMethod,
                                onSetAutoLockPolicy = mainViewModel::setAutoLockPolicy,
                                onSetShowSystemApps = mainViewModel::setShowSystemApps,
                                onUpdateRecoveryConfig = mainViewModel::updateRecoveryConfig,
                                onSendRecoveryEmail = { mainViewModel.generateAndSendRecoveryEmail(context) },
                                onResetPinWithCode = mainViewModel::resetPinWithRecoveryCode,
                                onResetPinWithAnswer = mainViewModel::resetPinWithSecurityAnswer,
                                onOpenIntruders = { navController.navigate(IntrudersRoute) },
                                onOpenHistory = { navController.navigate(HistoryRoute) }
                            )
                        }

                        composable<IntrudersRoute> {
                            IntruderDetectionScreen(
                                alerts = intruderAlerts,
                                settings = settings,
                                permissions = permissions,
                                loadIntruderPhoto = mainViewModel::loadIntruderPhoto,
                                onUpdateIntruderSettings = mainViewModel::setIntruderSettings,
                                onRefreshPermissions = mainViewModel::refreshPermissions,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<HistoryRoute> {
                            HistoryLogScreen(
                                events = securityEvents,
                                onClearHistory = mainViewModel::clearSecurityHistory,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
