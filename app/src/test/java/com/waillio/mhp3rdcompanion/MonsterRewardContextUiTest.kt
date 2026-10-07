package com.waillio.mhp3rdcompanion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.waillio.mhp3rdcompanion.data.*
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
class MonsterRewardContextUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun contextSelectorFiltersRowsAndSurvivesTabSwitch() {
        setMonsterContent(
            listOf(
                MonsterReward("Guild Scale", "Body Carve", "Body", "50%", RewardContext.GUILD_1_2, 1, 1, "guild-scale"),
                MonsterReward("Guild Fang", "Part Break", "Head", "25%", RewardContext.GUILD_1_2, 1, 2, "guild-fang"),
                MonsterReward("Low Scale", "Body Carve", "Body", "40%", RewardContext.LOW, 1, 3, "low-scale"),
                MonsterReward("High Scale+", "Body Carve", "Body", "35%", RewardContext.HIGH, 1, 4, "high-scale")
            )
        )
        rule.onNodeWithTag("monster-tab-1").performClick()

        rule.onNodeWithTag("reward-context-reward-test-LOW").assertIsSelected()
        rule.onNodeWithTag("reward-context-reward-test-LOW").assertExists()
        rule.onNodeWithTag("reward-context-reward-test-HIGH").assertExists()
        rule.onNodeWithTag("reward-context-reward-test-VILLAGE_2_SPECIAL").assertDoesNotExist()
        rule.onNodeWithText("Guild Scale").assertDoesNotExist()
        rule.onNodeWithText("Body Carve", substring = true).assertExists()
        rule.onNodeWithText("Head Break").assertDoesNotExist()

        rule.onNodeWithTag("reward-context-reward-test-GUILD_1_2").performClick().assertIsSelected()
        rule.onNodeWithText("Guild Scale").assertExists()
        rule.onNodeWithText("Head Break", substring = true).assertExists()
        rule.onNodeWithText("Low Scale").assertDoesNotExist()
        rule.onNodeWithText("High Scale+").assertDoesNotExist()

        rule.onNodeWithTag("monster-tab-0").performClick()
        rule.onNodeWithTag("monster-tab-1").performClick()
        rule.onNodeWithTag("reward-context-reward-test-GUILD_1_2").assertIsSelected()
        rule.onNodeWithText("Guild Scale").assertExists()
        rule.onNodeWithTag("screen-monster").assertIsDisplayed()
    }

    @Test
    fun highOnlyMonsterGetsNoEmptyLowRankSelector() {
        setMonsterContent(listOf(MonsterReward("Rare Scale+", "Body Carve", "Body", "100%", RewardContext.HIGH, 1, 9, "rare-scale")))
        rule.onNodeWithTag("monster-tab-1").performClick()

        rule.onNodeWithTag("reward-context-reward-test-HIGH").assertIsSelected()
        rule.onNodeWithTag("reward-context-reward-test-LOW").assertDoesNotExist()
        rule.onNodeWithTag("reward-context-reward-test-GUILD_1_2").assertDoesNotExist()
        rule.onNodeWithTag("reward-context-reward-test-VILLAGE_2_SPECIAL").assertDoesNotExist()
        rule.onNodeWithText("Rare Scale+").assertExists()
    }

    @Test
    fun allModeKeepsRoutesInItemLocalCellsAndFilteredRowsStayCompact() {
        setMonsterContent(
            listOf(
                MonsterReward("Shared Scale", "Body Carve", "Body", "50%", RewardContext.LOW, 1, 25, "body"),
                MonsterReward("Shared Scale", "Tail Carve", "Tail", "22%", RewardContext.LOW, 1, 25, "tail"),
                MonsterReward("Shared Scale", "Part Break", "Head", "22%", RewardContext.LOW, 2, 25, "head-a"),
                MonsterReward("Shared Scale", "Part Break", "Head", "11%", RewardContext.LOW, 1, 25, "head-b"),
                MonsterReward("Rare Fang", "Capture", "Capture", "4%", RewardContext.LOW, 1, 26, "capture")
            )
        )
        rule.onNodeWithTag("monster-tab-1").performClick()
        rule.onAllNodesWithText("Rewards").assertCountEquals(1)
        rule.onNodeWithTag("reward-filter-reward-test-all").assertIsSelected()
        rule.onNodeWithTag("reward-filter-reward-test-carve").assertExists()
        rule.onNodeWithTag("reward-filter-reward-test-capture").assertExists()
        rule.onNodeWithTag("reward-filter-reward-test-break").assertExists()
        rule.onNodeWithTag("reward-filter-reward-test-shiny").assertDoesNotExist()
        rule.onNodeWithText("Shared Scale").assertExists()
        rule.onNodeWithTag("monster-reward-item-reward-test-25").assertExists()
        rule.onNodeWithTag("monster-reward-routes-reward-test-25").assertExists()
        rule.onNodeWithTag("monster-reward-route-label-reward-test-25-0").assertTextEquals("Body Carve")
        rule.onNodeWithTag("monster-reward-route-values-reward-test-25-0").assertTextEquals("50% · ×1")
        rule.onNodeWithTag("monster-reward-route-label-reward-test-25-1").assertTextEquals("Tail Carve")
        rule.onNodeWithTag("monster-reward-route-label-reward-test-25-2").assertTextEquals("Head Break")
        rule.onNodeWithTag("monster-reward-route-values-reward-test-25-2").assertTextEquals("22% · ×2 / 11% · ×1")
        rule.onNodeWithTag("reward-mode-matrix-reward-test").assertDoesNotExist()
        rule.onNodeWithTag("monster-reward-matrix-reward-test").assertDoesNotExist()

        rule.onNodeWithTag("reward-filter-reward-test-carve").performClick()
        rule.onNodeWithTag("monster-reward-route-label-reward-test-25-0").assertTextEquals("Body Carve")
        rule.onNodeWithTag("monster-reward-route-values-reward-test-25-0").assertTextEquals("50% · ×1")
        rule.onNodeWithTag("monster-reward-route-label-reward-test-25-1").assertTextEquals("Tail Carve")
        rule.onNodeWithTag("monster-reward-route-values-reward-test-25-1").assertTextEquals("22% · ×1")

        rule.onNodeWithTag("reward-filter-reward-test-break").performClick()
        rule.onNodeWithTag("monster-reward-item-reward-test-25").assertExists()
        rule.onNodeWithTag("monster-reward-item-reward-test-26").assertDoesNotExist()
        rule.onNodeWithTag("monster-reward-route-label-reward-test-25-0").assertTextEquals("Head Break")
        rule.onNodeWithTag("monster-reward-route-values-reward-test-25-0").assertTextEquals("22% · ×2 / 11% · ×1")
        val breakItem = rule.onNodeWithTag("monster-reward-item-reward-test-25").fetchSemanticsNode().boundsInRoot
        val breakRoutes = rule.onNodeWithTag("monster-reward-route-label-reward-test-25-0").fetchSemanticsNode().boundsInRoot
        assertEquals("a single filtered route shares the Item row", breakItem.center.y, breakRoutes.center.y, 1f)

        rule.onNodeWithTag("reward-filter-reward-test-capture").performClick()
        val simpleItem = rule.onNodeWithTag("monster-reward-item-reward-test-26").fetchSemanticsNode().boundsInRoot
        val simpleRoute = rule.onNodeWithTag("monster-reward-route-label-reward-test-26-0").fetchSemanticsNode().boundsInRoot
        assertEquals("single-route Items use a one-line row", simpleItem.center.y, simpleRoute.center.y, 1f)
        rule.onNodeWithTag("monster-reward-route-label-reward-test-26-0").assertTextEquals("Capture")
        rule.onNodeWithTag("monster-reward-route-values-reward-test-26-0").assertTextEquals("4% · ×1")
    }

    @Test
    fun rankAndFilterControlsStayOnOneRowAtNarrowPreviewWidth() {
        setMonsterContent(
            listOf(
                MonsterReward("Scale", "Body Carve", "Body", "50%", RewardContext.LOW, 1, 25, "low"),
                MonsterReward("Scale+", "Body Carve", "Body", "40%", RewardContext.HIGH, 1, 26, "high")
            )
        )
        rule.onNodeWithTag("monster-tab-1").performClick()
        val rankBounds = rule.onNodeWithTag("reward-context-selector-reward-test").fetchSemanticsNode().boundsInRoot
        val filterBounds = rule.onNodeWithTag("reward-method-filter-strip-reward-test").fetchSemanticsNode().boundsInRoot
        assertEquals("controls share one row", rankBounds.center.y, filterBounds.center.y, 1f)
        assertTrue("rank controls stay left of the method group", rankBounds.right < filterBounds.left)
        rule.onNodeWithTag("reward-mode-matrix-reward-test").assertDoesNotExist()
    }

    @Test
    fun rankAndMethodControlsUseSeparateRowsOnCompactPortrait() {
        setMonsterContent(
            listOf(
                MonsterReward("Scale", "Body Carve", "Body", "50%", RewardContext.LOW, 1, 25, "low"),
                MonsterReward("Scale+", "Body Carve", "Body", "40%", RewardContext.HIGH, 1, 26, "high")
            ),
            layout = appWindowLayout(360, 800)
        )
        rule.onNodeWithTag("monster-tab-1").performClick()

        val rankBounds = rule.onNodeWithTag("reward-context-selector-reward-test").fetchSemanticsNode().boundsInRoot
        val methodBounds = rule.onNodeWithTag("reward-method-filter-strip-reward-test").fetchSemanticsNode().boundsInRoot
        val controlsBounds = rule.onNodeWithTag("monster-reward-controls-reward-test").fetchSemanticsNode().boundsInRoot
        assertTrue("rank selectors occupy the first control row", rankBounds.bottom < methodBounds.top)
        assertTrue("the mobile controls container is tall enough for both rows", controlsBounds.height >= rankBounds.height + methodBounds.height)
        rule.onNodeWithTag("reward-context-reward-test-LOW").assertIsDisplayed()
        rule.onNodeWithTag("reward-filter-reward-test-all").assertIsDisplayed()
    }

    @Test
    fun allModeUsesOneFixedItemColumnAndTwoAlignedRouteColumns() {
        setMonsterContent(
            listOf(
                MonsterReward("Short", "Body Carve", "Body", "50%", RewardContext.LOW, 1, 25, "short-body"),
                MonsterReward("Short", "Capture", "Capture", "20%", RewardContext.LOW, 1, 25, "short-capture"),
                MonsterReward("Long Rathian Scale Plus", "Body Carve", "Body", "40%", RewardContext.LOW, 1, 26, "long-body")
            )
        )
        rule.onNodeWithTag("monster-tab-1").performClick()

        val shortIdentity = rule.onNodeWithTag("monster-reward-item-reward-test-25").fetchSemanticsNode().boundsInRoot
        val longIdentity = rule.onNodeWithTag("monster-reward-item-reward-test-26").fetchSemanticsNode().boundsInRoot
        val shortRoutes = rule.onNodeWithTag("monster-reward-routes-reward-test-25").fetchSemanticsNode().boundsInRoot
        val longRoutes = rule.onNodeWithTag("monster-reward-routes-reward-test-26").fetchSemanticsNode().boundsInRoot
        assertEquals("every Item shares one fixed-width identity region", shortIdentity.width, longIdentity.width, 1f)
        assertEquals("every All-mode route begins at one X coordinate", shortRoutes.left, longRoutes.left, 1f)
        rule.onNodeWithTag("monster-reward-identity-divider-reward-test-25").assertExists()
        rule.onNodeWithTag("monster-reward-route-column-divider-reward-test-25-0").assertExists()
    }

    private fun setMonsterContent(
        rewards: List<MonsterReward>,
        layout: AppWindowLayout = appWindowLayout(620, 540)
    ) {
        val monster = Monster(
            id = "reward-test", name = "Reward Test", subtitle = "Flying Wyvern", type = "Flying Wyvern",
            hitzones = listOf(Hitzone("Head", 50, 50, 50, 0, 0, 0, 0, 0)), rewards = rewards
        )
        rule.setContent {
            CompositionLocalProvider(LocalAppWindowLayout provides layout) {
                CompanionTheme {
                    DetailShell(selectedRoute = Routes.MONSTER, onNavigate = {}) { padding ->
                        val tab = remember { mutableIntStateOf(0) }
                        Box(Modifier.padding(padding)) {
                            MonsterScreen(monster, emptyList(), emptyList(), tab.intValue, false, {}, {}, { tab.intValue = it }, {})
                        }
                    }
                }
            }
        }
    }
}
