package com.waillio.mhp3rdcompanion

import android.graphics.Color as AndroidColor
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewParent
import android.view.Window
import android.view.WindowManager
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal const val OLED_SCREEN_SAVER_TIMEOUT_KEY = "oled_screen_saver_timeout_minutes"
internal const val OLED_SCREEN_SAVER_LAST_DURATION_KEY = "oled_screen_saver_last_duration_minutes"
internal val OLED_SCREEN_SAVER_TIMEOUT_OPTIONS = listOf(0, 1, 2, 5, 10, 15)

internal fun normalizeOledScreenSaverTimeoutMinutes(minutes: Int): Int =
    minutes.takeIf { it in OLED_SCREEN_SAVER_TIMEOUT_OPTIONS } ?: 0

internal fun oledScreenSaverTimeoutMillis(minutes: Int): Long =
    normalizeOledScreenSaverTimeoutMinutes(minutes) * 60_000L

internal fun readOledScreenSaverTimeoutMinutes(preferences: android.content.SharedPreferences): Int =
    normalizeOledScreenSaverTimeoutMinutes(preferences.getInt(OLED_SCREEN_SAVER_TIMEOUT_KEY, 0))

/** Migrates the legacy active timeout into a retained duration without changing enabled state. */
internal fun readOledScreenSaverLastDurationMinutes(preferences: android.content.SharedPreferences): Int {
    val activeDuration = readOledScreenSaverTimeoutMinutes(preferences)
    if (activeDuration > 0) {
        preferences.edit { putInt(OLED_SCREEN_SAVER_LAST_DURATION_KEY, activeDuration) }
        return activeDuration
    }
    val saved = normalizeOledScreenSaverTimeoutMinutes(preferences.getInt(OLED_SCREEN_SAVER_LAST_DURATION_KEY, 0))
    if (saved > 0) return saved
    val migrated = 1
    preferences.edit { putInt(OLED_SCREEN_SAVER_LAST_DURATION_KEY, migrated) }
    return migrated
}

internal fun persistOledScreenSaverTimeoutMinutes(
    preferences: android.content.SharedPreferences,
    minutes: Int
) {
    preferences.edit {
        val normalized = normalizeOledScreenSaverTimeoutMinutes(minutes)
        putInt(OLED_SCREEN_SAVER_TIMEOUT_KEY, normalized)
        if (normalized > 0) putInt(OLED_SCREEN_SAVER_LAST_DURATION_KEY, normalized)
    }
}

/** One lifecycle-bound delay is restarted by each genuine input/configuration event. */
internal class InactivityScreenSaverController(
    scope: CoroutineScope,
    initialTimeoutMinutes: Int = 0,
    private val timeoutMillis: (Int) -> Long = ::oledScreenSaverTimeoutMillis
) {
    private data class TimerInput(val timeoutMinutes: Int, val resumed: Boolean, val generation: Long)

    private val timerInput = MutableStateFlow(
        TimerInput(normalizeOledScreenSaverTimeoutMinutes(initialTimeoutMinutes), resumed = false, generation = 0L)
    )
    private val _saverActive = MutableStateFlow(false)
    val saverActive: StateFlow<Boolean> = _saverActive.asStateFlow()
    init {
        scope.launch {
            timerInput.collectLatest { input ->
                _saverActive.value = false
                if (!input.resumed || input.timeoutMinutes == 0) return@collectLatest

                delay(timeoutMillis(input.timeoutMinutes))
                if (timerInput.value == input) _saverActive.value = true
            }
        }
    }

    fun setTimeoutMinutes(minutes: Int) {
        val normalized = normalizeOledScreenSaverTimeoutMinutes(minutes)
        if (normalized == 0) _saverActive.value = false
        timerInput.value = timerInput.value.copy(
            timeoutMinutes = normalized,
            generation = timerInput.value.generation + 1L
        )
    }

    fun onUserActivity() {
        timerInput.value = timerInput.value.copy(generation = timerInput.value.generation + 1L)
    }

    fun onForegroundResumed() {
        _saverActive.value = false
        timerInput.value = timerInput.value.copy(resumed = true, generation = timerInput.value.generation + 1L)
    }

    fun onForegroundPaused() {
        _saverActive.value = false
        timerInput.value = timerInput.value.copy(resumed = false, generation = timerInput.value.generation + 1L)
    }

    fun wake() {
        _saverActive.value = false
        onUserActivity()
    }
}

internal val LocalUserActivityReporter = staticCompositionLocalOf<() -> Unit> { {} }

/** Use for app Dialogs: Activity dispatch alone cannot observe a Dialog's separate Window. */
@Composable
internal fun TrackedDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    val reporter = LocalUserActivityReporter.current
    Dialog(onDismissRequest = { reporter(); onDismissRequest() }, properties = properties) {
        Box(modifier = Modifier.dialogUserActivityModifier(reporter), propagateMinConstraints = true) {
            DialogWindowActivityTracker()
            content()
        }
    }
}

@Composable
internal fun Modifier.dialogUserActivityModifier(): Modifier =
    dialogUserActivityModifier(LocalUserActivityReporter.current)

private fun Modifier.dialogUserActivityModifier(reporter: () -> Unit): Modifier = pointerInput(reporter) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        reporter()
        waitForUpOrCancellation()
    }
}

/** Installs a Window.Callback decorator so taps, drags, scrolls, and keys inside Dialogs count. */
@Composable
internal fun DialogWindowActivityTracker() {
    val reporter = LocalUserActivityReporter.current
    val view = LocalView.current
    val window = view.findDialogWindow()

    DisposableEffect(window, reporter) {
        val originalCallback = window?.callback
        if (window == null || originalCallback == null) {
            onDispose { }
        } else {
            val trackedCallback = object : Window.Callback by originalCallback {
                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                    if (event.action == KeyEvent.ACTION_DOWN) reporter()
                    return originalCallback.dispatchKeyEvent(event)
                }

                override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
                    reporter()
                    return originalCallback.dispatchGenericMotionEvent(event)
                }
            }
            window.callback = trackedCallback
            onDispose {
                if (window.callback === trackedCallback) window.callback = originalCallback
            }
        }
    }
}

private fun View.findDialogWindow(): Window? {
    var parent: ViewParent? = this.parent
    while (parent != null) {
        if (parent is DialogWindowProvider) return parent.window
        parent = parent.parent
    }
    return null
}

@Composable
internal fun OledScreenSaverDialog(onWake: () -> Unit) {
    Dialog(
        onDismissRequest = onWake,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        val window = view.findDialogWindow()
        DisposableEffect(window) {
            if (window != null) {
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                window.setBackgroundDrawable(AndroidColor.BLACK.toDrawable())
                window.statusBarColor = AndroidColor.BLACK
                window.navigationBarColor = AndroidColor.BLACK
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                    window.isStatusBarContrastEnforced = false
                }
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    hide(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
            onDispose { }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("oled-screen-saver")
                .semantics { contentDescription = "Screen saver active. Tap to wake." }
                .pointerInput(onWake) { detectTapGestures(onTap = { onWake() }) }
        )
    }
}
