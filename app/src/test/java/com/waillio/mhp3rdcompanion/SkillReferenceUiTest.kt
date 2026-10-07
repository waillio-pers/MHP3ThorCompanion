package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkillReferenceUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun fieldGuideSkillsBrowseAndSearch() {
        openSkills()
        rule.onNodeWithTag("skills-browser").assertIsDisplayed()
        rule.onNodeWithTag("skills-list").performScrollToNode(hasTestTag("skill-row-mhp3_skill_tree_072"))
        rule.onNodeWithTag("skill-row-mhp3_skill_tree_072").assertIsDisplayed()

        rule.onNodeWithTag("global-search").performTextInput("Punishing")
        rule.onNodeWithTag("skill-row-mhp3_skill_tree_072").assertIsDisplayed().performClick()
        rule.onNodeWithTag("skill-expanded-mhp3_skill_tree_072", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("stun", substring = true, ignoreCase = true).assertExists()
    }

    @Test
    fun negativeAndSpecialSkillStatesRemainVisible() {
        openSkills()
        rule.onNodeWithTag("global-search").performTextInput("Attack")
        rule.onNodeWithTag("skill-row-mhp3_skill_tree_033").performClick()
        rule.onNodeWithTag("skill-threshold-mhp3_skill_tree_033--10", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText("-10", substring = true, useUnmergedTree = true).onFirst().assertExists()

        rule.onNodeWithTag("global-search").performTextClearance()
        rule.onNodeWithTag("skills-list").performScrollToNode(hasTestTag("skill-row-mhp3_skill_tree_001"))
        rule.onNodeWithTag("skill-row-mhp3_skill_tree_001").performClick()
        rule.onNodeWithTag("skill-special-TORSO_COPY", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("+0", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun skillJewelSubsectionIsCollapsedThenExpandsWithSignedRelations() {
        openSkills()
        rule.onNodeWithTag("global-search").performTextInput("Punishing")
        rule.onNodeWithTag("skill-row-mhp3_skill_tree_072").performClick()
        rule.onNodeWithTag("skill-jewels-toggle-mhp3_skill_tree_072", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Jewels (2)", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("skill-jewel-row-mhp3_skill_tree_072-item_gambit_jewel_1", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("skill-jewels-toggle-mhp3_skill_tree_072", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("skill-jewel-row-mhp3_skill_tree_072-item_gambit_jewel_1", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("+1", useUnmergedTree = true).assertExists()
    }

    @Test
    fun jewelItemDetailShowsSkillsAndLinksBackToSkillReference() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-material").performScrollTo().performClick()
        rule.onNodeWithTag("global-search").performTextInput("Gambit Jewel 1")
        rule.onNodeWithTag("result-material-item_gambit_jewel_1").performClick()
        rule.onNodeWithTag("item-jewel-skills-item_gambit_jewel_1", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Punishing Draw", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Slots required: 1", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("item-jewel-skill-item_gambit_jewel_1-mhp3_skill_tree_072", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("skills-browser").assertIsDisplayed()
        rule.onNodeWithTag("skill-expanded-mhp3_skill_tree_072", useUnmergedTree = true).assertExists()
    }

    @Test
    fun mixedJewelDetailUsesSignedChipsAndPositiveFirstFullRows() {
        openMaterial("Capture Jewel 3", "item_capture_jewel_3")

        val positive = rule.onNodeWithTag(
            "item-jewel-skill-item_capture_jewel_3-mhp3_skill_tree_069",
            useUnmergedTree = true
        )
        val negative = rule.onNodeWithTag(
            "item-jewel-skill-item_capture_jewel_3-mhp3_skill_tree_051",
            useUnmergedTree = true
        )
        positive.assertExists()
        negative.assertExists()
        rule.onAllNodesWithText("+4", useUnmergedTree = true).fetchSemanticsNodes().also { check(it.isNotEmpty()) }
        rule.onAllNodesWithText("-2", useUnmergedTree = true).fetchSemanticsNodes().also { check(it.isNotEmpty()) }
        rule.onNodeWithTag("item-jewel-skill-name-item_capture_jewel_3-mhp3_skill_tree_069", useUnmergedTree = true)
            .assertTextContains("Capture")
        rule.onNodeWithTag("item-jewel-skill-name-item_capture_jewel_3-mhp3_skill_tree_051", useUnmergedTree = true)
            .assertTextContains("Fate")
        rule.onNodeWithTag(
            "item-jewel-effects-order-item_capture_jewel_3-mhp3_skill_tree_069_mhp3_skill_tree_051",
            useUnmergedTree = true
        ).assertExists()
    }

    @Test
    fun singleSkillJewelShowsExactlyOneFullEffectRow() {
        openMaterial("Alarm Jewel 1", "item_alarm_jewel_1")

        rule.onNodeWithTag("item-jewel-skill-item_alarm_jewel_1-mhp3_skill_tree_037", useUnmergedTree = true)
            .assertExists()
        rule.onAllNodesWithText("+2", useUnmergedTree = true).fetchSemanticsNodes().also { check(it.isNotEmpty()) }
        rule.onNodeWithTag("item-jewel-skill-name-item_alarm_jewel_1-mhp3_skill_tree_037", useUnmergedTree = true)
            .assertTextContains("Anti-Theft")
        rule.onNodeWithTag(
            "item-jewel-effects-order-item_alarm_jewel_1-mhp3_skill_tree_037",
            useUnmergedTree = true
        ).assertExists()
    }

    @Test
    fun skillJewelsShowCurrentAndSecondaryEffectsWithCurrentFirst() {
        openSkills()
        rule.onNodeWithTag("global-search").performTextInput("Punishing")
        rule.onNodeWithTag("skill-row-mhp3_skill_tree_072").performClick()
        rule.onNodeWithTag("skill-jewels-toggle-mhp3_skill_tree_072", useUnmergedTree = true).performClick()

        val jewel = rule.onNodeWithTag("skill-jewel-row-mhp3_skill_tree_072-item_gambit_jewel_1", useUnmergedTree = true)
        jewel.assertExists()
        rule.onNodeWithTag("skill-jewel-effect-item_gambit_jewel_1-mhp3_skill_tree_072", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("skill-jewel-effect-item_gambit_jewel_1-mhp3_skill_tree_010", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("skill-jewel-effect-name-item_gambit_jewel_1-mhp3_skill_tree_072", useUnmergedTree = true)
            .assertTextContains("Punishing Draw")
        rule.onNodeWithTag("skill-jewel-effect-name-item_gambit_jewel_1-mhp3_skill_tree_010", useUnmergedTree = true)
            .assertTextContains("Sharpness")
        rule.onNodeWithTag(
            "skill-jewel-effects-order-item_gambit_jewel_1-mhp3_skill_tree_072_mhp3_skill_tree_010",
            useUnmergedTree = true
        ).assertExists()
    }

    @Test
    fun negativeCurrentSkillRelationIsFirstButSecondaryPositiveRemainsVisible() {
        openSkills()
        rule.onNodeWithTag("global-search").performTextInput("Paralysis")
        rule.onNodeWithTag("skill-row-mhp3_skill_tree_003").performClick()
        rule.onNodeWithTag("skill-jewels-toggle-mhp3_skill_tree_003", useUnmergedTree = true).performClick()

        rule.onNodeWithTag("skill-jewel-row-mhp3_skill_tree_003-item_steadfast_jwl_1", useUnmergedTree = true).assertExists()
        val current = rule.onNodeWithTag("skill-jewel-effect-item_steadfast_jwl_1-mhp3_skill_tree_003", useUnmergedTree = true)
        val secondary = rule.onNodeWithTag("skill-jewel-effect-item_steadfast_jwl_1-mhp3_skill_tree_005", useUnmergedTree = true)
        current.assertExists()
        secondary.assertExists()
        rule.onNodeWithTag("skill-jewel-effect-name-item_steadfast_jwl_1-mhp3_skill_tree_003", useUnmergedTree = true)
            .assertTextContains("Paralysis")
        rule.onNodeWithTag("skill-jewel-effect-name-item_steadfast_jwl_1-mhp3_skill_tree_005", useUnmergedTree = true)
            .assertTextContains("Stun")
        rule.onNodeWithTag(
            "skill-jewel-effects-order-item_steadfast_jwl_1-mhp3_skill_tree_003_mhp3_skill_tree_005",
            useUnmergedTree = true
        ).assertExists()
    }

    private fun openMaterial(name: String, id: String) {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-material").performScrollTo().performClick()
        rule.onNodeWithTag("global-search").performTextInput(name)
        rule.onNodeWithTag("result-material-$id").performClick()
        rule.onNodeWithTag("item-jewel-skills-$id", useUnmergedTree = true).assertExists()
    }

    private fun openSkills() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-skill").performScrollTo().performClick()
    }
}
