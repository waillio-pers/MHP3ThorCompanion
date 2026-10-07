package com.waillio.mhp3rdcompanion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.waillio.mhp3rdcompanion.data.*
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
class InvasionRewardsCollapseUiTest {
    @get:Rule val rule = createComposeRule()
    private val uiScale = mutableFloatStateOf(1f)

    @Test
    fun monsterWithoutInvasionRewardsHasNoHeader() {
        setMonster(invasionRewards = emptyList())
        openRewards()

        rule.onNodeWithTag("monster-invasion-rewards-toggle-$monsterId").assertDoesNotExist()
    }

    @Test
    fun invasionRewardsStartCollapsedWithOnlyACompactHeader() {
        setMonster()
        openRewards()

        rule.onNodeWithTag("monster-rewards-single-scroll-$monsterId")
            .performScrollToNode(hasTestTag("monster-invasion-rewards-toggle-$monsterId"))
        val header = rule.onNodeWithTag("monster-invasion-rewards-toggle-$monsterId")
        header.assertIsDisplayed()
        val headerHeightPx = header.fetchSemanticsNode().boundsInRoot.height
        assertTrue("collapsed section should be one short header row, was ${headerHeightPx}px", headerHeightPx < 80f)
        rule.onNodeWithText("Item 212").assertDoesNotExist()
    }

    @Test
    fun expandingKeepsAllRowsInTheMainRewardScrollAndSurvivesRankAndMethodChanges() {
        setMonster()
        openRewards()

        rule.onNodeWithTag("monster-rewards-single-scroll-$monsterId").assert(hasScrollAction())
        rule.onNodeWithTag("monster-rewards-single-scroll-$monsterId")
            .performScrollToNode(hasTestTag("monster-invasion-rewards-toggle-$monsterId"))
        rule.onNodeWithTag("monster-invasion-rewards-toggle-$monsterId")
            .performClick()

        rule.onNodeWithText("Item 212").assertExists()
        rule.onNodeWithText("×2 · >=20%").assertExists()
        rule.onNodeWithText("Item 213").assertExists()
        rule.onNodeWithTag("monster-rewards-single-scroll-$monsterId")
            .performScrollToNode(hasTestTag("monster-reward-item-$monsterId-101"))
        rule.onNodeWithTag("monster-reward-item-$monsterId-101")
            .assertIsDisplayed()

        rule.onNodeWithTag("reward-filter-$monsterId-carve").performClick()
        rule.onNodeWithTag("monster-rewards-single-scroll-$monsterId")
            .performScrollToNode(hasTestTag("monster-invasion-rewards-toggle-$monsterId"))
        rule.onNodeWithText("Item 212").assertExists()
        rule.onNodeWithTag("reward-context-$monsterId-HIGH").performClick()
        rule.onNodeWithTag("monster-rewards-single-scroll-$monsterId")
            .performScrollToNode(hasTestTag("monster-invasion-rewards-toggle-$monsterId"))
        rule.onNodeWithText("Item 213").assertExists()

        rule.onNodeWithTag("monster-rewards-single-scroll-$monsterId")
            .performScrollToNode(hasTestTag("monster-invasion-rewards-toggle-$monsterId"))
        rule.onNodeWithTag("monster-invasion-rewards-toggle-$monsterId").performClick()
        rule.onNodeWithText("Item 213").assertDoesNotExist()
    }

    @Test
    fun headerAndExpansionRemainUsableAtSupportedUiScales() {
        setMonster()
        openRewards()

        listOf(0.80f, 1.00f, 1.10f).forEach { scale ->
            rule.runOnIdle { uiScale.floatValue = scale }
            rule.waitForIdle()
            rule.onNodeWithTag("monster-rewards-single-scroll-$monsterId")
                .performScrollToNode(hasTestTag("monster-invasion-rewards-toggle-$monsterId"))
            rule.onNodeWithTag("monster-invasion-rewards-toggle-$monsterId").assertIsDisplayed()
            rule.onNodeWithText("Invasion Rewards").assertIsDisplayed()
            rule.onNodeWithTag("monster-invasion-rewards-toggle-$monsterId").performClick()
            rule.onNodeWithText("Item 212").assertExists()
            rule.onNodeWithTag("monster-invasion-rewards-toggle-$monsterId").performClick()
            rule.onNodeWithText("Item 212").assertDoesNotExist()
        }
    }

    private fun openRewards() {
        rule.onNodeWithTag("monster-tab-1").performClick()
    }

    private fun setMonster(
        invasionRewards: List<InvasionReward> = invasionFixture()
    ) {
        val rewards = (1..18).map { index ->
            MonsterReward(
                item = "Reward $index",
                sourceType = if (index % 2 == 0) "Body Carve" else "Capture",
                condition = if (index % 2 == 0) "Body" else "Capture",
                chance = "50%",
                rank = if (index == 18) RewardContext.HIGH else RewardContext.LOW,
                gameItemId = 100 + index
            )
        }
        val monster = Monster(
            id = monsterId,
            name = "Invasion Test",
            subtitle = "Flying Wyvern",
            type = "Flying Wyvern",
            rewards = rewards,
            invasionRewards = invasionRewards
        )
        rule.setContent {
            val platformDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = platformDensity.density * uiScale.floatValue,
                    fontScale = platformDensity.fontScale
                )
            ) {
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

    private fun invasionFixture() = listOf(
        InvasionReward(
            id = "drop-low",
            monsterId = monsterId,
            context = "LOW",
            gameItemId = 212,
            quantity = 2,
            probabilityBand = InvasionProbabilityBand.AT_LEAST_20
        ),
        InvasionReward(
            id = "drop-high",
            monsterId = monsterId,
            context = "HIGH",
            gameItemId = 213,
            quantity = 1,
            probabilityBand = InvasionProbabilityBand.UNDER_2
        )
    )

    private companion object {
        const val monsterId = "invasion-test"
    }
}
