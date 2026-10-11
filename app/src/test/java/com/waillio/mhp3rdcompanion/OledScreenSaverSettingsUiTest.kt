package com.waillio.mhp3rdcompanion

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    fun settingDefaultsOffPersistsEveryChoiceAndRestoresAcrossActivityRecreation() {
        val preferences = rule.activity.getSharedPreferences(UI_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val originalHadValue = preferences.contains(OLED_SCREEN_SAVER_TIMEOUT_KEY)
        val originalValue = preferences.getInt(OLED_SCREEN_SAVER_TIMEOUT_KEY, 0)
        try {
            preferences.edit().remove(OLED_SCREEN_SAVER_TIMEOUT_KEY).commit()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()

            rule.onNodeWithTag("settings-button").performClick()
            rule.onNodeWithText("OLED screen saver").assertIsDisplayed()
            rule.onNodeWithTag("oled-screen-saver-setting").assertIsDisplayed()
            rule.onNodeWithText("Off").assertIsDisplayed()

            OLED_SCREEN_SAVER_TIMEOUT_OPTIONS.forEach { minutes ->
                rule.onNodeWithTag("oled-screen-saver-setting").performClick()
                rule.onNodeWithTag("oled-screen-saver-option-$minutes").performClick()
                assertEquals(minutes, preferences.getInt(OLED_SCREEN_SAVER_TIMEOUT_KEY, -1))
            }

            rule.onNodeWithTag("settings-done").performClick()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()
            rule.onNodeWithTag("settings-button").performClick()
            rule.onNodeWithTag("oled-screen-saver-setting").assertIsDisplayed()
            rule.onNodeWithText("15 minutes").assertIsDisplayed()
        } finally {
            val editor = preferences.edit()
            if (originalHadValue) editor.putInt(OLED_SCREEN_SAVER_TIMEOUT_KEY, originalValue)
            else editor.remove(OLED_SCREEN_SAVER_TIMEOUT_KEY)
            editor.commit()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()
        }
    }
}
