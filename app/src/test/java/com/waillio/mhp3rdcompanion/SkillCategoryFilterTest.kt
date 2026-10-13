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
    fun noSelectionIsUnfilteredAndSelectedCategoriesUseOrSemantics() {
        val universal = skill("universal", "BLADEMASTER", "GUNNER")
        val blademaster = skill("blademaster", "BLADEMASTER")
        val defense = skill("defense", "DEFENSE")
        val gunner = skill("gunner", "GUNNER")
        val skills = listOf(universal, blademaster, defense, gunner)

        assertEquals(
            listOf(universal, blademaster, defense),
            filterSkillTreesByCategories(skills, setOf("BLADEMASTER", "DEFENSE"))
        )
        assertEquals(skills, filterSkillTreesByCategories(skills, emptySet()))
        assertEquals(listOf(universal, gunner), filterSkillTreesByCategories(skills, setOf("GUNNER")))
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

    @Test
    fun portraitFilterRowUsesHysteresisForHideAndReveal() {
        assertEquals(true, skillsFilterVisibilityAfterScroll(currentlyVisible = true, accumulatedDeltaPx = 55f, thresholdPx = 56f))
        assertEquals(false, skillsFilterVisibilityAfterScroll(currentlyVisible = true, accumulatedDeltaPx = 56f, thresholdPx = 56f))
        assertEquals(false, skillsFilterVisibilityAfterScroll(currentlyVisible = false, accumulatedDeltaPx = -40f, thresholdPx = 56f))
        assertEquals(true, skillsFilterVisibilityAfterScroll(currentlyVisible = false, accumulatedDeltaPx = -56f, thresholdPx = 56f))
    }

    @Test
    fun skillAlphabetUsesOnlyCurrentFilteredDisplayNamesAndJumpsToFirstMatch() {
        val skills = listOf(
            SkillTree("attack", "Attack", "Attack", "", "", categories = listOf("BLADEMASTER")),
            SkillTree("focus", "Focus", "Focus", "", "", categories = listOf("UTILITY")),
            SkillTree("guard", "Guard", "Guard", "", "", categories = listOf("DEFENSE")),
            SkillTree("gathering", "Gathering", "Gathering", "", "", categories = listOf("UTILITY"))
        ).sortedBy { it.displayName.lowercase() }

        val utilityOnly = filterSkillTreesByCategories(skills, setOf("UTILITY"))
        assertEquals(listOf('F', 'G'), skillInitialLetters(utilityOnly))
        assertEquals(0, firstSkillIndexForLetter(utilityOnly, 'F'))
        assertEquals(1, firstSkillIndexForLetter(utilityOnly, 'G'))

        val searchFiltered = utilityOnly.filter { it.displayName.contains("gather", ignoreCase = true) }
        assertEquals(listOf('G'), skillInitialLetters(searchFiltered))
        assertEquals(-1, firstSkillIndexForLetter(searchFiltered, 'F'))
    }
}
