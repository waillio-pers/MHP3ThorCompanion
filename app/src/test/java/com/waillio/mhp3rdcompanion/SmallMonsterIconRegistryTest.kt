package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SmallMonsterIconRegistryTest {
    private val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data

    @Test fun allProductionSmallMonstersResolveExplicitAuthenticIcons() {
        val productionIds = data.smallMonsters.map { it.id }.toSet()
        assertEquals(20, productionIds.size)
        assertEquals(productionIds, SmallMonsterIconRegistry.mappedSmallMonsterIds)
        assertTrue(productionIds.all { SmallMonsterIconRegistry.resolve(it) is SmallMonsterIcon.Resource })
        assertSame(SmallMonsterIcon.Placeholder, SmallMonsterIconRegistry.resolve("small_monster_synthetic"))
    }

    @Test fun sourceFilenameExceptionsDoNotChangeCanonicalVisibleNames() {
        val names = data.smallMonsters.associate { it.id to it.name }
        assertSource("small_monster_aptonoth", "Aptanoth.png", "Aptonoth", names)
        assertSource("small_monster_gargwa", "Gagua.png", "Gargwa", names)
        assertSource("small_monster_wroggi", "Froggi.png", "Wroggi", names)
        assertSource("small_monster_slagtoth", "Zuwaroposu.png", "Slagtoth", names)
    }

    private fun assertSource(id: String, source: String, visible: String, names: Map<String, String>) {
        assertEquals(source, (SmallMonsterIconRegistry.resolve(id) as SmallMonsterIcon.Resource).sourceFileName)
        assertEquals(visible, names.getValue(id))
    }
}
