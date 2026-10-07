package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import com.waillio.mhp3rdcompanion.data.RegularQuestSupplyCoverageStatus
import com.waillio.mhp3rdcompanion.data.RegularQuestSupplyLifecycle
import com.waillio.mhp3rdcompanion.data.RegularQuestSupplyQuantitySemantics
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuestSupplyProductionIntegrationTest {
    private val repository = CompanionRepository(ApplicationProvider.getApplicationContext())

    @Test
    fun productionCorpusAndCoveragePartitionAreComplete() {
        val bytes = ApplicationProvider.getApplicationContext<android.content.Context>()
            .assets.open("mhp3rd-data.json").use { it.readBytes() }
        val generated = Json { ignoreUnknownKeys = true }
            .decodeFromString<com.waillio.mhp3rdcompanion.data.GeneratedDataset>(bytes.decodeToString())
        assertEquals(31, generated.schemaVersion)
        assertEquals(3498, generated.regularQuestSupplyItems.size)
        assertEquals(3491, generated.regularQuestSupplyItems.map { it.id }.distinct().size)
        assertEquals(57, generated.regularQuestSupplyItems.map { it.gameItemId }.distinct().size)
        assertEquals(1493, generated.regularQuestSupplyItems.count { it.lifecycle.name == "QUEST_LOCAL_SUPPLY_ONLY" })
        assertEquals(2005, generated.regularQuestSupplyItems.count { it.lifecycle.name == "PERSISTENT_NORMAL_SUPPLY" })
        assertEquals(354, generated.regularQuestSupplyCoverage.size)
        assertEquals(326, generated.regularQuestSupplyCoverage.count { it.status.name == "DATA_PRESENT" })
        assertEquals(6, generated.regularQuestSupplyCoverage.count { it.status.name == "EXPLICIT_NO_SUPPLY" })
        assertEquals(22, generated.regularQuestSupplyCoverage.count { it.status.name == "SOURCE_UNAVAILABLE" })
    }

    @Test
    fun repositoryExposesSupplyAndDistinguishesCoverageStates() {
        assertEquals(3498, repository.regularQuestSupplyItems.size)
        assertEquals(354, repository.regularQuestSupplyCoverage.size)
        assertEquals(RegularQuestSupplyCoverageStatus.EXPLICIT_NO_SUPPLY,
            repository.regularQuestSupplyCoverageForQuest("quest_guild_8_star_36"))
        assertEquals(RegularQuestSupplyCoverageStatus.SOURCE_UNAVAILABLE,
            repository.regularQuestSupplyCoverageForQuest("quest_guild_7_star_01"))
        val presentQuest = repository.regularQuestSupplyItems.first().questId
        assertEquals(RegularQuestSupplyCoverageStatus.DATA_PRESENT,
            repository.regularQuestSupplyCoverageForQuest(presentQuest))
        assertTrue(repository.regularQuestSupplyForQuest(presentQuest).isNotEmpty())
    }

    @Test
    fun persistentSupplyProjectsAsDistinctSourceAndLocalSupplyDoesNot() {
        val persistentItem = repository.data.materials.single { it.gameItemId == 138 }
        assertTrue(persistentItem.sources.any { it.type == MaterialSourceType.SUPPLY_BOX })
        assertTrue(persistentItem.sources.any { it.type == MaterialSourceType.QUEST_REWARD })

        val localOnlyItem = repository.data.materials.single { it.gameItemId == 670 }
        assertFalse(localOnlyItem.sources.any { it.type == MaterialSourceType.SUPPLY_BOX })

        val source = persistentItem.sources.first { it.type == MaterialSourceType.SUPPLY_BOX }
        assertEquals("Supply box layout", source.context)
        assertTrue(source.quantity == null)
    }

    @Test
    fun supplyQuantitySemanticsStayStructuredWithoutBundleMultiplication() {
        val row = repository.regularQuestSupplyItems.first { it.quantitySemantics == RegularQuestSupplyQuantitySemantics.FINITE_MULTI_BUNDLE }
        assertTrue((row.bundleCount ?: 0) > 1)
        assertTrue(row.quantityValue == null || row.quantityValue!! > 0)
        assertTrue(row.lifecycle == RegularQuestSupplyLifecycle.PERSISTENT_NORMAL_SUPPLY ||
            row.lifecycle == RegularQuestSupplyLifecycle.QUEST_LOCAL_SUPPLY_ONLY)
    }
}
