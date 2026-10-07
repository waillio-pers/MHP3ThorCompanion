package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SupplyBoxPresentationTest {
    private fun quest(id: String, name: String, category: String, rank: String = "", stars: Int, number: Int) =
        Quest(id, name, "", rank = rank, category = category, stars = stars, canonicalNumber = number)

    private fun source(questId: String, id: String, entry: SupplyBoxEntry) = MaterialSource(
        id = "material_supply_${questId}_1", type = MaterialSourceType.SUPPLY_BOX,
        name = questId, questId = questId, supplyPersistentAcquisition = true, supplyEntries = listOf(entry)
    )

    private fun entry(id: String, quantity: Int? = 1, bundles: Int? = null, semantics: RegularQuestSupplyQuantitySemantics = RegularQuestSupplyQuantitySemantics.FINITE_SINGLE_BUNDLE) =
        SupplyBoxEntry(id, 1, quantity, null, bundles, semantics, "SOURCE_LAYOUT", "SOURCE_UNSPECIFIED", RegularQuestSupplyLifecycle.PERSISTENT_NORMAL_SUPPLY, true)

    @Test fun projectionKeepsEveryRelationAndUsesProgressionOrder() {
        val quests = listOf(
            quest("event", "Event", "Event", stars = 1, number = 1),
            quest("guild-high", "Guild High", "Guild", "High Rank", 1, 2),
            quest("village", "Village", "Village", stars = 2, number = 3)
        )
        val sources = listOf(
            source("event", "e", entry("e")),
            source("guild-high", "g", entry("g")),
            source("village", "v", entry("v")),
            source("village", "v", entry("v2", quantity = 3))
        )
        val rows = projectSupplyQuests(sources, quests)
        assertEquals(listOf("village", "guild-high", "event"), rows.map { it.questId })
        assertEquals(2, rows.first().entries.size)
    }

    @Test fun quantityDimensionsRemainSeparate() {
        assertEquals("4 bundles · ×3", supplyQuantityLabel(entry("x", 3, 4, RegularQuestSupplyQuantitySemantics.FINITE_MULTI_BUNDLE)))
        assertEquals("4 bundles", supplyQuantityLabel(entry("x", null, 4, RegularQuestSupplyQuantitySemantics.FINITE_MULTI_BUNDLE)))
        assertEquals("Unlimited", supplyQuantityLabel(entry("x", null, null, RegularQuestSupplyQuantitySemantics.INFINITE)))
    }

    @Test fun searchAndFilterAreLocalAndCaseInsensitive() {
        val q = quest("q", "Normal S Lv1", "Guild", "Low Rank", 1, 1)
        val row = projectSupplyQuests(listOf(source("q", "row", entry("row"))), listOf(q)).single()
        assertTrue(supplyQuestSearchMatches(row, "normal s"))
        assertTrue(row.matchesFilter(SupplyQuestFilterKey.GUILD_LOW))
        assertTrue(!row.matchesFilter(SupplyQuestFilterKey.GUILD_HIGH))
    }

    @Test fun repositoryRetainsDuplicateEntriesUnderOneQuest() {
        val repository = CompanionRepository(ApplicationProvider.getApplicationContext())
        val material = repository.data.materials.single { it.gameItemId == 59 }
        val row = material.sources.first { it.type == MaterialSourceType.SUPPLY_BOX && it.questId == "quest_guild_7_star_04" }
        assertEquals(2, row.supplyEntries.size)
        assertEquals(listOf("×1", "×1"), row.supplyEntries.map(::supplyQuantityLabel))
    }
}
