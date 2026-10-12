package com.waillio.mhp3rdcompanion.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharmTableIndexTest {
    private val attack = "attack"
    private val expert = "expert"
    private val guard = "guard"

    private fun index(): CharmTableIndex {
        val rows = mutableListOf<CharmState>()
        fun add(
            table: Int,
            sourceId: Int,
            classIndex: Int = 0,
            skill1: Int = 0,
            points1: Int = 7,
            skill2: Int = 1,
            points2: Int = 10,
            slots: Int = 2
        ) {
            rows += CharmState(0, table, sourceId, classIndex, skill1, points1, skill2, points2, slots)
        }

        add(1, 1)
        add(2, 2)
        add(7, 3)
        add(7, 4) // same observable signature: preserve both source states.
        add(7, 5, skill1 = 2, points1 = 5, skill2 = -1, points2 = 0, slots = 0)
        add(7, 6, skill1 = 0, points1 = 8, skill2 = 1, points2 = 11, slots = 3)
        add(7, 7, skill1 = 0, points1 = -3, skill2 = 1, points2 = -10, slots = 2)
        add(8, 8, skill1 = 0, points1 = 0, skill2 = -1, points2 = 0, slots = 1)
        add(9, 9, skill1 = 0, points1 = 6, skill2 = -1, points2 = 0, slots = 0)
        (1..12).forEach { table ->
            add(table, 100 + table, classIndex = 1, skill1 = 1, points1 = 2, skill2 = -1, points2 = 0, slots = 3)
        }
        val source = CharmAssetSource(
            category = "TIMEWORN", repository = "test", commit = "test",
            upstreamCsvSha256 = "test", inputCsvSha256 = "test", skillCrosswalkSha256 = "test",
            license = "WTFPL-2.0", tables = (1..12).toList(), rowsPerTable = 5_400, sourceRows = rows.size
        )
        return CharmTableIndex(source, listOf("TIMEWORN"), listOf("Queen", "King", "Dragon"), listOf(attack, expert, guard), rows)
    }

    private fun observationA() = CharmObservation(TalismanClass.QUEEN, attack, 7, expert, 10, 2)
    private fun observationB() = CharmObservation(TalismanClass.QUEEN, guard, 5, null, 0, 0)
    private fun observationAllTables() = CharmObservation(TalismanClass.KING, expert, 2, null, 0, 3)

    @Test
    fun identifySupportsSingleMultipleAllTablesAndNoMatchStates() {
        val index = index()
        assertEquals(CharmIdentification(CharmIdentificationState.ONE_TABLE, listOf(7)), index.identify(listOf(observationB())))
        assertEquals(
            CharmIdentification(CharmIdentificationState.MULTIPLE_TABLES, listOf(1, 2, 7)),
            index.identify(listOf(observationA()))
        )
        assertEquals(
            CharmIdentification(CharmIdentificationState.ALL_TABLES, (1..12).toList()),
            index.identify(listOf(observationAllTables()))
        )
        assertEquals(
            CharmIdentification(CharmIdentificationState.NO_MATCH, emptyList()),
            index.identify(listOf(observationA(), observationB().copy(skill1Points = 99)))
        )
    }

    @Test
    fun observationsIntersectAndRemovingOneRecomputesTheCandidates() {
        val index = index()
        val both = listOf(observationA(), observationB())
        assertEquals(listOf(7), index.identify(both).tables)
        assertEquals(listOf(1, 2, 7), index.identify(both.dropLast(1)).tables)
    }

    @Test
    fun skillOrderIsSignificantForIdentification() {
        val reversed = CharmObservation(TalismanClass.QUEEN, expert, 10, attack, 7, 2)
        assertEquals(CharmIdentificationState.NO_MATCH, index().identify(listOf(reversed)).state)
    }

    @Test
    fun searchAppliesSkillPointSlotClassAndTableFilters() {
        val index = index()
        val minimum = index.search(
            CharmSearchCriteria(skill1Id = attack, skill1Points = 7, slots = 2, slotMode = CharmSlotMode.MINIMUM),
            currentTable = null
        )
        assertTrue(minimum.any { it.signature.skill1Points == 8 })
        assertTrue(index.search(CharmSearchCriteria(skill1Points = 99), currentTable = null).isEmpty())
        assertTrue(
            index.search(CharmSearchCriteria(skill2Id = expert, skill1Points = 99), currentTable = null).isNotEmpty()
        )
        val exactCombined = index.search(
            CharmSearchCriteria(
                skill1Id = attack, skill1Points = 7, skill1Mode = CharmPointOperator.EQUAL,
                skill2Id = expert, skill2Points = 10, skill2Mode = CharmPointOperator.EQUAL,
                slots = 2, slotMode = CharmSlotMode.EXACT, talismanClass = TalismanClass.QUEEN
            ),
            currentTable = null
        ).single()
        assertEquals(4, exactCombined.totalSourceStates)
        assertEquals(mapOf(1 to 1, 2 to 1, 7 to 2), exactCombined.sourceStateCountByTable)

        val explicit = index.search(CharmSearchCriteria(skill1Id = attack, tableMode = CharmTableMode.EXPLICIT, explicitTable = 2), null)
        assertEquals(setOf(2), explicit.flatMap { it.tables }.toSet())
        val current = index.search(CharmSearchCriteria(skill1Id = attack, tableMode = CharmTableMode.CURRENT), 7)
        assertEquals(setOf(7), current.flatMap { it.tables }.toSet())
        assertTrue(index.search(CharmSearchCriteria(skill1Id = attack, tableMode = CharmTableMode.CURRENT), null).isEmpty())
        assertTrue(index.search(CharmSearchCriteria(skill1Id = attack, talismanClass = TalismanClass.DRAGON), null).isEmpty())
    }

    @Test
    fun identicalSignaturesAggregateForDisplayWithoutLosingMultiplicity() {
        val group = index().search(CharmSearchCriteria(skill1Id = attack, tableMode = CharmTableMode.ANY), null)
            .single { it.signature.skill1Points == 7 }
        assertEquals(4, group.totalSourceStates)
        assertEquals(listOf(1, 2, 7), group.tables)
        assertEquals(mapOf(1 to 1, 2 to 1, 7 to 2), group.sourceStateCountByTable)
    }

    @Test
    fun allPointOperatorsCompareSignedIntegersExactlyIncludingZero() {
        val index = index()
        fun points(operator: CharmPointOperator, value: Int) = index.search(
            CharmSearchCriteria(skill1Id = attack, skill1Points = value, skill1Mode = operator), null
        ).map { it.signature.skill1Points }.toSet()

        assertEquals(setOf(7), points(CharmPointOperator.EQUAL, 7))
        assertEquals(setOf(8), points(CharmPointOperator.GREATER_THAN, 7))
        assertEquals(setOf(-3, 0, 6), points(CharmPointOperator.LESS_THAN, 7))
        assertEquals(setOf(7, 8), points(CharmPointOperator.GREATER_OR_EQUAL, 7))
        assertEquals(setOf(-3, 0, 6, 7), points(CharmPointOperator.LESS_OR_EQUAL, 7))
        assertEquals(setOf(0), points(CharmPointOperator.EQUAL, 0))
        assertEquals(setOf(-3), points(CharmPointOperator.EQUAL, -3))
        assertEquals(setOf(0, 6, 7, 8), points(CharmPointOperator.GREATER_THAN, -3))
        assertEquals(setOf(-3), points(CharmPointOperator.LESS_THAN, 0))
    }

    @Test
    fun slotChoicesAndNotationCoverOnlySupportedSemantics() {
        assertEquals(listOf("Any", ">=0", ">=1", ">=2", "=0", "=1", "=2", "=3"), charmSlotFilterChoices.map { it.label })
        assertFalse(charmSlotFilterChoices.any { it.label == ">=3" })
        assertEquals(listOf("---", "O--", "OO-", "OOO"), (0..3).map(::charmSlotNotation))

        val index = index()
        fun matching(slots: Int?, mode: CharmSlotMode = CharmSlotMode.MINIMUM) = index.search(
            CharmSearchCriteria(skill1Id = attack, slots = slots, slotMode = mode), null
        ).map { it.signature.slots }.toSet()
        assertEquals(setOf(0, 1, 2, 3), matching(null))
        assertEquals(setOf(0, 1, 2, 3), matching(0))
        assertEquals(setOf(1, 2, 3), matching(1))
        assertEquals(setOf(2, 3), matching(2))
        assertEquals(setOf(0), matching(0, CharmSlotMode.EXACT))
        assertEquals(setOf(1), matching(1, CharmSlotMode.EXACT))
        assertEquals(setOf(2), matching(2, CharmSlotMode.EXACT))
        assertEquals(setOf(3), matching(3, CharmSlotMode.EXACT))
    }

    @Test
    fun resultPreviewAndPaginationRespectThirtyAndFiftyBoundaries() {
        val groups = (0 until 126).map { number ->
            CharmResultGroup(CharmSignature(number % 3, 0, number, -1, 0, number % 4), mapOf(1 to 1))
        }
        assertEquals(30, charmSearchPreview(groups).size)
        assertEquals(30, charmSearchPreview(groups).last().signature.skill1Points + 1)
        assertEquals(3, charmSearchPageCount(groups.size))
        assertEquals(50, charmSearchPage(groups, 0).size)
        assertEquals(50, charmSearchPage(groups, 1).size)
        assertEquals(26, charmSearchPage(groups, 2).size)
        assertEquals(126, charmSearchPage(groups, 99).last().signature.skill1Points + 1)
        assertEquals(emptyList<CharmResultGroup>(), charmSearchPreview(emptyList()))
    }

    @Test
    fun savedSearchesDeduplicateExactCriteriaAndNeverOverwriteDuplicateNames() {
        val firstCriteria = CharmSearchCriteria(skill1Id = attack, skill1Points = -5, skill1Mode = CharmPointOperator.LESS_OR_EQUAL, tableMode = CharmTableMode.CURRENT)
        val otherCriteria = firstCriteria.copy(skill1Points = 0)
        val first = addCharmSavedSearch(emptyList(), "Negative", firstCriteria, "id-1")
        val duplicateSignature = addCharmSavedSearch(first, "Renamed", firstCriteria, "id-2")
        assertEquals(first, duplicateSignature)
        val duplicateName = addCharmSavedSearch(first, "Negative", otherCriteria, "id-3")
        assertEquals("Negative (2)", duplicateName.last().name)
        assertEquals(firstCriteria, duplicateName.first().criteria)
        assertEquals(listOf(duplicateName.last()), deleteCharmSavedSearch(duplicateName, "id-1"))
    }
}
