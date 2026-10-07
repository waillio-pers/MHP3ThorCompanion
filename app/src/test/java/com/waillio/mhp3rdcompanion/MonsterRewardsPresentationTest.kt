package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MonsterRewardsPresentationTest {
    private val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data

    private data class SimpleFilteredDemo(
        val monster: Monster,
        val rank: RewardContext,
        val filter: MonsterRewardFilter,
        val itemCount: Int
    )

    @Test
    fun productionItemFirstListIsLosslessForEveryMonsterAndRank() {
        assertEquals(1535, data.monsters.sumOf { it.rewards.size })
        assertEquals(40, data.monsters.count { it.rewards.isNotEmpty() })
        data.monsters.forEach { monster ->
            monster.rewards.availableContexts().forEach { rank ->
                val source = monster.rewards.filter { it.rank == rank }
                val projected = monster.rewards.itemFirstMonsterRewards(rank)
                val outputRelations = projected.flatMap { it.relations }
                assertEquals("${monster.id}/$rank relation count", source.size, outputRelations.size)
                assertEquals("${monster.id}/$rank relation identity", source.map { requireNotNull(it.relationId) }.sorted(), outputRelations.map { requireNotNull(it.relationId) }.sorted())
                assertTrue("${monster.id}/$rank Items must be grouped by numeric ID", projected.all { it.gameItemId > 0 })
                assertEquals("${monster.id}/$rank each Item once", projected.size, projected.map { it.gameItemId }.toSet().size)
                projected.forEach { item ->
                    assertEquals(item.itemName, item.relations.first().item)
                    assertTrue(item.routes.all { route -> route.relations.all { it.gameItemId == item.gameItemId } })
                    assertEquals(item.relations.size, item.routes.sumOf { it.relations.size })
                }
            }
        }
    }

    @Test
    fun methodFiltersAreAnExactDisjointPartitionAndCarveIncludesTailCarve() {
        val filters = MonsterRewardFilter.entries.filter { it != MonsterRewardFilter.ALL }
        data.monsters.forEach { monster ->
            monster.rewards.availableContexts().forEach { rank ->
                val allIds = monster.rewards.itemFirstMonsterRewards(rank).flatMap { it.relations }.map { requireNotNull(it.relationId) }
                val filtered = filters.flatMap { filter ->
                    monster.rewards.itemFirstMonsterRewards(rank, filter).flatMap { it.relations }
                }
                assertEquals("${monster.id}/$rank filter union", allIds.sorted(), filtered.map { requireNotNull(it.relationId) }.sorted())
                assertEquals("${monster.id}/$rank filters must not overlap", filtered.size, filtered.map { requireNotNull(it.relationId) }.toSet().size)
            }
        }
        val allCarves = data.monsters.flatMap { it.rewards }.filter { it.routeFamily() == MonsterRewardRouteFamily.CARVE }
        assertTrue(allCarves.any { it.sourceType == "Body Carve" })
        assertTrue(allCarves.any { it.sourceType == "Tail Carve" })
        assertTrue(data.monsters.flatMap { it.rewards }.any { it.routeFamily() == MonsterRewardRouteFamily.MINING })
    }

    @Test
    fun itemLocalRoutesRetainExactRelationsAndTheAcceptedRouteLabels() {
        val duplicateRouteCount = data.monsters.sumOf { monster ->
            monster.rewards.availableContexts().sumOf { rank ->
                monster.rewards.itemFirstMonsterRewards(rank).sumOf { item -> item.routes.count { it.relations.size > 1 } }
            }
        }
        assertEquals(21, duplicateRouteCount)

        val agnaktor = data.monsters.single { it.id == "monster_agnaktor" }
        val high = agnaktor.rewards.itemFirstMonsterRewards(RewardContext.HIGH)
        assertEquals(8, high.flatMap { item -> item.routes.map { it.label } }.distinct().size)
        assertEquals(agnaktor.rewards.count { it.rank == RewardContext.HIGH }, high.sumOf { it.relations.size })
        val claws = high.single { it.itemName == "Agnaktor Claw+" }
        val breakCell = claws.routes.single { it.label == "Front Legs/Hind Legs Break" }
        assertEquals(listOf("34%", "18%"), breakCell.relations.map { it.chance })
        assertEquals(listOf(1, 2), breakCell.relations.map { it.quantity })
        assertTrue(breakCell.relations.all { it.condition == "Front Legs/Hind Legs" })
    }

    @Test
    fun simpleFilteredScreenshotCaseIsSelectedFromTheProductionCorpus() {
        val candidates = buildList {
            data.monsters.forEach { monster ->
                monster.rewards.availableContexts().forEach { rank ->
                    MonsterRewardFilter.entries.filter { it != MonsterRewardFilter.ALL }.forEach { filter ->
                        val rows = monster.rewards.itemFirstMonsterRewards(rank, filter)
                        if (rows.isNotEmpty() && rows.all { it.routes.size == 1 }) {
                            add(SimpleFilteredDemo(monster, rank, filter, rows.size))
                        }
                    }
                }
            }
        }
        val selected = candidates.sortedWith(
            compareByDescending<SimpleFilteredDemo> { it.itemCount }
                .thenBy { it.monster.name }
                .thenBy { it.rank.name }
                .thenBy { it.filter.ordinal }
        ).first()

        assertEquals("Barioth", selected.monster.name)
        assertEquals(RewardContext.HIGH, selected.rank)
        assertEquals(MonsterRewardFilter.BREAK, selected.filter)
        assertEquals(8, selected.itemCount)
        println("SIMPLE_FILTERED_DEMO=${selected.monster.name}/${selected.rank}/${selected.filter}/${selected.itemCount}")
    }

    @Test
    fun routesAreHumanReadableAndUnknownSourceMethodsFailClosed() {
        val routeLabels = data.monsters.flatMap { it.rewards }.map { it.displayRouteLabel() }
        assertTrue(routeLabels.any { it == "Head Break" })
        assertTrue(routeLabels.any { it == "Tail Carve" })
        assertTrue(routeLabels.any { it == "Shiny Drop" })
        assertTrue(routeLabels.any { it == "Mining Points" })
        assertTrue(routeLabels.none { it.matches(Regex("[A-Z]+(?:_[A-Z]+)+")) })

        val unknown = MonsterReward("Unknown", "RAW_INTERNAL", "", "1%", gameItemId = 99, relationId = "unknown")
        val failure = runCatching { listOf(unknown).itemFirstMonsterRewards(RewardContext.LOW) }.exceptionOrNull()
        assertTrue("unmapped methods must fail before reaching visible UI", failure is IllegalStateException)
    }
}
