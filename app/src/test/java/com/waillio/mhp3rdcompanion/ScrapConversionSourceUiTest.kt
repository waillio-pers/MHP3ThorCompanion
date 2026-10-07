package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.*
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
class ScrapConversionSourceUiTest {
    @get:Rule val rule = createComposeRule()
    private val repository = CompanionRepository(ApplicationProvider.getApplicationContext())
    private val data = repository.data

    @Test
    fun simpleMaterialRowNavigatesWithoutTurningTheConversionCardIntoALink() {
        val output = data.materials.single { it.gameItemId == 733 }
        var openedStableItemId: String? = null
        rule.setContent {
            CompanionTheme {
                MaterialScreen(
                    material = output,
                    quests = emptyList(),
                    isFavorite = false,
                    onBack = {},
                    onFavorite = {},
                    onMaterial = { openedStableItemId = it },
                    materialIdForGameItem = { id -> data.materials.singleOrNull { it.gameItemId == id }?.id }
                )
            }
        }
        rule.onNodeWithTag("item-scrap-conversion-family").performScrollTo()
        rule.onNodeWithText("Scrap Conversion · 1 materials").assertIsDisplayed()
        rule.onNodeWithTag("item-scrap-conversion-trigger-summary").assertIsDisplayed()
        rule.onNodeWithText("Yukumo Wood").assertIsDisplayed()
        rule.onNodeWithText("Use ×1 · Produces ×1").assertIsDisplayed()
        rule.onNodeWithText("→ Wood Scraps").assertDoesNotExist()
        rule.onNodeWithTag("item-scrap-source-scrap:183:733").assertHasNoClickAction()
        rule.onNodeWithTag("item-scrap-material-183").assertHasClickAction().performClick()
        assertEquals("item_yukumo_wood", openedStableItemId)
    }

    @Test
    fun multipleMaterialsRemainSeparateAndDisplayThePublishedHigherYield() {
        val output = data.materials.single { it.gameItemId == 742 }
        rule.setContent { CompanionTheme { MaterialScreen(output, emptyList(), false, {}, {}) } }
        rule.onNodeWithTag("item-scrap-conversion-family").performScrollTo()
        rule.onNodeWithText("Scrap Conversion · 2 materials").assertIsDisplayed()
        rule.onNodeWithText("Rhenoplos Shell").assertIsDisplayed()
        rule.onNodeWithText("Use ×1 · Produces ×1").assertIsDisplayed()
        rule.onNodeWithText("Rhenoplos Scalp").assertIsDisplayed()
        rule.onNodeWithText("Use ×1 · Produces ×2").assertIsDisplayed()
        rule.onNodeWithTag("item-scrap-source-scrap:276:742").assertIsDisplayed()
        rule.onNodeWithTag("item-scrap-source-scrap:278:742").assertIsDisplayed()
    }

    @Test
    fun publishedHighYieldAndCourageRowsShowOnlyKnownQuantities() {
        val pumpkin = data.materials.single { it.gameItemId == 735 }
        rule.setContent { CompanionTheme { MaterialScreen(pumpkin, emptyList(), false, {}, {}) } }
        rule.onNodeWithTag("item-scrap-conversion-family").performScrollTo()
        rule.onNodeWithText("Bumblepumpkin").assertIsDisplayed()
        rule.onNodeWithText("Use ×1 · Produces ×3").assertIsDisplayed()
        rule.onAllNodesWithText("Palico Armory handoff · Also generated when used in hunter gear")
            .assertCountEquals(1)
        rule.onAllNodesWithText("EXACT_PUBLISHED").assertCountEquals(0)
        rule.onAllNodesWithText("DIRECT_PALICO_ARMORY_HANDOFF").assertCountEquals(0)
        rule.onAllNodesWithText("HUNTER_GEAR_MATERIAL_CONSUMPTION").assertCountEquals(0)
    }

    @Test
    fun courageYieldIsOmittedRatherThanInvented() {
        val output = data.materials.single { it.gameItemId == 801 }
        rule.setContent { CompanionTheme { MaterialScreen(output, emptyList(), false, {}, {}) } }
        rule.onNodeWithTag("item-scrap-conversion-family").performScrollTo()
        rule.onNodeWithText("Commendation").assertIsDisplayed()
        rule.onNodeWithText("Use ×1").assertIsDisplayed()
        rule.onNodeWithText("Produces ×1").assertDoesNotExist()
        rule.onAllNodesWithText("NOT_PUBLISHED").assertCountEquals(0)
    }

    @Test
    fun maximumScrapSourceUsesLazyViewAllAndClosingItKeepsTheParentVisible() {
        val output = data.materials.single { it.gameItemId == 793 }
        rule.setContent { CompanionTheme { MaterialScreen(output, emptyList(), false, {}, {}) } }
        rule.onNodeWithTag("item-scrap-conversion-family").performScrollTo()
        rule.onNodeWithText("Scrap Conversion · 12 materials").assertIsDisplayed()
        rule.onNodeWithText("+6 more materials").assertIsDisplayed()
        rule.onNodeWithTag("item-scrap-conversion-view-all").performClick()
        rule.onNodeWithTag("item-scrap-conversion-view-all-modal").assertIsDisplayed()
        rule.onNodeWithTag("item-scrap-conversion-view-all-title").assertIsDisplayed()
        rule.onNodeWithTag("item-scrap-conversion-view-all-list").assertIsDisplayed()
        rule.onNodeWithTag("item-scrap-conversion-view-all-list").performScrollToIndex(11)
        rule.onNodeWithTag("item-scrap-modal-material-541").assertIsDisplayed()
        rule.onNodeWithTag("item-scrap-conversion-family").assertExists()
        rule.onNodeWithTag("item-scrap-conversion-view-all-close").performClick()
        rule.onNodeWithTag("item-scrap-conversion-view-all-modal").assertDoesNotExist()
        rule.onNodeWithTag("item-scrap-conversion-family").assertExists()
    }

    @Test
    fun woodInitialGrantRemainsSpecialAndDoesNotAppearAsConversionInput() {
        val output = data.materials.single { it.gameItemId == 733 }
        val conversionRows = output.sources.filter { it.type == MaterialSourceType.SCRAP_CONVERSION }
        val initialGrant = output.sources.single { it.id == "INITIAL_WOOD_SCRAP" }
        assertEquals(1, conversionRows.size)
        assertEquals(183, conversionRows.single().inputGameItemId)
        assertEquals(1, conversionRows.single().quantity)
        assertEquals(MaterialSourceType.SPECIAL_FREE, initialGrant.type)
        assertEquals(4, initialGrant.quantity)

        rule.setContent { CompanionTheme { MaterialScreen(output, emptyList(), false, {}, {}) } }
        rule.onNodeWithTag("item-scrap-conversion-family").performScrollTo()
        rule.onNodeWithTag("item-scrap-source-scrap:183:733").assertExists()
        rule.onNodeWithTag("item-scrap-source-scrap:4:733").assertDoesNotExist()
    }
}
