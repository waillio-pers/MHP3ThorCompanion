package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class DevicePlaytestCleanupTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data

    @Test
    fun bumblepumpkinFarmActionsPreserveEveryQuantityAndProbability() {
        val farm = data.materials.single { it.name == "Bumblepumpkin" }.sources
            .filter { it.type == MaterialSourceType.FARM }
            .groupFarmSourcesForDisplay()

        assertEquals(3, farm.size)
        assertEquals(5, farm.sumOf { it.sourceRows.size })

        val red = farm.single { it.inputItemName == "Red Seed" }
        assertEquals(180, red.inputGameItemId)
        assertEquals("Plant this:", red.action)
        assertEquals(listOf(1 to 5, 2 to 3), red.outcomes.map { it.quantity to it.chance })

        val green = farm.single { it.inputItemName == "Green Seed" }
        assertEquals(181, green.inputGameItemId)
        assertEquals(listOf(1 to 4, 2 to 2), green.outcomes.map { it.quantity to it.chance })

        val bonus = farm.single { it.inputGameItemId == null }
        assertEquals("Bonus harvest slot", bonus.action)
        assertEquals(listOf(1 to 3), bonus.outcomes.map { it.quantity to it.chance })
    }

    @Test
    fun bumblepumpkinUiRendersCompactInputsIconsAndOutcomes() {
        openItem("Bumblepumpkin", "item_bumblepumpkin")

        rule.onNodeWithTag("farm-method-farm_field_red_seed::inputGameItemId:180").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("farm-method-trigger-farm_field_red_seed::inputGameItemId:180")
            .assertTextEquals("Plant this:")
        rule.onNodeWithTag("farm-method-outcome-farm_field_red_seed::inputGameItemId:180-0").assertTextEquals("Yield ×1 · 5%")
        rule.onNodeWithTag("farm-method-outcome-farm_field_red_seed::inputGameItemId:180-1").assertTextEquals("Yield ×2 · 3%")
        rule.onNodeWithTag("farm-input-icon-180", useUnmergedTree = true).assertExists()
        assertTrue(ItemIconRegistry.resolve(180) != null)
        rule.onNodeWithText("Red Seed").assertIsDisplayed()

        rule.onNodeWithTag("farm-method-farm_field_green_seed::inputGameItemId:181").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("farm-method-outcome-farm_field_green_seed::inputGameItemId:181-0").assertTextEquals("Yield ×1 · 4%")
        rule.onNodeWithTag("farm-method-outcome-farm_field_green_seed::inputGameItemId:181-1").assertTextEquals("Yield ×2 · 2%")
        rule.onNodeWithTag("farm-input-icon-181", useUnmergedTree = true).assertExists()
        assertTrue(ItemIconRegistry.resolve(181) != null)

        rule.onNodeWithTag("farm-method-farm_field_additional_slot::default").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Field · bonus harvest slot").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("farm-method-outcome-farm_field_additional_slot::default-0").assertTextEquals("Yield ×1 · 3%")
    }

    @Test
    fun gatheringContextUsesOnlyStructuralAreaAndPointIdentity() {
        assertEquals(
            "Area 8 · Point 1",
            structuralGatheringContext(
                "gathering_sandy_plains_area_8_point_1_gathering",
                "8",
                "①\nサボテン"
            )
        )

        val redSeed = data.materials.single { it.name == "Red Seed" }
        val sandyPlains = redSeed.sources.filter { it.locationId == "sandy_plains" }
        assertTrue(sandyPlains.any { it.context == "Area 8 · Point 1" && it.method == "GATHERING" })
        assertTrue(sandyPlains.any { it.rank == RewardContext.LOW })
        assertTrue(sandyPlains.any { it.rank == RewardContext.HIGH })
        assertFalse(sandyPlains.any { source -> source.context.orEmpty().any { it.code > 0x3000 } })
    }

    @Test
    fun unifiedAndLargeMonsterSearchUseTheSameAuthenticArtwork() {
        rule.onNodeWithTag("global-search").performTextInput("barroth")
        listOf("monster_barroth", "monster_jade_barroth").forEach { id ->
            rule.onNodeWithTag("result-monster-$id").assertIsDisplayed()
            rule.onNodeWithTag("monster-artwork-$id", useUnmergedTree = true).assertExists()
            rule.onNodeWithTag("monster-artwork-fallback-$id", useUnmergedTree = true).assertDoesNotExist()
        }

        rule.onNodeWithTag("field-filter-monster").performClick()
        listOf("monster_barroth", "monster_jade_barroth").forEach { id ->
            rule.onNodeWithTag("monster-artwork-$id", useUnmergedTree = true).assertExists()
        }

        rule.onNodeWithTag("field-filter-all").performClick()
        rule.onNodeWithTag("global-search").performTextClearance()
        rule.onNodeWithTag("global-search").performTextInput("Kelbi")
        rule.onNodeWithTag("small-monster-icon-small_monster_kelbi", useUnmergedTree = true).assertExists()
    }

    @Test
    fun questCategoryAndStarControlsShareOneCompactRowWithoutChangingSemantics() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-guide-category-strip")
            .performScrollToNode(hasTestTag("field-filter-quest"))
        rule.onNodeWithTag("field-filter-quest").performClick()

        val controls = rule.onNodeWithTag("quest-filter-controls").fetchSemanticsNode().boundsInRoot
        assertTrue(controls.height <= 88f)
        rule.onNodeWithTag("quest-filter-summary").assertTextEquals("All quests")

        rule.onNodeWithTag("quest-filters-button").performClick()
        rule.onNodeWithTag("quest-filter-type-guild").performClick().assertIsSelected()
        rule.onNodeWithTag("quest-filter-rank-high").performClick().assertIsSelected()
        rule.onNodeWithTag("quest-filter-star-7").performClick().assertIsSelected()
        rule.onNodeWithTag("quest-filter-apply").performClick()
        rule.onNodeWithTag("quest-count").assertTextEquals("35 quests")
    }

    private fun openItem(name: String, id: String) {
        rule.onNodeWithTag("global-search").performTextInput(name)
        rule.onNodeWithTag("result-material-$id").performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
    }
}
