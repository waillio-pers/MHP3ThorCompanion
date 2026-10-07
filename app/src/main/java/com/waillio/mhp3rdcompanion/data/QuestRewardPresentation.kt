package com.waillio.mhp3rdcompanion.data

/** Presentation-only projection for Item → Sources → Quest Rewards.
 * The raw relation identity remains on every entry; grouping is only for the
 * Quest-first presentation and never merges factual rows. */
data class QuestRewardEntryProjection(
    val source: MaterialSource,
    val rawRelationId: String,
    val questId: String,
    val rewardPool: String,
    val probabilityValuePercent: Int?,
    val probabilitySemantics: String?,
    val quantity: Int?,
    val condition: String?
)

data class QuestRewardPoolProjection(
    val rewardPool: String,
    val entries: List<QuestRewardEntryProjection>
)

data class QuestRewardQuestProjection(
    val quest: Quest,
    val pools: List<QuestRewardPoolProjection>,
    val sourceOrder: Int
) {
    val questId: String get() = quest.id
    val entries: List<QuestRewardEntryProjection> get() = pools.flatMap { it.entries }
}

private val questRewardPoolPriority = mapOf("FIXED" to 0, "BASIC" to 1, "ADDITIONAL" to 2)

fun questRewardPoolLabel(pool: String): String = when (pool.uppercase()) {
    "FIXED" -> "Fixed"
    "BASIC" -> "Basic"
    "ADDITIONAL" -> "Additional"
    else -> "Reward pool unavailable"
}

private val questRewardConditionPattern = Regex("^(\\d+)頭討伐$")

/** Current ordinary reward conditions are published as an exact monster-count
 * condition. Unknown future values are deliberately not mechanically exposed. */
fun questRewardConditionLabel(condition: String?): String? {
    if (condition == null) return null
    val count = questRewardConditionPattern.matchEntire(condition)?.groupValues?.get(1)
    return count?.let { "Slay $it monsters" } ?: "Condition label unavailable"
}

fun projectQuestRewards(
    sources: List<MaterialSource>,
    quests: List<Quest>
): List<QuestRewardQuestProjection> {
    val questsById = quests.associateBy { it.id }
    val orderById = quests.withIndex().associate { it.value.id to it.index }
    return sources
        .withIndex()
        .filter { it.value.type == MaterialSourceType.QUEST_REWARD && it.value.questId != null }
        .groupBy { it.value.questId!! }
        .mapNotNull { (questId, indexedRows) ->
            val quest = questsById[questId] ?: return@mapNotNull null
            val pools = indexedRows
                .groupBy { it.value.rewardPool ?: it.value.method.orEmpty().uppercase() }
                .map { (pool, rows) ->
                    QuestRewardPoolProjection(
                        rewardPool = pool,
                        entries = rows.map { indexed ->
                            val source = indexed.value
                            QuestRewardEntryProjection(
                                source = source,
                                rawRelationId = source.rewardRelationId ?: source.id,
                                questId = questId,
                                rewardPool = pool,
                                probabilityValuePercent = source.chance,
                                probabilitySemantics = source.probabilitySemantics,
                                quantity = source.quantity,
                                condition = source.condition
                            )
                        }
                    )
                }
                .sortedWith(compareBy { questRewardPoolPriority[it.rewardPool.uppercase()] ?: Int.MAX_VALUE })
            QuestRewardQuestProjection(quest, pools, orderById[questId] ?: indexedRows.minOf { it.index })
        }
        .map { projection ->
            // Convert through the accepted Supply ordering primitive so both
            // Quest-first source families share exactly one deterministic order.
            SupplyQuestProjection(
                quest = projection.quest,
                entries = emptyList(),
                sourceIds = listOf(projection.questId),
                sourceOrder = projection.sourceOrder
            ) to projection
        }
        .sortedWith(Comparator { left, right -> left.first.compareTo(right.first) })
        .map { it.second }
}

private operator fun SupplyQuestProjection.compareTo(other: SupplyQuestProjection): Int {
    val left = supplyQuestSortKey(this)
    val right = supplyQuestSortKey(other)
    for (index in left.indices) {
        @Suppress("UNCHECKED_CAST")
        val result = (left[index] as Comparable<Any>).compareTo(right[index] as Any)
        if (result != 0) return result
    }
    return 0
}

fun questRewardSearchMatches(projection: QuestRewardQuestProjection, query: String): Boolean {
    val needle = query.trim()
    if (needle.isEmpty()) return true
    return projection.quest.name.contains(needle, ignoreCase = true) ||
        projection.quest.objective.contains(needle, ignoreCase = true)
}
