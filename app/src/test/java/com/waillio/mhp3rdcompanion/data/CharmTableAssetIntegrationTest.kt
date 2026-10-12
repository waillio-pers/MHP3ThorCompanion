package com.waillio.mhp3rdcompanion.data

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CharmTableAssetIntegrationTest {
    @Test
    fun packagedAssetLoadsAgainstCanonicalSkillsAndIndexesAllStandardStates() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fixture = CompanionRepository(context).data
        val index = CharmTableRepository(context, fixture.skillTrees).index

        assertEquals((1..12).toList(), index.source.tables)
        assertEquals(5_400, index.source.rowsPerTable)
        assertEquals(64_800, index.source.sourceRows)
        assertEquals(64_800, index.states.size)
        assertEquals(index.source.tables, index.states.map { it.table }.distinct().sorted())
        assertTrue((1..12).all { table -> index.states.count { it.table == table } == 5_400 })
        assertEquals(64_800, index.states.map { it.sourceId }.distinct().size)
        assertTrue(index.skillTreeIds.all { stableId -> fixture.skillTrees.any { it.stableSkillTreeId == stableId } })
        assertEquals(listOf(TalismanClass.QUEEN, TalismanClass.KING, TalismanClass.DRAGON), index.availableClasses())
        assertEquals(
            mapOf(TalismanClass.QUEEN to 25_227, TalismanClass.KING to 28_864, TalismanClass.DRAGON to 10_709),
            index.classDistribution()
        )
        assertEquals(
            index.states.map { index.skillTreeId(it.skill1Index) }.toSet(),
            index.skillIdsAtPosition(1)
        )
        assertEquals(
            index.states.mapNotNull { index.skillTreeId(it.skill2Index) }.toSet(),
            index.skillIdsAtPosition(2)
        )
        assertTrue(index.availableClasses().all { index.classDistribution()[it] ?: 0 > 0 })

        val source = index.states.first()
        val observation = CharmObservation(
            talismanClass = requireNotNull(index.classAt(source.classIndex)),
            skill1Id = requireNotNull(index.skillTreeId(source.skill1Index)),
            skill1Points = source.skill1Points,
            skill2Id = index.skillTreeId(source.skill2Index),
            skill2Points = source.skill2Points,
            slots = source.slots
        )
        assertTrue(index.identify(listOf(observation)).tables.contains(source.table))
        assertEquals(5_400, index.search(CharmSearchCriteria(tableMode = CharmTableMode.EXPLICIT, explicitTable = source.table), null)
            .sumOf { it.totalSourceStates })

        val negativeObservationSource = index.states.first { it.skill1Points < 0 || (it.skill2Index >= 0 && it.skill2Points < 0) }
        val negativeObservation = CharmObservation(
            talismanClass = requireNotNull(index.classAt(negativeObservationSource.classIndex)),
            skill1Id = requireNotNull(index.skillTreeId(negativeObservationSource.skill1Index)),
            skill1Points = negativeObservationSource.skill1Points,
            skill2Id = index.skillTreeId(negativeObservationSource.skill2Index),
            skill2Points = negativeObservationSource.skill2Points,
            slots = negativeObservationSource.slots
        )
        assertTrue(negativeObservation.skill1Points < 0 || negativeObservation.skill2Points < 0)
        assertTrue(index.identify(listOf(negativeObservation)).tables.contains(negativeObservationSource.table))
    }
}
