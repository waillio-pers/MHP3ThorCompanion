package com.waillio.mhp3rdcompanion

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.SmallMonsterRewardContext
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
class SmallMonsterUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data

    @Test fun fieldGuideExposesDistinctPopulatedSmallMonsterIndexAndRestoresState() {
        openSmallIndex()
        rule.onNodeWithTag("field-filter-monster").assertIsDisplayed()
        rule.onNodeWithTag("field-filter-small_monster").assertIsDisplayed().assertIsSelected()
        rule.onNodeWithTag("small-monster-index-count").assertDoesNotExist()
        rule.onNodeWithTag("small-monster-card-small_monster_altaroth").assertIsDisplayed()
        rule.onNodeWithTag("monster-card-monster_great_jaggi").assertDoesNotExist()

        rule.onNodeWithTag("global-search").performTextInput("Bnahabra")
        rule.onNodeWithTag("small-monster-index-count").assertDoesNotExist()
        rule.onNodeWithTag("small-monster-card-small_monster_bnahabra").performClick()
        rule.onNodeWithTag("screen-small-monster").assertIsDisplayed()
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.onNodeWithTag("global-search").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Bnahabra"))
        )
        rule.onNodeWithTag("small-monster-card-small_monster_bnahabra").assertIsDisplayed()

        rule.onNodeWithTag("global-search").performTextClearance()
        rule.onNodeWithTag("small-monster-grid").performScrollToNode(hasTestTag("small-monster-card-small_monster_wroggi"))
        val before = rule.onNodeWithTag("small-monster-card-small_monster_wroggi").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("small-monster-card-small_monster_wroggi").performClick()
        rule.activity.onBackPressedDispatcher.onBackPressed()
        assertEquals(before, rule.onNodeWithTag("small-monster-card-small_monster_wroggi").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun bnahabraTipAndVariantTablesRenderFromData() {
        openSmallMonster("Bnahabra", "small_monster_bnahabra")
        rule.onNodeWithText("Important Note").assertIsDisplayed()
        rule.onNodeWithText("Poison kills preserve the corpse for carving; ordinary kills usually shatter it.").assertIsDisplayed()
        listOf(
            "Body Carve · Pale White · Misty Peaks / Tundra",
            "Body Carve · Pale Brown · Sandy Plains",
            "Body Carve · Pale Blue · Flooded Forest / Deserted Island",
            "Body Carve · Pale Red · Volcano"
        ).forEach { title ->
            rule.onNodeWithTag("screen-small-monster").performScrollToNode(hasText(title))
            rule.onNodeWithText(title).assertIsDisplayed()
        }
    }

    @Test fun altarothKelbiAndGargwaConditionalGroupsStaySeparate() {
        openSmallMonster("Altaroth", "small_monster_altaroth")
        rule.onNodeWithTag("screen-small-monster").performScrollToNode(hasText("Shiny Drop · Gold abdomen"))
        rule.onNodeWithText("Shiny Drop · Gold abdomen").assertIsDisplayed()

        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-small_monster").performClick()
        rule.onNodeWithTag("global-search").performTextInput("Kelbi")
        rule.onNodeWithTag("small-monster-card-small_monster_kelbi").performClick()
        rule.onNodeWithTag("screen-small-monster").performScrollToNode(hasText("Body Carve · Stunned"))
        rule.onNodeWithText("Body Carve · Stunned").assertIsDisplayed()
        rule.onNodeWithText("100%", useUnmergedTree = true).assertExists()

        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-small_monster").performClick()
        rule.onNodeWithTag("global-search").performTextInput("Gargwa")
        rule.onNodeWithTag("small-monster-card-small_monster_gargwa").performClick()
        rule.onNodeWithTag("screen-small-monster").performScrollToNode(hasText("Shiny Drop · Gold Egg"))
        rule.onNodeWithText("Shiny Drop · Normal Drop").assertExists()
        rule.onNodeWithText("Shiny Drop · White Egg").assertExists()
        rule.onNodeWithText("Shiny Drop · Gold Egg").assertIsDisplayed()
    }

    @Test fun rewardItemOpensExistingItemAndBackPreservesMonsterContext() {
        val kelbi = data.smallMonsters.single { it.name == "Kelbi" }
        val reward = kelbi.rewards.single {
            it.context == SmallMonsterRewardContext.LOW && it.condition == "STUNNED"
        }
        val item = data.materials.single { it.gameItemId == reward.gameItemId }
        openSmallMonster("Kelbi", kelbi.id)
        rule.onNodeWithTag("screen-small-monster").performScrollToNode(hasTestTag("small-reward-${reward.id}"))
        rule.onNodeWithTag("small-reward-${reward.id}").performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
        rule.onNodeWithText(item.name).assertIsDisplayed()
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.onNodeWithTag("screen-small-monster").assertIsDisplayed()
        rule.onNodeWithTag("small-reward-context-LOW").assertIsSelected()
        rule.onNodeWithTag("screen-small-monster").performScrollToNode(hasTestTag("small-reward-${reward.id}"))
        rule.onNodeWithTag("small-reward-${reward.id}").assertIsDisplayed()
    }

    @Test fun relatedQuestsUseTypedSmallMonsterTargetsAndUnsupportedContextIsHidden() {
        openSmallMonster("Jaggi", "small_monster_jaggi")
        rule.onNodeWithTag("screen-small-monster").performScrollToNode(hasTestTag("small-monster-quest-quest_village_1_star_03"))
        rule.onNodeWithText("Jaggi Takedown").assertIsDisplayed()
        rule.onNodeWithText("Great Jaggi", substring = true).assertDoesNotExist()

        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-small_monster").performClick()
        rule.onNodeWithTag("global-search").performTextInput("Uroktor")
        rule.onNodeWithTag("small-monster-card-small_monster_uroktor").performClick()
        rule.onNodeWithTag("small-reward-context-LOW").assertIsDisplayed()
        rule.onNodeWithTag("small-reward-context-HIGH").assertIsDisplayed()
        rule.onNodeWithTag("small-reward-context-GUILD_1_2").assertDoesNotExist()
    }

    private fun openSmallIndex() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-small_monster").performClick()
    }

    private fun openSmallMonster(name: String, id: String) {
        openSmallIndex()
        rule.onNodeWithTag("global-search").performTextInput(name)
        rule.onNodeWithTag("small-monster-card-$id").performClick()
        rule.onNodeWithTag("screen-small-monster").assertIsDisplayed()
    }
}
