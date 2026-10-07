package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class ItemIconTargetedCorrectionTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun correctedTargetsUseOnlyTheirExplicitAuthenticKeys() {
        mapOf(
            39 to "game:inv:meat:red", 40 to "game:inv:meat:purple", 41 to "game:inv:meat:yellow",
            42 to "game:inv:meat:cyan", 46 to "game:inv:meat:cyan", 47 to "game:inv:meat:red",
            91 to "game:inv:whetstone:yellow", 138 to "game:inv:bait:gray", 139 to "game:inv:bait:pink",
            672 to "game:inv:meat:orange", 713 to "game:inv:egg:white", 714 to "game:inv:egg:orange",
            43 to "game:inv:meat:orange", 44 to "game:inv:meat:orange", 45 to "game:inv:meat:gray",
            69 to "game:inv:spider_web:gray", 70 to "game:inv:spider_web:gray",
            76 to "game:inv:barrel:red", 691 to "game:inv:super_sprout:brown", 723 to "game:inv:question:gray"
        ).forEach { (id, key) ->
            assertEquals(key, ItemIconRegistry.resolve(id)?.iconKey)
            assertNotNull(ItemIconRegistry.resolve(id)?.resourceId)
        }
    }

    @Test
    fun nonTargetedAndExplicitFollowupTargetsStayStable() {
        assertEquals("atlas:PAL_2:80:16", ItemIconRegistry.resolve(715)?.iconKey)
        assertEquals("atlas:PAL_10:80:16", ItemIconRegistry.resolve(716)?.iconKey)
    }

    @Test
    fun productionJsonAndIconRegistryMatchTheCurrentSchema31Baseline() {
        val bytes = context.assets.open("mhp3rd-data.json").use { it.readBytes() }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02X".format(it) }
        assertEquals("657CFA9376FC8CBE56C67E67D4D1A27C455D21021F51E21B62427C8DDBB26155", digest)
        assertEquals(978, ItemIconRegistry.mappingCount)
        assertEquals(308, ItemIconRegistry.uniqueIconKeyCount)
        assertEquals(307, ItemIconRegistry.uniqueResourceCount)
    }
}
