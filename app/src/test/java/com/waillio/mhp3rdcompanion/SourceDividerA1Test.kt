package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.MaterialSource
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import com.waillio.mhp3rdcompanion.data.groupForDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceDividerA1Test {
    @Test
    fun baggiScaleIngredientOnlyCraftingLinkDoesNotCreateAVisibleFamily() {
        val itemGameId = 316
        val reverseDecorationLink = MaterialSource(
            id = "decoration-ingredient-link",
            type = MaterialSourceType.DECORATION_CRAFTING,
            name = "Decoration Ingredient",
            decorationOutputGameItemId = 900,
            decorationIngredientGameItemId = itemGameId
        )
        val smallMonster = MaterialSource("small", MaterialSourceType.SMALL_MONSTER, "Baggi")
        val palico = MaterialSource("palico", MaterialSourceType.PALICO_EXPEDITION, "Palico Expedition")
        val monster = MaterialSource("monster", MaterialSourceType.MONSTER_REWARD, "Great Baggi")
        val groups = listOf(reverseDecorationLink, smallMonster, palico, monster).groupForDisplay()

        assertEquals(
            listOf("decoration-crafting", "small-monster", "monster-reward", "palico-expedition"),
            sourceFamilyIdsForDisplay(groups)
        )
        val rendered = sourceFamilyIdsForRenderedMaterial(groups, itemGameId)
        assertEquals(listOf("small-monster", "monster-reward", "palico-expedition"), rendered)
        assertTrue(rendered.indexOf("monster-reward") < rendered.indexOf("palico-expedition"))
        assertTrue(decorationCraftingOutputRows(itemGameId, groups.decorationCrafting).isEmpty())
    }

    @Test
    fun actualDecorationOutputsRemainFirstAndVisible() {
        val itemGameId = 316
        val output = MaterialSource(
            id = "decoration-output",
            type = MaterialSourceType.DECORATION_CRAFTING,
            name = "Crafted Item",
            decorationOutputGameItemId = itemGameId,
            decorationIngredientGameItemId = 900
        )
        val smallMonster = MaterialSource("small", MaterialSourceType.SMALL_MONSTER, "Baggi")
        val groups = listOf(smallMonster, output).groupForDisplay()

        assertEquals(
            listOf("decoration-crafting", "small-monster"),
            sourceFamilyIdsForRenderedMaterial(groups, itemGameId)
        )
        assertEquals(listOf(output), decorationCraftingOutputRows(itemGameId, groups.decorationCrafting))
    }
}
