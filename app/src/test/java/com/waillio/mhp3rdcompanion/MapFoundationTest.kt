package com.waillio.mhp3rdcompanion

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapFoundationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val mapNodes = SemanticsMatcher("runtime map nodes") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("map-node-ff-") == true
    }

    @Test fun registryUsesCleanBaseAndNormalizedRuntimeCoordinateData() {
        val map = requireNotNull(MapRegistry.resolve("flooded_forest"))
        assertEquals("Flooded Forest", map.displayName)
        assertEquals(256, map.imageWidth)
        assertEquals(256, map.imageHeight)
        assertEquals(170, map.sourceViewportWidth)
        assertEquals(170, map.sourceViewportHeight)
        assertEquals(R.drawable.map_flooded_forest, map.baseImageRes)
        assertEquals(44, map.nodes.size)
        assertTrue(map.nodes.all { it.x in 0f..1f && it.y in 0f..1f })
        assertEquals(MapNodeCategory.entries.toSet(), map.nodes.map { it.category }.toSet())

        val sample = map.nodes.first { it.nodeId == "ff-a5-mining" }
        val (x, y) = MapOverlayLogic.scaledPosition(sample, 500f, 400f)
        assertEquals(sample.x * 500f, x, .001f)
        assertEquals(sample.y * 400f, y, .001f)
    }

    @Test fun registryContainsAllSixHuntingMapsWithCandidateOverlays() {
        val expected = listOf(
            "misty_peaks",
            "sandy_plains",
            "flooded_forest",
            "tundra",
            "volcano",
            "deserted_island"
        )
        assertEquals(expected, MapRegistry.maps.map { it.mapId })
        assertTrue(MapRegistry.maps.all { it.nodes.isNotEmpty() })
        assertTrue(MapRegistry.maps.flatMap { it.nodes }.all { it.x in 0f..1f && it.y in 0f..1f })
        assertTrue(MapRegistry.maps.all { it.sourceViewportWidth == 170 && it.sourceViewportHeight == 170 })
        assertEquals(MapRegistry.maps.size, MapRegistry.maps.map { it.baseImageRes }.distinct().size)
    }

    @Test fun editedDesktopDatasetIsImportedCompletelyIncludingCamp() {
        val expectedCounts = mapOf(
            "misty_peaks" to 40,
            "sandy_plains" to 38,
            "flooded_forest" to 44,
            "tundra" to 26,
            "volcano" to 34,
            "deserted_island" to 43
        )
        assertEquals(225, MapRegistry.maps.sumOf { it.nodes.size })
        assertEquals(expectedCounts, MapRegistry.maps.associate { it.mapId to it.nodes.size })
        assertTrue(MapRegistry.maps.all { map -> map.nodes.size == map.nodes.map { it.nodeId }.distinct().size })
        assertEquals("c", requireNotNull(MapRegistry.resolve("flooded_forest")).nodes.first { it.nodeId == "ff-c-fish" }.areaNumber)
        assertEquals("c", requireNotNull(MapRegistry.resolve("tundra")).nodes.first { it.areaNumber == "c" }.areaNumber)
    }

    @Test fun categorySelectionSupportsAllSingleAndSubsetFiltering() {
        val map = requireNotNull(MapRegistry.resolve("flooded_forest"))
        // ff-a8-misc is retained for calibration identity but hidden as the
        // deprecated duplicate of the canonical ff-a8-bones marker.
        assertEquals(43, MapOverlayLogic.visibleNodes(map, emptySet()).size)

        val mining = MapOverlayLogic.toggle(emptySet(), MapNodeCategory.MINING)
        assertEquals(setOf(MapNodeCategory.MINING), mining)
        assertEquals(8, MapOverlayLogic.visibleNodes(map, mining).size)
        assertTrue(MapOverlayLogic.visibleNodes(map, mining).all { it.category == MapNodeCategory.MINING })

        val subset = MapOverlayLogic.toggle(mining, MapNodeCategory.WHETSTONE)
        assertEquals(setOf(MapNodeCategory.MINING, MapNodeCategory.WHETSTONE), subset)
        assertEquals(9, MapOverlayLogic.visibleNodes(map, subset).size)
        assertEquals(1, MapOverlayLogic.visibleNodes(map, setOf(MapNodeCategory.WHETSTONE)).size)
    }

    @Test fun mapsEntryIndexDetailAndRuntimeOverlaysRenderOnAcceptedProfile() {
        openMaps()
        rule.onNodeWithTag("maps-index").assertIsDisplayed()
        rule.onNodeWithTag("maps-list").performScrollToNode(hasTestTag("map-card-flooded_forest"))
        rule.onNodeWithTag("map-card-flooded_forest").assertIsDisplayed()
        rule.onNodeWithTag("map-card-flooded_forest").performClick()

        rule.onNodeWithTag("map-detail-flooded_forest").assertIsDisplayed()
        rule.onNodeWithTag("map-base-image").assertIsDisplayed()
        rule.onAllNodes(mapNodes, useUnmergedTree = true).assertCountEquals(43)
        val viewport = rule.onNodeWithTag("map-viewport").fetchSemanticsNode().boundsInRoot
        val overlay = rule.onNodeWithTag("map-overlay").fetchSemanticsNode().boundsInRoot
        assertTrue(overlay.width <= viewport.width && overlay.height <= viewport.height)
        assertEquals(overlay.width, overlay.height, 1f)
    }

    @Test fun allSixMapCardsOpenTheSharedDetailRenderer() {
        openMaps()
        rule.onNodeWithText("6 available").assertDoesNotExist()
        MapRegistry.maps.forEach { map ->
            rule.onNodeWithTag("maps-list").performScrollToNode(hasTestTag("map-card-${map.mapId}"))
            rule.onNodeWithTag("map-card-${map.mapId}").performClick()
            rule.onNodeWithTag("map-detail-${map.mapId}").assertIsDisplayed()
            rule.onNodeWithTag("map-base-image").assertIsDisplayed()
            rule.onNodeWithTag("map-fullscreen-open").assertIsDisplayed()
            rule.onNodeWithTag("map-debug-toggle").assertDoesNotExist()
            rule.onNodeWithTag("map-back").performClick()
            rule.onNodeWithTag("maps-index").assertIsDisplayed()
        }
    }

    @Test fun miningWhetstoneAndAllFiltersChangeRuntimeNodeSet() {
        openMapDetail()
        rule.onNodeWithTag("map-filter-mining").performClick()
        rule.onAllNodes(mapNodes, useUnmergedTree = true).assertCountEquals(8)
        rule.onNodeWithTag("map-node-ff-a1-mining", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("map-node-ff-a7-honey", useUnmergedTree = true).assertDoesNotExist()

        // Whetstone is a practical item lens, not a physical map category.
        rule.onNodeWithTag("map-filter-whetstone").assertExists().performClick()
        rule.onNodeWithTag("map-rank-low").assertDoesNotExist()
        rule.onNodeWithTag("map-rank-high").assertDoesNotExist()
        assertTrue(rule.onAllNodes(mapNodes, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())

        rule.onNodeWithTag("map-filter-all").performClick()
        rule.onAllNodes(mapNodes, useUnmergedTree = true).assertCountEquals(43)
    }

    @Test fun mapBackReturnsToIndexWithoutRegressingFieldGuide() {
        openMapDetail()
        rule.onNodeWithTag("map-back").performClick()
        rule.onNodeWithTag("maps-index").assertIsDisplayed()
        rule.onNodeWithTag("nav-search").assertIsDisplayed()
        rule.onNodeWithTag("field-filter-monster").assertExists()
    }

    @Test fun fullscreenMapOpensAboveShellAndClosesBackToDetail() {
        openMapDetail()
        rule.onNodeWithTag("map-fullscreen-open").performClick()
        rule.onNodeWithTag("map-fullscreen").assertIsDisplayed()
        rule.onNodeWithTag("map-fullscreen-close").assertIsDisplayed().performClick()
        rule.onNodeWithTag("map-detail-flooded_forest").assertIsDisplayed()
        rule.onNodeWithTag("map-fullscreen").assertDoesNotExist()
    }

    @Test fun equivalentBambooAndCactusMarkersOpenHonestDeduplicatedDetails() {
        openMaps()
        rule.onNodeWithTag("map-card-misty_peaks").performClick()
        rule.onNodeWithTag("map-node-mp-a3-bamboo-shoot-north", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        rule.onNodeWithTag("map-tap-chooser").assertIsDisplayed()
        rule.onNodeWithTag("map-tap-chooser-row-mp-a3-bamboo-shoot-north").assertExists()
        rule.onNodeWithTag("map-tap-chooser-row-mp-a3-bamboo-shoot-south").assertExists()
        rule.onNodeWithTag("map-tap-chooser-row-mp-a3-bamboo-shoot-north").performClick()
        rule.onNodeWithTag("map-node-detail-modal").assertIsDisplayed()
        rule.onNodeWithText("Area 3 · Bamboo Shoot · Gathering").assertIsDisplayed()
        rule.onNodeWithText("Area 3 · Point 2 · Gathering").assertDoesNotExist()
        rule.onNodeWithText("Area 3 · Point 3 · Gathering").assertDoesNotExist()
        rule.onNodeWithTag("map-node-detail-item-691").assertIsDisplayed()
        rule.onNodeWithTag("map-node-detail-close").performClick()
        rule.onNodeWithTag("map-node-mp-a3-bamboo-shoot-south", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        rule.onNodeWithTag("map-tap-chooser-row-mp-a3-bamboo-shoot-south").performClick()
        rule.onNodeWithText("Area 3 · Bamboo Shoot · Gathering").assertIsDisplayed()
        rule.onNodeWithTag("map-node-detail-close").performClick()
        rule.onNodeWithTag("map-back").performClick()

        rule.onNodeWithTag("maps-list").performScrollToNode(hasTestTag("map-card-sandy_plains"))
        rule.onNodeWithTag("map-card-sandy_plains").performClick()
        rule.onNodeWithTag("map-node-sp-a8-plants-west", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        chooseMapRowIfAmbiguous("sp-a8-plants-west")
        rule.onNodeWithText("Area 8 · Cactus · Gathering").assertIsDisplayed()
        rule.onNodeWithText("Area 8 · Point 1 · Gathering").assertDoesNotExist()
        rule.onNodeWithText("Area 8 · Point 2 · Gathering").assertDoesNotExist()
        rule.onNodeWithTag("map-node-detail-item-159").assertIsDisplayed()
        rule.onNodeWithTag("map-node-detail-close").performClick()
        rule.onNodeWithTag("map-node-sp-a8-plants-east", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        chooseMapRowIfAmbiguous("sp-a8-plants-east")
        rule.onNodeWithText("Area 8 · Cactus · Gathering").assertIsDisplayed()
    }

    @Test fun c3FinalOrdinaryMarkersOpenNodeDetails() {
        openMaps()
        rule.onNodeWithTag("map-card-misty_peaks").performClick()
        rule.onNodeWithTag("map-node-mp-a3-spider-web", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        rule.onNodeWithTag("map-tap-chooser").assertIsDisplayed()
        rule.onNodeWithTag("map-tap-chooser-row-mp-a3-spider-web").performClick()
        rule.onNodeWithText("Area 3 · Point 1 · Gathering").assertIsDisplayed()
        rule.onNodeWithTag("map-node-detail-close").performClick()
        rule.onNodeWithTag("map-back").performClick()

        rule.onNodeWithTag("maps-list").performScrollToNode(hasTestTag("map-card-flooded_forest"))
        rule.onNodeWithTag("map-card-flooded_forest").performClick()
        rule.onNodeWithTag("map-node-ff-a4-fish", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        rule.onNodeWithText("Area 4 · Point 5 · Fishing").assertIsDisplayed()
        rule.onNodeWithTag("map-node-detail-close").performClick()
        rule.onNodeWithTag("map-node-ff-a10-dung", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        rule.onNodeWithTag("map-tap-chooser-row-ff-a10-dung").performClick()
        rule.onNodeWithText("Area 10 · Point 3 · Gathering").assertIsDisplayed()
        rule.onNodeWithTag("map-node-detail-close").performClick()
        rule.onNodeWithTag("map-back").performClick()

        rule.onNodeWithTag("maps-list").performScrollToNode(hasTestTag("map-card-deserted_island"))
        rule.onNodeWithTag("map-card-deserted_island").performClick()
        rule.onNodeWithTag("map-node-di-a6-bones-west", useUnmergedTree = true)
            .assertIsDisplayed().performClick()
        rule.onNodeWithText("Area 6 · Point 1 · Gathering").assertIsDisplayed()
    }

    private fun openMaps() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-map").performScrollTo().performClick()
    }

    private fun openMapDetail() {
        openMaps()
        rule.onNodeWithTag("map-card-flooded_forest").performClick()
    }

    private fun chooseMapRowIfAmbiguous(nodeId: String) {
        val row = rule.onAllNodesWithTag("map-tap-chooser-row-$nodeId")
        if (row.fetchSemanticsNodes().isNotEmpty()) row.onFirst().performClick()
    }
}
