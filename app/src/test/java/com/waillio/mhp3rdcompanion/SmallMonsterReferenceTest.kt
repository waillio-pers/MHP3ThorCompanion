package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SmallMonsterReferenceTest {
    private val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data

    @Test fun indexIsSeparateCompleteAlphabeticalAndSearchable() {
        assertEquals(20, data.smallMonsters.size)
        assertTrue(data.monsters.map { it.id }.toSet().intersect(data.smallMonsters.map { it.id }.toSet()).isEmpty())
        assertEquals(listOf("Altaroth", "Anteka", "Aptonoth", "Baggi", "Bnahabra"), filterSmallMonsters(data.smallMonsters, "").take(5).map { it.name })
        assertEquals(listOf("Bnahabra"), filterSmallMonsters(data.smallMonsters, "NAHAB" ).map { it.name })
    }

    @Test fun contextsUseLowHighSpecialPriorityAndOnlyAvailableValues() {
        val altaroth = data.smallMonsters.single { it.name == "Altaroth" }
        assertEquals(
            listOf(SmallMonsterRewardContext.LOW, SmallMonsterRewardContext.HIGH, SmallMonsterRewardContext.GUILD_1_2),
            altaroth.rewards.availableSmallMonsterContexts()
        )
        val uroktor = data.smallMonsters.single { it.name == "Uroktor" }
        assertEquals(listOf(SmallMonsterRewardContext.LOW, SmallMonsterRewardContext.HIGH), uroktor.rewards.availableSmallMonsterContexts())
    }

    @Test fun sourceNativeRewardVariantsRemainDistinct() {
        val bnahabra = data.smallMonsters.single { it.name == "Bnahabra" }
        val bnahabraGroups = bnahabra.rewards.groupForSmallMonsterDisplay(SmallMonsterRewardContext.LOW)
        assertEquals(setOf("Pale White", "Pale Brown", "Pale Blue", "Pale Red"),
            bnahabraGroups.mapNotNull { it.conditionDetails?.wingColor?.lowercase()?.split('_')?.joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) } }.toSet())
        assertEquals(4, bnahabraGroups.size)

        val altaroth = data.smallMonsters.single { it.name == "Altaroth" }
        val altarothGroups = altaroth.rewards.groupForSmallMonsterDisplay(SmallMonsterRewardContext.LOW)
        assertEquals(5, altarothGroups.size)
        assertEquals(setOf("BLUE", "ORANGE", "GREEN", "GOLD"), altarothGroups.mapNotNull { it.conditionDetails?.color }.toSet())

        val kelbi = data.smallMonsters.single { it.name == "Kelbi" }
        val stunned = kelbi.rewards.groupForSmallMonsterDisplay(SmallMonsterRewardContext.LOW).single { it.condition == "STUNNED" }
        assertEquals(1, stunned.rewards.size)
        assertEquals("Kelbi Horn", stunned.rewards.single().itemName)
        assertEquals(100, stunned.rewards.single().chancePercent)

        val gargwa = data.smallMonsters.single { it.name == "Gargwa" }
        val gargwaConditions = gargwa.rewards.groupForSmallMonsterDisplay(SmallMonsterRewardContext.LOW).mapNotNull { it.condition }.toSet()
        assertTrue("WHITE_EGG" in gargwaConditions)
        assertTrue("GOLD_EGG" in gargwaConditions)
        assertTrue("NORMAL_DROP" in gargwaConditions)
    }

    @Test fun tipsAndRelatedQuestsComeOnlyFromStructuredProductionData() {
        val bnahabra = data.smallMonsters.single { it.name == "Bnahabra" }
        assertEquals("Poison kills preserve the corpse for carving; ordinary kills usually shatter it.", bnahabra.tips.single().text)
        assertTrue("quest_guild_6_star_08" in bnahabra.relatedQuestIds)

        val jaggi = data.smallMonsters.single { it.name == "Jaggi" }
        assertTrue("quest_village_1_star_03" in jaggi.relatedQuestIds)
        assertFalse(data.quests.single { it.id == "quest_village_1_star_03" }.objectiveTargetMonsterIds.contains("monster_great_jaggi"))
        assertTrue(data.quests.single { it.id == "quest_village_1_star_03" }.objectiveTargetSmallMonsterIds.contains(jaggi.id))

        val wroggi = data.smallMonsters.single { it.name == "Wroggi" }
        assertTrue(wroggi.relatedQuestIds.isNotEmpty())
        assertTrue(wroggi.relatedQuestIds.all { questId ->
            "monster_great_wroggi" !in data.quests.single { it.id == questId }.objectiveTargetMonsterIds
        })
    }
}
