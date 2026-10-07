package com.waillio.mhp3rdcompanion

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun homeScreenLaunches() {
        rule.onNodeWithText("MHP3rd").assertIsDisplayed()
        rule.onNodeWithText("Items").assertIsDisplayed()
    }

    @Test fun interfaceScaleSnapsAndPersistsAcrossActivityRecreation() {
        val preferences = rule.activity.getSharedPreferences(UI_SETTINGS_PREFS, 0)
        preferences.edit().putInt(UI_SCALE_STEP_KEY, UI_SCALE_DEFAULT_STEP).commit()
        rule.runOnUiThread { rule.activity.recreate() }
        rule.waitForIdle()

        try {
            rule.onNodeWithTag("settings-button").performClick()
            rule.onNodeWithText("1.00").assertIsDisplayed()
            rule.onNodeWithTag("ui-scale-slider").performSemanticsAction(SemanticsActions.SetProgress) { it(2f) }
            assertEquals(2, preferences.getInt(UI_SCALE_STEP_KEY, -1))
            rule.waitForIdle()
            rule.onNodeWithTag("settings-done").assertIsDisplayed().performClick()

            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()
            rule.onNodeWithTag("settings-button").performClick()
            rule.onNodeWithText("0.90").assertIsDisplayed()
        } finally {
            preferences.edit().putInt(UI_SCALE_STEP_KEY, UI_SCALE_DEFAULT_STEP).commit()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()
        }
    }

    @Test fun palicoExpeditionSourceUsesGroupedCards() {
        rule.onNodeWithText("Items").performClick()
        rule.onNodeWithTag("global-search").performTextInput("Rusted Fragment")
        rule.onNodeWithTag("result-material-item_rusted_fragment").performClick()
        rule.onNodeWithTag("item-palico-expedition-family").assertIsDisplayed()
        rule.onNodeWithTag("item-palico-tier-1").assertIsDisplayed()
        rule.onNodeWithTag("item-palico-reward-1-palico_expedition_1_01_reward_016").fetchSemanticsNode()
    }
    @Test fun invasionRewardSourceUsesMonsterFirstProjection() {
        rule.onNodeWithText("Items").performClick()
        rule.onNodeWithTag("global-search").performTextInput("Deviljho Fang")
        rule.onNodeWithTag("result-material-item_deviljho_fang").performClick()
        rule.onNodeWithText("Invasion Reward · 1 monster").assertIsDisplayed()
        rule.onNodeWithTag("item-invasion-monster-source-monster_deviljho").assertIsDisplayed()
        rule.onNodeWithTag("item-invasion-context-monster_deviljho-HIGH").assertIsDisplayed()
        rule.onNodeWithText("×1 · 10–<20%").assertIsDisplayed()
        rule.onNodeWithText("×2 · 5–<10%").assertIsDisplayed()
    }
}
