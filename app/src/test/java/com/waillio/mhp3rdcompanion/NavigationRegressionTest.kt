package com.waillio.mhp3rdcompanion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NavigationRegressionTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun bottomNavigationIsReferenceOnly() {
        rule.onNodeWithTag("nav-home").assertIsDisplayed()
        rule.onNodeWithTag("nav-search").assertIsDisplayed()
        rule.onNodeWithTag("nav-favorites").assertIsDisplayed()
        rule.onNodeWithText("Hunt List").assertDoesNotExist()
        rule.onNodeWithText("Current Hunt").assertDoesNotExist()
        rule.onNodeWithTag("set-current-hunt").assertDoesNotExist()
    }

    @Test fun homeMapsTileOpensMapsIndexAndFieldGuideBottomNavRemainsIntact() {
        rule.onNodeWithTag("category-maps").assertIsDisplayed().performClick()
        rule.onNodeWithTag("maps-index").assertIsDisplayed()
        rule.onNodeWithText("Hunting Maps").assertDoesNotExist()

        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-monster").assertIsDisplayed()
        rule.onNodeWithTag("nav-home").assertIsDisplayed()
        rule.onNodeWithTag("nav-favorites").assertIsDisplayed()
    }

    @Test fun hrOnlyItemFocusShowsStaticRankWithoutDeadControls() {
        openMaterial("Bathycite Ore", "item_deposite_ore")
        rule.onNodeWithTag("field-show-map-flooded_forest").performClick()
        rule.onNodeWithTag("map-detail-flooded_forest").assertIsDisplayed()
        rule.onNodeWithTag("map-focus-rank-static").assertIsDisplayed()
        rule.onNodeWithText("HR").assertIsDisplayed()
        rule.onNodeWithTag("map-rank-all").assertDoesNotExist()
        rule.onNodeWithTag("map-rank-low").assertDoesNotExist()
        rule.onNodeWithTag("map-rank-high").assertDoesNotExist()
    }

    @Test fun dualRankItemFocusRetainsAllLowHighControls() {
        openMaterial("Stone", "item_stone")
        rule.onNodeWithTag("field-show-map-misty_peaks").performClick()
        rule.onNodeWithTag("map-detail-misty_peaks").assertIsDisplayed()
        rule.onNodeWithTag("map-rank-all").assertIsDisplayed()
        rule.onNodeWithTag("map-rank-low").assertIsDisplayed()
        rule.onNodeWithTag("map-rank-high").assertIsDisplayed()
        rule.onNodeWithTag("map-focus-rank-static").assertDoesNotExist()
    }

    @Test fun favoriteMonsterAddsOpensRemovesAndPersists() {
        openMonster("Zinogre", "monster_zinogre")
        ensureNotFavorite()
        rule.onNodeWithContentDescription("Add to favorites").performClick()
        rule.waitForIdle()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("nav-favorites").performClick()
        rule.onNodeWithTag("favorites-list").performScrollToNode(hasTestTag("favorite-monster-monster_zinogre"))
        rule.onNodeWithTag("favorite-monster-monster_zinogre").assertIsDisplayed().performClick()
        rule.onNodeWithTag("screen-monster").assertIsDisplayed()
        rule.onNodeWithContentDescription("Remove from favorites").assertIsDisplayed()
        rule.onNodeWithTag("monster-favorite").performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithContentDescription("Add to favorites").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Add to favorites").assertIsDisplayed()
        rule.onNodeWithTag("nav-favorites").performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag("favorite-monster-monster_zinogre").fetchSemanticsNodes().isEmpty()
        }
        rule.onNodeWithTag("favorite-monster-monster_zinogre").assertDoesNotExist()
    }

    @Test fun favoriteMaterialAddsOpensAndPersists() {
        openMaterial("Mega Demondrug", "item_mega_demondrug")
        ensureNotFavorite()
        rule.onNodeWithContentDescription("Add to favorites").performClick()
        rule.waitForIdle()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("nav-favorites").performClick()
        rule.onNodeWithTag("favorites-list").performScrollToNode(hasTestTag("favorite-material-item_mega_demondrug"))
        rule.onNodeWithTag("favorite-material-item_mega_demondrug").assertIsDisplayed().performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
        rule.onNodeWithText("Mega Demondrug").assertIsDisplayed()
    }

    @Test fun realRecentIsDeduplicatedNewestFirstAndPersists() {
        openMonster("Zinogre", "monster_zinogre")
        rule.onNodeWithTag("nav-home").performClick()
        rule.onNodeWithTag("category-materials").performClick()
        rule.onNodeWithTag("global-search").performTextInput("Ice Crystal")
        rule.onNodeWithTag("result-material-item_ice_crystal").performClick()
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("global-search").performTextInput("Zinogre")
        rule.onNodeWithTag("monster-card-monster_zinogre").performClick()
        rule.onNodeWithTag("nav-home").performClick()
        val zinogre = rule.onNodeWithTag("reference-monster-monster_zinogre").fetchSemanticsNode().boundsInRoot
        val crystal = rule.onNodeWithTag("reference-material-item_ice_crystal").fetchSemanticsNode().boundsInRoot
        assertTrue("reopened entity must move to the front", zinogre.top <= crystal.top)
        rule.waitForIdle()
        rule.activityRule.scenario.recreate()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag("reference-monster-monster_zinogre").fetchSemanticsNodes().isNotEmpty() &&
                rule.onAllNodesWithTag("reference-material-item_ice_crystal").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("reference-monster-monster_zinogre").assertIsDisplayed()
        rule.onNodeWithTag("reference-material-item_ice_crystal").assertIsDisplayed()
    }

    @Test fun alphabetScrubberClickJumpsToLetter() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("alphabet-scrubber").assertIsDisplayed()
        rule.onNodeWithTag("alphabet-letter-Z").performClick()
        rule.onNodeWithTag("monster-card-monster_zinogre").assertIsDisplayed()
    }

    @Test fun alphabetScrubberDragJumpsThroughLetters() {
        rule.onNodeWithTag("nav-search").performClick()
        val node = rule.onNodeWithTag("alphabet-scrubber")
        node.performTouchInput { swipe(Offset(centerX, 1f), Offset(centerX, height - 1f), 700) }
        rule.onNodeWithTag("monster-card-monster_zinogre").assertIsDisplayed()
    }

    @Test fun scrubberIsHiddenDuringTextSearch() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("global-search").performTextInput("Zinogre")
        rule.onNodeWithTag("alphabet-scrubber").assertDoesNotExist()
    }

    @Test fun zinogreAlternateSelectorChangesHitzoneTable() {
        openMonster("Zinogre", "monster_zinogre")
        rule.onNodeWithTag("monster-tab-2").performClick()
        rule.onNodeWithTag("hitzone-state-normal").assertIsDisplayed()
        rule.onNodeWithText("Supercharged").assertIsDisplayed()
        rule.onNodeWithTag("hitzones-list-normal").assertIsDisplayed()
        rule.onNodeWithTag("hitzone-state-alternate").performClick()
        rule.onNodeWithTag("hitzones-list-alternate").assertIsDisplayed()
        rule.onNodeWithTag("hitzone-elemental").performClick()
        rule.onAllNodesWithText("Impact").assertCountEquals(0)
    }

    @Test fun normalOnlyMonsterHasNoStateSelector() {
        openMonster("Arzuros", "monster_arzuros")
        rule.onNodeWithTag("monster-tab-2").performClick()
        rule.onNodeWithTag("hitzone-state-normal").assertDoesNotExist()
        rule.onNodeWithTag("hitzone-physical").assertIsDisplayed()
    }

    @Test fun previouslySourceLessMaterialShowsImportedSourcesWithoutFakeUses() {
        openMaterial("Ice Crystal", "item_ice_crystal")
        rule.onNodeWithText("Sources").assertIsDisplayed()
        rule.onNodeWithText("Used In").assertDoesNotExist()
        rule.onNodeWithText("Related Quests").assertDoesNotExist()
        rule.onNodeWithText("Rarity", substring = true).assertDoesNotExist()
    }

    @Test fun conditionalQuestRewardSourceShowsHumanReadableCondition() {
        openMaterial("Barroth Tail", "item_barroth_tail")
        rule.onNodeWithText("Slay 6 monsters", substring = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Additional", substring = true).assertExists()
        rule.onNodeWithText("40% reward slot", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun materialSearchBackRestoresProductionQueryAndResult() {
        rule.onNodeWithTag("global-search").performTextInput("Ice Crystal")
        rule.onNodeWithTag("result-material-item_ice_crystal").performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()

        rule.activity.onBackPressedDispatcher.onBackPressed()

        rule.onNodeWithTag("global-search").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Ice Crystal"))
        )
        rule.onNodeWithTag("result-material-item_ice_crystal").assertIsDisplayed()
    }

    @Test fun itemsBrowseIsPopulatedWithoutQueryAndPromotedItemPreservesState() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-material").performClick()
        rule.onNodeWithTag("item-index-count").assertDoesNotExist()
        rule.onNodeWithTag("search-results").performScrollToNode(hasTestTag("result-material-item_divine_rhino"))
        val before = rule.onNodeWithTag("result-material-item_divine_rhino").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("result-material-item_divine_rhino").performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
        rule.onNodeWithText("Divine Rhino").assertIsDisplayed()
        ensureNotFavorite()
        rule.onNodeWithContentDescription("Add to favorites").performClick()
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.onNodeWithTag("global-search").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
        )
        assertEquals(before, rule.onNodeWithTag("result-material-item_divine_rhino").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun monsterSearchBackAndScrollRestorationStillWork() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("global-search").performTextInput("Alatreon")
        rule.onNodeWithTag("monster-card-monster_alatreon").performClick()
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.onNodeWithTag("global-search").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Alatreon"))
        )
        rule.onNodeWithTag("monster-card-monster_alatreon").assertIsDisplayed()

        rule.onNodeWithTag("global-search").performTextClearance()
        rule.onNodeWithTag("monster-grid").performScrollToNode(hasTestTag("monster-card-monster_zinogre"))
        val before = rule.onNodeWithTag("monster-card-monster_zinogre").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("monster-card-monster_zinogre").performClick()
        rule.activity.onBackPressedDispatcher.onBackPressed()
        assertEquals(before, rule.onNodeWithTag("monster-card-monster_zinogre").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun existingQuestAndRewardNavigationDoNotRegress() {
        openMonster("Zinogre", "monster_zinogre")
        rule.onNodeWithTag("monster-tab-1").performClick()
        rule.onNodeWithTag("reward-context-monster_zinogre-HIGH").performClick()
        rule.onNodeWithText("Zinogre Claw+").performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.onNodeWithTag("monster-rewards").assertIsDisplayed()
        rule.onNodeWithTag("reward-context-monster_zinogre-HIGH").assertIsSelected()
        rule.onNodeWithTag("monster-tab-3").performClick()
        rule.onNodeWithTag("quests-list")
            .performScrollToNode(hasTestTag("monster-quest-quest_guild_8_star_01"))
        rule.onNodeWithTag("monster-quest-quest_guild_8_star_01")
            .assertIsDisplayed()
            .assertHasNoClickAction()
    }

    private fun openMonster(name: String, id: String) {
        rule.onNodeWithTag("global-search").performTextInput(name)
        rule.onNodeWithTag("result-monster-$id").performClick()
        rule.onNodeWithTag("screen-monster").assertIsDisplayed()
    }

    private fun openMaterial(name: String, id: String) {
        rule.onNodeWithTag("global-search").performTextInput(name)
        rule.onNodeWithTag("result-material-$id").performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
    }

    private fun ensureNotFavorite() {
        if (rule.onAllNodesWithContentDescription("Remove from favorites").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithContentDescription("Remove from favorites").performClick()
        }
        rule.onNodeWithContentDescription("Add to favorites").assertIsDisplayed()
    }
}
