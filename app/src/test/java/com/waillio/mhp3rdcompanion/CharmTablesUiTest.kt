package com.waillio.mhp3rdcompanion

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.waillio.mhp3rdcompanion.data.CharmAssetSource
import com.waillio.mhp3rdcompanion.data.CharmIdentifyWorkingState
import com.waillio.mhp3rdcompanion.data.CharmPointOperator
import com.waillio.mhp3rdcompanion.data.CharmObservation
import com.waillio.mhp3rdcompanion.data.CharmState
import com.waillio.mhp3rdcompanion.data.CharmTableIndex
import com.waillio.mhp3rdcompanion.data.SkillTree
import com.waillio.mhp3rdcompanion.data.TalismanClass
import com.waillio.mhp3rdcompanion.data.addCharmSavedSearch
import com.waillio.mhp3rdcompanion.data.deleteCharmSavedSearch
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private object CharmUiFixture {
    val skills = listOf(
        SkillTree("attack", "Attack", "Attack", "", ""),
        SkillTree("expert", "Expert", "Expert", "", ""),
        SkillTree("guard", "Guard", "Guard", "", "")
    )
    val index = CharmTableIndex(
        CharmAssetSource("TIMEWORN", "test", "test", "test", "test", "test", "WTFPL-2.0", (1..12).toList(), 5_400, 3),
        listOf("TIMEWORN"), listOf("Queen", "King", "Dragon"), skills.map { it.stableSkillTreeId },
        listOf(
            CharmState(0, 7, 700, 0, 0, 7, 1, 10, 2),
            CharmState(0, 7, 701, 0, 0, 7, 1, 10, 2),
            CharmState(0, 7, 702, 0, 0, -3, 1, -10, 2)
        )
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-port-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CharmTablesPhoneUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun narrowPortraitShowsCurrentTableAndSearchableSkillSelectorWithoutHorizontalOverflow() {
        val identifyState = mutableStateOf(CharmIdentifyWorkingState())
        rule.setContent {
            CompanionTheme {
                CharmTablesScreen(
                    index = CharmUiFixture.index,
                    skillTrees = CharmUiFixture.skills,
                    currentTable = 7,
                    loadingFailed = false,
                    identifyWorkingState = identifyState.value,
                    onUpdateIdentifyWorkingState = { identifyState.value = it(identifyState.value) },
                    onSaveCurrentTable = {}
                )
            }
        }
        rule.onNodeWithTag("screen-charm-tables").assertIsDisplayed()
        rule.onNodeWithTag("current-charm-table-value").assertTextEquals("7")
        val screen = rule.onNodeWithTag("screen-charm-tables").fetchSemanticsNode().boundsInRoot
        val filter = rule.onNodeWithTag("charm-search-filters").fetchSemanticsNode().boundsInRoot
        assertTrue("search filters fit the narrow portrait viewport", filter.width <= screen.width + 1f)

        rule.onNodeWithTag("charm-search-skill1").performScrollTo().performClick()
        assertTrue(rule.onAllNodesWithTag("charm-search-skill1-points").fetchSemanticsNodes().isEmpty())
        rule.onNodeWithTag("charm-search-skill1-picker-search").assertIsDisplayed()
        rule.onNodeWithTag("charm-search-skill1-picker-option-attack").assertIsDisplayed().performClick()
        val screenWidth = rule.onNodeWithTag("screen-charm-tables").fetchSemanticsNode().boundsInRoot.width
        val pointsBounds = rule.onNodeWithTag("charm-search-skill1-points").fetchSemanticsNode().boundsInRoot
        val operatorBounds = rule.onNodeWithTag("charm-search-skill1-points-operator").fetchSemanticsNode().boundsInRoot
        assertTrue("narrow-phone points field remains wide enough for its label", pointsBounds.width > screenWidth * .4f)
        assertTrue("operator remains aligned beside the points field", kotlin.math.abs(pointsBounds.center.y - operatorBounds.center.y) < 30f)
        rule.onNodeWithTag("charm-search-skill1-points").performScrollTo().performTextInput("99")
        rule.onNodeWithTag("charm-search-empty").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("charm-search-table").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun phoneCanSearchNegativePointsUsingInlineSignToggleWithoutShowingCorpusCounts() {
        val identifyState = mutableStateOf(CharmIdentifyWorkingState())
        rule.setContent {
            CompanionTheme {
                CharmTablesScreen(
                    index = CharmUiFixture.index,
                    skillTrees = CharmUiFixture.skills,
                    currentTable = 7,
                    loadingFailed = false,
                    identifyWorkingState = identifyState.value,
                    onUpdateIdentifyWorkingState = { identifyState.value = it(identifyState.value) },
                    onSaveCurrentTable = {}
                )
            }
        }
        rule.onNodeWithTag("charm-search-skill1").performScrollTo().performClick()
        rule.onNodeWithTag("charm-search-skill1-picker-option-attack").assertIsDisplayed().performClick()
        rule.onNodeWithTag("charm-search-skill1-points-sign-toggle").performScrollTo().performClick()
        rule.onNodeWithTag("charm-search-skill1-points").performTextInput("3")
        rule.onNodeWithTag("charm-search-skill1-points-operator").performScrollTo().performClick()
        rule.onNodeWithTag("charm-search-skill1-points-operator-equal").performClick()
        rule.onNodeWithTag("charm-search-count").performScrollTo().assertTextContains("1 results")
        assertTrue(rule.onAllNodesWithTag("charm-result-source-count").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun skillPickerOptionsAreDerivedFromTheirActualSourcePositionsAndRarityHasNoDeadCommonOption() {
        val skill1 = CharmUiFixture.index.skillIdsAtPosition(1)
        val skill2 = CharmUiFixture.index.skillIdsAtPosition(2)
        assertEquals(setOf("attack"), skill1)
        assertEquals(setOf("expert"), skill2)
        assertEquals(listOf("attack"), filterCharmSkills(CharmUiFixture.skills, "", skill1).map { it.stableSkillTreeId })
        assertEquals(listOf("expert"), filterCharmSkills(CharmUiFixture.skills, "", skill2).map { it.stableSkillTreeId })
        assertFalse("expert" in skill1)
        assertFalse("attack" in skill2)
        assertEquals(listOf(TalismanClass.QUEEN), CharmUiFixture.index.availableClasses())
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w800dp-h1080dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CharmTablesThorUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun thorWidthCanIdentifySaveAndRemoveAnExactObservation() {
        val table = mutableStateOf<Int?>(null)
        val identifyState = mutableStateOf(CharmIdentifyWorkingState())
        rule.setContent {
            CompanionTheme {
                CharmTablesScreen(
                    index = CharmUiFixture.index,
                    skillTrees = CharmUiFixture.skills,
                    currentTable = table.value,
                    loadingFailed = false,
                    identifyWorkingState = identifyState.value,
                    onUpdateIdentifyWorkingState = { identifyState.value = it(identifyState.value) },
                    onSaveCurrentTable = { table.value = it }
                )
            }
        }
        val screen = rule.onNodeWithTag("screen-charm-tables")
        screen.assertIsDisplayed()
        val bounds = screen.fetchSemanticsNode().boundsInRoot
        assertTrue("Thor-width screen is wide enough for the adaptive UI", bounds.width >= 790f)
        rule.onNodeWithTag("charm-page-identify").performClick()

        pickSkill("charm-identify-skill1", "Attack", "attack")
        rule.onNodeWithTag("charm-identify-skill1-points").performScrollTo().performTextInput("7")
        pickSkill("charm-identify-skill2", "Expert", "expert")
        rule.onNodeWithTag("charm-identify-skill2-points").performScrollTo().performTextInput("10")
        rule.onNodeWithTag("charm-identify-slots").performScrollTo().performClick()
        rule.onNodeWithTag("charm-identify-slots-value-2").performClick()
        rule.onNodeWithTag("charm-identify-live-one-table").performScrollTo().assertTextContains("Possible table: 7", substring = true)
        rule.onNodeWithTag("charm-identify-add").performScrollTo().performClick()
        rule.onNodeWithTag("charm-identify-pool-one-table").performScrollTo().assertTextContains("Possible table: 7", substring = true)
        rule.onNodeWithTag("charm-identify-pool-save-table-7").performScrollTo().performClick()
        rule.onNodeWithTag("current-charm-table-value").assertTextEquals("7")
        rule.onNodeWithTag("charm-observation-edit-0").performScrollTo().performClick()
        rule.onNodeWithTag("charm-identify-skill1-points").performScrollTo().performTextClearance()
        rule.onNodeWithTag("charm-identify-skill1-points").performTextInput("99")
        rule.onNodeWithTag("charm-identify-live-no-match").assertIsDisplayed()
        rule.onNodeWithTag("charm-identify-add").performScrollTo().performClick()
        rule.onNodeWithTag("charm-identify-pool-no-match").assertIsDisplayed()
        rule.onNodeWithTag("charm-observation-remove-0").performScrollTo().performClick()
        assertTrue(rule.onAllNodesWithTag("charm-identify-pool").fetchSemanticsNodes().isEmpty())
}

    @Test
    fun identifyFormAcceptsNegativePointsInBothSkillsAndMatchesExactObservation() {
        val table = mutableStateOf<Int?>(null)
        val identifyState = mutableStateOf(CharmIdentifyWorkingState())
        rule.setContent {
            CompanionTheme {
                CharmTablesScreen(
                    index = CharmUiFixture.index,
                    skillTrees = CharmUiFixture.skills,
                    currentTable = table.value,
                    loadingFailed = false,
                    identifyWorkingState = identifyState.value,
                    onUpdateIdentifyWorkingState = { identifyState.value = it(identifyState.value) },
                    onSaveCurrentTable = { table.value = it }
                )
            }
        }
        rule.onNodeWithTag("charm-page-identify").performClick()
        pickSkill("charm-identify-skill1", "Attack", "attack")
        rule.onNodeWithTag("charm-identify-skill1-points-sign-toggle").performScrollTo().performClick()
        rule.onNodeWithTag("charm-identify-skill1-points").performTextInput("3")
        rule.onNodeWithTag("charm-identify-skill1-points").assertTextContains("-3")
        pickSkill("charm-identify-skill2", "Expert", "expert")
        rule.onNodeWithTag("charm-identify-skill2-points-sign-toggle").performScrollTo().performClick()
        rule.onNodeWithTag("charm-identify-skill2-points").performTextInput("10")
        rule.onNodeWithTag("charm-identify-skill2-points").assertTextContains("-10")
        rule.onNodeWithTag("charm-identify-slots").performScrollTo().performClick()
        rule.onNodeWithTag("charm-identify-slots-value-2").performClick()
        rule.onNodeWithTag("charm-identify-live-one-table").performScrollTo().assertTextContains("Possible table: 7", substring = true)
        rule.onNodeWithTag("charm-identify-save-table").performScrollTo().performClick()
        rule.onNodeWithTag("current-charm-table-value").assertTextEquals("7")
        rule.onNodeWithTag("charm-identify-add").performScrollTo().performClick()
        rule.onNodeWithTag("charm-identify-pool-one-table").performScrollTo().assertTextContains("Possible table: 7", substring = true)
    }

    private fun pickSkill(tag: String, query: String, id: String) {
        rule.onNodeWithTag(tag).performScrollTo().performClick()
        rule.onNodeWithTag("$tag-picker-search").performTextInput(query)
        rule.onNodeWithTag("$tag-picker-option-$id").assertIsDisplayed().performClick()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CharmIdentifyConfigurationTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun identifyObservationsEditTargetAndFormSurviveActivityRecreationInViewModel() {
        val viewModel = ViewModelProvider(rule.activity)[AppViewModel::class.java]
        val workingState = CharmIdentifyWorkingState(
            observations = listOf(CharmObservation(TalismanClass.QUEEN, "skill-1", -3, "skill-2", -10, 2)),
            editingObservation = 0,
            formClass = TalismanClass.KING,
            formSkill1Id = "skill-3",
            formSkill1Points = "-7",
            formSkill2Id = "skill-4",
            formSkill2Points = "-2",
            formSlots = 1
        )
        viewModel.updateCharmIdentifyWorkingState { workingState }

        rule.activityRule.scenario.recreate()

        val recreatedViewModel = ViewModelProvider(rule.activity)[AppViewModel::class.java]
        assertSame(viewModel, recreatedViewModel)
        assertEquals(workingState, recreatedViewModel.state.value.charmIdentifyWorkingState)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-port-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CharmSavedSearchUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun savedSearchRestoresNegativeValueAndComparatorAndCanBeDeleted() {
        val identifyState = mutableStateOf(CharmIdentifyWorkingState())
        val saved = mutableStateOf(emptyList<com.waillio.mhp3rdcompanion.data.CharmSavedSearch>())
        val skillsById = CharmUiFixture.skills.associateBy { it.stableSkillTreeId }
        rule.setContent {
            CompanionTheme {
                CharmTablesScreen(
                    index = CharmUiFixture.index,
                    skillTrees = CharmUiFixture.skills,
                    currentTable = 7,
                    loadingFailed = false,
                    identifyWorkingState = identifyState.value,
                    savedSearches = saved.value,
                    onUpdateIdentifyWorkingState = { identifyState.value = it(identifyState.value) },
                    onSaveCurrentTable = {},
                    onSaveSearch = { name, criteria -> saved.value = addCharmSavedSearch(saved.value, name, criteria, "ui-test") },
                    onDeleteSearch = { id -> saved.value = deleteCharmSavedSearch(saved.value, id) }
                )
            }
        }
        rule.onNodeWithTag("charm-search-skill1").performScrollTo().performClick()
        rule.onNodeWithTag("charm-search-skill1-picker-option-attack").performClick()
        rule.onNodeWithTag("charm-search-skill1-points-sign-toggle").performScrollTo().performClick()
        rule.onNodeWithTag("charm-search-skill1-points").performTextInput("3")
        rule.onNodeWithTag("charm-save-search").performScrollTo().performClick()
        rule.onNodeWithTag("charm-save-search-confirm").performClick()

        val savedCriteria = saved.value.single().criteria
        assertEquals(-3, savedCriteria.skill1Points)
        assertEquals(CharmPointOperator.GREATER_OR_EQUAL, savedCriteria.skill1Mode)
        rule.onNodeWithTag("charm-search-skill1-points").performScrollTo().performTextClearance()
        rule.onNodeWithTag("charm-saved-searches-toggle").performScrollTo().performClick()
        rule.onNodeWithTag("charm-saved-search-ui-test").performClick()
        rule.onNodeWithTag("charm-search-skill1-points").assertTextContains("-3")
        rule.onNodeWithTag("charm-search-skill1-points-operator").assertTextContains(">=")

        rule.onNodeWithTag("charm-saved-search-delete-ui-test").performClick()
        assertTrue(saved.value.isEmpty())
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-port-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CharmResultsPaginationUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun moreThanThirtyResultsUsePreviewAndFullscreenFiftyItemPages() {
        val states = (0 until 75).map { number ->
            CharmState(0, number % 12 + 1, number, 0, 0, number, -1, 0, number % 4)
        }
        val source = CharmAssetSource("TIMEWORN", "test", "test", "test", "test", "test", "WTFPL-2.0", (1..12).toList(), 5_400, states.size)
        val index = CharmTableIndex(source, listOf("TIMEWORN"), listOf("Queen", "King", "Dragon"), listOf("attack", "expert", "guard"), states)
        val identifyState = mutableStateOf(CharmIdentifyWorkingState())
        rule.setContent {
            CompanionTheme {
                CharmTablesScreen(
                    index = index,
                    skillTrees = CharmUiFixture.skills,
                    currentTable = null,
                    loadingFailed = false,
                    identifyWorkingState = identifyState.value,
                    onUpdateIdentifyWorkingState = { identifyState.value = it(identifyState.value) },
                    onSaveCurrentTable = {}
                )
            }
        }
        rule.onNodeWithTag("charm-search-skill1").performScrollTo().performClick()
        rule.onNodeWithTag("charm-search-skill1-picker-option-attack").performClick()
        rule.onNodeWithTag("charm-search-count").performScrollTo().assertTextContains("75 results · showing first 30")
        rule.onNodeWithTag("screen-charm-tables").performScrollToNode(hasTestTag("charm-view-all"))
        rule.onNodeWithTag("charm-view-all").performClick()
        rule.onNodeWithTag("charm-view-all-dialog").assertIsDisplayed()
        rule.onNodeWithTag("charm-results-page").assertTextEquals("Page 1 / 2")
        rule.onNodeWithTag("charm-results-previous").assertIsNotEnabled()
        rule.onNodeWithTag("charm-results-next").performClick()
        rule.onNodeWithTag("charm-results-page").assertTextEquals("Page 2 / 2")
        rule.onNodeWithTag("charm-results-next").assertIsNotEnabled()
        rule.onNodeWithTag("charm-results-previous").performClick()
        rule.onNodeWithTag("charm-results-page").assertTextEquals("Page 1 / 2")
        rule.onNodeWithTag("charm-view-all-close").performClick()
        assertTrue(rule.onAllNodesWithTag("charm-view-all-dialog").fetchSemanticsNodes().isEmpty())
    }
}
