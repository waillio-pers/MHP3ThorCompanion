package com.waillio.mhp3rdcompanion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

abstract class AdaptivePublicReleaseSmokeBase {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    protected abstract val screenshotProfile: String?
    protected abstract val expectedWindowDp: Pair<Int, Int>

    @Test
    fun representativeNavigationFitsThisWindowAndScaleRange() {
        rule.waitForIdle()
        rule.onNodeWithTag("adaptive-profile-${expectedProfileTag()}").assertExists()
        rule.onNodeWithContentDescription("adaptive-window-${expectedWindowDp.first}x${expectedWindowDp.second}-dp")
            .assertExists()
        saveScreenshot("01-home.png")

        rule.onNodeWithTag("category-maps").performClick()
        rule.onNodeWithTag("maps-index").assertIsDisplayed()
        rule.onNodeWithTag("map-card-misty_peaks").performClick()
        rule.onNodeWithTag("map-node-mp-a3-bamboo-shoot-north", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        val hasAmbiguityChooser = rule.onAllNodesWithTag("map-tap-chooser").fetchSemanticsNodes().isNotEmpty()
        if (hasAmbiguityChooser) {
            rule.onNodeWithTag("map-tap-chooser").assertIsDisplayed()
            rule.onNodeWithTag("map-tap-chooser-row-mp-a3-bamboo-shoot-north").performClick()
        }
        rule.onNodeWithTag("map-node-detail-modal").assertIsDisplayed()
        rule.onNodeWithTag("map-node-detail-close").performClick()
        rule.onNodeWithTag("map-back").performClick()

        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-monster").performScrollTo().performClick()
        rule.onNodeWithTag("monster-grid")
            .performScrollToNode(hasTestTag("monster-card-monster_agnaktor"))
        saveScreenshot("02-large-monster-index.png")
        rule.onNodeWithTag("monster-card-monster_agnaktor").performClick()
        rule.onNodeWithTag("monster-tab-1").performClick()
        rule.onNodeWithTag("monster-rewards").assertIsDisplayed()
        saveScreenshot("03-monster-rewards.png")

        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-material").performScrollTo().performClick()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val material = CompanionRepository(context).data.materials
            .filter { it.sources.isNotEmpty() }
            .maxBy { it.sources.size }
        val materialTag = "result-material-${material.id}"
        rule.onNodeWithTag("search-results").performScrollToNode(hasTestTag(materialTag))
        rule.onNodeWithTag(materialTag).performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
        rule.onNodeWithTag("item-sources-section").performScrollTo()
        saveScreenshot("04-dense-item-sources.png")

        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-weapons").performScrollTo().performClick()
        rule.onNodeWithTag("weapons-chooser").assertIsDisplayed()
        saveScreenshot("05-weapon-type-chooser.png")
        rule.onNodeWithTag("weapon-type-great-sword").performClick()
        rule.onNodeWithTag("weapon-expand-weapon_great_sword_001").performClick()
        rule.onNodeWithTag("weapon-row-weapon_great_sword_002").performClick()
        rule.onNodeWithTag("weapon-detail-modal").assertIsDisplayed()
        rule.onNodeWithTag("weapon-detail-close").performClick()
        rule.onNodeWithTag("weapon-mode-attack").performClick()
        rule.onNodeWithTag("weapon-mode-rarity").performClick()
        rule.onNodeWithTag("weapon-filters-open").performClick()
        rule.onNodeWithTag("weapon-filters-done").performClick()

        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-skill").performScrollTo().performClick()
        rule.onNodeWithTag("skills-browser").assertIsDisplayed()
        rule.onNodeWithTag("field-filter-quest").performScrollTo().performClick()
        rule.onNodeWithTag("quest-browser").assertIsDisplayed()

        rule.onNodeWithTag("settings-button").performClick()
        rule.onNodeWithTag("ui-scale-slider").assertIsDisplayed()
        saveScreenshot("06-settings.png")
        rule.onNodeWithTag("settings-about").performClick()
        rule.onNodeWithTag("about-dialog").assertIsDisplayed()
        rule.onNodeWithTag("about-done").assertIsDisplayed()
        rule.onNodeWithTag("about-done").performClick()
        rule.onNodeWithTag("settings-button").performClick()

        listOf(0, 4, 6).forEach { step ->
            rule.onNodeWithTag("ui-scale-slider").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(step.toFloat()) }
            rule.onNodeWithTag("settings-done").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("global-search").assertIsDisplayed()
            rule.onNodeWithTag("settings-button").assertIsDisplayed()
            rule.onNodeWithTag("nav-home").assertIsDisplayed()
            rule.onNodeWithTag("nav-search").assertIsDisplayed()
            rule.onNodeWithTag("nav-favorites").assertIsDisplayed()
            if (step != 6) rule.onNodeWithTag("settings-button").performClick()
        }
        rule.runOnUiThread { rule.activity.recreate() }
        rule.waitForIdle()
        rule.onNodeWithTag("adaptive-profile-${expectedProfileTag()}").assertExists()
        rule.onNodeWithTag("quest-browser").assertIsDisplayed()
    }

    private fun expectedProfileTag(): String = when (screenshotProfile) {
        "small-portrait", "large-portrait", null -> "compact_portrait"
        "phone-landscape" -> "compact_landscape"
        "thor" -> "medium"
        "expanded" -> "expanded"
        else -> error("Unknown adaptive test profile")
    }

    private fun saveScreenshot(fileName: String) {
        val profile = screenshotProfile ?: return
        val root = File(System.getProperty("user.dir"), "build/reports/adaptive-public-release-review/$profile")
        assertTrue("Could not create screenshot output directory: $root", root.mkdirs() || root.isDirectory)
        rule.waitForIdle()
        val bitmap = rule.activity.window.decorView.let { decor ->
            require(decor.width > 0 && decor.height > 0) { "Window has no laid out pixels for $profile screenshot" }
            Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888).also { target ->
                rule.runOnUiThread { decor.draw(Canvas(target)) }
            }
        }
        FileOutputStream(File(root, fileName)).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AdaptiveSmallPortraitSmokeTest : AdaptivePublicReleaseSmokeBase() {
    override val screenshotProfile = "small-portrait"
    override val expectedWindowDp = 360 to 800
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AdaptiveLargePortraitSmokeTest : AdaptivePublicReleaseSmokeBase() {
    override val screenshotProfile: String? = null
    override val expectedWindowDp = 412 to 915
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w800dp-h360dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AdaptivePhoneLandscapeSmokeTest : AdaptivePublicReleaseSmokeBase() {
    override val screenshotProfile = "phone-landscape"
    override val expectedWindowDp = 800 to 360
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w827dp-h720dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AdaptiveThorSmokeTest : AdaptivePublicReleaseSmokeBase() {
    override val screenshotProfile = "thor"
    override val expectedWindowDp = 827 to 720
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1280dp-h800dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AdaptiveExpandedSmokeTest : AdaptivePublicReleaseSmokeBase() {
    override val screenshotProfile = "expanded"
    override val expectedWindowDp = 1280 to 800
}
