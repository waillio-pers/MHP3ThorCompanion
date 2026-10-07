package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.GeneratedDataset
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ItemCombinationProductionIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val repository = CompanionRepository(context)

    @Test
    fun productionCombinationLayerHasCompleteCensus() {
        val generated = context.assets.open("mhp3rd-data.json").bufferedReader().use {
            Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(it.readText())
        }
        assertEquals(31, generated.schemaVersion)
        assertEquals(117, generated.itemCombinationRecipes.size)
        assertEquals((1..117).toList(), generated.itemCombinationRecipes.map { it.recipeNumber })
        assertEquals(1, generated.itemCombinationFailureRelations.size)
        assertEquals(144, generated.itemCombinationFailureRelations.single().outputGameItemId)
        val validIds = generated.items.mapNotNull { it.gameItemId }.toSet()
        assertTrue(generated.itemCombinationRecipes.all { it.outputGameItemId in validIds })
        assertTrue(generated.itemCombinationRecipes.all { it.ingredientAGameItemId in validIds && it.ingredientBGameItemId in validIds })
        assertEquals(114, generated.itemCombinationRecipes.map { it.outputGameItemId }.toSet().size)
        assertEquals(100, generated.itemCombinationRecipes.flatMap { listOf(it.ingredientAGameItemId, it.ingredientBGameItemId) }.toSet().size)
        assertEquals(92, generated.itemCombinationRecipes.count { it.outputQuantityMin == it.outputQuantityMax })
        assertEquals(25, generated.itemCombinationRecipes.count { it.outputQuantityMin != it.outputQuantityMax })
        assertEquals(listOf(65, 66, 112, 113, 114, 115, 116, 117), generated.itemCombinationRecipes.filter { it.baseSuccessPercent == 100 }.map { it.recipeNumber })
        assertEquals(listOf(25, 33, 37, 42, 43, 46), generated.itemCombinationRecipes.filter { it.successOverrideSourceNote != null }.map { it.recipeNumber })
    }

    @Test
    fun reverseSourcesExposeNormalRangedSupplyAlternateAndFailureCases() {
        val potion = repository.data.materials.single { it.gameItemId == 8 }
        val potionRecipe = potion.sources.single { it.type == MaterialSourceType.COMBINATION }
        assertEquals(1, potionRecipe.recipeNumber)
        assertEquals("Herb", potionRecipe.ingredientAName)
        assertEquals("Blue Mushroom", potionRecipe.ingredientBName)
        assertEquals("95% base", "${potionRecipe.baseSuccessPercent}% base")
        assertEquals("×1", combinationQuantityLabel(potionRecipe.outputQuantityMin, potionRecipe.outputQuantityMax))

        val ranged = repository.data.materials.flatMap { it.sources }.first {
            it.type == MaterialSourceType.COMBINATION && it.outputQuantityMin != it.outputQuantityMax
        }
        assertTrue(combinationQuantityLabel(ranged.outputQuantityMin, ranged.outputQuantityMax)!!.contains("–"))

        val throwingKnife = repository.data.materials.single { it.gameItemId == 54 }
            .sources.single { it.type == MaterialSourceType.COMBINATION }
        assertEquals("QUEST_SUPPLY_ONLY", throwingKnife.ingredientAvailability)

        val armorSphere = repository.data.materials.single { it.gameItemId == 804 }
        assertEquals(listOf(112, 113), armorSphere.sources.filter { it.type == MaterialSourceType.COMBINATION }.map { it.recipeNumber })

        val garbage = repository.data.materials.single { it.gameItemId == 144 }
        val failure = garbage.sources.single { it.type == MaterialSourceType.COMBINATION_FAILURE }
        assertEquals("Failed combination", failure.name)
        assertEquals("VARIES_BY_RECIPE_AND_MODIFIERS", failure.probabilitySemantics)
        assertFalse(failure.recipeNumber != null)
        assertTrue(garbage.description.contains("Combination"))
    }

    @Test
    fun sourceNotesRemainDataOnlyAndSupplyNotesAreNotSuccessOverrides() {
        val knifeRecipe = repository.data.materials.single { it.gameItemId == 54 }
            .sources.single { it.type == MaterialSourceType.COMBINATION }
        assertNotNull(knifeRecipe.sourceNote)
        assertEquals(null, knifeRecipe.successOverrideSourceNote)
        val override = repository.data.materials.flatMap { it.sources }.single {
            it.type == MaterialSourceType.COMBINATION && it.recipeNumber == 25
        }
        assertNotNull(override.successOverrideSourceNote)
    }
}
