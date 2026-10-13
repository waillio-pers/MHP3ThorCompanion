package com.waillio.mhp3rdcompanion

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.TalismanClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CharmRarityIconsTest {
    @Test
    fun rarityMappingsUseTransparentSuppliedIcons() {
        assertEquals(R.drawable.charm_rarity_queen, charmRarityIconResource(TalismanClass.QUEEN))
        assertEquals(R.drawable.charm_rarity_king, charmRarityIconResource(TalismanClass.KING))
        assertEquals(R.drawable.charm_rarity_dragon, charmRarityIconResource(TalismanClass.DRAGON))

        val resources = ApplicationProvider.getApplicationContext<Context>().resources
        val expected = listOf(
            Triple(TalismanClass.QUEEN, 59, 64),
            Triple(TalismanClass.KING, 60, 62),
            Triple(TalismanClass.DRAGON, 59, 61)
        )
        expected.forEach { (rarity, width, height) ->
            val bitmap = BitmapFactory.decodeResource(resources, charmRarityIconResource(rarity))
            assertNotNull("${rarity.label} resource should decode", bitmap)
            assertEquals(width, bitmap.width)
            assertEquals(height, bitmap.height)
            assertEquals("${rarity.label} icon corner must remain transparent", 0, bitmap.getPixel(0, 0) ushr 24)
        }
    }
}
