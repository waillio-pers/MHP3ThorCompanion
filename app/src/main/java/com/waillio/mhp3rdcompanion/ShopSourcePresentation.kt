package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.MaterialSource
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import java.text.NumberFormat
import java.util.Locale

internal data class ShopDisplayRow(
    val label: String,
    val relationIds: List<String>,
    val priceZenny: Int,
    val availabilityLabel: String? = null,
    val qualifier: String? = null
) {
    val priceLabel: String get() = "${formatShopZenny(priceZenny)}z"
}

internal data class ShopSourceProjection(
    val relationCount: Int,
    val storeRows: List<ShopDisplayRow>,
    val peddlerRows: List<ShopDisplayRow>
)

private data class StoreEquivalenceKey(
    val gameItemId: Int?,
    val priceZenny: Int?,
    val availabilityConditionId: String?
)

/** Source-side presentation keeps purchase relation identity separate from visual aggregation. */
internal fun shopSourcesPresentation(sources: List<MaterialSource>): ShopSourceProjection {
    val purchases = sources.filter { it.type == MaterialSourceType.SHOP_PURCHASE }
    val general = purchases.filter { it.shopId == "GENERAL_STORE" }.sortedBy { it.id }.toMutableList()
    val hunters = purchases.filter { it.shopId == "HUNTERS_STORE" }.sortedBy { it.id }.toMutableList()
    val storeRows = mutableListOf<ShopDisplayRow>()

    general.forEach { generalSource ->
        val key = generalSource.storeEquivalenceKey()
        val hunterIndex = hunters.indexOfFirst { it.storeEquivalenceKey() == key }
        val matchedHunter = if (hunterIndex >= 0) hunters.removeAt(hunterIndex) else null
        val matchedSources = listOfNotNull(generalSource, matchedHunter)
        storeRows += storeRow(
            label = if (matchedHunter == null) "General Store" else "General Store / Hunter's Store",
            sources = matchedSources
        )
    }
    hunters.forEach { storeRows += storeRow("Hunter's Store", listOf(it)) }

    // Unknown shop identifiers remain represented but are never rendered as raw enum strings.
    purchases.filter { it.shopId !in setOf("GENERAL_STORE", "HUNTERS_STORE", "PEDDLER") }
        .sortedBy { it.id }
        .forEach { source -> storeRows += storeRow("Other shop", listOf(source)) }

    val peddlerRows = purchases.filter { it.shopId == "PEDDLER" }
        .sortedWith(compareBy<MaterialSource> { peddlerProfileOrder(it.shopInventoryProfileId) }.thenBy { it.id })
        .map { source ->
            val profile = peddlerDisplay(source.shopInventoryProfileId, source.shopProfileSemantics)
            ShopDisplayRow(
                label = profile.label,
                relationIds = listOf(source.id),
                priceZenny = requireNotNull(source.priceZenny) { "Shop relation ${source.id} is missing its published price" },
                qualifier = profile.qualifier
            )
        }

    return ShopSourceProjection(
        relationCount = purchases.map { it.id }.distinct().size,
        storeRows = storeRows,
        peddlerRows = peddlerRows
    )
}

private fun MaterialSource.storeEquivalenceKey() = StoreEquivalenceKey(
    gameItemId = shopGameItemId,
    priceZenny = priceZenny,
    availabilityConditionId = shopAvailabilityConditionId
)

private fun storeRow(label: String, sources: List<MaterialSource>): ShopDisplayRow {
    val source = sources.first()
    val price = requireNotNull(source.priceZenny) { "Shop relation ${source.id} is missing its published price" }
    require(sources.all { it.priceZenny == price }) { "Cannot aggregate stores with different prices" }
    return ShopDisplayRow(
        label = label,
        relationIds = sources.map { it.id },
        priceZenny = price,
        availabilityLabel = shopAvailabilityLabel(source)
    )
}

internal fun shopAvailabilityLabel(source: MaterialSource): String {
    if (source.shopAvailabilityConditionId == null || source.shopAvailabilityConditionId == "INITIAL") return "Initial stock"
    val label = source.condition?.takeIf { it.isNotBlank() } ?: return ""
    return when (source.shopAvailabilityConditionSemantics) {
        "OR" -> "Either: ${label.replace(" / ", " or ")}"
        "CLEAR" -> "After clearing: $label"
        "APPEARS" -> "When the appearance condition is met: $label"
        "OR_WITH_APPEARS" -> "Clear or appearance: ${label.replace(" / ", " or ")}"
        else -> label
    }
}

private data class PeddlerDisplay(val label: String, val qualifier: String?)

private fun peddlerDisplay(profileId: String?, semantics: String?): PeddlerDisplay = when (profileId) {
    "PEDDLER_PATTERN_1" -> PeddlerDisplay("Inventory 1", "Half-price inventory".takeIf { semantics == "HALF_PRICE_PROFILE" })
    "PEDDLER_PATTERN_2" -> PeddlerDisplay("Inventory 2", null)
    "PEDDLER_PATTERN_3" -> PeddlerDisplay("Inventory 3", "Half-price inventory".takeIf { semantics == "HALF_PRICE_PROFILE" })
    "PEDDLER_PATTERN_4" -> PeddlerDisplay("Inventory 4", null)
    "PEDDLER_PATTERN_5" -> PeddlerDisplay("Download bonus inventory", null)
    else -> PeddlerDisplay("Peddler inventory", null)
}

private fun peddlerProfileOrder(profileId: String?): Int = when (profileId) {
    "PEDDLER_PATTERN_1" -> 1
    "PEDDLER_PATTERN_2" -> 2
    "PEDDLER_PATTERN_3" -> 3
    "PEDDLER_PATTERN_4" -> 4
    "PEDDLER_PATTERN_5" -> 5
    else -> Int.MAX_VALUE
}

internal fun formatShopZenny(priceZenny: Int): String =
    NumberFormat.getIntegerInstance(Locale.US).format(priceZenny)
