package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.QuestProgressionRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class QuestProgressionIntegrationTest {
    private val repository = CompanionRepository(ApplicationProvider.getApplicationContext())
    private val progression = repository.questProgression

    @Test
    fun androidAssetLoadsAllAcceptedMembershipsAndRoles() {
        assertEquals(354, progression.production.questCount)
        assertEquals(53, progression.keyQuestIds.size)
        assertEquals(22, progression.steps.filter { it.scope == "VILLAGE" }.sumOf { it.keyQuestIds.size })
        assertEquals(14, progression.steps.filter { it.scope == "GUILD_LOW" }.sumOf { it.keyQuestIds.size })
        assertEquals(17, progression.steps.filter { it.scope == "GUILD_HIGH" }.sumOf { it.keyQuestIds.size })
        assertEquals(QuestProgressionRole.KEY, progression.roleFor("quest_village_1_star_03"))
        assertEquals(QuestProgressionRole.URGENT, progression.roleFor("quest_guild_5_star_22"))
        assertEquals(QuestProgressionRole.OPTIONAL_FINAL_CHALLENGE, progression.roleFor("quest_village_6_star_16"))
        assertNull(progression.roleFor("quest_guild_7_star_21"))
    }

    @Test
    fun prerequisitesResolveAgainstRuntimeQuestNamesWithoutChangingMembership() {
        val target = "quest_guild_7_star_08"
        val prerequisite = progression.prerequisiteFor(target)
        assertEquals("quest_guild_7_star_20", prerequisite)
        assertTrue(target in progression.keyQuestIds)
        assertFalse(prerequisite in progression.keyQuestIds)
        val prerequisiteQuest = repository.data.quests.single { it.id == prerequisite }
        assertEquals("Fanged Wyvern, Barioth!", prerequisiteQuest.name)
    }

    @Test
    fun guildEightSequenceIsAKeySetThenJhenThenAmatsuAtTheSameStar() {
        val keyStep = progression.steps.single { it.id == "GUILD_HIGH_8_STAR" }
        val followUp = progression.steps.single { it.id == "GUILD_HIGH_8_POST_JHEN" }
        assertEquals(6, keyStep.keyQuestIds.size)
        assertEquals("quest_guild_8_urgent_jhen", keyStep.urgent?.companionQuestId)
        assertEquals(followUp.id, keyStep.nextStepId)
        assertEquals(8, followUp.star)
        assertEquals(keyStep.urgent?.id, followUp.requiresUrgentId)
        assertEquals("quest_guild_8_urgent_amatsu", followUp.urgent?.companionQuestId)
        assertFalse(progression.steps.any { it.star == 9 })
    }
}
