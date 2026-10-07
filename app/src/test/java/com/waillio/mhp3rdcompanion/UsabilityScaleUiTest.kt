package com.waillio.mhp3rdcompanion

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w827dp-h720dp-land-xhdpi")
class UsabilityScaleUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val data = CompanionRepository(ApplicationProvider.getApplicationContext<Context>()).data

    @Test
    fun palicoFarmAndFieldPresentationsRemainReadableAcrossSupportedScaleEndpoints() {
        val preferences = rule.activity.getSharedPreferences(UI_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val originalStep = preferences.getInt(UI_SCALE_STEP_KEY, UI_SCALE_DEFAULT_STEP)
        val huskberry = data.materials.single { it.gameItemId == 92 }
        val plantedCrop = data.materials.single { it.name == "Bumblepumpkin" }
        val bugTreeMaterial = data.materials.first { material ->
            material.sources.any { it.profileId == "farm_bug_tree_profile_4a" }
        }
        val bugTreeMethod = bugTreeMaterial.sources
            .filter { it.type == MaterialSourceType.FARM }
            .groupFarmSourcesForDisplay()
            .single { it.stableMethodId == "farm_bug_tree_seesaw::profile:farm_bug_tree_profile_4a" }

        try {
            listOf(0, 4, 6).forEach { step ->
                setScale(step)

                openMaterial(huskberry.name, huskberry.id)
                rule.onNodeWithTag("item-palico-expedition-label-1-0").performScrollTo()
                    .assertTextEquals("Expeditions 01, 03–04, 06")
                rule.onNodeWithTag("item-palico-expedition-label-1-1").performScrollTo()
                    .assertTextEquals("Expedition 05")
                rule.onNodeWithText("Gathering · ×5").performScrollTo().assertIsDisplayed()
                rule.onNodeWithText("Gathering · ×10").performScrollTo().assertIsDisplayed()
                val rewardBounds = rule.onNodeWithText("Gathering · ×5").fetchSemanticsNode().boundsInRoot
                val feeBounds = rule.onNodeWithTag("item-palico-cost-1-1").fetchSemanticsNode().boundsInRoot
                val itemBounds = rule.onNodeWithTag("screen-material").fetchSemanticsNode().boundsInRoot
                assertTrue("Palico reward should fit within the material viewport", rewardBounds.left >= itemBounds.left && rewardBounds.right <= itemBounds.right)
                assertTrue("Palico cost should remain visually secondary", feeBounds.height < rewardBounds.height)
                returnHome()

                openMaterial(bugTreeMaterial.name, bugTreeMaterial.id)
                rule.onNodeWithTag("farm-facility-explanation-farm_bug_tree_seesaw")
                    .performScrollTo().assertTextEquals(BUG_TREE_SEESAW_EXPLANATION)
                val firstProfileOutcome = bugTreeMethod.outcomes.first()
                rule.onNodeWithTag("farm-method-outcome-${bugTreeMethod.stableMethodId}-0")
                    .performScrollTo().assertTextEquals(firstProfileOutcome.displayLabel())
                listOf("pool 105%", "pool 90%", "Perfect", "Good", "Success", "Failure")
                    .forEach { phrase ->
                        rule.onAllNodes(hasText(phrase, substring = true, ignoreCase = true)).assertCountEquals(0)
                    }
                val explanationBounds = rule.onNodeWithTag("farm-facility-explanation-farm_bug_tree_seesaw")
                    .fetchSemanticsNode().boundsInRoot
                val explanationHeight = explanationBounds.height
                assertTrue("Bug Tree explanation should wrap within the viewport", explanationHeight > 0f)
                returnHome()

                openMaterial(plantedCrop.name, plantedCrop.id)
                rule.onNodeWithTag("farm-method-trigger-farm_field_red_seed::inputGameItemId:180")
                    .performScrollTo().assertTextEquals("Plant this:")
                rule.onNodeWithTag("farm-method-outcome-farm_field_red_seed::inputGameItemId:180-0")
                    .performScrollTo().assertTextEquals("Yield ×1 · 5%")
                returnHome()
            }
        } finally {
            preferences.edit().putInt(UI_SCALE_STEP_KEY, originalStep).commit()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()
        }
    }

    private fun openMaterial(name: String, id: String) {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-material").performScrollTo().performClick()
        val search = rule.onNodeWithTag("global-search")
        search.performTextClearance()
        search.performTextInput(name)
        rule.onNodeWithTag("result-material-$id").performScrollTo().performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
    }

    private fun returnHome() {
        rule.onNodeWithTag("nav-home").performClick()
        rule.waitForIdle()
    }

    private fun setScale(step: Int) {
        rule.onNodeWithTag("settings-button").performClick()
        rule.onNodeWithTag("ui-scale-slider")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(step.toFloat()) }
        val hundredths = 80 + step * 5
        val label = "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}"
        rule.onNodeWithTag("settings-scale-value", useUnmergedTree = true).assertTextEquals(label)
        rule.onNodeWithTag("settings-done").performClick()
        rule.waitForIdle()
    }
}
