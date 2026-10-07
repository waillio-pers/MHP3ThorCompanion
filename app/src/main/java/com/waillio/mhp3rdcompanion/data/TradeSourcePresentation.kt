package com.waillio.mhp3rdcompanion.data

/** Source-only presentation grouping; raw relations and their ordering are retained. */
data class TradeSourceInputGroup(
    val inputGameItemId: Int,
    val inputName: String,
    val routes: List<MaterialSource>
) {
    val costQuantities: List<Int> get() = routes.mapNotNull { it.tradeInputQuantity }.distinct()
}

data class TradeSourcesPresentation(
    val routeCount: Int,
    val veggieElderRoutes: List<MaterialSource>,
    val veggieElderInputs: List<TradeSourceInputGroup>,
    val farmManagerRoutes: List<MaterialSource>
)

fun tradeSourcesPresentation(sources: List<MaterialSource>): TradeSourcesPresentation {
    val elder = sources.filter { it.tradeMechanism == "VEGGIE_ELDER_ITEM_EXCHANGE" && it.inputGameItemId != null }
    val grouped = linkedMapOf<Int, MutableList<MaterialSource>>()
    elder.forEach { source -> grouped.getOrPut(source.inputGameItemId!!) { mutableListOf() }.add(source) }
    val inputs = grouped.map { (gameItemId, routes) ->
        val name = routes.firstNotNullOfOrNull { route -> route.inputItemName?.takeIf(String::isNotBlank) }
            ?: "Item #$gameItemId"
        TradeSourceInputGroup(gameItemId, name, routes.toList())
    }
    val farm = sources.filter { it.tradeMechanism == "FARM_MANAGER_POINT_EXCHANGE" }
    return TradeSourcesPresentation(
        routeCount = elder.size + farm.size,
        veggieElderRoutes = elder,
        veggieElderInputs = inputs,
        farmManagerRoutes = farm
    )
}

/** First N raw input/route units in accepted source order, without pair deduplication. */
fun tradeRoutePreview(sources: List<MaterialSource>, limit: Int = 6): List<MaterialSource> =
    sources.filter { it.tradeMechanism == "VEGGIE_ELDER_ITEM_EXCHANGE" }.take(limit)

fun tradeUsageRouteSummary(relation: ItemUsageRelation): String = listOfNotNull(
    relation.outputQuantity?.let { "Receive ×$it" },
    relation.quantity?.let { "Cost ×$it" },
    tradeUsageRouteMetadata(relation)
).joinToString(" · ")
