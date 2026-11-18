package com.example.ui.lock

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.FrameLayout
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Displays the Compose lock screen directly via WindowManager (TYPE_APPLICATION_OVERLAY)
 * when Display Over Other Apps is granted, bypassing Android 10-16 background activity
 * launch restrictions and the 5-second Home-button stopAppSwitches() cooldown.
 */
class LockOverlayWindow(
    private val context: Context
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var rootContainer: OverlayRootLayout? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    @Volatile
    var currentTargetPackage: String? = null
        private set

    val isShowing: Boolean
        get() = rootContainer != null

    fun show(targetPackage: String) {
        mainScope.launch {
            if (rootContainer != null) {
                if (currentTargetPackage == targetPackage) return@launch
                dismissImmediate()
            }

            val app = context.applicationContext as NarendraAppLockApplication
            val owner = OverlayLifecycleOwner()
            owner.performRestore()
            owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

            val container = OverlayRootLayout(context) {
                dismissAndGoHome()
            }.apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
            }

            val composeView = ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    NarendraAppLockTheme {
                        OverlayLockContent(
                            app = app,
                            lifecycleOwner = owner,
                            targetPackage = targetPackage,
                            onUnlocked = { dismissImmediate() },
                            onExitToHome = { dismissAndGoHome() }
                        )
                    }
                }
            }
            container.addView(
                composeView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                windowType,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )

            runCatching {
                windowManager.addView(container, params)
                owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
                owner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                rootContainer = container
                lifecycleOwner = owner
                currentTargetPackage = targetPackage
                AppSessionManager.onLockGateVisible()
            }.onFailure {
                runCatching {
                    owner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                    owner.viewModelStore.clear()
                }
                AppSessionManager.onLockGateHidden()
            }
        }
    }

    fun dismiss() {
        mainScope.launch {
            dismissImmediate()
        }
    }

    private fun dismissAndGoHome() {
        dismissImmediate()
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(home) }
    }

    private fun dismissImmediate() {
        val container = rootContainer ?: return
        val owner = lifecycleOwner
        rootContainer = null
        lifecycleOwner = null
        currentTargetPackage = null
        AppSessionManager.onLockGateHidden()

        runCatching {
            owner?.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            owner?.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            owner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            owner?.viewModelStore?.clear()
        }
        runCatching { windowManager.removeViewImmediate(container) }
    }

    private class OverlayRootLayout(
        context: Context,
        private val onBackPressed: () -> Unit
    ) : FrameLayout(context) {
        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                onBackPressed()
                return true
            }
            return super.dispatchKeyEvent(event)
        }
    }

    private class OverlayLifecycleOwner :
        LifecycleOwner,
        ViewModelStoreOwner,
        SavedStateRegistryOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private val store = ViewModelStore()
        private val savedStateController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        override val viewModelStore: ViewModelStore
            get() = store

        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry

        fun performRestore() {
            savedStateController.performRestore(null)
        }

        fun handleLifecycleEvent(event: Lifecycle.Event) {
            lifecycleRegistry.handleLifecycleEvent(event)
        }
    }
}

@Composable
private fun OverlayLockContent(
    app: NarendraAppLockApplication,
    lifecycleOwner: LifecycleOwner,
    targetPackage: String,
    onUnlocked: () -> Unit,
    onExitToHome: () -> Unit
) {
    val targetLabel = remember(targetPackage) { app.appsRepository.getAppLabel(targetPackage) }
    val targetIcon = remember(targetPackage) { app.appsRepository.getAppIcon(targetPackage) }
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
        AppSessionManager.markAppUnlocked(targetPackage, settings.autoLockPolicy)
        onUnlocked()
        scope.launch {
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
                        lifecycleOwner = lifecycleOwner,
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
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = onExitToHome,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Exit to Home",
                            tint = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
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
                        text = "Protected by Narendra AppLock",
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
