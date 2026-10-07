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
class ScrapConversionIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun generated(): GeneratedDataset = context.assets.open("mhp3rd-data.json").bufferedReader().use {
        Json { ignoreUnknownKeys = true }.decodeFromString(it.readText())
    }

    @Test
    fun productionScrapLayerHasAcceptedCensusAndCourageSemantics() {
        val data = generated()
        assertEquals(31, data.schemaVersion)
        assertEquals(328, data.itemScrapConversionRules.size)
        assertEquals(328, data.itemScrapConversionRules.map { it.inputGameItemId }.distinct().size)
        assertEquals(70, data.itemScrapConversionRules.map { it.outputScrapGameItemId }.distinct().size)
        assertEquals(326, data.itemScrapConversionRules.count { it.quantitySemantics == "EXACT_PUBLISHED" })
        assertEquals(2, data.itemScrapConversionRules.count { it.quantitySemantics == "NOT_PUBLISHED" })
        val courage = data.itemScrapConversionRules.associateBy { it.inputGameItemId }
        assertEquals(801, courage.getValue(605).outputScrapGameItemId)
        assertEquals(802, courage.getValue(606).outputScrapGameItemId)
        assertTrue(courage.getValue(605).outputQuantity == null)
        assertTrue(courage.getValue(606).outputQuantity == null)
        assertEquals("NOT_PUBLISHED", courage.getValue(605).quantitySemantics)
        assertEquals("NOT_PUBLISHED", courage.getValue(606).quantitySemantics)
        assertEquals(
            listOf("DIRECT_PALICO_ARMORY_HANDOFF", "HUNTER_GEAR_MATERIAL_CONSUMPTION"),
            courage.getValue(605).triggerModes
        )
    }

    @Test
    fun repositoryProjectsScrapOnlyOnOutputItemsWithQuantitiesAndFallbackYield() {
        val materials = CompanionRepository(context).data.materials
        val courageScraps = materials.single { it.gameItemId == 801 }
        val courageSource = courageScraps.sources.single { it.type == MaterialSourceType.SCRAP_CONVERSION }
        assertEquals("Courage Scraps", courageSource.name)
        assertEquals("Commendation", courageSource.inputItemName)
        assertEquals(null, courageSource.quantity)
        assertEquals("NOT_PUBLISHED", courageSource.quantitySemantics)
        assertTrue(courageSource.context.orEmpty().contains("Palico smith"))
        assertTrue(courageSource.sourceNote.orEmpty().contains("DIRECT_PALICO_ARMORY_HANDOFF"))

        val commendation = materials.single { it.gameItemId == 605 }
        assertFalse(commendation.sources.any { it.type == MaterialSourceType.SCRAP_CONVERSION })

        val quantifiedRule = generated().itemScrapConversionRules.first { it.outputQuantity != null }
        val output = materials.single { it.gameItemId == quantifiedRule.outputScrapGameItemId }
        val source = output.sources.singleOrNull {
            it.type == MaterialSourceType.SCRAP_CONVERSION && it.inputGameItemId == quantifiedRule.inputGameItemId
        }
        assertNotNull(source)
        assertEquals(quantifiedRule.outputQuantity, source?.quantity)
    }
}
