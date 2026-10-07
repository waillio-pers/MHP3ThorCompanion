package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MonsterArtworkRegistryTest {
    private val productionMonsterIds = CompanionRepository(
        ApplicationProvider.getApplicationContext()
    ).data.monsters.mapTo(linkedSetOf()) { it.id }

    @Test
    fun knownMonsterIdsResolveToTheirPackagedArtwork() {
        assertEquals(
            MonsterArtwork.Resource(R.drawable.monster_alatreon),
            MonsterArtworkRegistry.resolve("monster_alatreon")
        )
        assertEquals(
            MonsterArtwork.Resource(R.drawable.monster_nargacuga),
            MonsterArtworkRegistry.resolve("monster_nargacuga")
        )
        assertEquals(
            MonsterArtwork.Resource(R.drawable.monster_zinogre),
            MonsterArtworkRegistry.resolve("monster_zinogre")
        )
    }

    @Test
    fun registryExactlyMatchesProductionMonsterIds() {
        assertEquals(40, productionMonsterIds.size)
        assertEquals(productionMonsterIds, MonsterArtworkRegistry.mappedMonsterIds)
    }

    @Test
    fun productionSetHasNoFallbackArtwork() {
        assertEquals(
            0,
            productionMonsterIds.count {
                MonsterArtworkRegistry.resolve(it) === MonsterArtwork.Placeholder
            }
        )
    }

    @Test
    fun unknownSyntheticIdResolvesToSharedPlaceholder() {
        assertSame(MonsterArtwork.Placeholder, MonsterArtworkRegistry.resolve("monster_missing"))
    }
}
