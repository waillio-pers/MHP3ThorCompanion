package com.waillio.mhp3rdcompanion

/** Mechanical identity of a visual map marker. */
internal enum class MapMarkerIdentity { EXACT, EQUIVALENT, NONE }

/** A physical marker pair whose canonical point ordinal is intentionally unknown. */
internal data class EquivalentSourceGroup(
    val groupId: String,
    val markerIds: Set<String>,
    val sourceGatheringNodeIds: Set<String>,
    val displayLabel: String,
    val header: String
)

/**
 * Exact curated Maps 2.0 marker -> production gathering-node join.
 *
 * This is intentionally an explicit stable-ID table: no coordinate, label or
 * category matching is performed at runtime.
 */
internal object MapSourceCrosswalk {
    val markerToSource: Map<String, String> = mapOf(
        "di-a1-bugs" to "gathering_deserted_island_area_1_point_4_bugnet",
        "di-a1-whetstone" to "gathering_deserted_island_area_1_point_3_gathering",
        "di-a10-bait" to "gathering_deserted_island_area_10_point_1_gathering",
        "di-a10-bugs" to "gathering_deserted_island_area_10_point_3_bugnet",
        "di-a10-egg" to "gathering_deserted_island_area_10_point_2_gathering",
        "di-a10-mining" to "gathering_deserted_island_area_10_point_4_mining",
        "di-a10-mushrooms" to "gathering_deserted_island_area_10_point_5_fishing",
        "di-a2-berries" to "gathering_deserted_island_area_2_point_1_gathering",
        "di-a2-bugs" to "gathering_deserted_island_area_2_point_5_bugnet",
        "di-a2-honey" to "gathering_deserted_island_area_2_point_4_gathering",
        "di-a2-mushrooms" to "gathering_deserted_island_area_2_point_3_gathering",
        "di-a2-plants-north" to "gathering_deserted_island_area_2_point_2_gathering",
        "di-a3-bugs" to "gathering_deserted_island_area_3_point_4_bugnet",
        "di-a3-honey" to "gathering_deserted_island_area_3_point_3_gathering",
        "di-a4-barrel" to "gathering_deserted_island_area_4_point_2_gathering",
        "di-a4-bugs" to "gathering_deserted_island_area_4_point_5_bugnet",
        "di-a4-mushrooms" to "gathering_deserted_island_area_4_point_1_gathering",
        "di-a5-berries" to "gathering_deserted_island_area_5_point_3_gathering",
        "di-a5-bones" to "gathering_deserted_island_area_5_point_1_gathering",
        "di-a5-bugs" to "gathering_deserted_island_area_5_point_4_bugnet",
        "di-a5-spider-web" to "gathering_deserted_island_area_5_point_2_gathering",
        "di-a6-bones" to "gathering_deserted_island_area_6_point_2_gathering",
        "di-a6-bones-west" to "gathering_deserted_island_area_6_point_1_gathering",
        "di-a6-mining" to "gathering_deserted_island_area_6_point_4_mining",
        "di-a6-mushrooms" to "gathering_deserted_island_area_6_point_3_gathering",
        "di-a7-mining" to "gathering_deserted_island_area_7_point_4_mining",
        "di-a7-mushrooms" to "gathering_deserted_island_area_7_point_3_gathering",
        "di-a7-plants" to "gathering_deserted_island_area_7_point_1_gathering",
        "di-a7-whetstone" to "gathering_deserted_island_area_7_point_2_gathering",
        "di-a8-bones" to "gathering_deserted_island_area_8_point_1_gathering",
        "di-a8-dung" to "gathering_deserted_island_area_8_point_3_gathering",
        "di-a8-egg" to "gathering_deserted_island_area_8_point_2_gathering",
        "di-a9-plants" to "gathering_deserted_island_area_9_point_1_gathering",
        "ff-a1-mining" to "gathering_flooded_forest_area_1_point_4_mining",
        "ff-a1-mushroom" to "gathering_flooded_forest_area_1_point_3_gathering",
        "ff-a10-bones" to "gathering_flooded_forest_area_10_point_1_gathering",
        "ff-a10-bugs" to "gathering_flooded_forest_area_10_point_5_bugnet",
        "ff-a10-dung" to "gathering_flooded_forest_area_10_point_3_gathering",
        "ff-a10-mining" to "gathering_flooded_forest_area_10_point_4_mining",
        "ff-a10-misc" to "gathering_flooded_forest_area_10_point_2_gathering",
        "ff-a2-bugs" to "gathering_flooded_forest_area_2_point_5_bugnet",
        "ff-a2-mushroom" to "gathering_flooded_forest_area_2_point_1_gathering",
        "ff-a2-plants" to "gathering_flooded_forest_area_2_point_2_gathering",
        "ff-a3-mining" to "gathering_flooded_forest_area_3_point_4_mining",
        "ff-a3-spider-web" to "gathering_flooded_forest_area_3_point_2_gathering",
        "ff-a4-bugs" to "gathering_flooded_forest_area_4_point_4_bugnet",
        "ff-a4-fish" to "gathering_flooded_forest_area_4_point_5_fishing",
        "ff-a4-mushroom" to "gathering_flooded_forest_area_4_point_2_gathering",
        "ff-a5-bones" to "gathering_flooded_forest_area_5_point_3_gathering",
        "ff-a5-bugs" to "gathering_flooded_forest_area_5_point_5_bugnet",
        "ff-a5-mining" to "gathering_flooded_forest_area_5_point_4_mining",
        "ff-a5-misc" to "gathering_flooded_forest_area_5_point_2_gathering",
        "ff-a5-plants" to "gathering_flooded_forest_area_5_point_1_gathering",
        "ff-a6-mining-north" to "gathering_flooded_forest_area_6_point_2_mining",
        "ff-a6-mining-south" to "gathering_flooded_forest_area_6_point_3_mining",
        "ff-a6-whetstone" to "gathering_flooded_forest_area_6_point_1_gathering",
        "ff-a7-berries-north" to "gathering_flooded_forest_area_7_point_4_gathering",
        "ff-a7-berries-south" to "gathering_flooded_forest_area_7_point_2_gathering",
        "ff-a7-bugs" to "gathering_flooded_forest_area_7_point_3_bugnet",
        "ff-a7-honey" to "gathering_flooded_forest_area_7_point_1_gathering",
        "ff-a8-bones" to "gathering_flooded_forest_area_8_point_2_gathering",
        "ff-a8-mushroom" to "gathering_flooded_forest_area_8_point_1_gathering",
        "ff-a9-bugs" to "gathering_flooded_forest_area_9_point_3_bugnet",
        "ff-a9-mushroom" to "gathering_flooded_forest_area_9_point_2_gathering",
        "ff-a9-plants" to "gathering_flooded_forest_area_9_point_1_gathering",
        "ff-c-fish" to "gathering_flooded_forest_area_base_camp_point_1_fishing",
        "mp-a1-berries" to "gathering_misty_peaks_area_1_point_1_gathering",
        "mp-a1-bugs" to "gathering_misty_peaks_area_1_point_4_bugnet",
        "mp-a1-plants" to "gathering_misty_peaks_area_1_point_3_gathering",
        "mp-a1-whetstone" to "gathering_misty_peaks_area_1_point_2_gathering",
        "mp-a2-bones" to "gathering_misty_peaks_area_2_point_1_gathering",
        "mp-a2-dung" to "gathering_misty_peaks_area_2_point_2_gathering",
        "mp-a2-mining" to "gathering_misty_peaks_area_2_point_4_mining",
        "mp-a2-mushrooms" to "gathering_misty_peaks_area_2_point_3_gathering",
        "mp-a3-mining" to "gathering_misty_peaks_area_3_point_4_mining",
        "mp-a3-spider-web" to "gathering_misty_peaks_area_3_point_1_gathering",
        "mp-a4-berries" to "gathering_misty_peaks_area_4_point_1_gathering",
        "mp-a4-mining" to "gathering_misty_peaks_area_4_point_4_mining",
        "mp-a4-plants" to "gathering_misty_peaks_area_4_point_2_gathering",
        "mp-a4-yukumo-wood" to "gathering_misty_peaks_area_4_point_3_gathering",
        "mp-a5-honey" to "gathering_misty_peaks_area_5_point_2_gathering",
        "mp-a5-mushrooms" to "gathering_misty_peaks_area_5_point_4_gathering",
        "mp-a5-plants" to "gathering_misty_peaks_area_5_point_1_gathering",
        "mp-a5-yukumo-wood" to "gathering_misty_peaks_area_5_point_3_gathering",
        "mp-a6-bait" to "gathering_misty_peaks_area_6_point_3_gathering",
        "mp-a6-berries" to "gathering_misty_peaks_area_6_point_1_gathering",
        "mp-a6-fish" to "gathering_misty_peaks_area_6_point_5_fishing",
        "mp-a6-mining" to "gathering_misty_peaks_area_6_point_4_mining",
        "mp-a6-plants" to "gathering_misty_peaks_area_6_point_2_gathering",
        "mp-a7-berries" to "gathering_misty_peaks_area_7_point_1_gathering",
        "mp-a7-bugs" to "gathering_misty_peaks_area_7_point_3_bugnet",
        "mp-a7-fish" to "gathering_misty_peaks_area_7_point_4_fishing",
        "mp-a7-mushrooms" to "gathering_misty_peaks_area_7_point_2_gathering",
        "mp-a8-bones" to "gathering_misty_peaks_area_8_point_2_gathering",
        "mp-a8-dung" to "gathering_misty_peaks_area_8_point_4_gathering",
        "mp-a8-eggs" to "gathering_misty_peaks_area_8_point_3_gathering",
        "mp-a8-mining" to "gathering_misty_peaks_area_8_point_5_mining",
        "mp-a8-whetstone" to "gathering_misty_peaks_area_8_point_1_gathering",
        "mp-a9-honey" to "gathering_misty_peaks_area_9_point_1_gathering",
        "mp-a9-mushrooms" to "gathering_misty_peaks_area_9_point_3_gathering",
        "mp-a9-yukumo-wood" to "gathering_misty_peaks_area_9_point_2_gathering",
        "sp-11-dung" to "gathering_sandy_plains_area_11_point_2_gathering",
        "sp-6-supplies" to "gathering_sandy_plains_area_6_point_2_gathering",
        "sp-a1-berries" to "gathering_sandy_plains_area_1_point_1_gathering",
        "sp-a1-bugs" to "gathering_sandy_plains_area_1_point_3_bugnet",
        "sp-a1-whetstone" to "gathering_sandy_plains_area_1_point_2_gathering",
        "sp-a10-bugs" to "gathering_sandy_plains_area_10_point_3_bugnet",
        "sp-a10-plants" to "gathering_sandy_plains_area_10_point_2_gathering",
        "sp-a10-whetstone" to "gathering_sandy_plains_area_10_point_1_gathering",
        "sp-a11-bones" to "gathering_sandy_plains_area_11_point_1_gathering",
        "sp-a2-bait" to "gathering_sandy_plains_area_2_point_1_gathering",
        "sp-a2-bugs" to "gathering_sandy_plains_area_2_point_3_bugnet",
        "sp-a2-plants" to "gathering_sandy_plains_area_2_point_2_gathering",
        "sp-a3-berries" to "gathering_sandy_plains_area_3_point_2_gathering",
        "sp-a4-bugs" to "gathering_sandy_plains_area_4_point_4_bugnet",
        "sp-a4-plants" to "gathering_sandy_plains_area_4_point_1_gathering",
        "sp-a5-dung" to "gathering_sandy_plains_area_5_point_3_gathering",
        "sp-a6-mushrooms" to "gathering_sandy_plains_area_6_point_1_gathering",
        "sp-a7-egg" to "gathering_sandy_plains_area_7_point_3_gathering",
        "sp-a7-fish" to "gathering_sandy_plains_area_7_point_4_fishing",
        "sp-a7-mushrooms" to "gathering_sandy_plains_area_7_point_1_gathering",
        "sp-a7-plants" to "gathering_sandy_plains_area_7_point_2_gathering",
        "sp-a8-bones" to "gathering_sandy_plains_area_8_point_3_gathering",
        "sp-a9-bones" to "gathering_sandy_plains_area_9_point_2_gathering",
        "tu-a1-berries" to "gathering_tundra_area_1_point_2_gathering",
        "tu-a1-bugs" to "gathering_tundra_area_1_point_1_bugnet",
        "tu-a1-honey" to "gathering_tundra_area_1_point_3_gathering",
        "tu-a2-berries" to "gathering_tundra_area_2_point_2_gathering",
        "tu-a2-mining" to "gathering_tundra_area_2_point_3_mining",
        "tu-a2-plants" to "gathering_tundra_area_2_point_1_gathering",
        "tu-a3-bugs" to "gathering_tundra_area_3_point_1_bugnet",
        "tu-a3-plants" to "gathering_tundra_area_3_point_3_gathering",
        "tu-a3-whetstone" to "gathering_tundra_area_3_point_2_gathering",
        "tu-a4-bones" to "gathering_tundra_area_4_point_3_gathering",
        "tu-a4-mushrooms" to "gathering_tundra_area_4_point_4_gathering",
        "tu-a5-bones" to "gathering_tundra_area_5_point_1_gathering",
        "tu-a5-mushrooms" to "gathering_tundra_area_5_point_2_gathering",
        "tu-a6-mining" to "gathering_tundra_area_6_point_1_mining",
        "tu-a6-plants" to "gathering_tundra_area_6_point_3_gathering",
        "tu-a6-whetstone" to "gathering_tundra_area_6_point_2_gathering",
        "tu-a7-bones" to "gathering_tundra_area_7_point_2_gathering",
        "tu-c-fish" to "gathering_tundra_area_base_camp_point_1_fishing",
        "vo-a1-berries-east" to "gathering_volcano_area_1_point_3_gathering",
        "vo-a1-berries-west" to "gathering_volcano_area_1_point_1_gathering",
        "vo-a1-bugs" to "gathering_volcano_area_1_point_4_bugnet",
        "vo-a1-plants" to "gathering_volcano_area_1_point_2_gathering",
        "vo-a10-mining-east" to "gathering_volcano_area_10_point_2_mining",
        "vo-a10-mining-west" to "gathering_volcano_area_10_point_1_mining",
        "vo-a10-misc" to "gathering_volcano_area_10_point_3_gathering",
        "vo-a2-bugs" to "gathering_volcano_area_2_point_1_bugnet",
        "vo-a2-plants" to "gathering_volcano_area_2_point_2_gathering",
        "vo-a2-whetstone" to "gathering_volcano_area_2_point_3_gathering",
        "vo-a3-bones" to "gathering_volcano_area_3_point_1_gathering",
        "vo-a3-mining" to "gathering_volcano_area_3_point_3_mining",
        "vo-a3-misc" to "gathering_volcano_area_3_point_2_gathering",
        "vo-a4-berries" to "gathering_volcano_area_4_point_3_gathering",
        "vo-a4-bugs" to "gathering_volcano_area_4_point_4_bugnet",
        "vo-a4-fish" to "gathering_volcano_area_4_point_5_fishing",
        "vo-a4-misc" to "gathering_volcano_area_4_point_2_gathering",
        "vo-a4-plants" to "gathering_volcano_area_4_point_1_gathering",
        "vo-a5-mining-north" to "gathering_volcano_area_5_point_1_mining",
        "vo-a5-mining-south" to "gathering_volcano_area_5_point_2_mining",
        "vo-a5-plants" to "gathering_volcano_area_5_point_3_gathering",
        "vo-a6-plants" to "gathering_volcano_area_6_point_1_gathering",
        "vo-a6-whetstone-east" to "gathering_volcano_area_6_point_3_gathering",
        "vo-a6-whetstone-west" to "gathering_volcano_area_6_point_2_gathering",
        "vo-a7-mining" to "gathering_volcano_area_7_point_2_mining",
        "vo-a7-plants-north" to "gathering_volcano_area_7_point_3_gathering",
        "vo-a7-plants-south" to "gathering_volcano_area_7_point_1_gathering",
        "vo-a8-berries" to "gathering_volcano_area_8_point_2_gathering",
        "vo-a8-bones" to "gathering_volcano_area_8_point_1_gathering",
        "vo-a8-mining" to "gathering_volcano_area_8_point_3_mining",
        "vo-a9-mining-east" to "gathering_volcano_area_9_point_3_1_mining",
        "vo-a9-mining-north" to "gathering_volcano_area_9_point_1_2_mining",
        "vo-a9-mining-west" to "gathering_volcano_area_9_point_2_2_mining",
        // Maps 2.0 C2 exact closure (authoritative supplied delta).
        "mp-a9-bugs-west" to "gathering_misty_peaks_area_9_point_4_bugnet",
        "mp-a9-bugs-north" to "gathering_misty_peaks_area_9_point_5_bugnet",
        "sp-a9-plants-north" to "gathering_sandy_plains_area_9_point_1_gathering",
        "sp-a9-plants-south" to "gathering_sandy_plains_area_9_point_3_gathering",
        "sp-a5-bones-west" to "gathering_sandy_plains_area_5_point_1_gathering",
        "sp-a5-bones-east" to "gathering_sandy_plains_area_5_point_2_gathering",
        "sp-a4-mining-north" to "gathering_sandy_plains_area_4_point_2_mining",
        "sp-a4-mining-south" to "gathering_sandy_plains_area_4_point_3_mining",
        "sp-3-plants-north" to "gathering_sandy_plains_area_3_point_1_gathering",
        "sp-a3-plants-south" to "gathering_sandy_plains_area_3_point_3_gathering",
        "sp-a6-mining-east" to "gathering_sandy_plains_area_6_point_3_mining",
        "sp-a6-mining-north" to "gathering_sandy_plains_area_6_point_4_mining",
        "sp-11-mining-west" to "gathering_sandy_plains_area_11_point_3_mining",
        "sp-a11-mining-east" to "gathering_sandy_plains_area_11_point_4_mining",
        "ff-a1-plants-west" to "gathering_flooded_forest_area_1_point_1_gathering",
        "ff-a1-plants-east" to "gathering_flooded_forest_area_1_point_2_gathering",
        "ff-a2-berries-south" to "gathering_flooded_forest_area_2_point_3_gathering",
        "ff-a2-berries-north" to "gathering_flooded_forest_area_2_point_4_gathering",
        "ff-a3-plants-east" to "gathering_flooded_forest_area_3_point_1_gathering",
        "ff-a3-plants-west" to "gathering_flooded_forest_area_3_point_3_gathering",
        "ff-a4-plants-west" to "gathering_flooded_forest_area_4_point_1_gathering",
        "ff-a4-plants-east" to "gathering_flooded_forest_area_4_point_3_gathering",
        "ff-a8-mining-south" to "gathering_flooded_forest_area_8_point_3_mining",
        "ff-a8-mining-north" to "gathering_flooded_forest_area_8_point_4_mining",
        "tu-a4-mining-north" to "gathering_tundra_area_4_point_1_mining",
        "tu-a4-mining-south" to "gathering_tundra_area_4_point_2_mining",
        "tu-a5-mining-west" to "gathering_tundra_area_5_point_3_mining",
        "tu-a5-mining-north" to "gathering_tundra_area_5_point_4_mining",
        "tu-a5-mining-east" to "gathering_tundra_area_5_point_5_mining",
        "tu-a7-mining-north" to "gathering_tundra_area_7_point_1_mining",
        "tu-a7-mining-south" to "gathering_tundra_area_7_point_3_mining",
        "di-a1-plants-east" to "gathering_deserted_island_area_1_point_1_gathering",
        "di-a1-plants-west" to "gathering_deserted_island_area_1_point_2_gathering",
        "di-a3-mushrooms-south" to "gathering_deserted_island_area_3_point_1_gathering",
        "di-a3-mushrooms-north" to "gathering_deserted_island_area_3_point_2_gathering",
        "di-a9-berries-north" to "gathering_deserted_island_area_9_point_2_gathering",
        "di-a9-berries-south" to "gathering_deserted_island_area_9_point_3_gathering",
        "di-a4-mining-east" to "gathering_deserted_island_area_4_point_4_mining",
        "di-a4-mining-south" to "gathering_deserted_island_area_4_point_3_mining",
        "di-a4-mining-north" to "gathering_deserted_island_area_4_point_6_mining"
    )

    /** Equivalent physical points supplied by the C2 audit. No point ordinal is inferred. */
    val equivalentGroups: Map<String, EquivalentSourceGroup> = mapOf(
        "eq_misty_peaks_area3_bamboo" to EquivalentSourceGroup(
            groupId = "eq_misty_peaks_area3_bamboo",
            markerIds = setOf("mp-a3-bamboo-shoot-north", "mp-a3-bamboo-shoot-south"),
            sourceGatheringNodeIds = setOf(
                "gathering_misty_peaks_area_3_point_2_gathering",
                "gathering_misty_peaks_area_3_point_3_gathering"
            ),
            displayLabel = "Bamboo Shoot",
            header = "Area 3 · Bamboo Shoot · Gathering"
        ),
        "eq_sandy_plains_area8_cactus" to EquivalentSourceGroup(
            groupId = "eq_sandy_plains_area8_cactus",
            markerIds = setOf("sp-a8-plants-west", "sp-a8-plants-east"),
            sourceGatheringNodeIds = setOf(
                "gathering_sandy_plains_area_8_point_1_gathering",
                "gathering_sandy_plains_area_8_point_2_gathering"
            ),
            displayLabel = "Cactus",
            header = "Area 8 · Cactus · Gathering"
        )
    )

    val equivalentMarkerToSources: Map<String, Set<String>> = equivalentGroups.values
        .flatMap { group -> group.markerIds.map { markerId -> markerId to group.sourceGatheringNodeIds } }
        .toMap()

    /** Source nodes that must remain textual (no normal-map marker). */
    val specialSourceNodeIds: Set<String> = setOf(
        "gathering_misty_peaks_area_3_point_6_mining",
        "gathering_misty_peaks_area_4_point_point_gathering",
        "gathering_misty_peaks_area_5_point_point_gathering",
        "gathering_sandy_plains_area_6_point_6_mining",
        "gathering_flooded_forest_area_6_point_4_bugnet",
        "gathering_flooded_forest_area_6_point_5_mining",
        "gathering_deserted_island_area_7_point_5_bugnet",
        "gathering_tundra_area_1_point_5_gathering",
        "gathering_tundra_area_1_point_6_mining",
        "gathering_tundra_area_8_point_1_gathering",
        "gathering_tundra_area_8_point_2_gathering",
        "gathering_tundra_area_8_point_3_bugnet",
        "gathering_tundra_area_9_point_1_mining",
        "gathering_tundra_area_9_point_2_bugnet",
        "gathering_tundra_area_9_point_3_gathering",
        "gathering_volcano_area_9_point_4_bugnet",
        "gathering_volcano_area_9_point_5_mining"
    )

    val specialSourceReasonByNodeId: Map<String, String> = mapOf(
        "gathering_misty_peaks_area_3_point_6_mining" to "SECRET_AREA",
        "gathering_misty_peaks_area_4_point_point_gathering" to "CONDITIONAL_POST_DESTRUCTION",
        "gathering_misty_peaks_area_5_point_point_gathering" to "CONDITIONAL_POST_DESTRUCTION",
        "gathering_sandy_plains_area_6_point_6_mining" to "SECRET_AREA",
        "gathering_flooded_forest_area_6_point_4_bugnet" to "SECRET_AREA",
        "gathering_flooded_forest_area_6_point_5_mining" to "SECRET_AREA",
        "gathering_deserted_island_area_7_point_5_bugnet" to "SECRET_AREA",
        "gathering_tundra_area_1_point_5_gathering" to "SECRET_AREA",
        "gathering_tundra_area_1_point_6_mining" to "SECRET_AREA",
        "gathering_tundra_area_8_point_1_gathering" to "HIDDEN_AREA",
        "gathering_tundra_area_8_point_2_gathering" to "HIDDEN_AREA",
        "gathering_tundra_area_8_point_3_bugnet" to "HIDDEN_AREA",
        "gathering_tundra_area_9_point_1_mining" to "HIDDEN_AREA",
        "gathering_tundra_area_9_point_2_bugnet" to "HIDDEN_AREA",
        "gathering_tundra_area_9_point_3_gathering" to "HIDDEN_AREA",
        "gathering_volcano_area_9_point_4_bugnet" to "SECRET_AREA",
        "gathering_volcano_area_9_point_5_mining" to "SECRET_AREA"
    )

    /** No ordinary production source remains without a physical marker. */
    val knownMissingOrdinarySourceNodeIds: Set<String> = emptySet()

    val decorativeMarkerIds: Set<String> = setOf(
        "di-a4-felyne",
        "mp-a3-felyne",
        "sp-6-felyne",
        "tu-a1-felyne",
        "vo-a1-misc"
    )

    /** Duplicate marker retained for calibration identity, but not mechanical UI. */
    val deprecatedDuplicateMarkerIds: Set<String> = setOf("ff-a8-misc")

    val sourceToMarkers: Map<String, Set<String>> by lazy {
        (markerToSource.entries.map { it.value to it.key } +
            equivalentMarkerToSources.flatMap { (markerId, sourceIds) -> sourceIds.map { it to markerId } })
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }
    }

    fun sourceIdsForMarker(markerId: String): Set<String> =
        equivalentMarkerToSources[markerId] ?: markerToSource[markerId]?.let(::setOf).orEmpty()

    fun identityForMarker(markerId: String): MapMarkerIdentity = when {
        markerId in markerToSource -> MapMarkerIdentity.EXACT
        markerId in equivalentMarkerToSources -> MapMarkerIdentity.EQUIVALENT
        else -> MapMarkerIdentity.NONE
    }

    fun equivalentGroupForMarker(markerId: String): EquivalentSourceGroup? =
        equivalentGroups.values.firstOrNull { markerId in it.markerIds }

    fun isLinked(markerId: String): Boolean = identityForMarker(markerId) != MapMarkerIdentity.NONE

    fun hasMarkerForSource(sourceId: String): Boolean = sourceId in sourceToMarkers

    fun isSpecialSource(sourceId: String): Boolean = sourceId in specialSourceNodeIds

    fun isDeprecatedDuplicateMarker(markerId: String): Boolean = markerId in deprecatedDuplicateMarkerIds
}
