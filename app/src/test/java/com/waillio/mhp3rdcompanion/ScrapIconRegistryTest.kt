package com.waillio.mhp3rdcompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Runtime guard for the source-locked MHP3 scrap icon family. */
class ScrapIconRegistryTest {
    private val expectedPalettes = mapOf(
        733 to "YELLOW", 734 to "YELLOW", 735 to "ORANGE", 736 to "GRAY", 737 to "GRAY",
        738 to "YELLOW", 739 to "YELLOW", 740 to "ORANGE", 741 to "ORANGE", 742 to "GRAY",
        743 to "GRAY", 744 to "GREEN", 745 to "GREEN", 746 to "YELLOW", 747 to "YELLOW",
        748 to "GRAY", 749 to "GRAY", 750 to "WHITE", 751 to "WHITE", 752 to "PURPLE",
        753 to "PURPLE", 754 to "ORANGE", 755 to "ORANGE", 756 to "CYAN", 757 to "CYAN",
        758 to "BLUE", 759 to "BLUE", 760 to "WHITE", 761 to "WHITE", 762 to "RED",
        763 to "RED", 764 to "LIME", 765 to "LIME", 766 to "YELLOW", 767 to "YELLOW",
        768 to "ORANGE", 769 to "ORANGE", 770 to "BROWN", 771 to "BROWN", 772 to "RED",
        773 to "RED", 774 to "GREEN", 775 to "GREEN", 776 to "RED", 777 to "RED",
        778 to "TEAL", 779 to "TEAL", 780 to "GRAY", 781 to "GRAY", 782 to "WHITE",
        783 to "WHITE", 784 to "YELLOW", 785 to "YELLOW", 786 to "GREEN", 787 to "GREEN",
        788 to "YELLOW", 789 to "YELLOW", 790 to "BROWN", 791 to "BROWN", 792 to "RED",
        793 to "RED", 794 to "BLUE", 795 to "BLUE", 796 to "GREENDARK", 797 to "GRAY",
        798 to "WHITE", 799 to "PURPLE", 800 to "WHITE", 801 to "ORANGE", 802 to "ORANGE",
    )

    @Test
    fun everyProductionScrapUsesExplicitCanonicalCellAndPalette() {
        assertEquals(70, expectedPalettes.size)
        expectedPalettes.forEach { (id, palette) ->
            val ref = ItemIconRegistry.resolve(id)
            assertNotNull("scrap $id must resolve", ref)
            assertEquals("atlas:INV_$palette:576:256", ref?.iconKey)
            assertNotNull("scrap $id must have a packaged resource", ref?.resourceId)
        }
    }

    @Test
    fun unresolvedAndCollateralBindingsRemainSafe() {
        assertNull(ItemIconRegistry.resolve(999_999))
        assertEquals("atlas:PAL_10:80:16", ItemIconRegistry.resolve(716)?.iconKey)
        assertTrue((733..802).all { ItemIconRegistry.productionGameItemIds.contains(it) })
        assertEquals(0, expectedPalettes.keys.count { ItemIconRegistry.resolve(it) == null })
    }

    @Test
    fun specialPaletteRecoveryIsExplicit() {
        assertEquals("atlas:INV_GREENDARK:576:256", ItemIconRegistry.resolve(796)?.iconKey)
        assertEquals("atlas:INV_ORANGE:576:256", ItemIconRegistry.resolve(801)?.iconKey)
        assertEquals("atlas:INV_ORANGE:576:256", ItemIconRegistry.resolve(802)?.iconKey)
    }
}
