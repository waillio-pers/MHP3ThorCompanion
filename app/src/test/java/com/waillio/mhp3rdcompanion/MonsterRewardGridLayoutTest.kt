package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.MonsterRewardFilter
import com.waillio.mhp3rdcompanion.data.RewardContext
import com.waillio.mhp3rdcompanion.data.itemFirstMonsterRewards
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MonsterRewardGridLayoutTest {
    private val monsters = CompanionRepository(ApplicationProvider.getApplicationContext()).data.monsters

    @Test
    fun allModeSpansTheLongAgnaktorRouteAndResumesTheAcceptedOrder() {
        val scale = monsters.single { it.name == "Agnaktor" }
            .rewards.itemFirstMonsterRewards(RewardContext.LOW)
            .single { it.itemName == "Agnaktor Scale" }
        val lines = packMonsterRewardRoutes(scale.routes) { it.label != "Front Legs/Hind Legs Break" }
        val longLineIndex = lines.indexOfFirst { it.routes.singleOrNull()?.label == "Front Legs/Hind Legs Break" }

        assertTrue("the real long route spans the full route grid", longLineIndex >= 0)
        assertTrue(lines[longLineIndex].spansBothColumns)
        assertTrue("short routes above it remain paired", lines.take(longLineIndex).any { it.routes.size == 2 })
        assertTrue("short routes after it resume as an aligned pair", lines.drop(longLineIndex + 1).any { it.routes.size == 2 })
        assertEquals(scale.routes, lines.flatMap { it.routes })
        assertEquals(
            scale.relations.map { requireNotNull(it.relationId) }.sorted(),
            lines.flatMap { it.routes }.flatMap { it.relations }.map { requireNotNull(it.relationId) }.sorted()
        )
    }

    @Test
    fun filteredPackingPairsOnlyConsecutiveSafeItemsAndNeverDropsRelations() {
        val captureItems = monsters.single { it.name == "Rathian" }
            .rewards.itemFirstMonsterRewards(RewardContext.HIGH, MonsterRewardFilter.CAPTURE)
        val lines = packFilteredMonsterRewardItems(captureItems) { item ->
            item.routes.size == 1 && item.itemName != "Inferno Sac"
        }

        assertTrue("Rathian HR Capture has at least one compact pair", lines.any { it.isTwoUp })
        assertTrue("the configured long item falls back to full width", lines.any { !it.isTwoUp && it.items.single().itemName == "Inferno Sac" })
        assertEquals(captureItems, lines.flatMap { it.items })
        assertEquals(
            captureItems.flatMap { it.relations }.map { requireNotNull(it.relationId) }.sorted(),
            lines.flatMap { it.items }.flatMap { it.relations }.map { requireNotNull(it.relationId) }.sorted()
        )
        assertTrue("each pair contains exactly two entries", lines.filter { it.isTwoUp }.all { it.items.size == 2 })
        assertFalse("full-width fallbacks never contain paired Items", lines.filterNot { it.isTwoUp }.any { it.items.size != 1 })
    }

    @Test
    fun filteredOddSingletonAndMultiRouteItemsSpanWithoutReordering() {
        val allItems = monsters.single { it.name == "Agnaktor" }
            .rewards.itemFirstMonsterRewards(RewardContext.LOW, MonsterRewardFilter.CARVE)
        val lines = packFilteredMonsterRewardItems(allItems) { it.routes.size == 1 }

        assertEquals(allItems, lines.flatMap { it.items })
        assertTrue("multi-route carve item uses a full-width fallback", lines.any { !it.isTwoUp && it.items.single().routes.size > 1 })
        val safeSingleton = allItems.first { it.routes.size == 1 }
        val orphan = packFilteredMonsterRewardItems(listOf(safeSingleton)) { true }.single()
        assertEquals("an unpaired safe Item gets a full-width row", listOf(safeSingleton), orphan.items)
        assertFalse(orphan.isTwoUp)
    }
}
