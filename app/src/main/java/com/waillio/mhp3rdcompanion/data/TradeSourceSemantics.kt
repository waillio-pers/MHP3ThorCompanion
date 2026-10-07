package com.waillio.mhp3rdcompanion.data

/** Canonical, explicit labels for source-backed Veggie Elder map routes. */
fun tradeMapLabel(mapId: String?): String? = when (mapId) {
    "MISTY_PEAKS" -> "Misty Peaks"
    "SANDY_PLAINS" -> "Sandy Plains"
    "FLOODED_FOREST" -> "Flooded Forest"
    "DESERTED_ISLAND" -> "Deserted Island"
    "TUNDRA" -> "Tundra"
    "VOLCANO" -> "Volcano"
    else -> null
}

/**
 * A single shared location phrase used by the forward Source projection and
 * inverse Usage projection so route identity cannot disappear in either UI.
 */
fun tradeLocationContextLabel(
    scopeType: String?,
    mapId: String?,
    locations: List<ItemVeggieElderLocation>
): String? {
    if (scopeType == "ALL_SIX_GATHERING_MAPS") return "All gathering maps"
    if (scopeType != "MAP_SPECIFIC") return null
    val map = tradeMapLabel(mapId) ?: return null
    val location = locations.firstOrNull { it.mapId == mapId } ?: return map
    return formatTradeLocation(mapId, map, location.locationType, location.areaNumber)
}

/** Generated fixture counterpart used while constructing forward source rows. */
fun tradeLocationContextLabel(
    scopeType: String?,
    mapId: String?,
    locations: Iterable<GeneratedItemVeggieElderLocation>
): String? {
    if (scopeType == "ALL_SIX_GATHERING_MAPS") return "All gathering maps"
    if (scopeType != "MAP_SPECIFIC") return null
    val map = tradeMapLabel(mapId) ?: return null
    val location = locations.firstOrNull { it.mapId == mapId } ?: return map
    return formatTradeLocation(mapId, map, location.locationType, location.areaNumber)
}

private fun formatTradeLocation(mapId: String?, map: String, type: String?, area: Int?): String = when (type) {
    "AREA" -> area?.let { "$map · Area $it" } ?: map
    "BASE_CAMP" -> if (mapId == "TUNDRA") "$map · Base Camp area" else "$map · Base Camp"
    else -> map
}

fun tradeQuestContextLabel(value: String?): String? = when (value) {
    "GATHERING_QUESTS_ONLY" -> "Gathering quests"
    else -> null
}

fun tradeInteractionWindowLabel(value: String?): String? = when (value) {
    "BEFORE_QUEST_COMPLETION" -> "before completion"
    else -> null
}

fun tradeRouteMetadataLabel(
    scopeType: String?,
    mapId: String?,
    questContext: String?,
    interactionWindow: String?,
    locationContext: String?
): String? = listOfNotNull(
    when (scopeType) {
        "ALL_SIX_GATHERING_MAPS" -> "All gathering maps"
        "MAP_SPECIFIC" -> locationContext ?: tradeMapLabel(mapId)
        else -> null
    },
    listOfNotNull(tradeQuestContextLabel(questContext), tradeInteractionWindowLabel(interactionWindow))
        .joinToString(" · ").takeIf(String::isNotBlank)
).joinToString(" · ").takeIf(String::isNotBlank)

fun tradeSourceRouteMetadata(source: MaterialSource): String? = tradeRouteMetadataLabel(
    source.tradeScopeType, source.tradeMapId, source.tradeQuestContext,
    source.tradeInteractionWindow, source.context
)

fun tradeUsageRouteMetadata(relation: ItemUsageRelation): String? = tradeRouteMetadataLabel(
    relation.scopeType, relation.mapId, relation.questContext,
    relation.interactionWindow, relation.context
)
