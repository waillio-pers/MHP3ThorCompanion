package com.waillio.mhp3rdcompanion.data

/** Precomputed field graph indexes used by Maps 2.0 and Item Detail. */
data class FieldItemRankKey(val gameItemId: Int, val rank: GeneratedMaterialRankContext)

data class FieldSourceCoverage(
    val mappedSourceNodeIds: Set<String>,
    val specialSourceNodeIds: Set<String>,
    val unplacedSourceNodeIds: Set<String>,
    /** Physical markers, de-duplicated across many-to-many equivalent links. */
    val mappedMarkerIds: Set<String> = emptySet(),
    val specialReasonCounts: Map<String, Int> = emptyMap()
) {
    val mappedCount: Int get() = mappedMarkerIds.size
    val specialCount: Int get() = specialSourceNodeIds.size
    val unplacedCount: Int get() = unplacedSourceNodeIds.size
}

data class FieldSourceIndex(
    val gatheringNodeById: Map<String, GeneratedGatheringNode>,
    val dropsByNodeId: Map<String, List<GeneratedGatheringDrop>>,
    val fieldNodeIdsByGameItemId: Map<Int, Set<String>>,
    val fieldNodeIdsByGameItemIdAndRank: Map<FieldItemRankKey, Set<String>>,
    val mapMarkerIdsByGatheringNodeId: Map<String, Set<String>>,
    val itemNameByGameItemId: Map<Int, String>,
    val specialSourceNodeIds: Set<String> = emptySet(),
    val specialSourceReasonByNodeId: Map<String, String> = emptyMap()
) {
    fun nodesForItem(gameItemId: Int, rank: GeneratedMaterialRankContext? = null): Set<String> =
        if (rank == null) fieldNodeIdsByGameItemId[gameItemId].orEmpty()
        else fieldNodeIdsByGameItemIdAndRank[FieldItemRankKey(gameItemId, rank)].orEmpty()

    fun nodesForItemOnMap(
        gameItemId: Int,
        mapId: String,
        rank: GeneratedMaterialRankContext? = null
    ): Set<String> = nodesForItem(gameItemId, rank).filterTo(linkedSetOf()) { nodeId ->
        gatheringNodeById[nodeId]?.locationId == mapId
    }

    fun availableRanksForItemOnMap(gameItemId: Int, mapId: String): Set<GeneratedMaterialRankContext> =
        nodesForItemOnMap(gameItemId, mapId).flatMapTo(linkedSetOf()) { nodeId ->
            dropsByNodeId[nodeId].orEmpty().map { it.rankContext }
        }

    /** Rank contexts that have an exact map marker or an intentional textual
     * special source. Unplaced ordinary sources must not create dead toggles. */
    fun availableMappedOrSpecialRanksForItemOnMap(
        gameItemId: Int,
        mapId: String
    ): Set<GeneratedMaterialRankContext> = GeneratedMaterialRankContext.entries
        .filterTo(linkedSetOf()) { context ->
            val coverage = sourceCoverageForItemOnMap(gameItemId, mapId, context)
            coverage.mappedCount > 0 || coverage.specialCount > 0
        }

    fun sourceCoverageForItemOnMap(
        gameItemId: Int,
        mapId: String,
        rank: GeneratedMaterialRankContext? = null
    ): FieldSourceCoverage {
        val sourceIds = nodesForItemOnMap(gameItemId, mapId, rank)
        val mapped = sourceIds.filterTo(linkedSetOf()) { nodeId ->
            mapMarkerIdsByGatheringNodeId[nodeId].orEmpty().isNotEmpty()
        }
        val mappedMarkers = mapped.flatMapTo(linkedSetOf()) { nodeId ->
            mapMarkerIdsByGatheringNodeId[nodeId].orEmpty()
        }
        val special = sourceIds.filterTo(linkedSetOf()) { it in specialSourceNodeIds }
        return FieldSourceCoverage(
            mappedSourceNodeIds = mapped,
            specialSourceNodeIds = special,
            unplacedSourceNodeIds = sourceIds - mapped - special,
            mappedMarkerIds = mappedMarkers,
            specialReasonCounts = special.groupingBy { specialSourceReasonByNodeId[it] ?: "SPECIAL" }.eachCount()
        )
    }

    companion object {
        val EMPTY = FieldSourceIndex(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptySet(), emptyMap())

        fun build(
            nodes: List<GeneratedGatheringNode>,
            drops: List<GeneratedGatheringDrop>,
            itemNames: Map<Int, String>,
            markerIdsByGatheringNodeId: Map<String, Set<String>>,
            specialSourceNodeIds: Set<String> = emptySet(),
            specialSourceReasonByNodeId: Map<String, String> = emptyMap()
        ): FieldSourceIndex {
            val nodeById = nodes.associateBy { it.id }
            val dropsByNode = drops.groupBy { it.nodeId }
            val itemToNodes = drops.groupBy { it.gameItemId }
                .mapValues { (_, rows) -> rows.mapTo(linkedSetOf()) { it.nodeId } }
            val itemRankToNodes = drops.groupBy { FieldItemRankKey(it.gameItemId, it.rankContext) }
                .mapValues { (_, rows) -> rows.mapTo(linkedSetOf()) { it.nodeId } }
            return FieldSourceIndex(
                nodeById,
                dropsByNode,
                itemToNodes,
                itemRankToNodes,
                markerIdsByGatheringNodeId,
                itemNames,
                specialSourceNodeIds,
                specialSourceReasonByNodeId
            )
        }
    }
}
