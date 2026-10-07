package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuestRewardsConnectedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun armorSphereQuestRewardsUsesQuestFirstPreviewAndModal() {
        rule.onNodeWithText("Items").performClick()
        rule.onNodeWithTag("global-search").performTextInput("Armor Sphere")
        rule.onNodeWithTag("result-material-item_armor_sphere").performClick()
        rule.onNodeWithTag("quest-rewards-header").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("quest-rewards-more-count").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("quest-rewards-view-all").performScrollTo().performClick()
        rule.onNodeWithTag("quest-rewards-modal").assertIsDisplayed()
        rule.onNodeWithTag("quest-rewards-modal-search").assertIsDisplayed()
        rule.onNodeWithTag("quest-rewards-modal-list").assertIsDisplayed()
    }
}
