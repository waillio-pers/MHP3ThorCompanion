package com.waillio.mhp3rdcompanion

import android.content.Context
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
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class RoastingProductionIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun generated(): GeneratedDataset = context.assets.open("mhp3rd-data.json")
        .bufferedReader()
        .use { Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(it.readText()) }

    @Test
    fun productionHasTheFrozenSchema21RoastingCorpus() {
        val data = generated()
        assertEquals(31, data.schemaVersion)
        assertEquals(30, data.roastingConversions.size)
        assertEquals(3, data.roastingConversions.count { it.context == "FIELD_BBQ" })
        assertEquals(27, data.roastingConversions.count { it.context == "FARM_CUSTOM_ROASTER" })
        assertEquals(setOf(39, 186, 187, 189, 190, 192, 193, 195, 199), data.roastingConversions.map { it.inputGameItemId }.toSet())
        assertEquals(setOf(43, 44, 45, 48, 49, 50), data.roastingConversions.map { it.outputGameItemId }.toSet())
        assertTrue(data.roastingConversions.all { it.inputQuantity == 1 && it.outputQuantity == 1 })
        assertTrue(data.roastingConversions.all { it.probability == null && it.resultSemantics == "PLAYER_TIMING_RESULT" })
        assertTrue(data.roastingConversions.filter { it.context == "FIELD_BBQ" }.all { it.supportedToolGameItemIds == listOf(90, 673) })
        assertTrue(data.roastingConversions.filter { it.context == "FARM_CUSTOM_ROASTER" }.all { it.facilityId == "farm_custom_roaster" && it.batchCapacity == 10 })
        assertTrue(data.roastingConversions.none { it.outputGameItemId == 185 })
    }

    @Test
    fun repositoryProjectsOutputAndReverseUsageWithoutSpitIngredients() {
        val data = CompanionRepository(context).data
        val output = data.materials.single { it.gameItemId == 43 }
        val rawMeat = data.materials.single { it.gameItemId == 39 }
        val outputSources = output.sources.filter { it.type == MaterialSourceType.ROASTING }
        val usageSources = rawMeat.sources.filter { it.type == MaterialSourceType.ROASTING }
        assertEquals(2, outputSources.size)
        assertEquals(6, usageSources.size)
        assertTrue(outputSources.all { it.name in setOf("Field BBQ", "Farm Custom Roaster") })
        assertTrue(usageSources.all { it.name in setOf("Field BBQ", "Farm Custom Roaster") })
        assertTrue(outputSources.all { it.roastingInputQuantity == 1 })
        assertTrue(usageSources.none { it.inputGameItemId == 90 || it.inputGameItemId == 673 })
        assertTrue(data.materials.single { it.gameItemId == 90 }.sources.none { it.type == MaterialSourceType.ROASTING })
        assertTrue(data.materials.single { it.gameItemId == 673 }.sources.none { it.type == MaterialSourceType.ROASTING })
        assertTrue(output.description.contains("Roasting"))
        assertTrue(rawMeat.description.contains("Roasting"))
        assertNotNull(outputSources.first().inputItemName)
    }

    @Test
    fun embeddedAssetMatchesAcceptedIntegratedSha() {
        val bytes = context.assets.open("mhp3rd-data.json").use { it.readBytes() }
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02X".format(it) }
        assertEquals("657CFA9376FC8CBE56C67E67D4D1A27C455D21021F51E21B62427C8DDBB26155", sha)
        assertFalse(bytes.isEmpty())
    }
}
