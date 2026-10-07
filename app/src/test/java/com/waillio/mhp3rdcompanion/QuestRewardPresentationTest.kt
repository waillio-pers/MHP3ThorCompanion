package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuestRewardPresentationTest {
    private fun quest(id: String, category: String = "Village", rank: String = "Low Rank", stars: Int = 1, number: Int = 1) =
        Quest(id, "Quest $id", "", category = category, rank = rank, stars = stars, canonicalNumber = number, objective = "Hunt $id")

    private fun source(id: String, questId: String, pool: String, chance: Int, quantity: Int, condition: String? = null) =
        MaterialSource(
            id = id,
            type = MaterialSourceType.QUEST_REWARD,
            name = questId,
            questId = questId,
            method = pool,
            rewardPool = pool,
            rewardRelationId = "raw-$id",
            chance = chance,
            probabilitySemantics = "QUEST_REWARD_SLOT",
            quantity = quantity,
            condition = condition
        )

    @Test
    fun questFirstProjectionKeepsPoolOrderAndDuplicateRows() {
        val q = quest("q")
        val rows = projectQuestRewards(
            listOf(
                source("basic-1", "q", "BASIC", 12, 1),
                source("fixed", "q", "FIXED", 100, 1),
                source("basic-2", "q", "BASIC", 5, 2),
                source("additional", "q", "ADDITIONAL", 8, 1, "2頭討伐")
            ), listOf(q)
        )
        assertEquals(1, rows.size)
        assertEquals(listOf("FIXED", "BASIC", "ADDITIONAL"), rows.single().pools.map { it.rewardPool })
        assertEquals(2, rows.single().pools[1].entries.size)
        assertEquals("raw-basic-2", rows.single().pools[1].entries[1].rawRelationId)
        assertEquals("Slay 2 monsters", questRewardConditionLabel("2頭討伐"))
        assertEquals("Condition label unavailable", questRewardConditionLabel("future-condition"))
    }

    @Test
    fun questOrderingAndSearchReuseSupplyGrammar() {
        val quests = listOf(
            quest("event", "Event", "", 1, 1),
            quest("guild", "Guild", "High Rank", 1, 2),
            quest("village", "Village", "Low Rank", 2, 3)
        )
        val rows = projectQuestRewards(
            listOf(
                source("e", "event", "BASIC", 1, 1),
                source("g", "guild", "BASIC", 2, 1),
                source("v", "village", "BASIC", 3, 1)
            ), quests
        )
        assertEquals(listOf("village", "guild", "event"), rows.map { it.questId })
        assertTrue(questRewardSearchMatches(rows[1], "hunt guild"))
        assertTrue(rows[1].quest.let { SupplyQuestProjection(it, emptyList(), emptyList(), 0).matchesFilter(SupplyQuestFilterKey.GUILD_HIGH) })
    }

    @Test
    fun repositoryProjectsExactOrdinaryRewardCorpus() {
        val repository = CompanionRepository(ApplicationProvider.getApplicationContext())
        val rows = repository.data.materials.flatMap { material ->
            material.sources.filter { it.type == MaterialSourceType.QUEST_REWARD }
        }
        assertEquals(4206, rows.size)
        assertEquals(4206, rows.mapNotNull { it.rewardRelationId }.distinct().size)
        assertEquals(16, rows.count { it.condition != null })
        assertEquals(setOf("FIXED", "BASIC", "ADDITIONAL"), rows.mapNotNull { it.rewardPool }.toSet())
        assertTrue(rows.all { it.probabilitySemantics == "QUEST_REWARD_SLOT" })
        assertNotEquals(null, rows.first().quantity)
    }
}
