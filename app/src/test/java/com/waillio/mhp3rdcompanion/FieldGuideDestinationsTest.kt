package com.waillio.mhp3rdcompanion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.EntityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FieldGuideDestinationsTest {
    private val prefs by lazy { preferences() }

    @Before
    fun clearShortcutPreference() {
        prefs.edit().clear().commit()
    }

    @After
    fun clearShortcutPreferenceAfterTest() {
        prefs.edit().clear().commit()
    }

    @Test
    fun defaultMigrationMatchesTheExistingHomeShortcutsAndCharmFollowsQuests() {
        assertEquals(
            listOf("large_monsters", "items", "maps", "charm_tables"),
            readHomeShortcutSlots(prefs)
        )
        assertEquals(FieldGuideRootPage.QUESTS, fieldGuideCategoryOrder[fieldGuideCategoryOrder.indexOf(FieldGuideRootPage.CHARM_TABLES) - 1])
        assertFalse(fieldGuideShortcutChoices.contains(FieldGuideRootPage.ALL))
        assertEquals(fieldGuideCategoryOrder.filterNot { it == FieldGuideRootPage.ALL }, fieldGuideShortcutChoices)
        assertEquals(FieldGuideRootPage.WEAPONS, fieldGuidePageForCategory(EntityType.WEAPON))
    }

    @Test
    fun normalizationSupportsZeroThroughFourOrderedUniqueChoicesAndNoneSlots() {
        val choices = listOf("weapons", "items", "quests", "maps")
        for (count in 0..HOME_SHORTCUT_SLOT_COUNT) {
            val slots = normalizeHomeShortcutSlots(choices.take(count))
            assertEquals(HOME_SHORTCUT_SLOT_COUNT, slots.size)
            assertEquals(choices.take(count), slots.filterNotNull())
            assertEquals(count, slots.filterNotNull().toSet().size)
        }

        assertEquals(listOf("items", null, "quests", null), normalizeHomeShortcutSlots(listOf("items", null, "quests")))
        assertEquals(listOf("items", null, "quests", null), normalizeHomeShortcutSlots(listOf("items", "items", "quests", "all")))
        assertEquals(listOf("items", null, "quests", null), selectHomeShortcut(listOf("items", null, "quests", null), 1, "items"))
        assertEquals(listOf("items", null, "quests", null), selectHomeShortcut(listOf("items", null, "quests", null), 1, null))
        assertNotEquals("all", normalizeHomeShortcutSlots(listOf("all", null, null, null)).first())
    }

    @Test
    fun customShortcutOrderAndEmptySlotsPersistAcrossPreferenceRead() {
        val ordered = listOf("quests", null, "charm_tables", "weapons")
        persistHomeShortcutSlots(prefs, ordered)

        assertEquals("quests||charm_tables|weapons", prefs.getString(HOME_SHORTCUTS_KEY, null))
        assertEquals(ordered, readHomeShortcutSlots(prefs))
        prefs.edit().clear().commit()
        assertNull(prefs.getString(HOME_SHORTCUTS_KEY, null))
    }

    @Test
    fun unknownIdsAndDuplicateSelectionsAreSafelyRejected() {
        val normalized = normalizeHomeShortcutSlots(listOf("items", "not-a-category", "items", "quests"))
        assertEquals(listOf("items", null, null, "quests"), normalized)
        assertEquals(normalized, selectHomeShortcut(normalized, 3, "items"))
        assertTrue(fieldGuideShortcutChoices.all { it != FieldGuideRootPage.ALL })
    }

    private fun preferences() = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("home-shortcuts-test", Context.MODE_PRIVATE)
}
