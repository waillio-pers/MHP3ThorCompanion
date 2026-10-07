package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import com.waillio.mhp3rdcompanion.data.RewardContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuestRuntimeMicrofixTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val data = CompanionRepository(context).data

    @Test
    fun smallMonsterQuestIdsReachRuntimeWithoutUnknownSecondField() {
        val anteka = data.smallMonsters.single { it.name == "Anteka" }
        assertTrue(anteka.relatedQuestIds.containsAll(listOf("quest_event_603", "quest_event_604")))
        val aptonoth = data.smallMonsters.single { it.name == "Aptonoth" }
        assertTrue("quest_event_605" in aptonoth.relatedQuestIds)
        val gargwa = data.smallMonsters.single { it.name == "Gargwa" }
        assertTrue(gargwa.relatedQuestIds.containsAll(listOf("quest_event_615", "quest_event_634", "quest_event_635")))
        val bullfango = data.smallMonsters.single { it.name == "Bullfango" }
        assertTrue(bullfango.relatedQuestIds.containsAll(listOf("quest_hot_spring_40101", "quest_event_601")))
        val delex = data.smallMonsters.single { it.name == "Delex" }
        assertTrue(delex.relatedQuestIds.containsAll(listOf("quest_event_610", "quest_event_621", "quest_event_629")))
        assertEquals(82, data.smallMonsters.sumOf { it.relatedQuestIds.size })
        val generated = context.assets.open("mhp3rd-data.json").bufferedReader().use { it.readText() }
        assertFalse(generated.contains("relatedQuestIds"))
    }

    @Test
    fun questRewardSourceRankDerivesFromQuestAndLeavesSpecialRanksNull() {
        val sources = data.materials.flatMap { it.sources }.filter { it.type == MaterialSourceType.QUEST_REWARD }
        assertEquals(4206, sources.size)
        assertEquals(1914, sources.count { it.rank == RewardContext.HIGH })
        assertEquals(1976, sources.count { it.rank == RewardContext.LOW })
        assertEquals(316, sources.count { it.rank == null })
        assertEquals(0, sources.count { it.rank !in setOf(null, RewardContext.LOW, RewardContext.HIGH) })
        assertTrue(sources.filter { it.questId == "quest_event_600" }.all { it.rank == RewardContext.LOW })
        assertTrue(sources.filter { it.questId == "quest_event_629" }.all { it.rank == RewardContext.HIGH })
        assertTrue(sources.filter { it.questId == "quest_hot_spring_40101" }.all { it.rank == null })
        assertTrue(sources.filter { it.questId == "quest_drink_40201" }.all { it.rank == null })
    }

    @Test
    fun allRegularQuestCategoryFiltersUseExplicitProductionNames() {
        val expected = mapOf(
            QuestCategoryFilter.ALL to 354,
            QuestCategoryFilter.VILLAGE to 91,
            QuestCategoryFilter.GUILD to 184,
            QuestCategoryFilter.HOT_SPRING to 7,
            QuestCategoryFilter.DRINK to 20,
            QuestCategoryFilter.EVENT to 52,
        )
        expected.forEach { (filter, count) ->
            assertEquals(count, filterQuests(data.quests, "", null, filter).size)
        }
        assertEquals("Hot Spring", QuestCategoryFilter.HOT_SPRING.productionCategory())
        assertEquals("Drink", QuestCategoryFilter.DRINK.productionCategory())
        assertEquals("Event", QuestCategoryFilter.EVENT.productionCategory())
    }

    @Test
    fun villageGuildLowQuestRewardsReachTheExistingItemSourceIndex() {
        val village = data.materials.single { it.gameItemId == 162 }
        assertTrue(village.sources.any { it.type == MaterialSourceType.QUEST_REWARD && it.questId == "quest_village_1_star_01" })

        val guildLow = data.materials.single { it.gameItemId == 167 }
        assertTrue(guildLow.sources.any { it.type == MaterialSourceType.QUEST_REWARD && it.questId == "quest_guild_4_star_07" })

        val conditional = data.materials.single { it.gameItemId == 241 }.sources.filter {
            it.type == MaterialSourceType.QUEST_REWARD && it.questId == "quest_guild_5_star_16"
        }
        assertEquals(3, conditional.size)
        val conditionalAdditional = conditional.filter { it.condition != null }
        assertEquals(setOf("2頭討伐", "10頭討伐"), conditionalAdditional.mapNotNull { it.condition }.toSet())
        assertTrue(conditionalAdditional.all { it.context in setOf("2頭討伐", "10頭討伐") })
        assertTrue(data.materials.flatMap { it.sources }.filter { it.type == MaterialSourceType.QUEST_REWARD }
            .any { it.questId == "quest_guild_5_star_16" && it.condition == "4頭討伐" })
    }

    @Test
    fun questRewardConditionFormatterHandlesAcceptedCountsAndUnknownValues() {
        mapOf(
            "2頭討伐" to "Slay 2 monsters",
            "4頭討伐" to "Slay 4 monsters",
            "6頭討伐" to "Slay 6 monsters",
            "8頭討伐" to "Slay 8 monsters",
            "10頭討伐" to "Slay 10 monsters"
        ).forEach { (raw, expected) -> assertEquals(expected, formatQuestRewardCondition(raw)) }
        assertEquals(null, formatQuestRewardCondition(null))
        assertEquals("Break horns", formatQuestRewardCondition("Break horns"))
    }

    @Test
    fun everyProductionConditionalQuestRewardSourceHasVisibleConditionLabel() {
        val conditional = data.materials.flatMap { it.sources }.filter {
            it.type == MaterialSourceType.QUEST_REWARD && it.condition != null
        }
        assertEquals(16, conditional.size)
        assertTrue(conditional.all { !formatQuestRewardCondition(it.condition).isNullOrBlank() })
    }
}
