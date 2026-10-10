package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.SkillTree
import org.junit.Assert.assertEquals
import org.junit.Test

class CharmSkillPickerTest {
    private val skills = listOf(
        SkillTree("thunder", "Thunder Attack", "ThunderAtk", "雷属性攻撃", ""),
        SkillTree("attack", "Attack", "Attack", "攻撃", ""),
        SkillTree("expert", "Expert", "Expert", "達人", "")
    )

    @Test
    fun selectorSearchMatchesCanonicalTmoAndJapaneseNamesAndSortsByDisplayName() {
        assertEquals(listOf("attack", "thunder"), filterCharmSkills(skills, "ATTACK").map { it.stableSkillTreeId })
        assertEquals(listOf("thunder"), filterCharmSkills(skills, "thunderatk").map { it.stableSkillTreeId })
        assertEquals(listOf("thunder"), filterCharmSkills(skills, "雷属性攻撃").map { it.stableSkillTreeId })
        assertEquals(emptyList<String>(), filterCharmSkills(skills, "not a skill").map { it.stableSkillTreeId })
        assertEquals(listOf("attack", "expert", "thunder"), filterCharmSkills(skills, "").map { it.stableSkillTreeId })
    }
}
