package com.waillio.mhp3rdcompanion

import org.junit.Assert.*
import org.junit.Test

class PalicoSkillsTest {
    @Test fun tmoNamesStayBoundToTheirPalicoIdentities() {
        val names = palicoSkills.associate { it.id to it.name }
        assertEquals("Cure Flute", names["antidote_flute"])
        assertEquals("Wind Negate", names["wind_immunity"])
        assertEquals("Boomerang Up", names["big_boomerang"])
        assertEquals("Hunter", names["no_gathering"])
        assertEquals("Friendship", names["bond"])
        // TMO PSP v6.1 r754, language table 0, entries 1782..1833.
        val expected = listOf(
            "Demon Flute", "Armor Flute", "Cure Flute", "Health Flute", "Health Flute (L)",
            "Elemental Attack Up", "Status Atk [Poison]", "StatusAtk [Paralysis]", "Status Atk [Sleep]", "Abn Status Atk Up",
            "Mud/Snow Quick Rec", "Poison Negate", "Wind Negate", "Earplug", "Earplugs/Wind Negate",
            "Attack Up（S）", "Attack Up (L)", "Defense Up（S）", "Defense Up (L)", "Health Up（S）", "Health Up (L)",
            "Guard Up（S）", "Guard Up (L)", "Affinity Boost", "Rob'em Blind", "Shock Trap", "Felvine Bomb",
            "Large Barrel Bomb", "Bomb Strength Up", "Bomb Dmg Reduction", "Boomerang Up", "Pierce Boomerang",
            "Cannon/Ballista Up", "Quick Riser", "9 Lives", "Last Stand", "Resist Fire", "Resist Water",
            "Resist Thunder", "Resist Ice", "Resist Dragon", "Resist all", "Clairvoyance", "Speedy Gathering",
            "Speedy Recovery", "Hunter", "Workaholic", "Take a Break", "Copycat", "Trap Bombing", "Buddy's Concern", "Friendship"
        )
        assertEquals(expected, palicoSkills.map { it.name })
    }
    @Test fun numericalEffectsIdentifyTheirRecipients() {
        val effects = palicoSkills.associate { it.id to it.effect }
        assertEquals("Temporarily increases the hunter's and Palico's attack by 10.", effects["demon_flute"])
        assertEquals("Increases the Palico's attack by 20.", effects["attack_large"])
        assertEquals("Multiplies the Palico's elemental attack by 1.3.", effects["element_attack"])
        assertEquals("Restores 50 health to the hunter and Palico.", effects["true_health_flute"])
        assertEquals("Prevents the Palico from flinching from most monster wind pressure.", effects["wind_immunity"])
        assertEquals("Prevents the Palico from being poisoned.", effects["poison_immunity"])
        assertTrue(effects.getValue("big_boomerang").contains("Palico's boomerangs"))
    }
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
