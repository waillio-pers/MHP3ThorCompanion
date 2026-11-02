package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-port-xhdpi")
class PalicoSkillsUiTest : MainActivityUiTestBase() {
    @Test fun switchingAndSearchingPreservesHunterSkills() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-guide-category-strip").performScrollToNode(hasTestTag("field-filter-skill"))
        rule.onNodeWithTag("field-filter-skill").performClick()
        rule.onNodeWithTag("skills-browser").assertIsDisplayed()
        rule.onNodeWithTag("palico-skills-tab").performClick()
        rule.onNodeWithTag("palico-skills-browser").assertIsDisplayed()
        rule.onNodeWithTag("global-search").performTextInput("Big Boomerang")
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("palico-skill-big_boomerang").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("palico-skill-big_boomerang").performClick()
        rule.onNodeWithTag("palico-skill-unlock-big_boomerang", useUnmergedTree = true).assertTextContains("Nargacuga or Barioth or Duramboros", substring = true)
        rule.onNodeWithTag("global-search").performTextClearance()
        rule.onNodeWithTag("hunter-skills-tab").performClick()
        rule.onNodeWithTag("skills-browser").assertIsDisplayed()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
class PalicoSkillsThorUiTest : MainActivityUiTestBase() {
    @Test fun sectionsAndPalicoSearchAreReachableOnThor() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-guide-category-strip").performScrollToNode(hasTestTag("field-filter-skill"))
        rule.onNodeWithTag("field-filter-skill").performClick()
        rule.onNodeWithTag("skills-browser").assertIsDisplayed()
        rule.onNodeWithTag("palico-skills-tab").assertIsDisplayed().performClick()
        rule.onNodeWithTag("global-search").performTextInput("Big Boomerang")
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("palico-skill-big_boomerang").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("palico-skill-big_boomerang").assertIsDisplayed().performClick()
        rule.onNodeWithTag("palico-skill-unlock-big_boomerang", useUnmergedTree = true)
            .assertTextContains("Nargacuga", substring = true)
    }
}
