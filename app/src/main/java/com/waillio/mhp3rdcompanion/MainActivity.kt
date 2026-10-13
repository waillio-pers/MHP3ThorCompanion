package com.waillio.mhp3rdcompanion

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.edit
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()
    private lateinit var screenSaverController: InactivityScreenSaverController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences(UI_SETTINGS_PREFS, MODE_PRIVATE)
        screenSaverController = InactivityScreenSaverController(
            scope = lifecycleScope,
            initialTimeoutMinutes = readOledScreenSaverTimeoutMinutes(preferences)
        )
        readOledScreenSaverLastDurationMinutes(preferences)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enterImmersiveMode()
        setContent {
            val scaleStep = remember {
                mutableIntStateOf(
                    getSharedPreferences(UI_SETTINGS_PREFS, MODE_PRIVATE)
                        .getInt(UI_SCALE_STEP_KEY, UI_SCALE_DEFAULT_STEP)
                        .coerceIn(UI_SCALE_MIN_STEP, UI_SCALE_MAX_STEP)
                )
            }
            val fieldGuideSwipeEnabled = remember {
                androidx.compose.runtime.mutableStateOf(
                    getSharedPreferences(UI_SETTINGS_PREFS, MODE_PRIVATE)
                        .getBoolean(FIELD_GUIDE_SECTION_SWIPE_KEY, false)
                )
            }
            val screenSaverTimeoutMinutes = remember {
                mutableIntStateOf(readOledScreenSaverTimeoutMinutes(preferences))
            }
            val screenSaverLastDurationMinutes = remember {
                mutableIntStateOf(readOledScreenSaverLastDurationMinutes(preferences))
            }
            val homeShortcutSlots = remember {
                mutableStateOf(readHomeShortcutSlots(preferences))
            }
            val screenSaverActive by screenSaverController.saverActive.collectAsStateWithLifecycle()
            val platformDensity = LocalDensity.current
            val uiScale = uiScaleFromStep(scaleStep.intValue)
            val presentationDensity = Density(
                density = platformDensity.density * uiScale,
                fontScale = platformDensity.fontScale
            )
            CompositionLocalProvider(LocalUserActivityReporter provides screenSaverController::onUserActivity) {
                CompositionLocalProvider(
                    LocalDensity provides presentationDensity,
                    LocalUnscaledDensity provides platformDensity
                ) {
                    AdaptiveLayoutRoot {
                        CompanionTheme {
                            Box(Modifier.fillMaxSize()) {
                                CompanionApp(
                                    viewModel = viewModel,
                                    uiScaleStep = scaleStep.intValue,
                                    homeShortcutSlots = homeShortcutSlots.value,
                                    screenSaverLastDurationMinutes = screenSaverLastDurationMinutes.intValue,
                                    onHomeShortcutSlotsChange = { slots ->
                                        val safeSlots = normalizeHomeShortcutSlots(slots)
                                        homeShortcutSlots.value = safeSlots
                                        persistHomeShortcutSlots(preferences, safeSlots)
                                    },
                                    fieldGuideSwipeEnabled = fieldGuideSwipeEnabled.value,
                                    screenSaverTimeoutMinutes = screenSaverTimeoutMinutes.intValue,
                                    onScreenSaverTimeoutMinutesChange = { minutes ->
                                        val safeMinutes = normalizeOledScreenSaverTimeoutMinutes(minutes)
                                        screenSaverTimeoutMinutes.intValue = safeMinutes
                                        persistOledScreenSaverTimeoutMinutes(preferences, safeMinutes)
                                        if (safeMinutes > 0) screenSaverLastDurationMinutes.intValue = safeMinutes
                                        screenSaverController.setTimeoutMinutes(safeMinutes)
                                    },
                                    onFieldGuideSwipeEnabledChange = { enabled ->
                                        fieldGuideSwipeEnabled.value = enabled
                                        preferences.edit {
                                            putBoolean(FIELD_GUIDE_SECTION_SWIPE_KEY, enabled)
                                        }
                                    },
                                    onUiScaleStepChange = { step ->
                                        val safeStep = step.coerceIn(UI_SCALE_MIN_STEP, UI_SCALE_MAX_STEP)
                                        scaleStep.intValue = safeStep
                                        preferences.edit { putInt(UI_SCALE_STEP_KEY, safeStep) }
                                    }
                                )
                                if (screenSaverActive) {
                                    OledScreenSaverDialog(onWake = screenSaverController::wake)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    override fun onResume() {
        super.onResume()
        if (::screenSaverController.isInitialized) screenSaverController.onForegroundResumed()
    }

    override fun onPause() {
        if (::screenSaverController.isInitialized) screenSaverController.onForegroundPaused()
        super.onPause()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (::screenSaverController.isInitialized) {
            screenSaverController.onUserActivity()
        }
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (::screenSaverController.isInitialized) screenSaverController.onUserActivity()
        return super.dispatchGenericMotionEvent(event)
    }

    private fun enterImmersiveMode() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            if (shouldShowPhoneStatusBar(resources.configuration.screenWidthDp, resources.configuration.screenHeightDp)) {
                isAppearanceLightStatusBars = false
                show(WindowInsetsCompat.Type.statusBars())
            } else {
                hide(WindowInsetsCompat.Type.statusBars())
            }
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

internal const val UI_SETTINGS_PREFS = "ui_settings"
internal const val UI_SCALE_STEP_KEY = "interface_scale_step"
internal const val FIELD_GUIDE_SECTION_SWIPE_KEY = "field_guide_section_swipe"
internal const val UI_SCALE_MIN_STEP = 0
internal const val UI_SCALE_MAX_STEP = 6
internal const val UI_SCALE_DEFAULT_STEP = 4

internal fun uiScaleFromStep(step: Int): Float = 0.80f + 0.05f * step.coerceIn(UI_SCALE_MIN_STEP, UI_SCALE_MAX_STEP)
