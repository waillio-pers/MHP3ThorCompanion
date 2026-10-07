package com.waillio.mhp3rdcompanion.data

/** Stable quest-parent projection for Item → Sources → Supply Box. */
data class SupplyQuestProjection(
    val quest: Quest,
    val entries: List<SupplyBoxEntry>,
    val sourceIds: List<String>,
    val sourceOrder: Int
) {
    val questId: String get() = quest.id
}

private fun Quest.supplyCategoryKey(): Int = when {
    category.equals("Village", true) -> 0
    category.equals("Guild", true) && rank.equals("High Rank", true) -> 2
    category.equals("Guild", true) && rank.equals("HIGH", true) -> 2
    category.equals("Guild", true) -> 1
    category.equals("Hot Spring", true) -> 3
    category.equals("Drink", true) -> 4
    category.equals("Event", true) -> 5
    else -> 6
}

/** Progression order is intentionally not alphabetical. */
fun supplyQuestSortKey(projection: SupplyQuestProjection): List<Comparable<*>> = listOf(
    projection.quest.supplyCategoryKey(),
    projection.quest.stars,
    projection.quest.canonicalNumber ?: Int.MAX_VALUE,
    projection.sourceOrder,
    projection.quest.id
)

fun List<SupplyQuestProjection>.sortedSupplyQuests(): List<SupplyQuestProjection> =
    sortedWith(
        compareBy<SupplyQuestProjection> { it.quest.supplyCategoryKey() }
            .thenBy { it.quest.stars }
            .thenBy { it.quest.canonicalNumber ?: Int.MAX_VALUE }
            .thenBy { it.sourceOrder }
            .thenBy { it.quest.id }
    )

fun projectSupplyQuests(
    sources: List<MaterialSource>,
    quests: List<Quest>
): List<SupplyQuestProjection> {
    val questsById = quests.associateBy { it.id }
    val orderById = quests.withIndex().associate { it.value.id to it.index }
    return sources
        .filter { it.type == MaterialSourceType.SUPPLY_BOX && it.questId != null && it.supplyPersistentAcquisition != false }
        .groupBy { it.questId!! }
        .mapNotNull { (questId, rows) ->
            val quest = questsById[questId] ?: return@mapNotNull null
            val entries = rows.flatMap { source ->
                source.supplyEntries.ifEmpty {
                    listOf(
                        SupplyBoxEntry(
                            id = source.id,
                            gameItemId = source.supplyQuantityValue ?: 0,
                            quantityValue = source.supplyQuantityValue,
                            quantityNotation = source.supplyQuantityNotation,
                            bundleCount = source.supplyBundleCount,
                            quantitySemantics = when (source.supplyDistributionSemantics) {
                                "SOURCE_LAYOUT_INFINITE" -> RegularQuestSupplyQuantitySemantics.INFINITE
                                else -> if ((source.supplyBundleCount ?: 1) > 1) RegularQuestSupplyQuantitySemantics.FINITE_MULTI_BUNDLE
                                else RegularQuestSupplyQuantitySemantics.FINITE_SINGLE_BUNDLE
                            },
                            distributionSemantics = source.supplyDistributionSemantics.orEmpty(),
                            availabilityTiming = source.supplyAvailabilityTiming.orEmpty(),
                            lifecycle = source.supplyLifecycle ?: RegularQuestSupplyLifecycle.PERSISTENT_NORMAL_SUPPLY,
                            persistentAcquisition = source.supplyPersistentAcquisition == true
                        )
                    )
                }
            }
            SupplyQuestProjection(quest, entries, rows.map { it.id }, orderById[questId] ?: Int.MAX_VALUE)
        }
        .sortedSupplyQuests()
}

fun supplyQuantityLabel(entry: SupplyBoxEntry): String = when {
    entry.quantitySemantics == RegularQuestSupplyQuantitySemantics.INFINITE -> "Unlimited"
    entry.bundleCount != null && entry.bundleCount > 1 && entry.quantityValue != null ->
        "${entry.bundleCount} bundles · ×${entry.quantityValue}"
    entry.bundleCount != null && entry.bundleCount > 1 -> "${entry.bundleCount} bundles"
    entry.quantityValue != null -> "×${entry.quantityValue}"
    !entry.quantityNotation.isNullOrBlank() -> entry.quantityNotation.orEmpty()
    else -> "Quantity not published"
}

fun supplyQuestSearchMatches(projection: SupplyQuestProjection, query: String): Boolean {
    val needle = query.trim()
    if (needle.isEmpty()) return true
    return projection.quest.name.contains(needle, ignoreCase = true) ||
        projection.quest.objective.contains(needle, ignoreCase = true)
}

enum class SupplyQuestFilterKey(val label: String) {
    ALL("All"), VILLAGE("Village"), GUILD_LOW("Guild Low"), GUILD_HIGH("Guild High"),
    HOT_SPRING("Hot Spring"), DRINK("Drink"), EVENT("Event")
}

fun SupplyQuestProjection.matchesFilter(filter: SupplyQuestFilterKey): Boolean = when (filter) {
    SupplyQuestFilterKey.ALL -> true
    SupplyQuestFilterKey.VILLAGE -> quest.category.equals("Village", true)
    SupplyQuestFilterKey.GUILD_LOW -> quest.category.equals("Guild", true) && !quest.rank.contains("High", true) && !quest.rank.equals("HIGH", true)
    SupplyQuestFilterKey.GUILD_HIGH -> quest.category.equals("Guild", true) && (quest.rank.contains("High", true) || quest.rank.equals("HIGH", true))
    SupplyQuestFilterKey.HOT_SPRING -> quest.category.equals("Hot Spring", true)
    SupplyQuestFilterKey.DRINK -> quest.category.equals("Drink", true)
    SupplyQuestFilterKey.EVENT -> quest.category.equals("Event", true)
}
