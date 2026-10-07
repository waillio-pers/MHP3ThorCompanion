package com.waillio.mhp3rdcompanion

import androidx.compose.ui.unit.IntOffset
import com.waillio.mhp3rdcompanion.data.FieldSourceIndex
import com.waillio.mhp3rdcompanion.data.GeneratedGatheringMethod
import com.waillio.mhp3rdcompanion.data.structuralGatheringContext
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** One currently visible, selectable marker whose actual circular hit region contains a tap. */
internal data class MapTapCandidate(
    val node: MapNode,
    val distanceSquaredPx: Float
) {
    val distancePx: Float get() = sqrt(distanceSquaredPx)
}

internal sealed interface MapTapResolution {
    data object None : MapTapResolution
    data class Open(val candidate: MapTapCandidate) : MapTapResolution
    data class Choose(val candidates: List<MapTapCandidate>) : MapTapResolution
}

/** Shared embedded/fullscreen tap resolver. Coordinates are local to the rendered map square. */
internal object MapTapResolver {
    fun candidates(
        visibleNodes: List<MapNode>,
        tapXpx: Float,
        tapYpx: Float,
        mapSizePx: Float,
        hitRadiusPx: Float
    ): List<MapTapCandidate> {
        if (!tapXpx.isFinite() || !tapYpx.isFinite() || mapSizePx <= 0f || hitRadiusPx < 0f) return emptyList()
        val radiusSquared = hitRadiusPx * hitRadiusPx
        return visibleNodes.asSequence()
            .filter { it.showInNormalOverlay && it.sourceGatheringNodeIds.isNotEmpty() }
            .map { node ->
                val dx = node.x * mapSizePx - tapXpx
                val dy = node.y * mapSizePx - tapYpx
                MapTapCandidate(node, dx * dx + dy * dy)
            }
            .filter { it.distanceSquaredPx <= radiusSquared }
            .sortedWith(compareBy<MapTapCandidate> { it.distanceSquaredPx }.thenBy { it.node.nodeId })
            .toList()
    }

    fun resolve(
        visibleNodes: List<MapNode>,
        tapXpx: Float,
        tapYpx: Float,
        mapSizePx: Float,
        hitRadiusPx: Float
    ): MapTapResolution {
        val candidates = candidates(visibleNodes, tapXpx, tapYpx, mapSizePx, hitRadiusPx)
        return when (candidates.size) {
            0 -> MapTapResolution.None
            1 -> MapTapResolution.Open(candidates.single())
            else -> MapTapResolution.Choose(candidates)
        }
    }
}

/** Safe chooser copy reuses the source identity and equivalent-marker wording already in the app. */
internal data class MapTapChooserEntry(
    val candidate: MapTapCandidate,
    val primaryLabel: String,
    val secondaryLabel: String?,
    val iconCategory: MapNodeCategory,
    val equivalentGroupId: String?,
    /** A temporary display index only; never a physical point ordinal. */
    val chooserChoiceIndex: Int? = null
)

internal object MapTapChooserPresentation {
    fun entries(
        candidates: List<MapTapCandidate>,
        fieldIndex: FieldSourceIndex,
        whetstoneLens: Boolean
    ): List<MapTapChooserEntry> {
        val entries = candidates.map { candidate ->
            val node = candidate.node
            val equivalent = MapSourceCrosswalk.equivalentGroupForMarker(node.nodeId)
            val source = node.sourceGatheringNodeIds.sorted().firstNotNullOfOrNull(fieldIndex.gatheringNodeById::get)
            val primary = equivalent?.header ?: source?.let {
                "${structuralGatheringContext(it.id, it.area, it.pointIndex)} · ${it.method.chooserMethodLabel()}"
            } ?: "${node.areaNumber?.let { if (it.equals("c", true)) "Base Camp" else "Area $it" } ?: "Area"} · ${node.label ?: node.category.displayName}"
            val methodLabel = source?.method?.chooserMethodLabel()
            val subtype = when {
                equivalent != null -> null
                !node.label.isNullOrBlank() && node.label != primary -> node.label
                node.category != MapNodeCategory.MISC && node.category.displayName != methodLabel -> node.category.displayName
                else -> null
            }
            MapTapChooserEntry(
                candidate = candidate,
                primaryLabel = primary,
                secondaryLabel = subtype,
                iconCategory = if (whetstoneLens) whetstoneIconCategory(node, fieldIndex) else node.category,
                equivalentGroupId = equivalent?.groupId
            )
        }
        val choiceIndexes = entries.indices
            .filter { entries[it].equivalentGroupId != null }
            .groupBy { entries[it].equivalentGroupId }
            .filterValues { it.size > 1 }
            .values
            .flatMap { indexes -> indexes.mapIndexed { index, entryIndex -> entryIndex to index + 1 } }
            .toMap()
        return entries.mapIndexed { index, entry ->
            entry.copy(chooserChoiceIndex = choiceIndexes[index])
        }
    }
}

/** Place a floating chooser beside the tap, then keep its full measured bounds in the viewport. */
internal fun mapTapChooserOriginPx(
    tapXpx: Float,
    tapYpx: Float,
    panelWidthPx: Int,
    panelHeightPx: Int,
    viewportWidthPx: Int,
    viewportHeightPx: Int,
    marginPx: Int,
    gapPx: Int
): IntOffset {
    val margin = marginPx.coerceAtLeast(0)
    val xAfter = tapXpx + gapPx
    val xBefore = tapXpx - gapPx - panelWidthPx
    val preferredX = if (xAfter + panelWidthPx <= viewportWidthPx - margin) xAfter else xBefore
    val yAfter = tapYpx + gapPx
    val yBefore = tapYpx - gapPx - panelHeightPx
    val preferredY = if (yAfter + panelHeightPx <= viewportHeightPx - margin) yAfter else yBefore
    val maxX = (viewportWidthPx - panelWidthPx - margin).coerceAtLeast(margin)
    val maxY = (viewportHeightPx - panelHeightPx - margin).coerceAtLeast(margin)
    return IntOffset(
        preferredX.roundToInt().coerceIn(margin, maxX),
        preferredY.roundToInt().coerceIn(margin, maxY)
    )
}

private fun GeneratedGatheringMethod.chooserMethodLabel(): String = when (this) {
    GeneratedGatheringMethod.GATHERING -> "Gathering"
    GeneratedGatheringMethod.MINING -> "Mining"
    GeneratedGatheringMethod.BUGNET -> "Bug Gathering"
    GeneratedGatheringMethod.FISHING -> "Fishing"
}
