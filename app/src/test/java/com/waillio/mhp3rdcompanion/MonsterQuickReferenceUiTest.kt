package com.waillio.mhp3rdcompanion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.waillio.mhp3rdcompanion.data.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MonsterQuickReferenceUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun conditionalTacticalReasonExpandsAndCollapses() {
        setMonsterContent(monsterWithConditionalTool())

        rule.onNodeWithTag("tactical-tool-61").assertExists().performClick()
        rule.onNodeWithTag("tactical-reason-61", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Use only during the long pounce charge.").assertExists()
        rule.onNodeWithTag("tactical-tool-61").performClick()
        rule.onNodeWithTag("tactical-reason-61", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("tactical-tool-61").assertHasClickAction()
    }

    @Test
    fun noneListedIsQualifiedAndSparseMonsterHasNoEmptyBringPanel() {
        setMonsterContent(
            Monster(
                id = "sparse", name = "Great Jaggi", subtitle = "Bird Wyvern", type = "Bird Wyvern",
                weaknesses = ElementValues(30, 20, 10, 15, 5),
                hitzones = listOf(Hitzone("Head", 65, 70, 60, 30, 20, 10, 15, 5)),
                breakableParts = listOf(BreakablePart("Head", "Break")),
                huntPrep = MonsterHuntPrep(
                    ThreatCoverageStatus.NONE_LISTED_IN_MASTER_TABLE,
                    threats = emptyList(), counterItems = emptyList(), tacticalTools = emptyList()
                )
            )
        )

        rule.onNodeWithText("No special status threat listed").assertIsDisplayed()
        rule.onNodeWithText("No threats").assertDoesNotExist()
        rule.onNodeWithTag("bring-card").assertDoesNotExist()
        rule.onNodeWithTag("best-hit-areas").assertExists()
        rule.onNodeWithTag("break-sever-card").assertExists()
    }

    private fun setMonsterContent(monster: Monster) {
        rule.setContent {
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

    private fun monsterWithConditionalTool() = Monster(
        id = "nargacuga", name = "Nargacuga", subtitle = "Flying Wyvern", type = "Flying Wyvern",
        weaknesses = ElementValues(20, 10, 25, 15, 10),
        hitzones = listOf(Hitzone("Head", 50, 55, 45, 20, 10, 25, 15, 10)),
        breakableParts = listOf(BreakablePart("Tail", "Break"), BreakablePart("Tail", "Sever")),
        huntPrep = MonsterHuntPrep(
            ThreatCoverageStatus.NONE_LISTED_IN_MASTER_TABLE,
            threats = emptyList(), counterItems = emptyList(),
            tacticalTools = listOf(
                TacticalTool(61, "Sonic Bomb", TacticalPriority.CONDITIONAL, "Use only during the long pounce charge.")
            )
        )
    )
}
