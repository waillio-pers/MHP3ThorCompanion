package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.GeneratedGatheringMethod
import com.waillio.mhp3rdcompanion.data.GeneratedMaterialRankContext
import com.waillio.mhp3rdcompanion.data.structuralGatheringContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import kotlinx.serialization.json.Json

class MapSourceLinkageTest {
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var index: com.waillio.mhp3rdcompanion.data.FieldSourceIndex
    private lateinit var productionFile: File

    @Before
    fun setUp() {
        val file = sequenceOf(
            File("data/generated/mhp3rd-data.json"),
            File("../data/generated/mhp3rd-data.json")
        ).firstOrNull(File::exists) ?: error("production JSON not found")
        productionFile = file
        val generated = json.decodeFromString<com.waillio.mhp3rdcompanion.data.GeneratedDataset>(file.readText())
        index = com.waillio.mhp3rdcompanion.data.FieldSourceIndex.build(
            generated.gatheringNodes,
            generated.gatheringDrops,
            generated.items.mapNotNull { item -> item.gameItemId?.let { it to item.name } }.toMap(),
            MapSourceCrosswalk.sourceToMarkers,
            MapSourceCrosswalk.specialSourceNodeIds,
            MapSourceCrosswalk.specialSourceReasonByNodeId
        )
    }

    @Test
    fun productionFieldGraphBaselineIsUnchanged() {
        assertEquals(236, index.gatheringNodeById.size)
        assertEquals(2132, index.dropsByNodeId.values.sumOf { it.size })
    }

    @Test
    fun productionJsonSchemaAndShaRemainStable() {
        val digest = MessageDigest.getInstance("SHA-256").digest(productionFile.readBytes())
            .joinToString("") { byte -> "%02X".format(byte) }
        assertEquals("657CFA9376FC8CBE56C67E67D4D1A27C455D21021F51E21B62427C8DDBB26155", digest)
        assertEquals(31, json.decodeFromString<com.waillio.mhp3rdcompanion.data.GeneratedDataset>(productionFile.readText()).schemaVersion)
    }

    @Test
    fun everyCrosswalkEntryResolvesToItsMapLocation() {
        val nodes = index.gatheringNodeById
        assertEquals(215, MapSourceCrosswalk.markerToSource.size)
        assertEquals(215, MapSourceCrosswalk.markerToSource.values.toSet().size)
        assertTrue(MapSourceCrosswalk.markerToSource.values.all { it in nodes })
        val markersById = MapRegistry.maps.flatMap { map -> map.nodes.map { map.mapId to it } }.associateBy { it.second.nodeId }
        MapSourceCrosswalk.markerToSource.forEach { (markerId, sourceId) ->
            val marker = requireNotNull(markersById[markerId])
            assertEquals(marker.first, nodes.getValue(sourceId).locationId)
            assertTrue(sameArea(requireNotNull(markersById[markerId]).second.areaNumber, nodes.getValue(sourceId).area))
            assertTrue(sourceId !in MapSourceCrosswalk.specialSourceNodeIds)
        }
    }

    @Test
    fun curatedLinkCountsAndPartialClosureAreExplicit() {
        val markersByMap = MapRegistry.maps.associate { map ->
            map.mapId to map.nodes.count { it.nodeId in MapSourceCrosswalk.markerToSource }
        }
        assertEquals(
            mapOf(
                "deserted_island" to 42,
                "flooded_forest" to 43,
                "misty_peaks" to 37,
                "sandy_plains" to 35,
                "tundra" to 25,
                "volcano" to 33
            ),
            markersByMap
        )
        assertEquals(215, markersByMap.values.sum())
        assertEquals(17, MapSourceCrosswalk.specialSourceNodeIds.size)
        assertTrue(MapSourceCrosswalk.specialSourceNodeIds.all { it in index.gatheringNodeById })
        assertTrue(MapSourceCrosswalk.specialSourceNodeIds.none(MapSourceCrosswalk::hasMarkerForSource))
        assertEquals(0, MapSourceCrosswalk.knownMissingOrdinarySourceNodeIds.size)

        val allMarkers = MapRegistry.maps.flatMap { it.nodes }.map { it.nodeId }.toSet()
        val unresolved = allMarkers - MapSourceCrosswalk.markerToSource.keys - MapSourceCrosswalk.equivalentMarkerToSources.keys - MapSourceCrosswalk.decorativeMarkerIds - MapSourceCrosswalk.deprecatedDuplicateMarkerIds
        assertEquals(0, unresolved.size)
        assertTrue(MapSourceCrosswalk.deprecatedDuplicateMarkerIds.all { it in allMarkers && it !in MapSourceCrosswalk.markerToSource })
        val exactSources = MapSourceCrosswalk.markerToSource.values.toSet()
        val equivalentSources = MapSourceCrosswalk.equivalentGroups.values.flatMap { it.sourceGatheringNodeIds }.toSet()
        assertEquals(215, exactSources.size)
        assertEquals(4, equivalentSources.size)
        assertEquals(
            236,
            exactSources.size + equivalentSources.size +
                MapSourceCrosswalk.knownMissingOrdinarySourceNodeIds.size + MapSourceCrosswalk.specialSourceNodeIds.size
        )
    }

    @Test
    fun suppliedExactDeltaIsFullyApplied() {
        val deltaMarkerIds = setOf(
            "mp-a9-bugs-west", "mp-a9-bugs-north", "sp-a9-plants-north", "sp-a9-plants-south",
            "sp-a5-bones-west", "sp-a5-bones-east", "sp-a4-mining-north", "sp-a4-mining-south",
            "sp-3-plants-north", "sp-a3-plants-south", "sp-a6-mining-east", "sp-a6-mining-north",
            "sp-11-mining-west", "sp-a11-mining-east", "ff-a1-plants-west", "ff-a1-plants-east",
            "ff-a2-berries-south", "ff-a2-berries-north", "ff-a3-plants-east", "ff-a3-plants-west",
            "ff-a4-plants-west", "ff-a4-plants-east", "ff-a8-mining-south", "ff-a8-mining-north",
            "tu-a4-mining-north", "tu-a4-mining-south", "tu-a5-mining-west", "tu-a5-mining-north",
            "tu-a5-mining-east", "tu-a7-mining-north", "tu-a7-mining-south", "di-a1-plants-east",
            "di-a1-plants-west", "di-a3-mushrooms-south", "di-a3-mushrooms-north", "di-a9-berries-north",
            "di-a9-berries-south", "di-a4-mining-east", "di-a4-mining-south", "di-a4-mining-north"
        )
        assertEquals(40, deltaMarkerIds.size)
        assertTrue(deltaMarkerIds.all { it in MapSourceCrosswalk.markerToSource })
    }

    @Test
    fun equivalentGroupsAreMechanicalAndManyToMany() {
        assertEquals(2, MapSourceCrosswalk.equivalentGroups.size)
        assertEquals(4, MapSourceCrosswalk.equivalentMarkerToSources.size)
        assertEquals(setOf("mp-a3-bamboo-shoot-north", "mp-a3-bamboo-shoot-south"), MapSourceCrosswalk.equivalentGroups.getValue("eq_misty_peaks_area3_bamboo").markerIds)
        assertEquals(setOf("sp-a8-plants-west", "sp-a8-plants-east"), MapSourceCrosswalk.equivalentGroups.getValue("eq_sandy_plains_area8_cactus").markerIds)
        assertMechanicalRowsEqual(
            "gathering_misty_peaks_area_3_point_2_gathering",
            "gathering_misty_peaks_area_3_point_3_gathering"
        )
        assertMechanicalRowsEqual(
            "gathering_sandy_plains_area_8_point_1_gathering",
            "gathering_sandy_plains_area_8_point_2_gathering"
        )
        MapSourceCrosswalk.equivalentGroups.values.forEach { group ->
            group.markerIds.forEach { markerId ->
                assertEquals(group.sourceGatheringNodeIds, MapSourceCrosswalk.sourceIdsForMarker(markerId))
                assertEquals(MapMarkerIdentity.EQUIVALENT, MapSourceCrosswalk.identityForMarker(markerId))
            }
            group.sourceGatheringNodeIds.forEach { sourceId ->
                assertEquals(group.markerIds, MapSourceCrosswalk.sourceToMarkers.getValue(sourceId))
            }
        }
    }

    @Test
    fun equivalentMarkersAreCountedOncePerPhysicalLocation() {
        val misty = requireNotNull(MapRegistry.resolve("misty_peaks"))
        val sourceIds = MapSourceCrosswalk.equivalentGroups.getValue("eq_misty_peaks_area3_bamboo").sourceGatheringNodeIds
        val visible = MapOverlayLogic.visibleNodes(misty, emptySet(), index, focusedGameItemId = 691)
        assertEquals(2, misty.nodes.count { it.sourceGatheringNodeIds.any(sourceIds::contains) })
        assertEquals(2, visible.count { it.nodeId in setOf("mp-a3-bamboo-shoot-north", "mp-a3-bamboo-shoot-south") })
        assertEquals(visible.distinctBy { it.nodeId }.size, visible.size)
        val coverage = index.sourceCoverageForItemOnMap(691, "misty_peaks")
        assertEquals(2, coverage.mappedCount)
        assertEquals(setOf("mp-a3-bamboo-shoot-north", "mp-a3-bamboo-shoot-south"), coverage.mappedMarkerIds)

        val sandy = requireNotNull(MapRegistry.resolve("sandy_plains"))
        val cactusVisible = MapOverlayLogic.visibleNodes(sandy, emptySet(), index, focusedGameItemId = 159)
        assertEquals(2, cactusVisible.count { it.nodeId in setOf("sp-a8-plants-west", "sp-a8-plants-east") })
    }

    private fun assertMechanicalRowsEqual(firstNodeId: String, secondNodeId: String) {
        GeneratedMaterialRankContext.entries.forEach { rank ->
            assertEquals(mechanicalRows(firstNodeId, rank), mechanicalRows(secondNodeId, rank))
        }
    }

    private fun mechanicalRows(nodeId: String, rank: GeneratedMaterialRankContext): List<String> =
        index.dropsByNodeId[nodeId].orEmpty()
            .filter { it.rankContext == rank }
            .map { drop ->
                listOf(
                    drop.gameItemId.toString(), drop.quantity.toString(), drop.quantityExplicit.toString(),
                    drop.probability.valuePercent?.toString() ?: "null", drop.probability.semantics.name
                ).joinToString("|")
            }
            .sorted()

    private fun sameArea(markerArea: String?, sourceArea: String): Boolean =
        markerArea.equals(sourceArea, ignoreCase = true) ||
            (markerArea.equals("c", ignoreCase = true) && sourceArea.equals("BASE_CAMP", ignoreCase = true))

    @Test
    fun itemFocusUsesNumericIdentityAndWhetstoneSpansPhysicalMethods() {
        assertTrue(index.nodesForItem(218).isNotEmpty()) // Iron Ore
        val whetstoneMethods = index.nodesForItem(91).mapNotNull { index.gatheringNodeById[it]?.method }.toSet()
        assertTrue(GeneratedGatheringMethod.GATHERING in whetstoneMethods)
        assertTrue(GeneratedGatheringMethod.MINING in whetstoneMethods)
        assertTrue(index.nodesForItem(233, GeneratedMaterialRankContext.LOW).isNotEmpty()) // Ice Crystal
    }

    @Test
    fun itemFocusRankContextsIgnoreUnplacedOrdinarySources() {
        assertEquals(
            setOf(GeneratedMaterialRankContext.HIGH),
            index.availableMappedOrSpecialRanksForItemOnMap(229, "flooded_forest") // Bathycite Ore
        )
        assertEquals(
            setOf(GeneratedMaterialRankContext.LOW),
            index.availableMappedOrSpecialRanksForItemOnMap(201, "sandy_plains") // Insect Husk
        )
        assertEquals(
            setOf(GeneratedMaterialRankContext.LOW, GeneratedMaterialRankContext.HIGH),
            index.availableMappedOrSpecialRanksForItemOnMap(217, "misty_peaks") // Stone
        )
    }

    @Test
    fun whetstoneLensPreservesPhysicalMiningIconAndUsesWhetstoneIconForGathering() {
        val map = requireNotNull(MapRegistry.resolve("flooded_forest"))
        val gatheringMarker = map.nodes.first { it.nodeId == "ff-a2-plants" }
        val miningMarker = map.nodes.first { it.nodeId == "ff-a10-mining" }
        assertEquals(MapNodeCategory.WHETSTONE, whetstoneIconCategory(gatheringMarker, index))
        assertEquals(MapNodeCategory.MINING, whetstoneIconCategory(miningMarker, index))
        val lensNodes = MapOverlayLogic.visibleNodes(map, emptySet(), index, whetstoneLens = true)
        assertTrue(lensNodes.any { it.nodeId == gatheringMarker.nodeId })
        assertTrue(lensNodes.any { it.nodeId == miningMarker.nodeId })
    }

    @Test
    fun bathyciteFloodedForestIncludesArea6MiningAndSpecialSource() {
        val bathycite = index.nodesForItemOnMap(229, "flooded_forest")
        assertEquals(
            setOf(
                "gathering_flooded_forest_area_1_point_4_mining",
                "gathering_flooded_forest_area_3_point_4_mining",
                "gathering_flooded_forest_area_6_point_2_mining",
                "gathering_flooded_forest_area_6_point_5_mining"
            ),
            bathycite
        )
        val coverage = index.sourceCoverageForItemOnMap(229, "flooded_forest", GeneratedMaterialRankContext.HIGH)
        assertEquals(3, coverage.mappedCount)
        assertEquals(1, coverage.specialCount)
        assertEquals(0, coverage.unplacedCount)
        assertEquals("SECRET_AREA", coverage.specialReasonCounts.keys.single())
        assertTrue(MapSourceCrosswalk.hasMarkerForSource("gathering_flooded_forest_area_6_point_2_mining"))
        assertTrue(MapSourceCrosswalk.hasMarkerForSource("gathering_flooded_forest_area_6_point_3_mining"))
    }

    @Test
    fun suppliedDisplayCorrectionsRemainStable() {
        val island = requireNotNull(MapRegistry.resolve("deserted_island"))
        val corrected = island.nodes.first { it.nodeId == "di-a10-mushrooms" }
        assertEquals(MapNodeCategory.FISH, corrected.category)
        assertEquals("gathering_deserted_island_area_10_point_5_fishing", corrected.sourceGatheringNodeIds.single())

        val forest = requireNotNull(MapRegistry.resolve("flooded_forest"))
        assertTrue(forest.nodes.first { it.nodeId == "ff-a8-bones" }.showInNormalOverlay)
        assertTrue(!forest.nodes.first { it.nodeId == "ff-a8-misc" }.showInNormalOverlay)
        assertTrue(forest.nodes.first { it.nodeId == "ff-a8-misc" }.sourceGatheringNodeIds.isEmpty())
    }

    @Test
    fun nodeSourceIdentityUsesAreaPointWithoutRawDescriptors() {
        assertEquals(
            "Area 6 · Point 2",
            structuralGatheringContext(
                "gathering_flooded_forest_area_6_point_2_mining",
                "6",
                "②\n採掘"
            )
        )
    }

    @Test
    fun calibrationCoordinatesDoNotParticipateInCrosswalk() {
        val before = MapSourceCrosswalk.markerToSource
        val moved = MapRegistry.maps.flatMap { it.nodes }.map { it.copy(x = (it.x + .01f).coerceAtMost(1f)) }
        assertEquals(before, MapSourceCrosswalk.markerToSource)
        assertEquals(225, moved.size)
    }

    @Test
    fun c3FinalFourMarkersUseCalibratedCoordinatesAndExactSources() {
        val expected = mapOf(
            "mp-a3-spider-web" to Triple("misty_peaks", "gathering_misty_peaks_area_3_point_1_gathering", 0.4544f to 0.7076f),
            "ff-a4-fish" to Triple("flooded_forest", "gathering_flooded_forest_area_4_point_5_fishing", 0.6630f to 0.6353f),
            "ff-a10-dung" to Triple("flooded_forest", "gathering_flooded_forest_area_10_point_3_gathering", 0.6091f to 0.1015f),
            "di-a6-bones-west" to Triple("deserted_island", "gathering_deserted_island_area_6_point_1_gathering", 0.4118f to 0.4577f)
        )
        val markers = MapRegistry.maps.flatMap { map -> map.nodes.map { map.mapId to it } }.associateBy { it.second.nodeId }
        expected.forEach { (markerId, expectation) ->
            val marker = requireNotNull(markers[markerId])
            assertEquals(expectation.first, marker.first)
            assertEquals(expectation.second, MapSourceCrosswalk.markerToSource[markerId])
            assertEquals(expectation.third.first, marker.second.x, 0.00001f)
            assertEquals(expectation.third.second, marker.second.y, 0.00001f)
            assertEquals(MapMarkerIdentity.EXACT, MapSourceCrosswalk.identityForMarker(markerId))
            assertEquals(expectation.second, marker.second.sourceGatheringNodeIds.single())
        }
    }

    @Test
    fun c3SourceCoverageHasNoOrdinaryUnplacedNodes() {
        val exactSources = MapSourceCrosswalk.markerToSource.values.toSet()
        val equivalentSources = MapSourceCrosswalk.equivalentGroups.values.flatMap { it.sourceGatheringNodeIds }.toSet()
        assertEquals(215, exactSources.size)
        assertEquals(4, equivalentSources.size)
        assertEquals(0, MapSourceCrosswalk.knownMissingOrdinarySourceNodeIds.size)
        assertEquals(236, exactSources.size + equivalentSources.size + MapSourceCrosswalk.specialSourceNodeIds.size)
    }

    @Test
    fun c3ItemFocusResolvesFinalSourcesByNumericGameItemId() {
        val cases = listOf(
            Triple("misty_peaks", "mp-a3-spider-web", "gathering_misty_peaks_area_3_point_1_gathering"),
            Triple("flooded_forest", "ff-a4-fish", "gathering_flooded_forest_area_4_point_5_fishing"),
            Triple("flooded_forest", "ff-a10-dung", "gathering_flooded_forest_area_10_point_3_gathering"),
            Triple("deserted_island", "di-a6-bones-west", "gathering_deserted_island_area_6_point_1_gathering")
        )
        cases.forEach { (mapId, markerId, sourceId) ->
            val gameItemId = index.dropsByNodeId.getValue(sourceId).first().gameItemId
            val map = requireNotNull(MapRegistry.resolve(mapId))
            val visible = MapOverlayLogic.visibleNodes(map, emptySet(), index, focusedGameItemId = gameItemId)
            assertTrue("$sourceId should resolve to $markerId", visible.any { it.nodeId == markerId })
            assertTrue(index.nodesForItemOnMap(gameItemId, mapId).contains(sourceId))
        }
    }
}
