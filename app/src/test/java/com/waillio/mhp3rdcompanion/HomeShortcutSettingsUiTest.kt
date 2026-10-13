package com.waillio.mhp3rdcompanion

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeShortcutSettingsUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun defaultsCanBeReorderedClearedAndPersistedAcrossActivityRecreation() {
        val preferences = rule.activity.getSharedPreferences(UI_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val hadShortcutPreference = preferences.contains(HOME_SHORTCUTS_KEY)
        val originalShortcutPreference = preferences.getString(HOME_SHORTCUTS_KEY, null)
        try {
            preferences.edit().remove(HOME_SHORTCUTS_KEY).commit()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()

            rule.onNodeWithTag("home-shortcut-large_monsters").assertIsDisplayed()
            rule.onNodeWithTag("home-shortcut-items").assertIsDisplayed()
            rule.onNodeWithTag("home-shortcut-maps").assertIsDisplayed()
            rule.onNodeWithTag("home-shortcut-charm_tables").assertIsDisplayed()

            rule.onNodeWithTag("settings-button").performClick()
            rule.onNodeWithTag("home-shortcuts-toggle").assertIsDisplayed()
            rule.onNodeWithTag("home-shortcut-slot-1").assertDoesNotExist()
            rule.onNodeWithTag("home-shortcuts-toggle").performClick()
            rule.onNodeWithTag("home-shortcut-slot-1").performClick()
            rule.onNodeWithTag("home-shortcut-option-1-quests").performClick()

            rule.onNodeWithTag("home-shortcut-slot-2").performClick()
            rule.onNodeWithTag("home-shortcut-option-2-quests").assertIsNotEnabled()
            rule.onNodeWithTag("home-shortcut-option-2-none").performClick()
            rule.onNodeWithTag("settings-done").performClick()

            org.junit.Assert.assertEquals("quests||maps|charm_tables", preferences.getString(HOME_SHORTCUTS_KEY, null))
            rule.onNodeWithTag("home-shortcut-quests").assertIsDisplayed()
            rule.onNodeWithTag("home-shortcut-items").assertDoesNotExist()

            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()
            rule.onNodeWithTag("home-shortcut-quests").assertIsDisplayed()
            rule.onNodeWithTag("home-shortcut-items").assertDoesNotExist()
            rule.onNodeWithTag("home-shortcut-maps").assertIsDisplayed()
            rule.onNodeWithTag("home-shortcut-charm_tables").assertIsDisplayed()

            rule.onNodeWithTag("settings-button").performClick()
            rule.onNodeWithTag("home-shortcut-slot-1").assertDoesNotExist()
            rule.onNodeWithTag("home-shortcuts-toggle").performClick()
            rule.onNodeWithTag("home-shortcut-slot-1").assertTextContains("Quests")
            rule.onNodeWithTag("home-shortcut-slot-2").assertTextContains("None")
        } finally {
            val editor = preferences.edit()
            if (hadShortcutPreference) editor.putString(HOME_SHORTCUTS_KEY, originalShortcutPreference)
            else editor.remove(HOME_SHORTCUTS_KEY)
            editor.commit()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w827dp-h720dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeShortcutThorLayoutUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun fourHomeShortcutsShareOneRowOnThor() {
        val tags = listOf(
            "home-shortcut-large_monsters",
            "home-shortcut-items",
            "home-shortcut-maps",
            "home-shortcut-charm_tables"
        )
        val bounds = tags.map { tag ->
            rule.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        }

        val rowTops = bounds.map { it.top }
        assertTrue("All four Thor shortcuts should share a row", rowTops.maxOrNull()!! - rowTops.minOrNull()!! < 1f)
    }
}
