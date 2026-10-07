package com.waillio.mhp3rdcompanion

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuestBrowserUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun emptyQueryShowsAll354QuestsWithStructuredBadge() {
        openQuests()
        rule.onNodeWithTag("quest-count").assertTextEquals("354 quests")
        rule.onNodeWithTag("quest-filters-button").assertIsDisplayed()
        rule.onNodeWithTag("quest-filter-summary").assertTextEquals("All quests")
        rule.onNodeWithTag("quest-category-village").assertDoesNotExist()
        rule.onNodeWithTag("quest-star-1").assertDoesNotExist()
        rule.onNodeWithTag("quest-list").performScrollToNode(hasTestTag("quest-row-quest_village_1_star_01"))
        rule.onNodeWithTag("quest-badge-quest_village_1_star_01").assertIsDisplayed()
        rule.onAllNodesWithText("Village").onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("Low Rank").onFirst().assertIsDisplayed()
        rule.onNodeWithTag("quest-list").performScrollToNode(hasTestTag("quest-row-quest_guild_6_star_01"))
        rule.onNodeWithTag("quest-badge-quest_guild_6_star_01").assertIsDisplayed()
        rule.onAllNodesWithText("★6").onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("Guild").onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("High Rank").onFirst().assertIsDisplayed()
    }

    @Test
    fun categoryAndStarFiltersExposeOnlyValidCurrentCorpusChoices() {
        openQuests()
        rule.onNodeWithTag("quest-filters-button").performClick()
        rule.onNodeWithTag("quest-filter-type-village").performClick().assertIsSelected()
        rule.onNodeWithTag("quest-filter-rank-section").assertDoesNotExist()
        rule.onNodeWithTag("quest-filter-star-1").assertExists()
        rule.onNodeWithTag("quest-filter-star-6").assertExists()
        rule.onNodeWithTag("quest-filter-star-7").assertDoesNotExist()
        rule.onNodeWithTag("quest-filter-star-2").performClick().assertIsSelected()
        rule.onNodeWithTag("quest-filter-apply").performClick()
        rule.onNodeWithTag("quest-count").assertTextEquals("17 quests")
        rule.onNodeWithTag("quest-filter-summary").assertTextEquals("Village · ★2")
        rule.onNodeWithTag("quest-list").performScrollToNode(hasTestTag("quest-row-quest_village_2_star_05"))
        rule.onNodeWithTag("quest-row-quest_village_2_star_05").assertIsDisplayed()

        rule.onNodeWithTag("quest-filters-button").performClick()
        rule.onNodeWithTag("quest-filter-type-guild").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("quest-filter-dialog").assertIsDisplayed()
        rule.onNodeWithTag("quest-filter-type-guild").assertIsSelected()
        rule.onNodeWithTag("quest-filter-rank-section").assertExists()
        rule.onNodeWithTag("quest-filter-rank-high").assertIsDisplayed()
        rule.onNodeWithTag("quest-filter-rank-high").performClick()
        rule.onNodeWithTag("quest-filter-star-6").performClick()
        rule.onNodeWithTag("quest-filter-star-7").performClick()
        rule.onNodeWithTag("quest-filter-apply").performClick()
        rule.onNodeWithTag("quest-count").assertTextEquals("58 quests")
        rule.onNodeWithTag("quest-filter-summary").assertTextEquals("Guild · High Rank · ★6–7")
        rule.onAllNodesWithText("★6").onFirst().assertIsDisplayed()

    }

    @Test
    fun globalSearchQuestResultUsesQuestBadge() {
        rule.onNodeWithTag("global-search").performTextInput("The Brilliant Darkness")
        rule.onNodeWithTag("result-quest-quest_guild_8_star_38").assertIsDisplayed()
        rule.onNodeWithTag("quest-badge-quest_guild_8_star_38").assertIsDisplayed()
    }

    @Test
    fun monsterQuestTabAndItemQuestSourceUseQuestBadge() {
        rule.onNodeWithTag("global-search").performTextInput("Rathalos")
        rule.onNodeWithTag("result-monster-monster_rathalos").performClick()
        rule.onNodeWithTag("monster-tab-3").performClick()
        rule.onNodeWithTag("quests-list").performScrollToNode(hasTestTag("monster-quest-quest_guild_7_star_13"))
        rule.onNodeWithTag("quest-badge-quest_guild_7_star_13").assertIsDisplayed()

        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-all").performClick()
        rule.onNodeWithTag("global-search").performTextInput("Blue Mushroom")
        rule.onNodeWithTag("result-material-item_blue_mushroom").performClick()
        rule.onNodeWithTag("item-quest-badge-quest_guild_6_star_21").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun queryStarFilterAndScrollSurviveActivityRecreation() {
        openQuests()
        rule.onNodeWithTag("global-search").performTextInput("a")
        rule.onNodeWithTag("quest-filters-button").performClick()
        rule.onNodeWithTag("quest-filter-type-guild").performClick()
        rule.onNodeWithTag("quest-filter-rank-high").performClick()
        rule.onNodeWithTag("quest-filter-star-8").performClick()
        rule.onNodeWithTag("quest-filter-apply").performClick()
        rule.onNodeWithTag("quest-list").performScrollToNode(hasTestTag("quest-row-quest_guild_8_urgent_amatsu"))
        val before = rule.onNodeWithTag("quest-row-quest_guild_8_urgent_amatsu").fetchSemanticsNode().boundsInRoot

        rule.activityRule.scenario.recreate()

        rule.onNodeWithTag("global-search").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("a"))
        )
        rule.onNodeWithTag("field-filter-quest").assertIsSelected()
        rule.onNodeWithTag("quest-filter-summary").assertTextEquals("Guild · High Rank · ★8")
        rule.onNodeWithTag("quest-filters-button").assertTextContains("Filters (3)")
        assertEquals(before, rule.onNodeWithTag("quest-row-quest_guild_8_urgent_amatsu").fetchSemanticsNode().boundsInRoot)
    }

    @Test
    fun everyCurrentQuestTypeUsesHumanLabelsAndDataDrivenCounts() {
        openQuests()
        val cases = listOf(
            "village" to ("Village" to "91 quests"),
            "guild" to ("Guild" to "184 quests"),
            "hot_spring" to ("Hot Spring" to "7 quests"),
            "drink" to ("Drinks" to "20 quests"),
            "event" to ("Event" to "52 quests")
        )
        cases.forEach { (tagSuffix, expected) ->
            rule.onNodeWithTag("quest-filters-button").performClick()
            rule.onNodeWithTag("quest-filter-type-$tagSuffix").performClick()
            rule.onNodeWithTag("quest-filter-apply").performClick()
            rule.onNodeWithTag("quest-filter-summary").assertTextEquals(expected.first)
            rule.onNodeWithTag("quest-count").assertTextEquals(expected.second)
        }
    }

    @Test
    fun draftDismissResetAndQuickClearPreserveExpectedAppliedState() {
        openQuests()
        rule.onNodeWithTag("quest-filters-button").performClick()
        rule.onNodeWithTag("quest-filter-type-guild").performClick()
        rule.onNodeWithTag("quest-filter-close").performClick()
        rule.onNodeWithTag("quest-filter-summary").assertTextEquals("All quests")
        rule.onNodeWithTag("quest-count").assertTextEquals("354 quests")

        rule.onNodeWithTag("quest-filters-button").performClick()
        rule.onNodeWithTag("quest-filter-type-guild").performClick()
        rule.onNodeWithTag("quest-filter-reset").performClick()
        rule.onNodeWithTag("quest-filter-apply").performClick()
        rule.onNodeWithTag("quest-filter-summary").assertTextEquals("All quests")

        rule.onNodeWithTag("quest-filters-button").performClick()
        rule.onNodeWithTag("quest-filter-type-event").performClick()
        rule.onNodeWithTag("quest-filter-apply").performClick()
        rule.onNodeWithTag("quest-filter-clear").performClick()
        rule.onNodeWithTag("quest-filter-summary").assertTextEquals("All quests")
        rule.onNodeWithTag("quest-count").assertTextEquals("354 quests")
    }

    @Test
    fun emptyQuestSearchShowsZeroCountWithoutCrashing() {
        openQuests()
        rule.onNodeWithTag("global-search").performTextInput("definitely-no-quest-match")
        rule.onNodeWithTag("quest-count").assertTextEquals("0 quests")
    }

    private fun openQuests() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-guide-category-strip")
            .performScrollToNode(hasTestTag("field-filter-quest"))
        rule.onNodeWithTag("field-filter-quest").performClick()
    }
}
