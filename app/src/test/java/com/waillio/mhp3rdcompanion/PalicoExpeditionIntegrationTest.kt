package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.GeneratedDataset
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import kotlinx.serialization.json.Json as KotlinJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PalicoExpeditionIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val repository = CompanionRepository(context)
    private val data = repository.data
    private val generated: GeneratedDataset = context.assets.open("mhp3rd-data.json").bufferedReader().use {
        KotlinJson { ignoreUnknownKeys = true }.decodeFromString(it.readText())
    }

    @Test fun allExpeditionsAndRewardsAreRepositoryAccessible() {
        assertEquals(31, generated.schemaVersion)
        assertEquals(24, repository.palicoExpeditions.size)
        assertEquals(662, repository.palicoExpeditionRewardDrops.size)
        assertEquals(662, repository.palicoExpeditionRewardDrops.map { it.id }.toSet().size)
        repository.palicoExpeditions.forEach { expedition ->
            assertTrue(repository.palicoExpeditionRewardsFor(expedition.id).isNotEmpty())
        }
    }

    @Test fun reverseProjectionIncludesPalicoExpeditionAndKeepsQuantities() {
        val exclusive = listOf(200, 598, 599, 600, 601)
        exclusive.forEach { gameItemId ->
            val material = data.materials.single { it.gameItemId == gameItemId }
            assertTrue(material.sources.any { it.type == MaterialSourceType.PALICO_EXPEDITION })
        }
        val quantityRows = data.materials.flatMap { it.sources }.filter { it.type == MaterialSourceType.PALICO_EXPEDITION && (it.quantity ?: 0) > 1 }
        assertEquals(33, quantityRows.size)
        assertFalse(data.materials.flatMap { it.sources }.any { it.type == MaterialSourceType.PALICO_EXPEDITION && (it.method == "SMALL_MONSTER" || it.method == "LARGE_MONSTER" || it.method == "GATHERING") })
    }

    @Test fun noLegacyFieldPalicoOrNyanterSourceLeaks() {
        val expeditionSources = data.materials.flatMap { it.sources }.filter { it.type == MaterialSourceType.PALICO_EXPEDITION }
        assertEquals(662, expeditionSources.size)
        assertTrue(expeditionSources.all { it.name.contains("Palico Expedition") })
        assertTrue(expeditionSources.none { it.name.contains("Monnyan", ignoreCase = true) || it.name.contains("Nyanter", ignoreCase = true) })
    }
}
