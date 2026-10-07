package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
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
class FieldRedesignUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun ironOreUsesSixCollapsedMapCardsAndProgressiveNodeComposition() {
        openMaterial("Iron Ore", "item_iron_ore")
        rule.onNodeWithTag("field-summary").assertTextContains("6 maps · 34 gathering points")
        listOf("misty_peaks", "sandy_plains", "flooded_forest", "deserted_island", "tundra", "volcano").forEach { mapId ->
            rule.onNodeWithTag("field-map-card-$mapId").assertExists()
            rule.onNodeWithTag("field-show-map-$mapId").assertExists()
            rule.onNodeWithTag("field-node-grid-$mapId").assertDoesNotExist()
        }
        rule.onNodeWithTag("field-map-toggle-misty_peaks").performClick()
        rule.onNodeWithTag("field-node-grid-misty_peaks").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("field-node-grid-sandy_plains").assertDoesNotExist()
        rule.onNodeWithTag("field-map-toggle-sandy_plains").performScrollTo().performClick()
        rule.onNodeWithTag("field-node-grid-misty_peaks").assertExists()
        rule.onNodeWithTag("field-node-grid-sandy_plains").assertExists()
    }

    @Test
    fun showOnMapIsAvailableFromCollapsedHeader() {
        openMaterial("Iron Ore", "item_iron_ore")
        rule.onNodeWithTag("field-node-grid-misty_peaks").assertDoesNotExist()
        rule.onNodeWithTag("field-show-map-misty_peaks").performClick()
        rule.onNodeWithTag("map-detail-misty_peaks").assertIsDisplayed()
    }

    @Test
    fun realShortCaseStartsExpanded() {
        openMaterial("Monster Bone M", "item_monster_bone_m")
        rule.onNodeWithTag("field-summary").assertTextContains("1 map · 1 gathering point")
        rule.onNodeWithTag("field-node-grid-tundra").assertExists()
        rule.onNodeWithTag("field-node-gathering_tundra_area_4_point_3_gathering").assertExists()
    }

    @Test
    fun rankOnlyMapSummariesAndChipsAreDerived() {
        openMaterial("Armor Sphere+", "item_armor_sphere_plus")
        rule.onNodeWithTag("field-map-card-misty_peaks").assertIsDisplayed()
        rule.onNodeWithTag("field-map-header-misty_peaks").assertTextContains("2 points · HR")
        rule.onNodeWithTag("field-map-toggle-misty_peaks").performClick()
        rule.onNodeWithTag("field-node-grid-misty_peaks").assertExists()
        rule.onNodeWithTag("field-node-rank-gathering_misty_peaks_area_3_point_6_mining-hr").assertExists()
        rule.onNodeWithTag("field-node-rank-gathering_misty_peaks_area_3_point_6_mining-lr").assertDoesNotExist()

    }

    @Test
    fun lrOnlyMapSummaryAndChipAreDerived() {
        openMaterial("Insect Husk", "item_insect_husk")
        rule.onNodeWithTag("field-map-header-sandy_plains").assertTextContains("3 points · LR")
        rule.onNodeWithTag("field-map-toggle-sandy_plains").performScrollTo().performClick()
        rule.onNodeWithTag("field-node-grid-sandy_plains").assertExists()
        rule.onNodeWithTag("field-node-rank-gathering_sandy_plains_area_1_point_3_bugnet-lr").assertExists()
        rule.onNodeWithTag("field-node-rank-gathering_sandy_plains_area_1_point_3_bugnet-hr").assertDoesNotExist()
    }

    @Test
    fun mysteryCharmPreservesRankSpecificQuantityAndNoChance() {
        openMaterial("Mystery Charm", "item_mystery_charm")
        rule.onNodeWithTag("field-map-toggle-tundra").performScrollTo().performClick()
        rule.onNodeWithTag("field-node-grid-tundra").assertExists()
        rule.onAllNodesWithText("HR ×2", substring = true).assertCountEquals(3)
        rule.onAllNodesWithText("LR ×1", substring = true).assertCountEquals(3)
    }

    @Test
    fun multiMethodMapKeepsMethodOnEachNode() {
        openMaterial("Whetstone", "item_whetstone")
        rule.onNodeWithTag("field-map-toggle-volcano").performScrollTo().performClick()
        rule.onNodeWithTag("field-node-grid-volcano").assertExists()
        rule.onNodeWithTag("field-node-method-gathering_volcano_area_2_point_3_gathering").assertTextContains("Gathering")
        rule.onNodeWithTag("field-node-method-gathering_volcano_area_3_point_3_mining").assertTextContains("Mining")
    }

    @Test
    fun thorContainerAwareFieldGridUsesTwoColumnsAtMeasuredGeometry() {
        openMaterial("Iron Ore", "item_iron_ore")
        rule.onNodeWithTag("field-map-toggle-misty_peaks").performClick()
        val grid = rule.onNodeWithTag("field-node-grid-misty_peaks").assertExists().fetchSemanticsNode()
        val density = rule.activity.resources.displayMetrics.density
        val configuration = rule.activity.resources.configuration
        val root = rule.activity.window.decorView.rootView
        val containerWidthDp = grid.boundsInRoot.width / density
        val contentWidthDp = containerWidthDp - 14f
        val minimumTwoColumnWidthDp = (FieldNodeMinCellWidth * 2 + FieldNodeGridGap).value
        println(
            "THOR_FIELD_DIAGNOSTICS density=$density fontScale=${configuration.fontScale} " +
                "screenWidthDp=${configuration.screenWidthDp} screenHeightDp=${configuration.screenHeightDp} " +
                "orientation=${configuration.orientation} windowMetricsPx=${root.width}x${root.height} " +
                "fieldContainerMaxWidthDp=$containerWidthDp fieldContentWidthDp=$contentWidthDp " +
                "selectedColumns=2"
        )
        assertTrue("Measured Thor content width must fit two accepted cells", contentWidthDp >= minimumTwoColumnWidthDp)
        rule.onNodeWithTag("field-node-grid-misty_peaks-columns-2").assertExists()
        rule.onNodeWithTag("field-node-grid-misty_peaks-columns-1").assertDoesNotExist()
    }

    @Test
    fun narrowPhoneBreakpointKeepsOneColumn() {
        val minimum = (FieldNodeMinCellWidth * 2 + FieldNodeGridGap).value
        assertEquals(1, fieldGridColumnCount((minimum - 1f).dp))
        assertEquals(2, fieldGridColumnCount(minimum.dp))
    }

    private fun openMaterial(name: String, id: String) {
        rule.onNodeWithTag("global-search").performTextInput(name)
        rule.onNodeWithTag("result-material-$id").performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
    }
}
