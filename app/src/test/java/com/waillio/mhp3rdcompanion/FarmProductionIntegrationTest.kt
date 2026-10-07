package com.waillio.mhp3rdcompanion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.GeneratedDataset
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FarmProductionIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val generated: GeneratedDataset = context.assets.open("mhp3rd-data.json").bufferedReader().use {
        Json { ignoreUnknownKeys = true }.decodeFromString(it.readText())
    }
    private val data = CompanionRepository(context).data

    @Test
    fun productionFarmLayerHasAcceptedSchemaAndCensus() {
        assertEquals(31, generated.schemaVersion)
        assertEquals(28, generated.farmFacilities.size)
        assertEquals(38, generated.farmProfiles.size)
        assertEquals(595, generated.farmDrops.size)
        assertEquals(595, data.materials.flatMap { it.sources }.count { it.type == MaterialSourceType.FARM })
    }

    @Test
    fun acceptedMechanismsHaveExplicitFacilityBreakdown() {
        val counts = generated.farmDrops.groupingBy { it.facilityId }.eachCount()
        assertEquals(595, counts.values.sum())
        assertEquals(22, counts["farm_field_planted_item"])
        assertEquals(1, counts["farm_mushroom_tree_pickaxe_shroom"])
        assertEquals(62, counts["farm_mining_cart"])
        assertEquals(124, counts["farm_bug_tree_seesaw"])
        assertEquals(174, counts["farm_giant_fish_basket"])
        assertEquals(8, counts["farm_custom_roaster"])
    }

    @Test
    fun directPlantingAndPickaxeRowsDoNotClaimPercentages() {
        val direct = generated.farmDrops.filter { it.facilityId == "farm_field_planted_item" && it.gameItemId in setOf(151, 152, 154, 156, 160, 161, 171, 172, 173, 174, 178, 179) }
        assertEquals(12, direct.size)
        assertTrue(direct.all { it.probability.valuePercent == null && it.probability.semantics.name == "DETERMINISTIC_PER_HARVEST_SLOT" })
        assertTrue(direct.all { it.quantitySemantics == "PER_HARVEST_SLOT" })
        val pickaxe = generated.farmDrops.single { it.facilityId == "farm_mushroom_tree_pickaxe_shroom" }
        assertEquals(100, pickaxe.probability.valuePercent)
        assertEquals("FARM_OUTPUT", pickaxe.probability.semantics.name)
        assertEquals("Mining Cart unlocked", pickaxe.condition)
    }

    @Test
    fun profileRowsResolveAndUnknownTriggersStayExplicitlyUnknown() {
        assertEquals(34, generated.farmProfiles.count { !it.triggerKnown })
        assertEquals(4, generated.farmProfiles.count { it.triggerKnown })
        assertTrue(generated.farmProfiles.filter { !it.triggerKnown }.all { it.triggerDescription == null })
        assertTrue(generated.farmDrops.filter { it.profileId != null }.all { it.profileId in generated.farmProfiles.map { profile -> profile.id } })
        val roaster = generated.farmProfiles.filter { it.facilityId == "farm_custom_roaster" }
        assertTrue(roaster.all { it.triggerKnown && !it.triggerDescription.isNullOrBlank() })
    }

    @Test
    fun oldMaterialAndQuestDomainsRemainIntact() {
        assertEquals(978, generated.items.size)
        assertEquals(987, generated.weapons.size)
        assertEquals(354, generated.quests.size)
        assertEquals(4206, generated.questRewardDrops.size)
        assertEquals(40, generated.monsters.size)
        assertFalse(generated.monsters.any { it.threat != null })
    }
}
