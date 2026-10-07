package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import com.waillio.mhp3rdcompanion.data.RewardContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoyalLudrothBodyCarveRuntimeTest {
    private val repository = CompanionRepository(ApplicationProvider.getApplicationContext())

    @Test
    fun royalLudrothDetailExposesAllThreeBodyCarveContexts() {
        val rewards = repository.data.monsters.single { it.id == "monster_royal_ludroth" }
            .rewards.filter { it.sourceType == "Body Carve" }
        assertEquals(15, rewards.size)
        assertEquals(mapOf(RewardContext.GUILD_1_2 to 5, RewardContext.LOW to 5, RewardContext.HIGH to 5), rewards.groupingBy { it.rank }.eachCount())
        assertTrue(rewards.all { it.condition == "Body" && it.quantity == 1 })
    }

    @Test
    fun affectedItemReverseProjectionIncludesRoyalLudrothBodyCarve() {
        val scale = repository.data.materials.single { it.gameItemId == 359 }
        val sources = scale.sources.filter {
            it.type == MaterialSourceType.MONSTER_REWARD &&
                it.monsterId == "monster_royal_ludroth" &&
                it.method == "BODY_CARVE"
        }
        assertEquals(setOf(RewardContext.GUILD_1_2, RewardContext.LOW), sources.mapNotNull { it.rank }.toSet())
        assertTrue(sources.all { it.condition == "Body" && it.quantity == 1 })
    }
}
