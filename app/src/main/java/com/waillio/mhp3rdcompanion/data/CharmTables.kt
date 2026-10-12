package com.waillio.mhp3rdcompanion.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class CharmTableAsset(
    val schemaVersion: Int,
    val source: CharmAssetSource,
    val sourceCategories: List<String>,
    val classes: List<String>,
    val skills: List<String>,
    val rows: List<List<Int>>
)

@Serializable
internal data class CharmAssetSource(
    val category: String,
    val repository: String,
    val commit: String,
    val upstreamCsvSha256: String,
    val inputCsvSha256: String,
    val skillCrosswalkSha256: String,
    val license: String,
    val tables: List<Int>,
    val rowsPerTable: Int,
    val sourceRows: Int
)

@Serializable
internal enum class TalismanClass(val label: String, val assetIndex: Int) {
    QUEEN("Queen", 0),
    KING("King", 1),
    DRAGON("Dragon", 2);

    companion object {
        fun fromAsset(index: Int): TalismanClass? = entries.firstOrNull { it.assetIndex == index }
    }
}

/** Compact runtime form of one source state; source multiplicity is retained. */
internal data class CharmState(
    val categoryIndex: Int,
    val table: Int,
    val sourceId: Int,
    val classIndex: Int,
    val skill1Index: Int,
    val skill1Points: Int,
    /** -1 is the source's absent second skill. */
    val skill2Index: Int,
    val skill2Points: Int,
    val slots: Int
) {
    fun signature() = CharmSignature(classIndex, skill1Index, skill1Points, skill2Index, skill2Points, slots)
}

/** Skill order is intentionally represented in separate fields. */
internal data class CharmSignature(
    val classIndex: Int,
    val skill1Index: Int,
    val skill1Points: Int,
    val skill2Index: Int,
    val skill2Points: Int,
    val slots: Int
)

internal data class CharmObservation(
    val talismanClass: TalismanClass,
    val skill1Id: String,
    val skill1Points: Int,
    val skill2Id: String? = null,
    val skill2Points: Int = 0,
    val slots: Int
)

/** Identify-form working state. Held only by the activity-scoped ViewModel, not persisted across launches. */
internal data class CharmIdentifyWorkingState(
    val observations: List<CharmObservation> = emptyList(),
    val editingObservation: Int = -1,
    val formClass: TalismanClass = TalismanClass.QUEEN,
    val formSkill1Id: String? = null,
    val formSkill1Points: String = "",
    val formSkill2Id: String? = null,
    val formSkill2Points: String = "",
    val formSlots: Int? = null
)

@Serializable
internal enum class CharmPointOperator(val symbol: String) {
    EQUAL("="),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    GREATER_OR_EQUAL(">="),
    LESS_OR_EQUAL("<=");

    fun matches(actual: Int, requested: Int): Boolean = when (this) {
        EQUAL -> actual == requested
        GREATER_THAN -> actual > requested
        LESS_THAN -> actual < requested
        GREATER_OR_EQUAL -> actual >= requested
        LESS_OR_EQUAL -> actual <= requested
    }

    companion object {
        val choices = listOf(EQUAL, GREATER_THAN, LESS_THAN, GREATER_OR_EQUAL, LESS_OR_EQUAL)
    }
}

@Serializable
internal enum class CharmSlotMode { MINIMUM, EXACT }

@Serializable
internal enum class CharmTableMode { ANY, CURRENT, EXPLICIT }

@Serializable
internal data class CharmSearchCriteria(
    val skill1Id: String? = null,
    val skill1Points: Int? = null,
    val skill1Mode: CharmPointOperator = CharmPointOperator.GREATER_OR_EQUAL,
    val skill2Id: String? = null,
    val skill2Points: Int? = null,
    val skill2Mode: CharmPointOperator = CharmPointOperator.GREATER_OR_EQUAL,
    val slots: Int? = null,
    val slotMode: CharmSlotMode = CharmSlotMode.MINIMUM,
    val tableMode: CharmTableMode = CharmTableMode.ANY,
    val explicitTable: Int? = null,
    val talismanClass: TalismanClass? = null
) {
    val hasFilters: Boolean
        get() = skill1Id != null || skill2Id != null || slots != null || talismanClass != null || tableMode != CharmTableMode.ANY
}

@Serializable
internal data class CharmSavedSearch(
    val id: String,
    val name: String,
    val criteria: CharmSearchCriteria
)

internal fun addCharmSavedSearch(
    existing: List<CharmSavedSearch>,
    requestedName: String,
    criteria: CharmSearchCriteria,
    id: String
): List<CharmSavedSearch> {
    if (existing.any { it.criteria == criteria }) return existing
    val base = requestedName.trim().ifBlank { "Charm search" }
    val used = existing.mapTo(hashSetOf()) { it.name.lowercase() }
    var name = base
    var suffix = 2
    while (name.lowercase() in used) name = "$base ($suffix)".also { suffix++ }
    return existing + CharmSavedSearch(id = id, name = name, criteria = criteria)
}

internal fun deleteCharmSavedSearch(existing: List<CharmSavedSearch>, id: String): List<CharmSavedSearch> =
    existing.filterNot { it.id == id }

internal const val CHARM_SEARCH_PREVIEW_LIMIT = 30
internal const val CHARM_SEARCH_PAGE_SIZE = 50

internal data class CharmSlotFilterChoice(val label: String, val slots: Int?, val mode: CharmSlotMode?)

internal val charmSlotFilterChoices = listOf(
    CharmSlotFilterChoice("Any", null, null),
    CharmSlotFilterChoice(">=0", 0, CharmSlotMode.MINIMUM),
    CharmSlotFilterChoice(">=1", 1, CharmSlotMode.MINIMUM),
    CharmSlotFilterChoice(">=2", 2, CharmSlotMode.MINIMUM),
    CharmSlotFilterChoice("=0", 0, CharmSlotMode.EXACT),
    CharmSlotFilterChoice("=1", 1, CharmSlotMode.EXACT),
    CharmSlotFilterChoice("=2", 2, CharmSlotMode.EXACT),
    CharmSlotFilterChoice("=3", 3, CharmSlotMode.EXACT)
)

internal fun charmSearchPreview(results: List<CharmResultGroup>): List<CharmResultGroup> =
    results.take(CHARM_SEARCH_PREVIEW_LIMIT)

internal fun charmSearchPageCount(total: Int, pageSize: Int = CHARM_SEARCH_PAGE_SIZE): Int =
    if (total <= 0) 0 else (total + pageSize - 1) / pageSize

internal fun charmSearchPage(results: List<CharmResultGroup>, pageIndex: Int, pageSize: Int = CHARM_SEARCH_PAGE_SIZE): List<CharmResultGroup> {
    require(pageSize > 0)
    val page = pageIndex.coerceIn(0, (charmSearchPageCount(results.size, pageSize) - 1).coerceAtLeast(0))
    return results.drop(page * pageSize).take(pageSize)
}

internal fun charmSlotNotation(slots: Int): String {
    require(slots in 0..3)
    return "O".repeat(slots) + "-".repeat(3 - slots)
}

internal data class CharmResultGroup(
    val signature: CharmSignature,
    /** Per-table counts are source-state counts, never drop probabilities. */
    val sourceStateCountByTable: Map<Int, Int>
) {
    val totalSourceStates: Int get() = sourceStateCountByTable.values.sum()
    val tables: List<Int> get() = sourceStateCountByTable.keys.sorted()
}

internal enum class CharmIdentificationState { NO_OBSERVATIONS, ONE_TABLE, MULTIPLE_TABLES, ALL_TABLES, NO_MATCH }

internal data class CharmIdentification(
    val state: CharmIdentificationState,
    val tables: List<Int>
)

/** Indexed immutable corpus. Search and identification never reparse JSON. */
internal class CharmTableIndex(
    val source: CharmAssetSource,
    val sourceCategories: List<String>,
    val classes: List<String>,
    val skillTreeIds: List<String>,
    states: List<CharmState>
) {
    val states: List<CharmState> = states.toList()
    private val skillIndexById = skillTreeIds.withIndex().associate { it.value to it.index }
    private val bySkill1: Map<Int, IntArray> = this.states.indices
        .groupBy { this.states[it].skill1Index }
        .mapValues { (_, indexes) -> indexes.toIntArray() }
    private val bySkill2: Map<Int, IntArray> = this.states.asSequence().withIndex()
        .filter { it.value.skill2Index >= 0 }
        .groupBy({ it.value.skill2Index }, { it.index })
        .mapValues { (_, indexes) -> indexes.toIntArray() }
    private val byTable: Map<Int, IntArray> = this.states.indices
        .groupBy { this.states[it].table }
        .mapValues { (_, indexes) -> indexes.toIntArray() }
    private val tableMaskBySignature: Map<CharmSignature, Int> = buildMap {
        this@CharmTableIndex.states.forEach { state ->
            val bit = 1 shl (state.table - 1)
            this[state.signature()] = (get(state.signature()) ?: 0) or bit
        }
    }

    fun skillTreeIndex(stableSkillTreeId: String?): Int? = stableSkillTreeId?.let(skillIndexById::get)

    fun skillTreeId(index: Int): String? = skillTreeIds.getOrNull(index)

    fun classAt(index: Int): TalismanClass? = TalismanClass.fromAsset(index)

    fun skillIdsAtPosition(position: Int): Set<String> {
        require(position == 1 || position == 2)
        return states.asSequence()
            .map { if (position == 1) it.skill1Index else it.skill2Index }
            .filter { it >= 0 }
            .distinct()
            .mapNotNull(::skillTreeId)
            .toSet()
    }

    fun availableClasses(): List<TalismanClass> = states.asSequence()
        .mapNotNull { classAt(it.classIndex) }
        .distinct()
        .sortedBy { it.assetIndex }
        .toList()

    fun classDistribution(): Map<TalismanClass, Int> = states.groupingBy { state ->
        requireNotNull(classAt(state.classIndex))
    }.eachCount()

    fun search(criteria: CharmSearchCriteria, currentTable: Int?): List<CharmResultGroup> {
        if (!criteria.hasFilters) return emptyList()
        val effectiveTable = when (criteria.tableMode) {
            CharmTableMode.ANY -> null
            CharmTableMode.CURRENT -> currentTable?.takeIf { it in 1..12 } ?: return emptyList()
            CharmTableMode.EXPLICIT -> criteria.explicitTable?.takeIf { it in 1..12 } ?: return emptyList()
        }
        val skill1Index = skillTreeIndex(criteria.skill1Id)
        if (criteria.skill1Id != null && skill1Index == null) return emptyList()
        val skill2Index = skillTreeIndex(criteria.skill2Id)
        if (criteria.skill2Id != null && skill2Index == null) return emptyList()

        val buckets = buildList {
            skill1Index?.let { bySkill1[it]?.let(::add) ?: return emptyList() }
            skill2Index?.let { bySkill2[it]?.let(::add) ?: return emptyList() }
            effectiveTable?.let { byTable[it]?.let(::add) ?: return emptyList() }
        }
        val candidates = buckets.minByOrNull { it.size } ?: states.indices.toList().toIntArray()
        val counts = LinkedHashMap<CharmSignature, IntArray>()
        for (index in candidates) {
            val state = states[index]
            if (effectiveTable != null && state.table != effectiveTable) continue
            if (criteria.talismanClass != null && state.classIndex != criteria.talismanClass.assetIndex) continue
            if (skill1Index != null && state.skill1Index != skill1Index) continue
            if (skill2Index != null && state.skill2Index != skill2Index) continue
            if (skill1Index != null && !pointMatches(state.skill1Points, criteria.skill1Points, criteria.skill1Mode)) continue
            if (skill2Index != null && !pointMatches(state.skill2Points, criteria.skill2Points, criteria.skill2Mode)) continue
            if (criteria.slots != null && !slotMatches(state.slots, criteria.slots, criteria.slotMode)) continue
            val perTable = counts.getOrPut(state.signature()) { IntArray(13) }
            perTable[state.table]++
        }
        return counts.entries.map { (signature, perTable) ->
            CharmResultGroup(signature, (1..12).mapNotNull { table ->
                perTable[table].takeIf { it > 0 }?.let { table to it }
            }.toMap())
        }.sortedWith(
            compareBy<CharmResultGroup> { it.signature.classIndex }
                .thenBy { skillTreeId(it.signature.skill1Index).orEmpty() }
                .thenByDescending { it.signature.skill1Points }
                .thenBy { skillTreeId(it.signature.skill2Index).orEmpty() }
                .thenByDescending { it.signature.skill2Points }
                .thenBy { it.signature.slots }
        )
    }

    fun identify(observations: List<CharmObservation>): CharmIdentification {
        if (observations.isEmpty()) return CharmIdentification(CharmIdentificationState.NO_OBSERVATIONS, emptyList())
        var candidateMask = 0xFFF
        observations.forEach { observation ->
            val skill1Index = skillTreeIndex(observation.skill1Id)
                ?: return CharmIdentification(CharmIdentificationState.NO_MATCH, emptyList())
            val skill2Index = observation.skill2Id?.let { skillTreeIndex(it) }
            if (observation.skill2Id != null && skill2Index == null) {
                return CharmIdentification(CharmIdentificationState.NO_MATCH, emptyList())
            }
            val signature = CharmSignature(
                observation.talismanClass.assetIndex,
                skill1Index,
                observation.skill1Points,
                skill2Index ?: -1,
                if (skill2Index == null) 0 else observation.skill2Points,
                observation.slots
            )
            candidateMask = candidateMask and (tableMaskBySignature[signature] ?: 0)
            if (candidateMask == 0) return CharmIdentification(CharmIdentificationState.NO_MATCH, emptyList())
        }
        val tables = (1..12).filter { candidateMask and (1 shl (it - 1)) != 0 }
        val state = when {
            tables.size == 1 -> CharmIdentificationState.ONE_TABLE
            tables.size == 12 -> CharmIdentificationState.ALL_TABLES
            else -> CharmIdentificationState.MULTIPLE_TABLES
        }
        return CharmIdentification(state, tables)
    }

    private fun pointMatches(actual: Int, requested: Int?, mode: CharmPointOperator): Boolean =
        requested == null || mode.matches(actual, requested)

    private fun slotMatches(actual: Int, requested: Int, mode: CharmSlotMode): Boolean =
        if (mode == CharmSlotMode.EXACT) actual == requested else actual >= requested

    companion object {
        fun fromAsset(asset: CharmTableAsset): CharmTableIndex {
            require(asset.schemaVersion == 1) { "Unsupported charm asset schema ${asset.schemaVersion}" }
            require(asset.sourceCategories.isNotEmpty() && asset.classes == TalismanClass.entries.map { it.label })
            require(asset.rows.size == asset.source.sourceRows)
            val states = asset.rows.mapIndexed { index, row ->
                require(row.size == 9) { "Charm row $index does not have nine packed fields" }
                CharmState(row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7], row[8]).also { state ->
                    require(state.categoryIndex in asset.sourceCategories.indices)
                    require(state.table in 1..12 && state.sourceId >= 0)
                    require(state.classIndex in TalismanClass.entries.indices)
                    require(state.skill1Index in asset.skills.indices)
                    require(state.skill2Index == -1 || state.skill2Index in asset.skills.indices)
                    require(state.slots in 0..3)
                }
            }
            return CharmTableIndex(asset.source, asset.sourceCategories, asset.classes, asset.skills, states)
        }
    }
}

internal class CharmTableRepository(private val context: Context, private val skillTrees: List<SkillTree>) {
    private val json = Json { ignoreUnknownKeys = false }
    val index: CharmTableIndex by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val encoded = context.assets.open("mhp3rd-charms.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val asset = json.decodeFromString<CharmTableAsset>(encoded)
        val parsed = CharmTableIndex.fromAsset(asset)
        val canonicalIds = skillTrees.mapTo(hashSetOf()) { it.stableSkillTreeId }
        require(parsed.skillTreeIds.size == 61 && parsed.skillTreeIds.all(canonicalIds::contains)) {
            "Charm corpus skill crosswalk no longer matches the Companion canonical skill trees"
        }
        parsed
    }
}
