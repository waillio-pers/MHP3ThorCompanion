package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.ItemUsageFamily
import com.waillio.mhp3rdcompanion.data.ItemUsageIndex
import com.waillio.mhp3rdcompanion.data.UsageTargetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ItemUsageProjectionTest {
    private val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data
    private val index = ItemUsageIndex.build(data)

    @Test
    fun acceptedUsageUniverseAndFamilyCountsAreStable() {
        assertEquals(5125, index.allRelations.size)
        assertEquals(3834, index.allRelations.count { it.family == ItemUsageFamily.WEAPONS })
        assertEquals(514, index.allRelations.count { it.family == ItemUsageFamily.DECORATIONS })
        assertEquals(234, index.allRelations.count { it.family == ItemUsageFamily.COMBINATIONS })
        assertEquals(50, index.allRelations.count { it.family == ItemUsageFamily.FARM })
        assertEquals(116, index.allRelations.count { it.family == ItemUsageFamily.TRADING })
        assertEquals(30, index.allRelations.count { it.family == ItemUsageFamily.ROASTING })
        assertEquals(328, index.allRelations.count { it.family == ItemUsageFamily.SCRAP_CONVERSION })
        assertEquals(19, index.allRelations.count { it.family == ItemUsageFamily.QUEST_DELIVERY })
        assertEquals(548, data.materials.mapNotNull { it.gameItemId }.count { index.forItem(it) != null })
    }

    @Test
    fun realRegressionItemsHaveExpectedFamilyAndTargetProjections() {
        val iron = index.forItem(218)!!
        assertEquals(5, iron.familyCount)
        assertEquals(78, iron.totalTargetCount)
        assertEquals(73, iron.families.first { it.family == ItemUsageFamily.WEAPONS }.targetCount)

        val sunspire = index.forItem(812)!!
        assertEquals(1, sunspire.familyCount)
        assertEquals(75, sunspire.totalTargetCount)

        val ancientPotion = index.forItem(30)!!
        val potionDecoration = ancientPotion.families.single { it.family == ItemUsageFamily.DECORATIONS }
        val medicine = potionDecoration.targets.single()
        assertEquals("item_medicine_jewel_2", medicine.targetId)
        assertTrue(medicine.relations.any { it.rank == "HIGH" && it.quantity == 1 })

        val huskberry = index.forItem(92)!!
        assertEquals(22, huskberry.families.single { it.family == ItemUsageFamily.COMBINATIONS }.targetCount)
        val farm = huskberry.families.single { it.family == ItemUsageFamily.FARM }
        assertEquals(4, farm.targets.single { it.targetGameItemId == 92 }.relations.size)
        assertTrue(farm.targets.single { it.targetGameItemId == 92 }.relations.all { it.outputQuantity != null })

        val pawPass = index.forItem(675)!!
        assertEquals(12, pawPass.families.single { it.family == ItemUsageFamily.QUEST_DELIVERY }.targetCount)

        val selfFarmTarget = farm.targets.single { it.targetGameItemId == 92 }
        assertEquals(UsageTargetKind.ITEM, selfFarmTarget.targetKind)
    }
}
