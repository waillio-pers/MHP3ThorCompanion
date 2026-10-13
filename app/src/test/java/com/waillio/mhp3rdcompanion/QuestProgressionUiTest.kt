package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuestProgressionUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun keyOnlyDefaultsOffAndFiltersToTheSelectedScopeAndStarWhenEnabled() {
        openQuests()
        rule.onNodeWithTag("quest-key-only-filter").assertIsDisplayed().assertIsNotSelected()
        rule.onNodeWithTag("quest-count").assertTextEquals("354 quests")
        selectFilters("village", null, "1")
        rule.onNodeWithTag("quest-progression-summary").assertIsDisplayed()
        rule.onNodeWithTag("quest-progression-urgent-summary").assertTextContains("Urgent: Blue Bear: Arzuros")
        rule.onNodeWithTag("quest-key-only-filter").performClick().assertIsSelected()
        rule.onNodeWithTag("quest-count").assertTextEquals("3 quests")
        rule.onNodeWithTag("quest-list").performScrollToNode(hasTestTag("quest-row-quest_village_1_star_03"))
        rule.onNodeWithTag("quest-role-badge-key-quest_village_1_star_03").assertIsDisplayed()
        rule.onNodeWithTag("quest-row-quest_village_1_star_01").assertDoesNotExist()
        rule.onNodeWithTag("quest-key-only-filter").performClick().assertIsNotSelected()
        rule.onNodeWithTag("quest-count").assertTextEquals("5 quests")
    }

    @Test
    fun mappedUrgentGetsItsBadgeButLastSupperIsAnOptionalChallenge() {
        openQuests()
        selectFilters("village", null, "2")
        rule.onNodeWithTag("quest-list").performScrollToNode(hasTestTag("quest-row-quest_village_2_star_04"))
        rule.onNodeWithTag("quest-role-badge-urgent-quest_village_2_star_04").assertIsDisplayed()

        selectFilters("village", null, "6")
        rule.onNodeWithTag("quest-list").performScrollToNode(hasTestTag("quest-row-quest_village_6_star_16"))
        rule.onNodeWithTag("quest-role-badge-optional-quest_village_6_star_16").assertIsDisplayed()
        rule.onNodeWithTag("quest-role-badge-urgent-quest_village_6_star_16").assertDoesNotExist()
    }

    @Test
    fun blockedUrgentSummaryAndGuildHighPrerequisiteUseExistingQuestNames() {
        openQuests()
        selectFilters("guild", "high", "6")
        rule.onNodeWithTag("quest-key-only-filter").performClick().assertIsSelected()
        rule.onNodeWithTag("quest-progression-urgent-summary").assertTextContains("Urgent: Hunt Nargacuga")
        rule.onNodeWithTag("quest-progression-summary").assertIsDisplayed()
        assertFalse(rule.onNodeWithTag("quest-progression-urgent-summary").fetchSemanticsNode().config.contains(SemanticsActions.OnClick))
        rule.onNodeWithTag("quest-list").performScrollToNode(hasTestTag("quest-row-quest_guild_6_star_20"))
        rule.onNodeWithText("To unlock this quest: complete Water Flow Island Storm!").assertIsDisplayed()
    }

    @Test
    fun jadeBarrothRemainsOrdinaryAndGuildEightShowsJhenThenAmatsu() {
        openQuests()
        selectFilters("guild", "high", "7")
        rule.onNodeWithTag("quest-list").performScrollToNode(hasTestTag("quest-row-quest_guild_7_star_21"))
        rule.onNodeWithTag("quest-role-badge-key-quest_guild_7_star_21").assertDoesNotExist()

        selectFilters("guild", "high", "8")
        rule.onNodeWithTag("quest-progression-urgent-summary").assertTextContains("Urgent: Jhen Mohran → Amatsu")
        rule.onNodeWithText("Complete all 6 Key Quests").assertIsDisplayed()
        rule.onNodeWithTag("quest-list").performScrollToNode(hasTestTag("quest-row-quest_guild_8_star_02"))
        rule.onNodeWithTag("quest-role-badge-key-quest_guild_8_star_02").assertIsDisplayed()
    }

    @Test
    fun questProgressionHeaderScrollsAwayAndLocalControlsHideAndReturn() {
        openQuests()
        selectFilters("guild", "high", "8")
        rule.onNodeWithTag("quest-progression-summary").assertIsDisplayed()
        repeat(2) {
            rule.onNodeWithTag("quest-list").performTouchInput { swipeUp() }
            rule.waitForIdle()
        }
        rule.onNodeWithTag("quest-filters-button").assertDoesNotExist()
        rule.onNodeWithTag("quest-key-only-filter").assertDoesNotExist()
        rule.onNodeWithTag("quest-progression-summary").assertDoesNotExist()

        rule.onNodeWithTag("quest-list").performTouchInput { swipeDown() }
        rule.waitForIdle()
        repeat(3) {
            if (rule.onAllNodesWithTag("quest-progression-summary").fetchSemanticsNodes().isEmpty()) {
                rule.onNodeWithTag("quest-list").performTouchInput { swipeDown() }
                rule.waitForIdle()
            }
        }
        rule.onNodeWithTag("quest-filters-button").assertIsDisplayed()
        rule.onNodeWithTag("quest-progression-summary").assertIsDisplayed()
    }

    private fun openQuests() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-guide-category-strip").performScrollToNode(hasTestTag("field-filter-quest"))
        rule.onNodeWithTag("field-filter-quest").performClick()
    }

    private fun selectFilters(category: String, rank: String?, star: String) {
        if (rule.onAllNodesWithTag("quest-filters-button").fetchSemanticsNodes().isEmpty()) {
            rule.onNodeWithTag("quest-list").performTouchInput { swipeDown() }
            rule.waitForIdle()
        }
        rule.onNodeWithTag("quest-filters-button").performClick()
        rule.onNodeWithTag("quest-filter-reset").performClick()
        rule.onNodeWithTag("quest-filter-type-$category").performClick()
        if (rank != null) rule.onNodeWithTag("quest-filter-rank-$rank").performClick()
        rule.onNodeWithTag("quest-filter-star-$star").performClick()
        rule.onNodeWithTag("quest-filter-apply").performClick()
    }
}

class QuestOutlineStyleTest {
    @Test
    fun ordinaryKeyPrerequisiteAndCombinedRolesHaveStableOutlinePriority() {
        assertEquals(QuestOutlineStyle.NONE, questOutlineStyle(isKeyQuest = false, isPrerequisiteOnly = false))
        assertEquals(QuestOutlineStyle.KEY_SOLID, questOutlineStyle(isKeyQuest = true, isPrerequisiteOnly = false))
        assertEquals(QuestOutlineStyle.PREREQUISITE_DASHED, questOutlineStyle(isKeyQuest = false, isPrerequisiteOnly = true))
        assertEquals(QuestOutlineStyle.KEY_SOLID, questOutlineStyle(isKeyQuest = true, isPrerequisiteOnly = true))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuestProgressionNarrowPortraitUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun keyFilterAndBadgesRemainInsideThePortraitViewport() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-guide-category-strip").performScrollToNode(hasTestTag("field-filter-quest"))
        rule.onNodeWithTag("field-filter-quest").performClick()
        val viewport = rule.onRoot().fetchSemanticsNode().boundsInRoot
        val filters = rule.onNodeWithTag("quest-filters-button").fetchSemanticsNode().boundsInRoot
        val keyFilter = rule.onNodeWithTag("quest-key-only-filter").fetchSemanticsNode().boundsInRoot
        assert(filters.left >= viewport.left && filters.right <= viewport.right)
        assert(keyFilter.left >= viewport.left && keyFilter.right <= viewport.right)
        rule.onNodeWithTag("quest-key-only-filter").performClick().assertIsSelected()
        rule.onNodeWithTag("quest-list").performScrollToNode(hasTestTag("quest-row-quest_village_1_star_03"))
        val row = rule.onNodeWithTag("quest-row-quest_village_1_star_03").fetchSemanticsNode().boundsInRoot
        val badge = rule.onNodeWithTag("quest-role-badge-key-quest_village_1_star_03").fetchSemanticsNode().boundsInRoot
        assert(badge.left >= row.left && badge.right <= row.right)
        assert(badge.height > 0f && badge.width > 0f)
    }

    private fun hasTestTag(tag: String) = androidx.compose.ui.test.hasTestTag(tag)
}
