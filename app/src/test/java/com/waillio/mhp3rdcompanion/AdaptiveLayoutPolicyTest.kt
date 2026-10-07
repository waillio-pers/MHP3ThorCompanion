package com.waillio.mhp3rdcompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutPolicyTest {
    @Test
    fun classifiesTheFivePublicWindowTargetsAndKeepsThorMedium() {
        assertEquals(AppLayoutProfile.COMPACT_PORTRAIT, appWindowLayout(360, 800).profile)
        assertEquals(AppLayoutProfile.COMPACT_PORTRAIT, appWindowLayout(412, 915).profile)
        assertEquals(AppLayoutProfile.COMPACT_LANDSCAPE, appWindowLayout(800, 360).profile)
        assertEquals(AppLayoutProfile.MEDIUM, appWindowLayout(827, 720).profile)
        assertEquals(AppLayoutProfile.EXPANDED, appWindowLayout(1280, 800).profile)
    }

    @Test
    fun liveWindowResizeAndRotationChangeTheProfileWithoutDeviceIdentity() {
        val portrait = appWindowLayout(360, 800)
        val landscape = appWindowLayout(800, 360)
        val portraitAgain = appWindowLayout(360, 800)

        assertNotEquals(portrait.profile, landscape.profile)
        assertEquals(portrait, portraitAgain)
    }

    @Test
    fun responsiveCardsAndWeaponChooserRespectWindowWidthAndThorGeometry() {
        assertEquals(1, responsiveGridColumns(290, AppLayoutProfile.COMPACT_PORTRAIT, 340))
        assertEquals(2, responsiveGridColumns(757, AppLayoutProfile.MEDIUM, 340))
        assertEquals(3, responsiveGridColumns(1210, AppLayoutProfile.EXPANDED, 340))

        assertEquals(2, weaponChooserColumns(332, AppLayoutProfile.COMPACT_PORTRAIT))
        assertEquals(3, weaponChooserColumns(799, AppLayoutProfile.MEDIUM))
        assertEquals(4, weaponChooserColumns(1252, AppLayoutProfile.EXPANDED))

        assertTrue(monsterRewardRowsStacked(AppLayoutProfile.COMPACT_PORTRAIT))
        assertTrue(!monsterRewardRowsStacked(AppLayoutProfile.COMPACT_LANDSCAPE))
        assertTrue(!monsterRewardRowsStacked(AppLayoutProfile.MEDIUM))

        assertTrue(weaponRowsStacked(360))
        assertTrue(weaponRowsStacked(380))
        assertTrue(!weaponRowsStacked(381))
    }

    @Test
    fun uiScaleIsNotAnInputToWindowProfileClassification() {
        val supportedScaleValues = listOf(.80f, 1.00f, 1.10f)
        val profiles = supportedScaleValues.map { _ -> appWindowLayout(827, 720).profile }
        assertTrue(profiles.all { it == AppLayoutProfile.MEDIUM })
    }
}
