package com.waillio.mhp3rdcompanion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.GeneratedDataset
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Regression coverage for the schema-27 structured Farm unlock projection. */
@RunWith(RobolectricTestRunner::class)
class FarmUnlockSemanticsSchema27Test {
    private val generated: GeneratedDataset = ApplicationProvider
        .getApplicationContext<Context>()
        .assets.open("mhp3rd-data.json")
        .bufferedReader()
        .use { Json { ignoreUnknownKeys = true }.decodeFromString(it.readText()) }

    @Test
    fun schema27CarriesAllSixteenAcceptedFarmUnlockRecords() {
        assertEquals(31, generated.schemaVersion)
        assertEquals(16, generated.farmFacilities.count { it.unlockAnyOf.isNotEmpty() })
        assertEquals(7, generated.farmFacilities.count { it.prerequisiteFacilityId != null })
    }

    @Test
    fun representativeRoutesPreserveAnyOfAndPrerequisiteSemantics() {
        val improved = generated.farmFacilities.single { it.id == "farm_beehive_improved" }
        assertEquals(2, improved.unlockAnyOf.size)
        assertTrue(improved.unlockAnyOf.any { it.contains("村長★5") })
        assertTrue(improved.unlockAnyOf.any { it.contains("集会浴場★5") })
        assertEquals("farm_beehive_standard", improved.prerequisiteFacilityId)

        val plus3 = generated.farmFacilities.single { it.id == "farm_mining_point_plus_3" }
        assertEquals(1, plus3.unlockAnyOf.size)
        assertEquals("farm_mining_point_plus_2", plus3.prerequisiteFacilityId)

        val roaster = generated.farmFacilities.single { it.id == "farm_custom_roaster" }
        assertEquals(3, roaster.unlockAnyOf.size)
        assertTrue(roaster.unlockAnyOf.any { it.contains("ダウンロード") })
    }
}
