package com.waillio.mhp3rdcompanion

import androidx.compose.ui.unit.IntOffset
import com.waillio.mhp3rdcompanion.data.FieldSourceIndex
import com.waillio.mhp3rdcompanion.data.GeneratedGatheringMethod
import com.waillio.mhp3rdcompanion.data.GeneratedGatheringNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapTapResolutionTest {
    @Test fun zeroAndOneCandidateHaveNoChooserAndKeepDirectOpenBehavior() {
        val node = selectable("one", x = .5f, y = .5f)
        assertTrue(MapTapResolver.resolve(listOf(node), 5f, 5f, 100f, 10f) is MapTapResolution.None)
        val result = MapTapResolver.resolve(listOf(node), 50f, 50f, 100f, 10f)
        assertTrue(result is MapTapResolution.Open)
        assertEquals("one", (result as MapTapResolution.Open).candidate.node.nodeId)
    }

    @Test fun twoAndFiveCandidatesAreOnlyThoseContainingTheSpecificTap() {
        val first = selectable("first", .45f, .5f)
        val second = selectable("second", .55f, .5f)
        val two = MapTapResolver.resolve(listOf(first, second), 50f, 50f, 100f, 10f)
        assertTrue(two is MapTapResolution.Choose)
        assertEquals(listOf("first", "second"), (two as MapTapResolution.Choose).candidates.map { it.node.nodeId })

        val actual = MapRegistry.resolve("deserted_island")!!
        val areaFourMaximumFivePoint = MapTapResolver.resolve(
            MapOverlayLogic.visibleNodes(actual, emptySet()),
            tapXpx = 271.8288f,
            tapYpx = 458.2732f,
            mapSizePx = 830f,
            hitRadiusPx = 30f
        ) as MapTapResolution.Choose
        assertEquals(5, areaFourMaximumFivePoint.candidates.size)
        assertEquals(
            listOf("di-a4-mining-east", "di-a4-mining-south", "di-a4-barrel", "di-a4-mining-north", "di-a4-bugs"),
            areaFourMaximumFivePoint.candidates.map { it.node.nodeId }
        )
    }

    @Test fun coincidentFloodedForestMarkersAreBothSelectableAndSortedByStableId() {
        val map = MapRegistry.resolve("flooded_forest")!!
        val nodes = MapOverlayLogic.visibleNodes(map, emptySet())
        val exact = nodes.first { it.nodeId == "ff-a10-bones" }
        val result = MapTapResolver.resolve(nodes, exact.x * 830f, exact.y * 830f, 830f, 30f)
        assertTrue(result is MapTapResolution.Choose)
        val candidates = (result as MapTapResolution.Choose).candidates
        assertTrue(candidates.map { it.node.nodeId }.containsAll(listOf("ff-a10-bones", "ff-a10-misc")))
        assertEquals(0f, candidates.first { it.node.nodeId == "ff-a10-bones" }.distanceSquaredPx, 0f)
        assertEquals(0f, candidates.first { it.node.nodeId == "ff-a10-misc" }.distanceSquaredPx, 0f)
    }

    @Test fun decorativeAndHiddenMarkersNeverEnterSelectableCandidateSet() {
        val misty = MapRegistry.resolve("misty_peaks")!!
        val felyne = misty.nodes.first { it.nodeId == "mp-a3-felyne" }
        val visible = MapOverlayLogic.visibleNodes(misty, emptySet())
        val atDecorativeCenter = MapTapResolver.candidates(visible, felyne.x * 830f, felyne.y * 830f, 830f, 30f)
        assertFalse(atDecorativeCenter.any { it.node.nodeId == "mp-a3-felyne" })
        assertTrue(atDecorativeCenter.all { it.node.sourceGatheringNodeIds.isNotEmpty() })

        val flooded = MapRegistry.resolve("flooded_forest")!!
        assertFalse(MapOverlayLogic.visibleNodes(flooded, emptySet()).any { it.nodeId == "ff-a8-misc" })
        assertFalse(
            MapTapResolver.candidates(flooded.nodes, 0f, 0f, 830f, 30f).any { it.node.nodeId == "ff-a8-misc" }
        )
    }

    @Test fun categoryFilteringRunsBeforeTapCandidateResolution() {
        val map = MapRegistry.resolve("flooded_forest")!!
        val bonesOnly = MapOverlayLogic.visibleNodes(map, setOf(MapNodeCategory.BONES))
        val bone = bonesOnly.first { it.nodeId == "ff-a10-bones" }
        val result = MapTapResolver.resolve(bonesOnly, bone.x * 830f, bone.y * 830f, 830f, 30f)
        assertTrue(result is MapTapResolution.Open)
        assertEquals("ff-a10-bones", (result as MapTapResolution.Open).candidate.node.nodeId)
    }

    @Test fun itemFocusAndWhetstoneLensUseOnlyTheirProjectedVisibleNodes() {
        val map = MapRegistry.resolve("deserted_island")!!
        val index = itemAndLensIndex(map)
        val focused = MapOverlayLogic.visibleNodes(map, emptySet(), index, focusedGameItemId = 91)
        assertEquals(setOf("di-a4-mining-east", "di-a4-mining-south", "di-a7-whetstone"), focused.map { it.nodeId }.toSet())
        val focusedPair = focused.filter { it.nodeId in setOf("di-a4-mining-east", "di-a4-mining-south") }
        val focusedTap = midpoint(focusedPair)
        val focusedResolution = MapTapResolver.resolve(focused, focusedTap.first, focusedTap.second, 830f, 30f)
        assertTrue(focusedResolution is MapTapResolution.Choose)
        assertEquals(2, (focusedResolution as MapTapResolution.Choose).candidates.size)

        val whetstone = MapOverlayLogic.visibleNodes(map, emptySet(), index, whetstoneLens = true)
        assertEquals(focused.map { it.nodeId }.toSet(), whetstone.map { it.nodeId }.toSet())
        val lensPair = whetstone.filter { it.nodeId in setOf("di-a4-mining-east", "di-a4-mining-south") }
        val lensTap = midpoint(lensPair)
        val lensResolution = MapTapResolver.resolve(whetstone, lensTap.first, lensTap.second, 830f, 30f)
        assertTrue(lensResolution is MapTapResolution.Choose)
        assertEquals(MapNodeCategory.MINING, whetstoneIconCategory(whetstone.first { it.nodeId == "di-a4-mining-east" }, index))
        assertEquals(MapNodeCategory.WHETSTONE, whetstoneIconCategory(whetstone.first { it.nodeId == "di-a7-whetstone" }, index))
    }

    @Test fun equivalentBambooUsesAcceptedSafeCopyAndOnlyTemporaryChoiceNumbers() {
        val map = MapRegistry.resolve("misty_peaks")!!
        val equivalent = map.nodes.filter { it.nodeId in setOf("mp-a3-bamboo-shoot-north", "mp-a3-bamboo-shoot-south") }
        val sources = MapSourceCrosswalk.equivalentGroups.getValue("eq_misty_peaks_area3_bamboo").sourceGatheringNodeIds
        val index = sourceIndex(sources.associateWith { source ->
            GeneratedGatheringNode(
                id = source, locationId = "misty_peaks", locationName = "Misty Peaks", area = "3", pointIndex = "?",
                method = GeneratedGatheringMethod.GATHERING, sourceId = source, sourceUrl = ""
            )
        })
        val tap = midpoint(equivalent)
        val candidates = MapTapResolver.candidates(equivalent, tap.first, tap.second, 830f, 30f)
        val entries = MapTapChooserPresentation.entries(candidates, index, whetstoneLens = false)
        assertEquals(2, entries.size)
        assertEquals(listOf("Area 3 · Bamboo Shoot · Gathering", "Area 3 · Bamboo Shoot · Gathering"), entries.map { it.primaryLabel })
        assertEquals(listOf(1, 2), entries.map { it.chooserChoiceIndex })
        assertTrue(entries.none { "Point 2" in it.primaryLabel || "Point 3" in it.primaryLabel })
    }

    @Test fun bambooPairRemainsAmbiguousAtAllSupportedScalesAndMapModes() {
        val map = MapRegistry.resolve("misty_peaks")!!
        val bamboo = map.nodes.filter { it.nodeId in setOf("mp-a3-bamboo-shoot-north", "mp-a3-bamboo-shoot-south") }
        val cases = listOf(
            881f to 24f, 830f to 30f, 807f to 33f,
            1080f to 31.2f, 1080f to 39f, 1080f to 42.9f
        )
        cases.forEach { (mapSizePx, radiusPx) ->
            val tap = midpoint(bamboo)
            val projectedTapX = tap.first / 830f * mapSizePx
            val projectedTapY = tap.second / 830f * mapSizePx
            val candidates = MapTapResolver.candidates(bamboo, projectedTapX, projectedTapY, mapSizePx, radiusPx)
            assertEquals(setOf("mp-a3-bamboo-shoot-north", "mp-a3-bamboo-shoot-south"), candidates.map { it.node.nodeId }.toSet())
        }
    }

    @Test fun chooserPlacementClampsAllEdgesInsideItsMapViewport() {
        val nearTopLeft = mapTapChooserOriginPx(3f, 4f, 280, 300, 500, 500, 12, 18)
        assertEquals(IntOffset(21, 22), nearTopLeft)
        val nearBottomRight = mapTapChooserOriginPx(495f, 496f, 280, 300, 500, 500, 12, 18)
        assertEquals(IntOffset(197, 178), nearBottomRight)
    }

    private fun selectable(id: String, x: Float, y: Float) = MapNode(
        nodeId = id, category = MapNodeCategory.MINING, x = x, y = y,
        areaNumber = "1", sourceGatheringNodeIds = setOf("gathering_$id")
    )

    private fun midpoint(nodes: List<MapNode>): Pair<Float, Float> {
        val a = nodes.first()
        val b = nodes.last()
        return (a.x + b.x) * 415f to (a.y + b.y) * 415f
    }

    private fun itemAndLensIndex(map: MapDefinition): FieldSourceIndex {
        val markerIds = listOf("di-a4-mining-east", "di-a4-mining-south", "di-a7-whetstone")
        val nodes = markerIds.flatMap { markerId ->
            val marker = map.nodes.first { it.nodeId == markerId }
            MapSourceCrosswalk.sourceIdsForMarker(markerId).map { sourceId ->
                GeneratedGatheringNode(
                    id = sourceId, locationId = map.mapId, locationName = map.displayName,
                    area = marker.areaNumber ?: "?", pointIndex = "?",
                    method = if (markerId == "di-a7-whetstone") GeneratedGatheringMethod.GATHERING else GeneratedGatheringMethod.MINING,
                    sourceId = sourceId, sourceUrl = ""
                )
            }
        }.associateBy { it.id }
        return sourceIndex(nodes, gameItemId = 91)
    }

    private fun sourceIndex(
        nodes: Map<String, GeneratedGatheringNode>,
        gameItemId: Int = 91
    ) = FieldSourceIndex(
        gatheringNodeById = nodes,
        dropsByNodeId = emptyMap(),
        fieldNodeIdsByGameItemId = mapOf(gameItemId to nodes.keys),
        fieldNodeIdsByGameItemIdAndRank = emptyMap(),
        mapMarkerIdsByGatheringNodeId = MapSourceCrosswalk.sourceToMarkers,
        itemNameByGameItemId = emptyMap()
    )
}
