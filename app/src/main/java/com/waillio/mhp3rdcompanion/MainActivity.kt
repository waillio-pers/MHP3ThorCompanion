package com.waillio.mhp3rdcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            val platformDensity = LocalDensity.current
            val uiScale = uiScaleFromStep(scaleStep.intValue)
            val presentationDensity = Density(
                density = platformDensity.density * uiScale,
                fontScale = platformDensity.fontScale
            )
            CompositionLocalProvider(
                LocalDensity provides presentationDensity,
                LocalUnscaledDensity provides platformDensity
            ) {
                AdaptiveLayoutRoot {
                    CompanionTheme {
                        CompanionApp(
                            viewModel = viewModel,
                            uiScaleStep = scaleStep.intValue,
                            onUiScaleStepChange = { step ->
                                val safeStep = step.coerceIn(UI_SCALE_MIN_STEP, UI_SCALE_MAX_STEP)
                                scaleStep.intValue = safeStep
                                getSharedPreferences(UI_SETTINGS_PREFS, MODE_PRIVATE)
                                    .edit()
                                    .putInt(UI_SCALE_STEP_KEY, safeStep)
                                    .apply()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    private fun enterImmersiveMode() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

internal const val UI_SETTINGS_PREFS = "ui_settings"
internal const val UI_SCALE_STEP_KEY = "interface_scale_step"
internal const val UI_SCALE_MIN_STEP = 0
internal const val UI_SCALE_MAX_STEP = 6
internal const val UI_SCALE_DEFAULT_STEP = 4

internal fun uiScaleFromStep(step: Int): Float = 0.80f + 0.05f * step.coerceIn(UI_SCALE_MIN_STEP, UI_SCALE_MAX_STEP)
