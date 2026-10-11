package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.EntityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldGuideSwipeLogicTest {
    @Test
    fun adjacentNavigationUsesSelectorOrderAndDoesNotWrap() {
        fieldGuideCategoryOrder.forEachIndexed { index, category ->
            val previous = if (index == 0) null else FieldGuideSectionDestination(fieldGuideCategoryOrder[index - 1])
            val next = if (index == fieldGuideCategoryOrder.lastIndex) null else FieldGuideSectionDestination(fieldGuideCategoryOrder[index + 1])
            assertEquals(previous, adjacentFieldGuideSection(category, -1))
            assertEquals(next, adjacentFieldGuideSection(category, 1))
        }
        assertNull(adjacentFieldGuideSection(null, 0))
    }

    @Test
    fun gestureRequiresThresholdAndHorizontalDominanceAndMapsNaturalDirection() {
        val thresholdPx = 56f
        assertNull(fieldGuideSwipeDirection(-55f, 0f, thresholdPx))
        assertNull(fieldGuideSwipeDirection(-120f, 90f, thresholdPx))
        assertEquals(1, fieldGuideSwipeDirection(-120f, 60f, thresholdPx))
        assertEquals(-1, fieldGuideSwipeDirection(120f, 60f, thresholdPx))
    }

    @Test
    fun settingAndCurrentWeaponScreenControlActivationScope() {
        assertFalse(fieldGuideSwipeAllowed(enabled = false, category = EntityType.MONSTER))
        assertTrue(fieldGuideSwipeAllowed(enabled = true, category = null))
        assertTrue(fieldGuideSwipeAllowed(enabled = true, category = EntityType.MAP))
        assertFalse(fieldGuideSwipeAllowed(enabled = true, category = EntityType.WEAPON))
    }

    @Test
    fun weaponCategoryUsesTheSameSelectionRuleAsTheTopChip() {
        var filtered: EntityType? = null
        var openedWeapons = 0
        selectFieldGuideCategory(
            category = EntityType.WEAPON,
            hasBlankQuery = true,
            onFilter = { filtered = it },
            onWeapons = { openedWeapons++ }
        )
        assertEquals(1, openedWeapons)
        assertNull(filtered)

        selectFieldGuideCategory(
            category = EntityType.WEAPON,
            hasBlankQuery = false,
            onFilter = { filtered = it },
            onWeapons = { openedWeapons++ }
        )
        assertEquals(EntityType.WEAPON, filtered)
        assertEquals(1, openedWeapons)
    }
}
