package com.waillio.mhp3rdcompanion

import androidx.annotation.DrawableRes

internal enum class MapNodeCategory(val displayName: String, val shortLabel: String) {
    MINING("Mining", "Mine"),
    BUGS("Bugs", "Bugs"),
    BONES("Bones", "Bones"),
    PLANTS("Plants", "Plants"),
    MUSHROOMS("Mushrooms", "Shrooms"),
    WHETSTONE("Whetstone", "Whet"),
    HONEY("Honey", "Honey"),
    BERRIES("Berries", "Berries"),
    FISH("Fish", "Fish"),
    MISC("Misc", "Misc"),
    SPIDER_WEB("Spider Web", "Web")
}

/** Coordinates are normalized to the clean base image bounds (0f..1f). */
internal data class MapNode(
    val nodeId: String,
    val category: MapNodeCategory,
    val x: Float,
    val y: Float,
    val areaNumber: String? = null,
    val label: String? = null,
    val sourceGroupId: String? = null,
    val sourceGatheringNodeIds: Set<String> = emptySet(),
    /** False only for a retained calibration marker that duplicates a canonical node. */
    val showInNormalOverlay: Boolean = true
) {
    init {
        require(x in 0f..1f && y in 0f..1f) { "Map node $nodeId is outside normalized image bounds" }
    }
}

internal data class MapDefinition(
    val mapId: String,
    val displayName: String,
    val localeId: String,
    @param:DrawableRes val baseImageRes: Int,
    val imageWidth: Int,
    val imageHeight: Int,
    val sourceViewportWidth: Int = imageWidth,
    val sourceViewportHeight: Int = imageHeight,
    val nodes: List<MapNode>
)

internal object MapRegistry {
    val maps: List<MapDefinition> by lazy { listOf(
        definition("misty_peaks", "Misty Peaks", R.drawable.map_misty_peaks, MapNodeData.nodes.getValue("misty_peaks")),
        definition("sandy_plains", "Sandy Plains", R.drawable.map_sandy_plains, MapNodeData.nodes.getValue("sandy_plains")),
        definition("flooded_forest", "Flooded Forest", R.drawable.map_flooded_forest, MapNodeData.nodes.getValue("flooded_forest")),
        definition("tundra", "Tundra", R.drawable.map_tundra, MapNodeData.nodes.getValue("tundra")),
        definition("volcano", "Volcano", R.drawable.map_volcano, MapNodeData.nodes.getValue("volcano")),
        definition("deserted_island", "Deserted Island", R.drawable.map_deserted_island, MapNodeData.nodes.getValue("deserted_island"))
    ) }

    fun resolve(mapId: String): MapDefinition? = maps.firstOrNull { it.mapId == mapId }

    private fun definition(mapId: String, displayName: String, @DrawableRes image: Int, nodes: List<MapNode>) =
        MapDefinition(
            mapId = mapId,
            displayName = displayName,
            localeId = mapId,
            baseImageRes = image,
            imageWidth = 256,
            imageHeight = 256,
            // Local PSP textures share an authored map square followed by an unused black tail.
            sourceViewportWidth = 170,
            sourceViewportHeight = 170,
            nodes = nodes.map { node ->
                node.copy(
                    category = when (node.nodeId) {
                        // Stable marker ID is retained, but C1.1 identifies this source as fishing.
                        "di-a10-mushrooms" -> MapNodeCategory.FISH
                        else -> node.category
                    },
                    sourceGroupId = MapSourceCrosswalk.equivalentGroupForMarker(node.nodeId)?.groupId,
                    sourceGatheringNodeIds = MapSourceCrosswalk.sourceIdsForMarker(node.nodeId),
                    showInNormalOverlay = !MapSourceCrosswalk.isDeprecatedDuplicateMarker(node.nodeId)
                )
            }
        )


}

internal object MapOverlayLogic {
    /** Empty selection means All; non-empty selection supports one or many categories. */
    fun visibleNodes(
        map: MapDefinition,
        selected: Set<MapNodeCategory>,
        fieldIndex: com.waillio.mhp3rdcompanion.data.FieldSourceIndex = com.waillio.mhp3rdcompanion.data.FieldSourceIndex.EMPTY,
        focusedGameItemId: Int? = null,
        rank: com.waillio.mhp3rdcompanion.data.GeneratedMaterialRankContext? = null,
        whetstoneLens: Boolean = false
    ): List<MapNode> {
        val categoryNodes = (if (whetstoneLens || focusedGameItemId != null || selected.isEmpty()) {
            map.nodes
        } else {
            map.nodes.filter { it.category in selected }
        }).filter { it.showInNormalOverlay }
        if (focusedGameItemId != null || whetstoneLens) {
            val sourceIds = fieldIndex.nodesForItem(if (whetstoneLens) 91 else focusedGameItemId!!, rank)
            return categoryNodes.filter { marker -> marker.sourceGatheringNodeIds.any { it in sourceIds } }
        }
        if (rank == null) return categoryNodes
        val nodesWithRank = fieldIndex.fieldNodeIdsByNodeRank(rank)
        return categoryNodes.filter { marker -> marker.sourceGatheringNodeIds.isNotEmpty() && marker.sourceGatheringNodeIds.any { it in nodesWithRank } }
    }

    private fun com.waillio.mhp3rdcompanion.data.FieldSourceIndex.fieldNodeIdsByNodeRank(
        rank: com.waillio.mhp3rdcompanion.data.GeneratedMaterialRankContext
    ): Set<String> = dropsByNodeId.filterValues { drops -> drops.any { it.rankContext == rank } }.keys

    fun toggle(selected: Set<MapNodeCategory>, category: MapNodeCategory): Set<MapNodeCategory> = when {
        selected.isEmpty() -> setOf(category)
        category in selected -> selected - category
        else -> selected + category
    }

    fun scaledPosition(node: MapNode, renderedWidth: Float, renderedHeight: Float): Pair<Float, Float> =
        node.x * renderedWidth to node.y * renderedHeight
}
