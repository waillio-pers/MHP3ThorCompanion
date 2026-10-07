package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
class TrainingRewardsUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun rawMeatUsesMissionPreviewAndTrainingRewardsModalWithoutMarkerLeak() {
        openMaterial("Raw Meat", "item_raw_meat")
        rule.onNodeWithTag("item-training-rewards-header").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("item-training-rewards-header").assertExists()
        rule.onNodeWithTag("item-training-rewards-more").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("item-training-rewards-view-all").performScrollTo().performClick()
        rule.onNodeWithTag("item-training-rewards-modal").assertIsDisplayed()
        rule.onNodeWithTag("item-training-rewards-modal-list").assertExists()
        rule.onNodeWithText("Atwiki Candidate", substring = true).assertDoesNotExist()
        rule.onNodeWithText("ATWIKI_CANDIDATE", substring = true).assertDoesNotExist()
        rule.onNodeWithText("基本報酬", substring = true).assertDoesNotExist()
        rule.onNodeWithText("確定報酬", substring = true).assertDoesNotExist()
        rule.onNodeWithTag("item-training-rewards-modal-close").performClick()
        rule.onNodeWithTag("item-training-rewards-modal").assertDoesNotExist()
    }

    @Test
    fun groupTrainingUsesProvenEnglishPoolLabel() {
        openMaterial("Armor Sphere+", "item_armor_sphere_plus")
        rule.onNodeWithTag("item-training-rewards-header").performScrollTo().assertExists()
        rule.onNodeWithTag("item-training-pool-quest_group_training_20205-基本報酬").assertTextContains("Basic Rewards")
        rule.onNodeWithText("基本報酬", substring = true).assertDoesNotExist()
    }

    private fun openMaterial(name: String, id: String) {
        rule.onNodeWithTag("global-search").performTextInput(name)
        rule.onNodeWithTag("result-material-$id").performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
    }
}
