package com.waillio.mhp3rdcompanion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
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
class ThorHardeningAUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun fieldGuideOrderIsExactIncludingAll() {
        assertEquals(
            listOf(
                null,
                EntityType.MONSTER,
                EntityType.SMALL_MONSTER,
                EntityType.MATERIAL,
                EntityType.MAP,
                EntityType.WEAPON,
                EntityType.SKILL,
                EntityType.QUEST,
                EntityType.TRAINING
            ),
            fieldGuideCategoryOrder
        )
    }

    @Test
    fun bestHitAreasKeepValuesWithoutRedundantNormalStateCaption() {
        val monster = Monster(
            id = "caption-test", name = "Caption Test", subtitle = "Wyvern", type = "Wyvern",
            hitzones = listOf(Hitzone("Head", 50, 50, 50, 0, 0, 0, 0, 0)),
            alternateHitzones = listOf(
                AlternateHitzoneState("charged", "Charged", listOf(Hitzone("Head", 60, 60, 60, 0, 0, 0, 0, 0)))
            )
        )
        rule.setContent { CompanionTheme { BestHitAreasCard(monster, Modifier.fillMaxWidth()) } }

        rule.onNodeWithText("Normal-state values").assertDoesNotExist()
        rule.onAllNodesWithText("Head 50").assertCountEquals(3)
    }

    @Test
    fun sharedQuestBadgesFitLowHighAndRanklessText() {
        val low = Quest("low", "Low Quest", "", rank = "Low Rank", stars = 2, category = "Village")
        val high = Quest("high", "High Quest", "", rank = "High Rank", stars = 5, category = "Guild")
        val rankless = Quest("none", "Rankless Quest", "", rank = "", stars = 1, category = "Event")
        rule.setContent {
            CompanionTheme {
                Column {
                    QuestBadge(low, Modifier.width(72.dp).testTag("badge-low"))
                    QuestBadge(high, Modifier.width(72.dp).testTag("badge-high"))
                    QuestBadge(rankless, Modifier.width(72.dp).testTag("badge-rankless"))
                }
            }
        }

        rule.onNodeWithText("Low Rank").assertIsDisplayed()
        rule.onNodeWithText("High Rank").assertIsDisplayed()
        rule.onNodeWithText("Village").assertIsDisplayed()
        rule.onNodeWithText("Guild").assertIsDisplayed()
        rule.onNodeWithText("Event").assertIsDisplayed()
        listOf("badge-low", "badge-high", "badge-rankless").forEach { tag ->
            rule.onNodeWithTag(tag).assertIsDisplayed().assertHeightIsAtLeast(46.dp)
        }
    }

    @Test
    fun palicoFollowsMonsterRewardsAndOtherFamiliesStayInSequence() {
        val source = MaterialSource("display-test", MaterialSourceType.SMALL_MONSTER, "Display test")
        val groups = GroupedMaterialSources(
            field = listOf(FieldSourceGroup("field", "Field", null, null, emptyList(), emptyList())),
            monsters = listOf(source),
            smallMonsters = listOf(source),
            quests = listOf(source),
            supplyBoxes = listOf(source),
            specialFree = listOf(source),
            trainingRewards = listOf(source),
            farm = listOf(
                FarmSourceGroup(
                    "farm-test", "farm_field", "Farm", "", null, null, null,
                    FarmTriggerStatus.FACILITY_ROUTE, "", emptyList(), emptyList()
                )
            ),
            trade = listOf(source),
            shop = listOf(source),
            combinations = listOf(source),
            combinationFailures = emptyList(),
            scrapConversions = listOf(source),
            decorationCrafting = listOf(source),
            roasting = listOf(source),
            invasionRewards = listOf(source),
            palicoExpeditions = listOf(source)
        )
        val rendered = sourceFamilyIdsForDisplay(groups)
        assertEquals(
            listOf(
                "field", "combination", "decoration-crafting", "roasting", "invasion-reward",
                "small-monster", "monster-reward", "palico-expedition", "supply-box", "quest-reward",
                "special", "training-reward", "farm", "trading", "shop", "scrap-conversion"
            ),
            rendered
        )
        assertTrue(rendered.indexOf("small-monster") < rendered.indexOf("palico-expedition"))
        assertTrue(rendered.indexOf("monster-reward") < rendered.indexOf("palico-expedition"))
        assertEquals(
            listOf("field", "combination", "decoration-crafting", "roasting", "invasion-reward", "monster-reward", "supply-box", "quest-reward", "special", "training-reward", "farm", "trading", "shop", "scrap-conversion"),
            rendered.filterNot { it == "small-monster" || it == "palico-expedition" }
        )
    }

    @Test
    fun questFilterKeepsFullCategoryAndStarLabelsInResponsiveRows() {
        val quests = (1..8).map { star ->
            Quest(
                id = "filter-$star", name = "Quest $star", subtitle = "",
                rank = if (star % 2 == 0) "High Rank" else "Low Rank",
                stars = star,
                category = if (star == 1) "Hot Spring" else "Guild"
            )
        }
        val index = QuestFilterIndex(quests)
        rule.setContent {
            CompanionTheme {
                QuestFilterDialog(
                    draft = QuestFilterState(),
                    index = index,
                    onDraftChange = {},
                    onDismiss = {},
                    onApply = {}
                )
            }
        }

        rule.onNodeWithText("Hot Spring").assertIsDisplayed()
        rule.onNodeWithText("★8").assertIsDisplayed()
        rule.onNodeWithText("Low Rank").assertIsDisplayed()
        rule.onNodeWithText("High Rank").assertIsDisplayed()
    }

    @Test
    fun skillsScreenKeepsExplanationAndDropsOnlyLargeHeading() {
        val skill = SkillTree("attack", "Attack", "Attack", "攻撃", "", listOf(SkillThreshold(10, "Attack Up", "POSITIVE", "")))
        rule.setContent {
            CompanionTheme {
                SkillsBrowser(
                    skills = listOf(skill), query = "", listState = androidx.compose.foundation.lazy.rememberLazyListState(),
                    expandedSkillTreeId = null, focusSkillTreeId = null,
                    decorationSkillRelations = emptyList(), materials = emptyList(),
                    onExpandedChange = {}, onFocusConsumed = {}, onMaterial = {}
                )
            }
        }

        rule.onNodeWithTag("skills-browser").assertIsDisplayed()
        rule.onNodeWithText("How skills work").assertIsDisplayed()
        rule.onNodeWithText("SKILLS").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun sharedSearchFieldPreservesMidStringBackspaceAndInsertion() {
        rule.setContent {
            CompanionTheme {
                var query by remember { mutableStateOf("abcd") }
                SearchInputField(
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier.width(240.dp).height(48.dp),
                    tag = "shared-search-test"
                )
            }
        }
        rule.onNodeWithTag("shared-search-test").assertIsDisplayed().assertHeightIsAtLeast(44.dp)
        rule.onNodeWithTag("shared-search-test").performClick()
        rule.onNodeWithTag("shared-search-test").performTextInputSelection(TextRange(2))
        rule.onNodeWithTag("shared-search-test").performKeyInput { pressKey(Key.Backspace) }
        rule.onNodeWithTag("shared-search-test").assertTextEquals("acd")
        rule.onNodeWithTag("shared-search-test").performTextInputSelection(TextRange(1))
        rule.onNodeWithTag("shared-search-test").performTextInput("X")
        rule.onNodeWithTag("shared-search-test").assertTextEquals("aXcd")
    }

    @Test
    fun alphabetFirstMiddleLastLabelsStayInsideScrubberBounds() {
        var selected by mutableStateOf('A')
        rule.setContent {
            CompanionTheme {
                Box(Modifier.height(500.dp)) {
                    AlphabetScrubber(('A'..'Z').toList(), selected, onJump = { selected = it }, modifier = Modifier.fillMaxHeight())
                }
            }
        }

        listOf('A', 'M', 'Z').forEach { letter ->
            selected = letter
            rule.waitForIdle()
            val scrubber = rule.onNodeWithTag("alphabet-scrubber").fetchSemanticsNode().boundsInRoot
            val label = rule.onNodeWithTag("alphabet-letter-label-$letter", useUnmergedTree = true)
                .assertExists().fetchSemanticsNode().boundsInRoot
            assertTrue("$letter clipped above", label.top >= scrubber.top)
            assertTrue("$letter clipped below", label.bottom <= scrubber.bottom)
        }
    }

    @Test
    fun previewFooterUsesSharedEmphasisAndActionTreatment() {
        var clicks = 0
        rule.setContent {
            CompanionTheme {
                PreviewFooter(
                    moreLabel = "+6 more materials", actionLabel = "View all 8 materials",
                    tag = "preview-footer-test", moreTag = "preview-footer-more",
                    onAction = { clicks++ }
                )
            }
        }

        rule.onNodeWithTag("preview-footer-more").assertTextEquals("+6 more materials")
        rule.onNodeWithText("View all 8 materials").assertIsDisplayed()
        rule.onNodeWithTag("preview-footer-test").performClick()
        assertEquals(1, clicks)
    }
}
