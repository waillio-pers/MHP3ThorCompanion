package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.AllSkillCategoryIds
import com.waillio.mhp3rdcompanion.data.SkillTree
import com.waillio.mhp3rdcompanion.data.filterSkillTreesByCategories
import org.junit.Assert.assertEquals
import org.junit.Test

class SkillCategoryFilterTest {
    private fun skill(id: String, vararg categories: String) =
        SkillTree(id, id, id, "", "", categories = categories.toList())

    @Test
    fun selectedCategoriesUseOrSemantics() {
        val universal = skill("universal", "BLADEMASTER", "GUNNER")
        val blademaster = skill("blademaster", "BLADEMASTER")
        val defense = skill("defense", "DEFENSE")
        val gunner = skill("gunner", "GUNNER")
        val skills = listOf(universal, blademaster, defense, gunner)

        assertEquals(
            listOf(universal, blademaster, defense),
            filterSkillTreesByCategories(skills, setOf("BLADEMASTER", "DEFENSE"))
        )
        assertEquals(emptyList<SkillTree>(), filterSkillTreesByCategories(skills, emptySet()))
    }

    @Test
    fun allCategoriesAreExactlyEquivalentToNoRestriction() {
        val skills = listOf(
            skill("one", "BLADEMASTER"),
            skill("two", "GUNNER", "UTILITY"),
            skill("three", "DEFENSE")
        )

        assertEquals(skills, filterSkillTreesByCategories(skills, AllSkillCategoryIds))
    }
}
