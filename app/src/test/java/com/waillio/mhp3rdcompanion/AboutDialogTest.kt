package com.waillio.mhp3rdcompanion

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w827dp-h720dp-land-xhdpi")
class AboutDialogTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun aboutShowsBuildMetadataRepositoryCreditsScrollableSourcesAndFanNote() {
        val preferences = rule.activity.getSharedPreferences(UI_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val originalStep = preferences.getInt(UI_SCALE_STEP_KEY, UI_SCALE_DEFAULT_STEP)
        try {
            listOf(0, 4, 6).forEach { step ->
                setScale(step)
                rule.onNodeWithTag("settings-button").performClick()
                rule.onNodeWithTag("settings-about").performClick()

                rule.onNodeWithTag("about-version").assertIsDisplayed().assertTextEquals("Version ${BuildConfig.VERSION_NAME}")
                rule.onNodeWithTag("about-repository-link").assertIsDisplayed()
                rule.onNodeWithTag("about-sources-heading").performScrollTo().assertIsDisplayed()
                rule.onNodeWithTag("about-scroll-content").performScrollToNode(hasTestTag("about-source-link-17"))
                rule.onNodeWithTag("about-source-link-17").assertIsDisplayed()
                rule.onNodeWithTag("about-scroll-content").performScrollToNode(hasTestTag("about-fan-disclaimer"))
                rule.onNodeWithTag("about-fan-disclaimer").assertIsDisplayed()
                rule.onNodeWithTag("about-done").assertIsDisplayed().performClick()
                rule.waitForIdle()
            }
        } finally {
            preferences.edit().putInt(UI_SCALE_STEP_KEY, originalStep).commit()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()
        }

        assertEquals(18, ABOUT_SOURCE_CREDITS.size)
        assertEquals("1.0.0", BuildConfig.VERSION_NAME)
        assertEquals(2, BuildConfig.VERSION_CODE)
        assertTrue(ABOUT_SOURCE_CREDITS.all { it.url.startsWith("https://") || it.url.startsWith("http://") })
        assertTrue(ABOUT_SOURCE_CREDITS.any { it.name.contains("TMO PSP v6.1 (r754)") })
        assertTrue(ABOUT_SOURCE_CREDITS.any { it.url == "https://github.com/gaugustini/monster-hunter-armor-data" })
        assertTrue(ABOUT_SOURCE_CREDITS.any { it.url == "https://monhammer.com/" })
        assertTrue(ABOUT_SOURCE_CREDITS.any { it.url == "https://game-cap.com/mhp3rd/data/monstlist.html" })
        assertTrue(ABOUT_SOURCE_CREDITS.any { it.url.contains("mhp3db.github.io/tree/7ad3cf1c5ba07ce2e63afdaa1fa37c4671edc3bf") })
        assertEquals("English names and translation references used throughout the app.", ABOUT_SOURCE_CREDITS[0].description)
        assertEquals("Monster data, maps, gathering spots, farm data, quests, items, and other game information.", ABOUT_SOURCE_CREDITS[1].description)
        assertEquals("Creator and rights holder of Monster Hunter Portable 3rd and its original game assets.", ABOUT_SOURCE_CREDITS[17].description)
        assertTrue(ABOUT_SOURCE_CREDITS.all { it.linkLabel in setOf("Visit website", "Open source", "Open on GitHub") })
    }

    @Test
    fun settingsAboutActionOpensDedicatedDialogAndKeepsDoneReachable() {
        rule.onNodeWithTag("settings-button").performClick()
        rule.onNodeWithTag("settings-about").assertIsDisplayed().performClick()
        rule.onNodeWithTag("about-sources-heading").assertIsDisplayed()
        rule.onNodeWithTag("about-done").assertIsDisplayed()
    }

    @Test
    fun aboutExternalLinksUseAndroidViewIntentAndTheRepositoryTarget() {
        val intent = aboutViewIntent(ABOUT_REPOSITORY_URL)

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(Uri.parse(ABOUT_REPOSITORY_URL), intent.data)
        assertEquals("https://github.com/waillio-pers/MHP3ThorCompanion", intent.dataString)
    }

    private fun setScale(step: Int) {
        rule.onNodeWithTag("settings-button").performClick()
        rule.onNodeWithTag("ui-scale-slider")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(step.toFloat()) }
        rule.onNodeWithTag("settings-scale-value", useUnmergedTree = true)
            .assertTextEquals("${(80 + step * 5) / 100}.${((80 + step * 5) % 100).toString().padStart(2, '0')}")
        rule.onNodeWithTag("settings-done").performClick()
        rule.waitForIdle()
    }
}
