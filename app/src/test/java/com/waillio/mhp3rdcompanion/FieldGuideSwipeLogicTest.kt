package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.EntityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldGuideSwipeLogicTest {
    @Test
    fun pagerOrderIsCanonicalAndCharmTablesFollowsQuests() {
        assertEquals(
            listOf(
                FieldGuideRootPage.ALL,
                FieldGuideRootPage.LARGE_MONSTERS,
                FieldGuideRootPage.SMALL_MONSTERS,
                FieldGuideRootPage.ITEMS,
                FieldGuideRootPage.MAPS,
                FieldGuideRootPage.WEAPONS,
                FieldGuideRootPage.SKILLS,
                FieldGuideRootPage.QUESTS,
                FieldGuideRootPage.CHARM_TABLES,
                FieldGuideRootPage.TRAINING
            ),
            fieldGuideCategoryOrder
        )
        assertEquals(
            fieldGuideCategoryOrder.indexOf(FieldGuideRootPage.QUESTS) + 1,
            fieldGuideCategoryOrder.indexOf(FieldGuideRootPage.CHARM_TABLES)
        )
        assertFalse(fieldGuideShortcutChoices.contains(FieldGuideRootPage.ALL))
    }

    @Test
    fun adjacentPagesUseCanonicalOrderAndNeverWrap() {
        fieldGuideCategoryOrder.forEachIndexed { index, page ->
            val previous = if (index == 0) null else FieldGuideSectionDestination(fieldGuideCategoryOrder[index - 1])
            val next = if (index == fieldGuideCategoryOrder.lastIndex) null else FieldGuideSectionDestination(fieldGuideCategoryOrder[index + 1])
            assertEquals(previous, adjacentFieldGuideSection(page, -1))
            assertEquals(next, adjacentFieldGuideSection(page, 1))
        }
        assertNull(adjacentFieldGuideSection(FieldGuideRootPage.ALL, 0))
    }

    @Test
    fun pageSwipeSettingAndDeeperGestureOwnersControlPager() {
        assertFalse(fieldGuideSwipeAllowed(enabled = false, page = FieldGuideRootPage.LARGE_MONSTERS))
        assertTrue(fieldGuideSwipeAllowed(enabled = true, page = FieldGuideRootPage.ALL))
        assertTrue(fieldGuideSwipeAllowed(enabled = true, page = FieldGuideRootPage.WEAPONS))
        assertTrue(fieldGuideSwipeAllowed(enabled = true, page = FieldGuideRootPage.MAPS))
        assertFalse(fieldGuideSwipeAllowed(enabled = true, page = FieldGuideRootPage.WEAPONS, childOwnsHorizontalGesture = true))
    }

    @Test
    fun rootWeaponSelectionStillOpensTheWeaponBrowser() {
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
