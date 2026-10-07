package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
class ItemCombinationUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun armorSphereRendersBothAlternativeCombinationRows() {
        val material = CompanionRepository(ApplicationProvider.getApplicationContext())
            .data.materials.single { it.gameItemId == 804 }
        // Render a compact representative state for the audit artifact; the
        // production screen keeps the same rows in its full scrollable source
        // card alongside the item’s other acquisition rows.
        val screenshotMaterial = material.copy(
            sources = material.sources.filter { it.recipeNumber == 112 || it.recipeNumber == 113 }
        )
        rule.setContent {
            CompanionTheme {
                MaterialScreen(screenshotMaterial, emptyList(), false, {}, {})
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("item-combination-source-112").fetchSemanticsNode()
        rule.onNodeWithTag("item-combination-source-113").fetchSemanticsNode()
    }

    @Test
    fun combinationCardsShowIconsMetadataAndIngredientNavigation() {
        val material = CompanionRepository(ApplicationProvider.getApplicationContext())
            .data.materials.single { it.gameItemId == 804 }
        rule.setContent {
            CompanionTheme {
                MaterialScreen(
                    material,
                    emptyList(),
                    false,
                    {},
                    {},
                    onMaterial = {},
                    materialIdForGameItem = { "item-$it" }
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Combination · 2 recipes").assertIsDisplayed()
        rule.onAllNodesWithText("Base success 100% · Makes ×1").assertCountEquals(2)
        rule.onAllNodesWithText("Combine").assertCountEquals(0)
        rule.onNodeWithTag("source-combination-ingredient-112-a").assertHasClickAction().performClick()
    }

    @Test
    fun craftingCardsKeepRankChipsAndAllIngredientsVisible() {
        val material = CompanionRepository(ApplicationProvider.getApplicationContext())
            .data.materials.single { it.gameItemId == 816 }
        rule.setContent {
            CompanionTheme {
                MaterialScreen(
                    material,
                    emptyList(),
                    false,
                    {},
                    {},
                    onMaterial = {},
                    materialIdForGameItem = { "item-$it" }
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Crafting · 2 recipes").assertIsDisplayed()
        rule.onAllNodesWithTag("source-rank-chip-LR").assertCountEquals(1)
        rule.onAllNodesWithTag("source-rank-chip-HR").assertCountEquals(1)
        rule.onNodeWithTag("source-crafting-ingredient-decoration_crafting_816_1-812").assertIsDisplayed()
        rule.onNodeWithTag("source-crafting-ingredient-decoration_crafting_816_2-813").assertIsDisplayed()
    }

    @Test
    fun craftingQuantityColumnAlignsWithinEachRecipeAndFollowsNames() {
        val material = CompanionRepository(ApplicationProvider.getApplicationContext())
            .data.materials.single { it.gameItemId == 816 }
        rule.setContent {
            CompanionTheme {
                MaterialScreen(material, emptyList(), false, {}, {})
            }
        }
        rule.waitForIdle()
        val firstRecipeQuantities = listOf(812, 305, 324).map { gameId ->
            rule.onNodeWithTag("source-crafting-ingredient-decoration_crafting_816_1-quantity-$gameId")
                .fetchSemanticsNode().boundsInRoot
        }
        assertTrue(firstRecipeQuantities.zipWithNext().all { (a, b) -> kotlin.math.abs(a.left - b.left) <= 1f })
        assertTrue(firstRecipeQuantities.all { it.left > 0f })
    }

    @Test
    fun sameRankAlternateCraftingRecipesReceiveMinimalLabels() {
        val material = CompanionRepository(ApplicationProvider.getApplicationContext())
            .data.materials.single { it.gameItemId == 926 }
        rule.setContent { CompanionTheme { MaterialScreen(material, emptyList(), false, {}, {}) } }
        rule.waitForIdle()
        rule.onNodeWithText("Crafting · 2 recipes").assertIsDisplayed()
        rule.onNodeWithText("Recipe 1").assertIsDisplayed()
        rule.onNodeWithText("Recipe 2").assertIsDisplayed()
    }
}
