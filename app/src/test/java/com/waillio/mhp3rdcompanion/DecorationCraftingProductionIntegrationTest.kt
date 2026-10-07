package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.GeneratedDataset
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DecorationCraftingProductionIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun generated(): GeneratedDataset = context.assets.open("mhp3rd-data.json").bufferedReader().use {
        Json { ignoreUnknownKeys = true }.decodeFromString(it.readText())
    }

    @Test
    fun acceptedDecorationRecipeCorpusIsPresentAndClosed() {
        val data = generated()
        assertEquals(31, data.schemaVersion)
        assertEquals(198, data.decorationCraftingRecipes.size)
        assertEquals(164, data.decorationCraftingRecipes.map { it.outputGameItemId }.distinct().size)
        assertEquals(514, data.decorationCraftingRecipes.sumOf { it.ingredients.size })
        assertEquals(514, data.decorationCraftingRecipes.flatMap { it.ingredients }.count { it.quantity != null })
        assertEquals(0, data.decorationCraftingRecipes.flatMap { it.ingredients }.count { it.quantitySemantics == "SOURCE_QUANTITY_CONFLICT" })
        assertTrue(data.decorationCraftingRecipes.flatMap { it.ingredients }.all { it.gameItemId in 1..978 })
    }

    @Test
    fun repositoryProjectsDecorationOutputsAndIngredientReverseUsage() {
        val materials = CompanionRepository(context).data.materials
        val decoration = materials.single { it.gameItemId == 815 }
        assertTrue(decoration.sources.any { it.type == MaterialSourceType.DECORATION_CRAFTING })
        val ingredient = materials.single { it.gameItemId == 811 }
        val usage = ingredient.sources.firstOrNull {
            it.type == MaterialSourceType.DECORATION_CRAFTING && it.decorationOutputGameItemId == 815
        }
        assertNotNull(usage)
        assertEquals("LOW", usage?.context?.let { if (it.startsWith("Low")) "LOW" else it })
        assertEquals(1, usage?.quantity)
    }

    @Test
    fun sourceConflictNeverInventsQuantity() {
        val data = generated()
        val conflict = data.decorationCraftingRecipes.single { it.outputGameItemId == 891 }
            .ingredients.single { it.gameItemId == 242 }
        assertEquals(1, conflict.quantity)
        assertEquals("SOURCE_ADJUDICATED_PUBLISHED", conflict.quantitySemantics)
        val material = CompanionRepository(context).data.materials.single { it.gameItemId == 891 }
        assertTrue(material.sources.any { it.type == MaterialSourceType.DECORATION_CRAFTING && it.quantity == 1 })
    }
}
