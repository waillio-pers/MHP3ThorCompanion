package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
class MonsterRewardsUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun monsterFirstGroupShowsMethodOrderAndExactSameRankRows() {
        openMaterial("Agnaktor Claw+")
        rule.onNodeWithTag("item-monster-rewards-groups").assertExists()
        rule.onNodeWithTag("item-monster-source-group-monster_agnaktor").assertIsDisplayed()
        rule.onNodeWithTag("item-monster-source-method-monster_agnaktor-BODY_CARVE-Body").assertExists()
        rule.onNodeWithTag("item-monster-source-method-monster_agnaktor-CAPTURE-Capture").assertExists()
        rule.onNodeWithTag("item-monster-source-method-monster_agnaktor-PART_BREAK-Front Legs/Hind Legs").assertExists()
        rule.onNodeWithTag("item-monster-reward-entry-reward_00707").assertExists()
        rule.onNodeWithTag("item-monster-reward-entry-reward_00709").assertExists()
    }

    @Test
    fun outlierUsesSixGroupPreviewAndLazyViewAllModal() {
        openMaterial("Lrg Wyvern Tear")
        rule.onNodeWithTag("item-monster-rewards-more").performScrollTo().assertExists()
        rule.onNodeWithTag("item-monster-rewards-view-all").performScrollTo().performClick()
        rule.onNodeWithTag("item-monster-rewards-modal").assertIsDisplayed()
        rule.onNodeWithTag("item-monster-rewards-modal-list").assertExists()
        rule.onAllNodesWithTag("item-monster-source-group-monster_agnaktor").assertCountEquals(2)
        rule.onNodeWithTag("item-monster-rewards-modal-close").performClick()
        rule.onNodeWithTag("item-monster-rewards-modal").assertDoesNotExist()
    }

    private fun openMaterial(name: String) {
        rule.onNodeWithTag("global-search").performTextInput(name)
        rule.onAllNodesWithTag("result-material-item_agnaktor_claw_plus").fetchSemanticsNodes().firstOrNull()?.let {
            rule.onNodeWithTag("result-material-item_agnaktor_claw_plus").performClick()
            return
        }
        rule.onNodeWithTag("result-material-item_wyvern_sobs").performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
    }
}
