package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.GeneratedDataset
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ItemIconRegistryTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun productionMaterialsAndHuntPrepCatalogResolveByGameItemId() {
        val json = context.assets.open("mhp3rd-data.json").bufferedReader().use { it.readText() }
        val generated = Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(json)
        val materialIds = generated.items.map { requireNotNull(it.gameItemId) }.toSet()
        val huntPrepIds = generated.huntPrepItemCatalog.map { it.gameItemId }.toSet()

        assertEquals(978, materialIds.size)
        assertEquals(17, huntPrepIds.size)
        assertEquals(978, materialIds.count { ItemIconRegistry.resolve(it) != null })
        assertTrue(huntPrepIds.all { ItemIconRegistry.resolve(it) != null })
        assertTrue(ItemIconRegistry.productionGameItemIds.all { it in materialIds })
        assertEquals(978, ItemIconRegistry.mappingCount)
        assertEquals(308, ItemIconRegistry.uniqueIconKeyCount)
        assertEquals(307, ItemIconRegistry.uniqueResourceCount)
        assertEquals(294, ItemIconRegistry.productionGameItemIds.mapNotNull { ItemIconRegistry.resolve(it)?.resourceId }.toSet().size)

        assertEquals("atlas:PAL_6:128:0", ItemIconRegistry.resolve(173)?.iconKey)
        assertEquals("atlas:PAL_12:128:0", ItemIconRegistry.resolve(174)?.iconKey)
        assertEquals("atlas:PAL_6:96:16", ItemIconRegistry.resolve(719)?.iconKey)
        assertEquals(ItemIconRegistry.resolve(719)?.resourceId, ItemIconRegistry.resolve(377)?.resourceId)
        assertEquals("atlas:PAL_12:16:16", ItemIconRegistry.resolve(200)?.iconKey)
        assertEquals("atlas:PAL_12:48:64", ItemIconRegistry.resolve(619)?.iconKey)
        assertEquals("atlas:PAL_2:96:48", ItemIconRegistry.resolve(623)?.iconKey)
        assertEquals("atlas:PAL_9:96:48", ItemIconRegistry.resolve(624)?.iconKey)
        assertEquals("atlas:PAL_10:96:48", ItemIconRegistry.resolve(638)?.iconKey)
        assertEquals("atlas:PAL_0:144:32", ItemIconRegistry.resolve(31)?.iconKey)
        assertEquals("atlas:PAL_12:96:48", ItemIconRegistry.resolve(611)?.iconKey)
        assertEquals("atlas:PAL_2:48:64", ItemIconRegistry.resolve(617)?.iconKey)
        assertEquals("atlas:PAL_9:96:48", ItemIconRegistry.resolve(621)?.iconKey)
        assertEquals("atlas:PAL_9:96:48", ItemIconRegistry.resolve(628)?.iconKey)
        assertEquals("atlas:PAL_12:96:48", ItemIconRegistry.resolve(632)?.iconKey)
        assertEquals("atlas:PAL_2:96:48", ItemIconRegistry.resolve(610)?.iconKey)
        assertEquals("atlas:PAL_5:48:64", ItemIconRegistry.resolve(618)?.iconKey)
        assertEquals("game:inv:meat:orange", ItemIconRegistry.resolve(43)?.iconKey)
        assertEquals("game:inv:meat:orange", ItemIconRegistry.resolve(44)?.iconKey)
        assertEquals("game:inv:meat:gray", ItemIconRegistry.resolve(45)?.iconKey)
        assertEquals("game:inv:spider_web:gray", ItemIconRegistry.resolve(69)?.iconKey)
        assertEquals("game:inv:spider_web:gray", ItemIconRegistry.resolve(70)?.iconKey)
        assertEquals("game:inv:barrel:red", ItemIconRegistry.resolve(76)?.iconKey)
        assertEquals("game:inv:super_sprout:brown", ItemIconRegistry.resolve(691)?.iconKey)
        assertEquals("game:inv:question:gray", ItemIconRegistry.resolve(723)?.iconKey)
        assertEquals("atlas:INV_GRAY:448:192", ItemIconRegistry.resolve(53)?.iconKey)
        assertEquals(ItemIconRegistry.resolve(53), ItemIconRegistry.resolve(54))
        assertEquals(ItemIconRegistry.resolve(53), ItemIconRegistry.resolve(55))
        assertEquals(ItemIconRegistry.resolve(53), ItemIconRegistry.resolve(56))
        assertEquals(ItemIconRegistry.resolve(53), ItemIconRegistry.resolve(57))
    }

    @Test
    fun exactAuditAnchorsAndIntentionalReuseArePreserved() {
        assertEquals("atlas:PAL_2:144:0", ItemIconRegistry.resolve(233)?.iconKey)
        assertEquals("atlas:PAL_13:144:0", ItemIconRegistry.resolve(218)?.iconKey)
        assertEquals("atlas:PAL_3:64:16", ItemIconRegistry.resolve(242)?.iconKey)
        assertEquals("atlas:PAL_12:32:32", ItemIconRegistry.resolve(12)?.iconKey)

        assertEquals(ItemIconRegistry.resolve(21), ItemIconRegistry.resolve(22))
        assertEquals(ItemIconRegistry.resolve(731), ItemIconRegistry.resolve(732))
        assertEquals(ItemIconRegistry.resolve(729), ItemIconRegistry.resolve(730))
        assertNotNull(ItemIconRegistry.resolve(377))
        assertNull(ItemIconRegistry.resolve(Int.MAX_VALUE))
    }

    @Test
    fun repositoryCarriesNumericIdentityIntoEveryIconConsumer() {
        val data = CompanionRepository(context).data
        assertTrue(data.materials.all { it.gameItemId != null })
        assertTrue(data.monsters.flatMap { it.rewards }.all { it.gameItemId != null })
        assertTrue(data.monsters.flatMap { it.huntPrep?.counterItems.orEmpty() }.all { ItemIconRegistry.resolve(it.itemGameId) != null })
        assertTrue(data.monsters.flatMap { it.huntPrep?.tacticalTools.orEmpty() }.all { ItemIconRegistry.resolve(it.itemGameId) != null })
    }

    @Test
    fun throwingKnifeCombinationFamilyUsesTheSinglePublishedKnifeCell() {
        val data = CompanionRepository(context).data
        val knifeRelations = data.itemCombinationRecipes.filter {
            it.ingredientAGameItemId == 53 || it.ingredientBGameItemId == 53
        }
        assertEquals(setOf(54, 55, 56, 57), knifeRelations.map { it.outputGameItemId }.toSet())
        assertTrue((53..57).all { ItemIconRegistry.resolve(it)?.iconKey == "atlas:INV_GRAY:448:192" })
        assertEquals(ItemIconRegistry.resolve(53), ItemIconRegistry.resolve(54))
        assertEquals(ItemIconRegistry.resolve(53), ItemIconRegistry.resolve(55))
        assertEquals(ItemIconRegistry.resolve(53), ItemIconRegistry.resolve(56))
        assertEquals(ItemIconRegistry.resolve(53), ItemIconRegistry.resolve(57))
    }
}
