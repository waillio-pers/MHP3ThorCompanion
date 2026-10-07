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
class MonsterRewardsConnectedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun thorShowsMonsterFirstOutlierPreviewAndModal() {
        rule.onNodeWithText("Items").performClick()
        rule.onNodeWithTag("global-search").performTextInput("Lrg Wyvern Tear")
        rule.onNodeWithTag("result-material-item_wyvern_sobs").performClick()
        rule.onNodeWithTag("item-monster-rewards-more").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("item-monster-rewards-view-all").performScrollTo().performClick()
        rule.onNodeWithTag("item-monster-rewards-modal").assertIsDisplayed()
        rule.onNodeWithTag("item-monster-rewards-modal-list").assertIsDisplayed()
    }
}
