package com.waillio.mhp3rdcompanion

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FieldGuideSwipeSettingsUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun settingDefaultsOffTakesEffectImmediatelyPersistsAndSurvivesRecreation() {
        val preferences = rule.activity.getSharedPreferences(UI_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val originalHadValue = preferences.contains(FIELD_GUIDE_SECTION_SWIPE_KEY)
        val originalValue = preferences.getBoolean(FIELD_GUIDE_SECTION_SWIPE_KEY, false)
        try {
            preferences.edit().remove(FIELD_GUIDE_SECTION_SWIPE_KEY).commit()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()

            rule.onNodeWithTag("settings-button").performClick()
            rule.onNodeWithText("Swipe between Field Guide sections").assertIsDisplayed()
            rule.onNodeWithTag("field-guide-swipe-toggle").assertIsOff()
            rule.onNodeWithTag("settings-done").performClick()

            rule.onNodeWithTag("nav-search").performClick()
            rule.onNodeWithTag("monster-index").assertIsDisplayed()
            rule.onNodeWithTag("field-guide-pager").performTouchInput { swipeLeft() }
            rule.waitForIdle()
            rule.onNodeWithTag("monster-index").assertIsDisplayed()

            // Enabling is live: one gesture advances exactly one adjacent category.
            rule.onNodeWithTag("settings-button").performClick()
            rule.onNodeWithTag("field-guide-swipe-toggle").performClick()
            assertTrue(preferences.getBoolean(FIELD_GUIDE_SECTION_SWIPE_KEY, false))
            rule.onNodeWithTag("settings-done").performClick()
            rule.onNodeWithTag("field-guide-pager").performTouchInput { swipeLeft() }
            rule.waitForIdle()
            rule.onNodeWithTag("small-monster-index").assertIsDisplayed()
            rule.onNodeWithTag("field-filter-small_monster").assertIsDisplayed()

            // Disabling also takes effect without restarting or changing the selected section.
            rule.onNodeWithTag("settings-button").performClick()
            rule.onNodeWithTag("field-guide-swipe-toggle").performClick()
            rule.onNodeWithTag("field-guide-swipe-toggle").assertIsOff()
            rule.onNodeWithTag("settings-done").performClick()
            rule.onNodeWithTag("field-guide-pager").performTouchInput { swipeLeft() }
            rule.waitForIdle()
            rule.onNodeWithTag("small-monster-index").assertIsDisplayed()

            // Re-enable and verify the stored value is restored by Activity recreation.
            rule.onNodeWithTag("settings-button").performClick()
            rule.onNodeWithTag("field-guide-swipe-toggle").performClick()
            rule.onNodeWithTag("settings-done").performClick()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()
            rule.onNodeWithTag("settings-button").performClick()
            rule.onNodeWithTag("field-guide-swipe-toggle").assertIsOn()
            rule.onNodeWithTag("settings-done").performClick()

            // Detail routes do not compose the root Field Guide swipe region.
            rule.onNodeWithTag("nav-search").performClick()
            rule.onNodeWithText("Agnaktor").performClick()
            rule.onNodeWithTag("monster-tabs").assertIsDisplayed()
            rule.onNodeWithTag("field-guide-pager").assertDoesNotExist()
        } finally {
            val editor = preferences.edit()
            if (originalHadValue) editor.putBoolean(FIELD_GUIDE_SECTION_SWIPE_KEY, originalValue)
            else editor.remove(FIELD_GUIDE_SECTION_SWIPE_KEY)
            editor.commit()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()
        }
    }
}
