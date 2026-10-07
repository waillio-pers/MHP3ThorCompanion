package com.waillio.mhp3rdcompanion

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import kotlin.math.floor
import kotlin.math.roundToInt

internal enum class AppLayoutProfile {
    COMPACT_PORTRAIT,
    COMPACT_LANDSCAPE,
    MEDIUM,
    EXPANDED
}

internal data class AppWindowLayout(
    val widthDp: Int,
    val heightDp: Int,
    val profile: AppLayoutProfile
)

/** Classifies the live app window. A square-ish Thor remains in the accepted medium profile. */
internal fun appWindowLayout(widthDp: Int, heightDp: Int): AppWindowLayout {
    val width = widthDp.coerceAtLeast(0)
    val height = heightDp.coerceAtLeast(0)
    val profile = when {
        width < 600 && height >= width -> AppLayoutProfile.COMPACT_PORTRAIT
        width < 600 || (width < 1000 && height < 480) -> AppLayoutProfile.COMPACT_LANDSCAPE
        width >= 1000 -> AppLayoutProfile.EXPANDED
        else -> AppLayoutProfile.MEDIUM
    }
    return AppWindowLayout(width, height, profile)
}

/** Width-derived column count with profile caps to retain the accepted Thor geometry. */
internal fun responsiveGridColumns(
    availableWidthDp: Int,
    profile: AppLayoutProfile,
    minimumCardWidthDp: Int = 176
): Int {
    val profileLimit = when (profile) {
        AppLayoutProfile.COMPACT_PORTRAIT -> 2
        AppLayoutProfile.COMPACT_LANDSCAPE -> 3
        AppLayoutProfile.MEDIUM -> 2
        AppLayoutProfile.EXPANDED -> 5
    }
    return floor(availableWidthDp.coerceAtLeast(1).toDouble() / minimumCardWidthDp.coerceAtLeast(1))
        .toInt().coerceIn(1, profileLimit)
}

internal fun weaponChooserColumns(availableWidthDp: Int, profile: AppLayoutProfile): Int {
    val minimumCardWidth = if (profile == AppLayoutProfile.COMPACT_PORTRAIT) 158 else 210
    val profileLimit = when (profile) {
        AppLayoutProfile.COMPACT_PORTRAIT -> 2
        AppLayoutProfile.COMPACT_LANDSCAPE, AppLayoutProfile.MEDIUM -> 3
        AppLayoutProfile.EXPANDED -> 4
    }
    return (availableWidthDp.coerceAtLeast(1) / minimumCardWidth).coerceIn(1, profileLimit)
}

internal fun monsterRewardRowsStacked(profile: AppLayoutProfile): Boolean =
    profile == AppLayoutProfile.COMPACT_PORTRAIT

/** Narrow phone rows need a second line so weapon names do not compete with fixed stats columns. */
internal fun weaponRowsStacked(windowWidthDp: Int): Boolean = windowWidthDp <= 380

internal val LocalAppWindowLayout = staticCompositionLocalOf {
    appWindowLayout(827, 720)
}

/** Kept separate from the presentation density so UI Scale cannot reclassify the window. */
internal val LocalUnscaledDensity = staticCompositionLocalOf { Density(1f) }

@Composable
internal fun AdaptiveLayoutRoot(content: @Composable () -> Unit) {
    val platformDensity = LocalUnscaledDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layout = appWindowLayout(
            widthDp = (constraints.maxWidth / platformDensity.density).roundToInt(),
            heightDp = (constraints.maxHeight / platformDensity.density).roundToInt()
        )
        CompositionLocalProvider(LocalAppWindowLayout provides layout) {
            Box(
                Modifier.fillMaxSize()
                    .semantics { contentDescription = "adaptive-window-${layout.widthDp}x${layout.heightDp}-dp" }
                    .testTag("adaptive-profile-${layout.profile.name.lowercase()}")
            ) {
                content()
            }
        }
    }
}
