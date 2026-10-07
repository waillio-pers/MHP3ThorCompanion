package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThorHardeningBIconRecoveryTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun reportedItemsRetainExactIdentityAndUseOnlyUserAdjudicatedMappings() {
        val reportedExactItems = linkedMapOf(
            "Anti-dragon Bomb" to 682,
            "Antidote Horn" to 147,
            "Iron Pickaxe" to 85,
            "Mega Pickaxe" to 86,
            "Old Pickaxe" to 84,
            "Armor Horn" to 149,
            "Barrel Bomb S" to 77,
            "BBQ Spit" to 90,
            "Binoculars" to 143,
            "Boomerang" to 52,
            "Bounce Bomb" to 80,
            "Bountiful Bait" to 142,
            "Bug Net" to 88,
            "Burst Bait" to 141,
            "Confidential Box" to 640,
            "Demon Horn" to 148,
            "Empty Phial" to 130,
            "EZ Barrel Bomb L" to 681,
            "EZ Pitfall Trap" to 680,
            "EZ Sonic Bomb" to 678,
            "Famitsu Coin" to 636,
            "Farcaster" to 67,
            "Felvine Bomb" to 81,
            "Field Horn" to 145,
            "Garbage" to 144,
            "Golden Egg" to 604,
            "Goldenfish Bait" to 140,
            "Health Horn" to 146,
            "JUMP Barrel Bomb" to 82,
            "Large Fragment" to 599,
            "Long Fragment" to 601,
            "Map" to 670,
            "Mega Bug Net" to 89,
            "Mini Whetstone" to 674,
            "Old Bug Net" to 87,
            "Pirate J Dubloon" to 631,
            "Polytan Bomb" to 83,
            "Portable Spit" to 673,
            "Rusted Fragment" to 598,
            "Shock Trap" to 73,
            "Silver Egg" to 603,
            "Slender Fragment" to 600,
            "Smoke Bomb" to 65,
            "Steel Egg" to 602,
            "Trap Tool" to 71,
            "Yukumo Egg" to 51
        )
        val data = CompanionRepository(context).data

        assertEquals(46, reportedExactItems.size)
        reportedExactItems.forEach { (canonicalName, expectedGameItemId) ->
            val item = data.materials.singleOrNull { it.name == canonicalName }
            assertEquals("Exact production identity for $canonicalName", expectedGameItemId, item?.gameItemId)
            assertTrue("$canonicalName must resolve after the user's atlas adjudication", ItemIconRegistry.resolve(expectedGameItemId) != null)
            assertEquals(
                ItemIconRegistry.USER_ADJUDICATED_PHYSICAL_GAME_REFERENCE,
                ItemIconRegistry.provenanceFor(expectedGameItemId)
            )
        }

        val finalAdjudicatedItem = data.materials.single { it.gameItemId == 66 }
        assertEquals("Poison Smoke Bmb", finalAdjudicatedItem.name)
        assertTrue(ItemIconRegistry.resolve(66) != null)
        assertEquals(ItemIconRegistry.USER_ADJUDICATED_PHYSICAL_GAME_REFERENCE, ItemIconRegistry.provenanceFor(66))
        assertEquals(978, ItemIconRegistry.mappingCount)
        assertEquals(308, ItemIconRegistry.uniqueIconKeyCount)
        assertEquals(47, reportedExactItems.size + 1)
    }
}
