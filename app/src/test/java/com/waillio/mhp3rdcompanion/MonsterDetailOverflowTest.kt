package com.waillio.mhp3rdcompanion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
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
class MonsterDetailOverflowTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun everyLongTabReachesItsLastRowWhileShellStaysFixed() {
        val hitzones = (1..14).map { index -> Hitzone("Body Part $index", 40, 45, 35, 5, 10, 15, 20, 5) }
        val rewards = (1..14).map { index ->
            MonsterReward("Reward $index", "Part Break", "Part $index", "$index%", gameItemId = index)
        }
        val quests = (1..9).map { index -> Quest("quest_$index", "Quest $index", "Demo", "High Rank", "Misty Peaks", "Nargacuga", "Reward $index") }
        val monster = Monster(
            id = "overflow", name = "Nargacuga", subtitle = "Flying Wyvern", type = "Flying Wyvern", threatLevel = 6,
            weaknesses = ElementValues(3, 1, 2, 1, 1), recommendedParts = listOf("Head", "Forelegs", "Tail"),
            breakableParts = listOf(BreakablePart("Head", "Break"), BreakablePart("Tail", "Sever")),
            rewards = rewards, hitzones = hitzones, questIds = quests.map { it.id }
        )
        rule.setContent {
            CompanionTheme {
                DetailShell(selectedRoute = Routes.MONSTER, onNavigate = {}) { padding ->
                    val tab = remember { mutableIntStateOf(0) }
                    Box(Modifier.padding(padding)) {
                        MonsterScreen(monster, quests, emptyList(), tab.intValue, false, {}, {}, { tab.intValue = it }, {})
                    }
                }
            }
        }

        rule.onAllNodesWithTag("global-search").assertCountEquals(0)
        val fixedTags = listOf("contextual-header", "monster-tabs", "bottom-navigation")
        val fixedBounds = fixedTags.associateWith { rule.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot }
        val rootHeight = rule.onRoot().fetchSemanticsNode().boundsInRoot.height
        val contentHeight = rule.onNodeWithTag("monster-tab-content").fetchSemanticsNode().boundsInRoot.height
        assertTrue("Tab content must use at least 65% of the full viewport", contentHeight / rootHeight >= .65f)

        rule.onNodeWithTag("monster-tab-2").performClick()
        rule.onNodeWithTag("hitzones-list").performScrollToIndex(13)
        rule.onNodeWithTag("hitzone-row-13").assertIsDisplayed()
        assertFixed(fixedBounds)

        rule.onNodeWithTag("hitzone-elemental").performClick()
        rule.onNodeWithTag("hitzones-list").performScrollToIndex(13)
        rule.onNodeWithTag("hitzone-row-13").assertIsDisplayed()
        assertFixed(fixedBounds)

        rule.onNodeWithTag("monster-tab-1").performClick()
        rule.onNodeWithTag("monster-rewards-single-scroll-overflow")
            .performScrollToNode(hasTestTag("monster-reward-row-overflow-14"))
        rule.onNodeWithTag("monster-reward-row-overflow-14").assertIsDisplayed()
        assertFixed(fixedBounds)

        rule.onNodeWithTag("monster-tab-3").performClick()
        rule.onNodeWithTag("quests-list").performScrollToIndex(8)
        rule.onNodeWithTag("monster-quest-quest_9").assertIsDisplayed()
        assertFixed(fixedBounds)
    }

    private fun assertFixed(expected: Map<String, androidx.compose.ui.geometry.Rect>) {
        expected.forEach { (tag, bounds) ->
            rule.onNodeWithTag(tag).assertIsDisplayed()
            assertEquals(bounds, rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot)
        }
    }
}
