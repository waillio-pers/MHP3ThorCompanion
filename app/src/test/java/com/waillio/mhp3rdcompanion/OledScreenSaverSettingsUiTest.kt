package com.waillio.mhp3rdcompanion

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OledScreenSaverSettingsUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun toggleDefaultsOffAndRetainsDurationWhenDisabledAndReenabled() {
        val preferences = rule.activity.getSharedPreferences(UI_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val originalTimeout = preferences.getInt(OLED_SCREEN_SAVER_TIMEOUT_KEY, 0)
        val hadTimeout = preferences.contains(OLED_SCREEN_SAVER_TIMEOUT_KEY)
        val originalLast = preferences.getInt(OLED_SCREEN_SAVER_LAST_DURATION_KEY, 0)
        val hadLast = preferences.contains(OLED_SCREEN_SAVER_LAST_DURATION_KEY)
        try {
            preferences.edit()
                .remove(OLED_SCREEN_SAVER_TIMEOUT_KEY)
                .remove(OLED_SCREEN_SAVER_LAST_DURATION_KEY)
                .commit()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()

            openSettingsAndScrollToSaver()
            rule.onNodeWithTag("oled-screen-saver-toggle").assertIsOff()
            rule.onNodeWithTag("oled-screen-saver-duration-1").assertDoesNotExist()

            rule.onNodeWithTag("oled-screen-saver-toggle").performClick()
            rule.onNodeWithTag("oled-screen-saver-duration-1").assertIsDisplayed()
            OLED_SCREEN_SAVER_TIMEOUT_OPTIONS.filter { it > 0 }.forEach { minutes ->
                rule.onNodeWithTag("oled-screen-saver-duration-$minutes").performClick()
                assertEquals(minutes, preferences.getInt(OLED_SCREEN_SAVER_TIMEOUT_KEY, -1))
                assertEquals(minutes, preferences.getInt(OLED_SCREEN_SAVER_LAST_DURATION_KEY, -1))
            }

            // Verify all durations above, then keep the saved recreation case at 5 minutes.
            rule.onNodeWithTag("oled-screen-saver-duration-5").performClick()

            rule.onNodeWithTag("oled-screen-saver-toggle").performClick()
            assertEquals(0, preferences.getInt(OLED_SCREEN_SAVER_TIMEOUT_KEY, -1))
            assertEquals(5, preferences.getInt(OLED_SCREEN_SAVER_LAST_DURATION_KEY, -1))
            rule.onNodeWithTag("oled-screen-saver-duration-5").assertDoesNotExist()

            rule.onNodeWithTag("oled-screen-saver-toggle").performClick()
            assertEquals(5, preferences.getInt(OLED_SCREEN_SAVER_TIMEOUT_KEY, -1))
            rule.onNodeWithTag("oled-screen-saver-duration-5").assertIsDisplayed()

            rule.onNodeWithTag("settings-done").performClick()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()
            openSettingsAndScrollToSaver()
            rule.onNodeWithTag("oled-screen-saver-toggle").assertIsOn()
            rule.onNodeWithTag("oled-screen-saver-duration-5").assertIsDisplayed()
        } finally {
            val editor = preferences.edit()
            if (hadTimeout) editor.putInt(OLED_SCREEN_SAVER_TIMEOUT_KEY, originalTimeout) else editor.remove(OLED_SCREEN_SAVER_TIMEOUT_KEY)
            if (hadLast) editor.putInt(OLED_SCREEN_SAVER_LAST_DURATION_KEY, originalLast) else editor.remove(OLED_SCREEN_SAVER_LAST_DURATION_KEY)
            editor.commit()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()
        }
    }

    @Test
    fun positiveLegacyTimeoutMigratesToEnabledStateAndRetainedDuration() {
        val preferences = rule.activity.getSharedPreferences(UI_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val originalTimeout = preferences.getInt(OLED_SCREEN_SAVER_TIMEOUT_KEY, 0)
        val hadTimeout = preferences.contains(OLED_SCREEN_SAVER_TIMEOUT_KEY)
        val originalLast = preferences.getInt(OLED_SCREEN_SAVER_LAST_DURATION_KEY, 0)
        val hadLast = preferences.contains(OLED_SCREEN_SAVER_LAST_DURATION_KEY)
        try {
            preferences.edit().putInt(OLED_SCREEN_SAVER_TIMEOUT_KEY, 5).remove(OLED_SCREEN_SAVER_LAST_DURATION_KEY).commit()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()
            openSettingsAndScrollToSaver()
            rule.onNodeWithTag("oled-screen-saver-toggle").assertIsOn()
            rule.onNodeWithTag("oled-screen-saver-duration-5").assertIsDisplayed()
            assertEquals(5, preferences.getInt(OLED_SCREEN_SAVER_LAST_DURATION_KEY, -1))
        } finally {
            val editor = preferences.edit()
            if (hadTimeout) editor.putInt(OLED_SCREEN_SAVER_TIMEOUT_KEY, originalTimeout) else editor.remove(OLED_SCREEN_SAVER_TIMEOUT_KEY)
            if (hadLast) editor.putInt(OLED_SCREEN_SAVER_LAST_DURATION_KEY, originalLast) else editor.remove(OLED_SCREEN_SAVER_LAST_DURATION_KEY)
            editor.commit()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()
        }
    }

    private fun openSettingsAndScrollToSaver() {
        rule.onNodeWithTag("settings-button").performClick()
        rule.onNodeWithTag("oled-screen-saver-toggle").performScrollTo()
        rule.onNodeWithTag("oled-screen-saver-toggle").assertIsDisplayed()
    }
}
