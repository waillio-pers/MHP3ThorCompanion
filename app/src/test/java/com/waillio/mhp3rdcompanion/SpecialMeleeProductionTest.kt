package com.waillio.mhp3rdcompanion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.GeneratedDataset
import com.waillio.mhp3rdcompanion.data.BowMechanics
import com.waillio.mhp3rdcompanion.data.GunlanceMechanics
import com.waillio.mhp3rdcompanion.data.HuntingHornMechanics
import com.waillio.mhp3rdcompanion.data.SwitchAxeMechanics
import com.waillio.mhp3rdcompanion.data.LightBowgunMechanics
import com.waillio.mhp3rdcompanion.data.HeavyBowgunMechanics
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpecialMeleeProductionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val data = CompanionRepository(context).data

    @Test
    fun schema11PromotesAllRangedAndTypedMechanics() {
        assertEquals(987, data.weapons.size)
        assertEquals(mapOf("GREAT_SWORD" to 91, "LONG_SWORD" to 85, "SWORD_AND_SHIELD" to 94, "DUAL_BLADES" to 82, "HAMMER" to 93, "LANCE" to 96, "HUNTING_HORN" to 85, "GUNLANCE" to 77, "SWITCH_AXE" to 71, "BOW" to 80, "LIGHT_BOWGUN" to 67, "HEAVY_BOWGUN" to 66), data.weapons.groupingBy { it.weaponType }.eachCount())
        assertTrue(data.weapons.take(541).all { it.mechanics == null })
        assertEquals(85, data.weapons.count { it.mechanics is HuntingHornMechanics })
        assertEquals(77, data.weapons.count { it.mechanics is GunlanceMechanics })
        assertEquals(71, data.weapons.count { it.mechanics is SwitchAxeMechanics })
        assertEquals(80, data.weapons.count { it.mechanics is BowMechanics })
        assertEquals(67, data.weapons.count { it.mechanics is LightBowgunMechanics })
        assertEquals(66, data.weapons.count { it.mechanics is HeavyBowgunMechanics })
        assertTrue(data.weapons.map { it.id }.distinct().size == 987)
    }

    @Test
    fun huntingHornSongCatalogResolvesEveryHornWithoutEncore() {
        val horns = data.weapons.filter { it.weaponType == "HUNTING_HORN" }
        assertEquals(25, data.huntingHornSongCatalog.size)
        assertEquals(85, horns.count { data.huntingHornSongCatalog.any { set -> set.noteSetKey == (it.mechanics as HuntingHornMechanics).noteSetKey } })
        assertEquals(139, data.huntingHornSongCatalog.sumOf { it.songs.size })
        assertEquals(49, data.huntingHornSongCatalog.flatMap { it.songs }.map { it.effectName }.distinct().size)
    }

    @Test
    fun generatedSchema11ModelsDecodeLegacyNullMechanicsAndTypedPayloads() {
        val generated = context.assets.open("mhp3rd-data.json").bufferedReader().use { Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(it.readText()) }
        assertEquals(31, generated.schemaVersion)
        assertNull(generated.weapons.first().mechanics)
        assertNotNull(generated.weapons[541].mechanics)
        assertEquals(85, generated.weapons.count { it.mechanics?.kind?.name == "HUNTING_HORN" })
        assertEquals(80, generated.weapons.count { it.mechanics?.kind?.name == "BOW" })
        assertEquals(67, generated.weapons.count { it.mechanics?.kind?.name == "LIGHT_BOWGUN" })
        assertEquals(66, generated.weapons.count { it.mechanics?.kind?.name == "HEAVY_BOWGUN" })
        assertTrue(generated.weapons.filter { it.mechanics?.kind?.name == "BOW" }.all { it.sharpness == null })
    }
}
