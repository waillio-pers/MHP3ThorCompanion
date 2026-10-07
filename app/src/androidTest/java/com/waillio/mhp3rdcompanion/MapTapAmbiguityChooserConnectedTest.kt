package com.waillio.mhp3rdcompanion

import android.os.ParcelFileDescriptor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.ceil

/** One connected run on the 1240x1080, 240-dpi Thor lower-screen AVD only. */
@RunWith(AndroidJUnit4::class)
class MapTapAmbiguityChooserConnectedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val markerMatcher = SemanticsMatcher("rendered map marker") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("map-node-") == true
    }
    private val chooserRowMatcher = SemanticsMatcher("tap chooser row") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("map-tap-chooser-row-") == true
    }

    @Test
    fun mapAmbiguityFlowsPassOnThorLowerScreenAvd() {
        val preferences = rule.activity.getSharedPreferences(UI_SETTINGS_PREFS, 0)
        preferences.edit().putInt(UI_SCALE_STEP_KEY, UI_SCALE_DEFAULT_STEP).commit()
        rule.runOnUiThread { rule.activity.recreate() }
        rule.waitForIdle()
        val display = rule.activity.window.decorView.rootView
        assertEquals("connected device is the Thor lower-screen width", 1240, display.width)
        assertEquals("connected device is the Thor lower-screen height", 1080, display.height)
        assertEquals("connected device is configured at 240 dpi", 1.5f, rule.activity.resources.displayMetrics.density, .01f)
        val screenshotDir = "/sdcard/Download/mhp3rd-map-tap-ambiguity-chooser-v1-${System.currentTimeMillis()}"
        shell("mkdir -p $screenshotDir")

        try {
            openMaps()

            // Flooded Forest A10 exact same-center pair: both rows stay independently selectable.
            openMap("flooded_forest")
            rule.onNodeWithTag("map-node-ff-a10-bones", useUnmergedTree = true).performClick()
            rule.onNodeWithTag("map-tap-chooser").assertIsDisplayed()
            assertChooserRows(setOf("ff-a10-bones", "ff-a10-misc"))
            assertChooserInsideMap()
            saveScreenshot(screenshotDir, "01-flooded-a10-two-point-chooser.png")
            pressBack()
            rule.waitForIdle()
            rule.onNodeWithTag("map-tap-chooser").assertDoesNotExist()
            rule.onNodeWithTag("map-detail-flooded_forest").assertIsDisplayed()

            rule.onNodeWithTag("map-node-ff-a10-bones", useUnmergedTree = true).performClick()
            rule.onNodeWithTag("map-tap-chooser-row-ff-a10-bones").performClick()
            rule.onNodeWithTag("map-node-detail-modal").assertIsDisplayed()
            rule.onNodeWithTag("map-node-detail-close").performClick()
            rule.onNodeWithTag("map-node-ff-a10-misc", useUnmergedTree = true).performClick()
            rule.onNodeWithTag("map-tap-chooser-row-ff-a10-misc").performClick()
            rule.onNodeWithTag("map-node-detail-modal").assertIsDisplayed()
            rule.onNodeWithTag("map-node-detail-close").performClick()

            // Visibility projection is applied before hit testing: Bones leaves one candidate.
            rule.onNodeWithTag("map-filter-bones").performClick()
            rule.onNodeWithTag("map-node-ff-a10-bones", useUnmergedTree = true).performClick()
            rule.onNodeWithTag("map-tap-chooser").assertDoesNotExist()
            rule.onNodeWithTag("map-node-detail-modal").assertIsDisplayed()
            rule.onNodeWithTag("map-node-detail-close").performClick()
            rule.onNodeWithTag("map-back").performClick()

            // Deserted Island A4 uses the audited five-hit intersection, not a transitive group.
            openMap("deserted_island")
            val areaFourNodes = renderedNodes("deserted_island")
            val areaFourMapSize = mapOverlay().fetchSemanticsNode().size.width.toFloat()
            val areaFourRadius = hitRadiusPx(fullscreen = false)
            val areaFourExpected = setOf(
                "di-a4-barrel", "di-a4-bugs", "di-a4-mining-east", "di-a4-mining-north", "di-a4-mining-south"
            )
            val fivePointTap = findTapWithExactly(areaFourNodes, areaFourMapSize, areaFourRadius, 5, areaFourExpected)
            assertEquals(
                areaFourExpected,
                fivePointTap.second.map { it.node.nodeId }.toSet()
            )
            tapMap(fivePointTap.first)
            rule.onNodeWithTag("map-tap-chooser").assertIsDisplayed()
            rule.onNodeWithTag("map-tap-chooser-count").assertTextEquals("5 nearby resource points")
            assertChooserRows(fivePointTap.second.map { it.node.nodeId }.toSet())
            assertChooserInsideMap()
            saveScreenshot(screenshotDir, "02-deserted-a4-five-point-chooser.png")
            rule.onNodeWithTag("map-tap-chooser-row-${fivePointTap.second.first().node.nodeId}").performClick()
            rule.onNodeWithTag("map-node-detail-modal").assertIsDisplayed()
            rule.onNodeWithTag("map-node-detail-close").performClick()
            rule.onNodeWithTag("map-back").performClick()

            // Real Item Focus: Ivy (gameItemId 155) leaves the equivalent Bamboo pair ambiguous.
            openItem("Ivy", "item_ivy")
            rule.onNodeWithTag("field-show-map-misty_peaks").performClick()
            rule.onNodeWithTag("map-item-focus").assertIsDisplayed()
            val ivyNodes = renderedNodes("misty_peaks")
            assertEquals(
                setOf("mp-a3-bamboo-shoot-north", "mp-a3-bamboo-shoot-south", "mp-a4-plants"),
                ivyNodes.map { it.nodeId }.toSet()
            )
            val bambooPair = ivyNodes.filter { it.nodeId in setOf("mp-a3-bamboo-shoot-north", "mp-a3-bamboo-shoot-south") }
            val bambooTap = pairMidpoint(bambooPair, mapOverlay().fetchSemanticsNode().size.width.toFloat())
            assertEquals(2, tapCandidates(ivyNodes, bambooTap, mapOverlay().fetchSemanticsNode().size.width.toFloat(), hitRadiusPx(false)).size)
            tapMap(bambooTap)
            rule.onNodeWithTag("map-tap-chooser-count").assertTextEquals("2 nearby resource points")
            assertChooserRows(setOf("mp-a3-bamboo-shoot-north", "mp-a3-bamboo-shoot-south"))
            assertChooserInsideMap()
            rule.onAllNodesWithText("Area 3 · Bamboo Shoot · Gathering").assertCountEquals(2)
            rule.onNodeWithText("Area 3 · Point 2 · Gathering").assertDoesNotExist()
            rule.onNodeWithText("Area 3 · Point 3 · Gathering").assertDoesNotExist()
            saveScreenshot(screenshotDir, "03-misty-bamboo-equivalent-chooser.png")
            rule.onNodeWithTag("map-tap-chooser-row-mp-a3-bamboo-shoot-south").performClick()
            rule.onNodeWithTag("map-node-detail-modal").assertIsDisplayed()
            rule.onNodeWithTag("map-node-detail-close").performClick()
            rule.onNodeWithTag("map-back").performClick()

            // Whetstone gameItemId 91 projection restricts candidates, then uses the same chooser.
            openMaps()
            openMap("deserted_island")
            rule.onNodeWithTag("map-filter-whetstone").performClick()
            val whetstoneNodes = renderedNodes("deserted_island")
            assertEquals(7, whetstoneNodes.size)
            val whetstonePair = whetstoneNodes.filter { it.nodeId in setOf("di-a4-mining-east", "di-a4-mining-south") }
            val whetstoneSize = mapOverlay().fetchSemanticsNode().size.width.toFloat()
            val whetstoneTap = pairMidpoint(whetstonePair, whetstoneSize)
            val whetstoneCandidates = tapCandidates(whetstoneNodes, whetstoneTap, whetstoneSize, hitRadiusPx(false))
            assertEquals(setOf("di-a4-mining-east", "di-a4-mining-south"), whetstoneCandidates.map { it.node.nodeId }.toSet())
            tapMap(whetstoneTap)
            rule.onNodeWithTag("map-tap-chooser-count").assertTextEquals("2 nearby resource points")
            assertChooserRows(setOf("di-a4-mining-east", "di-a4-mining-south"))
            assertChooserInsideMap()
            saveScreenshot(screenshotDir, "04-whetstone-chooser.png")

            // A second map tap re-resolves at its own location; an empty tap dismisses in place.
            val emptyTap = findEmptyTap(whetstoneNodes, whetstoneSize, hitRadiusPx(false))
            tapMap(emptyTap)
            rule.onNodeWithTag("map-tap-chooser").assertDoesNotExist()
            rule.onNodeWithTag("map-detail-deserted_island").assertIsDisplayed()

            // The fullscreen canvas routes through the same resolver and chooser.
            rule.onNodeWithTag("map-fullscreen-open").performClick()
            rule.onNodeWithTag("map-fullscreen").assertIsDisplayed()
            val fullscreenNodes = renderedNodes("deserted_island")
            val fullscreenSize = mapOverlay(fullscreen = true).fetchSemanticsNode().size.width.toFloat()
            val fullscreenPair = fullscreenNodes.filter { it.nodeId in setOf("di-a4-mining-east", "di-a4-mining-south") }
            val fullscreenTap = pairMidpoint(fullscreenPair, fullscreenSize)
            assertEquals(2, tapCandidates(fullscreenNodes, fullscreenTap, fullscreenSize, hitRadiusPx(true)).size)
            tapMap(fullscreenTap, fullscreen = true)
            rule.onNodeWithTag("map-tap-chooser-count").assertTextEquals("2 nearby resource points")
            assertChooserInsideMap(fullscreen = true)
            saveScreenshot(screenshotDir, "05-fullscreen-chooser.png")
            pressBack()
            rule.waitForIdle()
            rule.onNodeWithTag("map-tap-chooser").assertDoesNotExist()
            rule.onNodeWithTag("map-fullscreen").assertIsDisplayed()

            tapMap(fullscreenTap, fullscreen = true)
            rule.onNodeWithTag("map-tap-chooser").assertIsDisplayed()
            rule.onNodeWithTag("map-tap-chooser-close").performClick()
            rule.onNodeWithTag("map-tap-chooser").assertDoesNotExist()
            rule.onNodeWithTag("map-fullscreen").assertIsDisplayed()
            rule.onNodeWithTag("map-fullscreen-close").performClick()

            // Restore all categories and demonstrate an ordinary one-candidate direct detail.
            rule.onNodeWithTag("map-filter-all").performClick()
            val allDesertedNodes = renderedNodes("deserted_island")
            val embeddedSize = mapOverlay().fetchSemanticsNode().size.width.toFloat()
            val radius = hitRadiusPx(false)
            val sparse = allDesertedNodes.firstOrNull { node ->
                val point = Offset(node.x * embeddedSize, node.y * embeddedSize)
                tapCandidates(allDesertedNodes, point, embeddedSize, radius).singleOrNull()?.node?.nodeId == node.nodeId
            } ?: error("The connected map exposes no sparse single-candidate resource marker")
            rule.onNodeWithTag("map-node-${sparse.nodeId}", useUnmergedTree = true).performClick()
            rule.onNodeWithTag("map-tap-chooser").assertDoesNotExist()
            rule.onNodeWithTag("map-node-detail-modal").assertIsDisplayed()
            saveScreenshot(screenshotDir, "06-sparse-direct-node-detail.png")
            rule.onNodeWithTag("map-node-detail-close").performClick()
            rule.onNodeWithTag("map-back").performClick()

            // Actual embedded resolver retains the Bamboo ambiguity at the supported scale bounds.
            for ((step, label) in listOf(0 to "0.80", 6 to "1.10")) {
                setScale(step, label)
                openMap("misty_peaks")
                val scaledNodes = renderedNodes("misty_peaks")
                val scaledMapSize = mapOverlay().fetchSemanticsNode().size.width.toFloat()
                val scaledPair = scaledNodes.filter { it.nodeId in setOf("mp-a3-bamboo-shoot-north", "mp-a3-bamboo-shoot-south") }
                val scaledTap = pairMidpoint(scaledPair, scaledMapSize)
                val scaledCandidates = tapCandidates(scaledNodes, scaledTap, scaledMapSize, hitRadiusPx(false))
                val scaledCandidateIds = scaledCandidates.map { it.node.nodeId }.toSet()
                assertTrue(scaledCandidateIds.containsAll(setOf("mp-a3-bamboo-shoot-north", "mp-a3-bamboo-shoot-south")))
                tapMap(scaledTap)
                rule.onNodeWithTag("map-tap-chooser-count").assertTextEquals("${scaledCandidates.size} nearby resource points")
                assertChooserRows(scaledCandidateIds)
                rule.onNodeWithTag("map-tap-chooser-close").performClick()
                rule.onNodeWithTag("map-back").performClick()
            }
            setScale(UI_SCALE_DEFAULT_STEP, "1.00")
            openMap("misty_peaks")
            val defaultNodes = renderedNodes("misty_peaks")
            val defaultSize = mapOverlay().fetchSemanticsNode().size.width.toFloat()
            val defaultPair = defaultNodes.filter { it.nodeId in setOf("mp-a3-bamboo-shoot-north", "mp-a3-bamboo-shoot-south") }
            val defaultTap = pairMidpoint(defaultPair, defaultSize)
            val defaultCandidates = tapCandidates(defaultNodes, defaultTap, defaultSize, hitRadiusPx(false))
            assertTrue(defaultCandidates.map { it.node.nodeId }.toSet().containsAll(setOf("mp-a3-bamboo-shoot-north", "mp-a3-bamboo-shoot-south")))
            tapMap(defaultTap)
            rule.onNodeWithTag("map-tap-chooser-count").assertTextEquals("${defaultCandidates.size} nearby resource points")
            assertChooserRows(defaultCandidates.map { it.node.nodeId }.toSet())
            rule.onNodeWithTag("map-tap-chooser-close").performClick()

            val screenshotFiles = shell("ls -1 $screenshotDir").lineSequence().filter { it.endsWith(".png") }.toSet()
            assertEquals(
                setOf(
                    "01-flooded-a10-two-point-chooser.png",
                    "02-deserted-a4-five-point-chooser.png",
                    "03-misty-bamboo-equivalent-chooser.png",
                    "04-whetstone-chooser.png",
                    "05-fullscreen-chooser.png",
                    "06-sparse-direct-node-detail.png"
                ),
                screenshotFiles
            )
        } finally {
            preferences.edit().putInt(UI_SCALE_STEP_KEY, UI_SCALE_DEFAULT_STEP).commit()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()
        }
    }

    private fun openMaps() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-map").performScrollTo().performClick()
        rule.onNodeWithTag("maps-index").assertIsDisplayed()
    }

    private fun openMap(id: String) {
        rule.onNodeWithTag("maps-list").performScrollToNode(hasTestTag("map-card-$id"))
        rule.onNodeWithTag("map-card-$id").performClick()
        rule.onNodeWithTag("map-detail-$id").assertIsDisplayed()
    }

    private fun openItem(name: String, id: String) {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-material").performScrollTo().performClick()
        val search = rule.onNodeWithTag("global-search")
        search.performTextClearance()
        search.performTextInput(name)
        rule.onNodeWithTag("result-material-$id").performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
    }

    private fun setScale(step: Int, label: String) {
        rule.onNodeWithTag("settings-button").performClick()
        rule.onNodeWithTag("ui-scale-slider")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(step.toFloat()) }
        rule.onNodeWithTag("settings-scale-value", useUnmergedTree = true).assertTextEquals(label)
        rule.onNodeWithTag("settings-done").performClick()
        rule.waitForIdle()
        assertEquals(step, rule.activity.getSharedPreferences(UI_SETTINGS_PREFS, 0).getInt(UI_SCALE_STEP_KEY, -1))
    }

    private fun renderedNodes(mapId: String): List<MapNode> {
        val renderedIds = rule.onAllNodes(markerMatcher, useUnmergedTree = true).fetchSemanticsNodes()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }
            .map { it.removePrefix("map-node-") }
            .toSet()
        return requireNotNull(MapRegistry.resolve(mapId)).nodes.filter { it.nodeId in renderedIds }
    }

    private fun mapOverlay(fullscreen: Boolean = false): SemanticsNodeInteraction {
        val collection = rule.onAllNodesWithTag("map-overlay", useUnmergedTree = true)
        val nodes = collection.fetchSemanticsNodes()
        require(nodes.isNotEmpty()) { "No map overlay is currently composed" }
        val target = if (fullscreen) nodes.maxBy { it.size.width * it.size.height }
        else nodes.minBy { it.size.width * it.size.height }
        return collection.get(nodes.indexOf(target))
    }

    private fun mapHitSurface(fullscreen: Boolean = false): SemanticsNodeInteraction {
        val collection = rule.onAllNodesWithTag("map-hit-surface", useUnmergedTree = true)
        val nodes = collection.fetchSemanticsNodes()
        require(nodes.isNotEmpty()) { "No map hit surface is currently composed" }
        val target = if (fullscreen) nodes.maxBy { it.size.width * it.size.height }
        else nodes.minBy { it.size.width * it.size.height }
        return collection.get(nodes.indexOf(target))
    }

    private fun hitRadiusPx(fullscreen: Boolean): Float {
        val step = rule.activity.getSharedPreferences(UI_SETTINGS_PREFS, 0).getInt(UI_SCALE_STEP_KEY, UI_SCALE_DEFAULT_STEP)
        val effectiveDensity = rule.activity.resources.displayMetrics.density * uiScaleFromStep(step)
        return (if (fullscreen) 26f else 20f) * effectiveDensity
    }

    private fun pairMidpoint(nodes: List<MapNode>, mapSizePx: Float): Offset {
        require(nodes.size == 2) { "Expected exactly two projected physical markers; got ${nodes.map { it.nodeId }}" }
        return Offset((nodes[0].x + nodes[1].x) * mapSizePx / 2f, (nodes[0].y + nodes[1].y) * mapSizePx / 2f)
    }

    private fun tapCandidates(nodes: List<MapNode>, offset: Offset, mapSizePx: Float, radiusPx: Float) =
        MapTapResolver.candidates(nodes, offset.x, offset.y, mapSizePx, radiusPx)

    private fun findTapWithExactly(
        nodes: List<MapNode>, mapSizePx: Float, radiusPx: Float, targetCount: Int, expectedIds: Set<String>
    ): Pair<Offset, List<MapTapCandidate>> {
        var best: Pair<Offset, List<MapTapCandidate>>? = null
        var bestMargin = Float.NEGATIVE_INFINITY
        val left = (mapSizePx * .27f).toInt()
        val right = (mapSizePx * .39f).toInt()
        val top = (mapSizePx * .49f).toInt()
        val bottom = (mapSizePx * .61f).toInt()
        for (y in top..bottom) for (x in left..right) {
            val point = Offset(x.toFloat(), y.toFloat())
            val candidates = tapCandidates(nodes, point, mapSizePx, radiusPx)
            if (candidates.size == targetCount && candidates.map { it.node.nodeId }.toSet() == expectedIds) {
                val margin = candidates.minOf { radiusPx - it.distancePx }
                if (margin > bestMargin) {
                    best = point to candidates
                    bestMargin = margin
                }
            }
        }
        val result = best ?: error("No tap position had exactly $targetCount candidates at mapSize=$mapSizePx radius=$radiusPx")
        assertTrue("the five-marker intersection has room beyond touch rounding (margin=$bestMargin px)", bestMargin > 2f)
        return result
    }

    private fun findEmptyTap(nodes: List<MapNode>, mapSizePx: Float, radiusPx: Float): Offset {
        val limit = ceil(mapSizePx).toInt()
        for (y in 0..limit step 12) for (x in 0..limit step 12) {
            val point = Offset(x.toFloat(), y.toFloat())
            if (tapCandidates(nodes, point, mapSizePx, radiusPx).isEmpty()) return point
        }
        error("No empty tap point found on map")
    }

    private fun tapMap(offset: Offset, fullscreen: Boolean = false) {
        val mapBounds = mapOverlay(fullscreen).fetchSemanticsNode().boundsInRoot
        val hitBounds = mapHitSurface(fullscreen).fetchSemanticsNode().boundsInRoot
        val hitLocalOffset = Offset(offset.x + mapBounds.left - hitBounds.left, offset.y + mapBounds.top - hitBounds.top)
        mapHitSurface(fullscreen).performTouchInput { click(hitLocalOffset) }
        rule.waitForIdle()
    }

    private fun assertChooserRows(expectedIds: Set<String>) {
        val actual = rule.onAllNodes(chooserRowMatcher, useUnmergedTree = true).fetchSemanticsNodes()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }
            .map { it.removePrefix("map-tap-chooser-row-") }
            .toSet()
        assertEquals(expectedIds, actual)
    }

    private fun assertChooserInsideMap(fullscreen: Boolean = false) {
        val map = mapOverlay(fullscreen).fetchSemanticsNode().boundsInRoot
        val chooser = rule.onNodeWithTag("map-tap-chooser").fetchSemanticsNode().boundsInRoot
        assertTrue("chooser $chooser stays inside map viewport $map", chooser.left >= map.left && chooser.top >= map.top)
        assertTrue("chooser $chooser stays inside map viewport $map", chooser.right <= map.right && chooser.bottom <= map.bottom)
    }

    private fun saveScreenshot(directory: String, name: String) {
        rule.waitForIdle()
        shell("screencap -p $directory/$name")
        assertTrue("AVD screenshot exists: $name", shell("ls -l $directory/$name").contains(name))
    }

    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
    ).bufferedReader().use { it.readText() }
}
