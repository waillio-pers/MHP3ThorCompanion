package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.GeneratedDataset
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import com.waillio.mhp3rdcompanion.data.monsterPresentationProjection
import com.waillio.mhp3rdcompanion.data.monsterRewardMethodPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlinx.serialization.json.Json
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class MonsterSourcePresentationTest {
    private val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data
    private val monsterRows = data.materials.flatMap { it.sources }
        .filter { it.type == MaterialSourceType.MONSTER_REWARD }

    @Test
    fun productionSchemaAndEmbeddedShaRemainAccepted() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bytes = context.assets.open("mhp3rd-data.json").use { it.readBytes() }
        val generated = Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(bytes.decodeToString())
        assertEquals(31, generated.schemaVersion)
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02X".format(it) }
        assertEquals("657CFA9376FC8CBE56C67E67D4D1A27C455D21021F51E21B62427C8DDBB26155", sha)
    }

    @Test
    fun allOrdinaryMonsterRelationsRemainTraceableExactlyOnce() {
        assertEquals(1535, monsterRows.size)
        data.materials.filter { material -> material.sources.any { it.type == MaterialSourceType.MONSTER_REWARD } }
            .forEach { material ->
                val raw = material.sources.filter { it.type == MaterialSourceType.MONSTER_REWARD }
                val projection = raw.monsterPresentationProjection()
                assertEquals(raw.size, projection.rawRelationCount)
                assertEquals(raw.map { it.id }.sorted(), projection.rawRelationIds.sorted())
                assertEquals(projection.rawRelationIds.size, projection.rawRelationIds.toSet().size)
            }
    }

    @Test
    fun projectionIsMonsterFirstAndExcludesOtherFamilies() {
        val material = data.materials.single { it.id == "item_uragaan_marrow" }
        val projection = material.sources.monsterPresentationProjection()
        assertEquals(2, projection.monsterCount)
        assertTrue(projection.monsters.all { monster -> monster.methods.isNotEmpty() })
        assertTrue(projection.monsters.flatMap { it.entries }.all { it.source.type == MaterialSourceType.MONSTER_REWARD })
        assertFalse(projection.monsters.flatMap { it.entries }.any { it.source.type == MaterialSourceType.INVASION_REWARD })
        assertFalse(projection.monsters.flatMap { it.entries }.any { it.source.type == MaterialSourceType.SMALL_MONSTER })
    }

    @Test
    fun explicitMethodPriorityKeepsBreaksTogetherAndShinyLast() {
        data.materials.filter { it.sources.any { source -> source.type == MaterialSourceType.MONSTER_REWARD } }
            .flatMap { it.sources.monsterPresentationProjection().monsters }
            .forEach { monster ->
                val priorities = monster.methods.map { monsterRewardMethodPriority(it.method) }
                assertEquals(priorities.sorted(), priorities)
                val breakIndexes = monster.methods.mapIndexedNotNull { index, method ->
                    index.takeIf { method.method == "PART_BREAK" }
                }
                if (breakIndexes.isNotEmpty()) {
                    assertEquals((breakIndexes.first()..breakIndexes.last()).toList(), breakIndexes)
                }
                val shiny = monster.methods.indexOfFirst { it.method == "SHINY" }
                if (shiny >= 0) assertEquals(monster.methods.lastIndex, shiny)
            }
    }

    @Test
    fun breakConditionsAndQuantitiesAreNotCollapsed() {
        val partBreakMaterial = data.materials.first { material ->
            material.sources.filter { it.type == MaterialSourceType.MONSTER_REWARD && it.method == "PART_BREAK" }
                .groupBy { it.monsterId }.values.any { rows -> rows.map { it.condition }.distinct().size >= 2 }
        }
        val rawBreaks = partBreakMaterial.sources.filter {
            it.type == MaterialSourceType.MONSTER_REWARD && it.method == "PART_BREAK"
        }
        val projectedBreaks = partBreakMaterial.sources.monsterPresentationProjection().monsters
            .flatMap { it.methods }.filter { it.method == "PART_BREAK" }
        assertEquals(rawBreaks.size, projectedBreaks.flatMap { it.entries }.size)
        assertTrue(projectedBreaks.map { it.condition }.distinct().size >= 2)

        val duplicate = data.materials.asSequence().mapNotNull { material ->
            val rows = material.sources.filter { it.type == MaterialSourceType.MONSTER_REWARD }
            rows.groupBy { Triple(it.monsterId, it.method, it.rank) }.values.firstOrNull { it.size > 1 }
                ?.let { material to it }
        }.firstOrNull()
        assertNotNull(duplicate)
        val (material, rows) = duplicate!!
        val projected = material.sources.monsterPresentationProjection().monsters
            .flatMap { it.entries }.filter { it.source.id in rows.map { row -> row.id } }
        assertEquals(rows.size, projected.size)
        assertEquals(rows.map { it.chance to it.quantity }.sortedBy { it.first ?: -1 }, projected.map { it.chance to it.quantity }.sortedBy { it.first ?: -1 })
    }

    @Test
    fun distinctMonsterCountAndOutlierPreviewAreDeterministic() {
        val large = data.materials.single { it.id == "item_wyvern_tear" }
        val projection = large.sources.monsterPresentationProjection()
        assertEquals(30, projection.monsterCount)
        assertEquals(6, projection.monsters.take(6).size)
        assertEquals(projection.monsters.sortedWith(compareBy({ it.monsterName.lowercase() }, { it.monsterId })), projection.monsters)

        val small = data.materials.single { it.id == "item_akantor_tail" }
            .sources.monsterPresentationProjection()
        assertEquals(1, small.monsterCount)
        assertEquals(1, small.monsters.size)
    }

    @Test
    fun nullQuantityRemainsNullAndExplicitOneRemainsOne() {
        val nullRow = monsterRows.first { it.quantity == null }
        val nullProjected = data.materials.first { material -> material.sources.any { it.id == nullRow.id } }
            .sources.monsterPresentationProjection().rawRelationIds
        assertTrue(nullRow.id in nullProjected)
        val explicitOne = monsterRows.firstOrNull { it.quantity == 1 }
        assertNotNull(explicitOne)
        val material = data.materials.first { item -> item.sources.any { it.id == explicitOne!!.id } }
        val projected = material.sources.monsterPresentationProjection().monsters.flatMap { it.entries }
            .single { it.rawRelationIdentity == explicitOne!!.id }
        assertEquals(1, projected.quantity)
    }
}
