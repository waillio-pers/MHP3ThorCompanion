package com.waillio.mhp3rdcompanion

import org.junit.Assert.*
import org.junit.Test

class PalicoSkillsTest {
    @Test fun corpusHasUniqueIdentitiesAndValidCosts() {
        assertEquals(52, palicoSkills.size)
        assertEquals(palicoSkills.size, palicoSkills.map { it.id }.distinct().size)
        assertEquals(palicoSkills.size, palicoSkills.map { it.japaneseName }.distinct().size)
        assertTrue(palicoSkills.all { it.points > 0 && it.memory in 1..4 && it.name.isNotBlank() && it.effect.isNotBlank() })
    }

    @Test fun monsterUnlockListsAlternativesAndRequiresTheIntendedPalico() {
        val skill = palicoSkills.single { it.id == "big_boomerang" }
        assertEquals(100, skill.points)
        assertEquals(2, skill.memory)
        val unlock = skill.unlock as PalicoUnlock.HuntAny
        assertEquals(listOf("Nargacuga", "Barioth", "Duramboros"), unlock.monsters)
        assertEquals("Hunt Nargacuga or Barioth or Duramboros with the Palico you want to train", palicoUnlockText(unlock))
    }

    @Test fun combinedRequirementsRemainAnd() {
        val unlock = PalicoUnlock.All(listOf(PalicoUnlock.Level(15), PalicoUnlock.Affection(4)))
        assertEquals("Palico level 15 and Affection 4", palicoUnlockText(unlock))
    }
}
