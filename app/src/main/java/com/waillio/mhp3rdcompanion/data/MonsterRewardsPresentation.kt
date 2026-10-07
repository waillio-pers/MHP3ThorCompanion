package com.waillio.mhp3rdcompanion.data

enum class MonsterRewardFilter(val label: String) {
    ALL("All"),
    CARVE("Carve"),
    CAPTURE("Capture"),
    BREAK("Break"),
    SHINY("Shiny"),
    MINING("Mining")
}

enum class MonsterRewardRouteFamily {
    CARVE,
    CAPTURE,
    BREAK,
    SHINY,
    MINING
}

data class MonsterRewardRoute(
    val label: String,
    val family: MonsterRewardRouteFamily,
    val relations: List<MonsterReward>
)

data class MonsterRewardItemRow(
    val gameItemId: Int,
    val itemName: String,
    val firstRelationIndex: Int,
    val routes: List<MonsterRewardRoute>
) {
    val relations: List<MonsterReward> get() = routes.flatMap { it.relations }
}

/**
 * Re-groups accepted normal reward relations by numeric game Item identity.
 * Linked maps deliberately preserve the source's first-seen Item and route order.
 */
fun List<MonsterReward>.itemFirstMonsterRewards(
    context: RewardContext,
    filter: MonsterRewardFilter = MonsterRewardFilter.ALL
): List<MonsterRewardItemRow> {
    val selected = asSequence()
        .filter { it.rank == context }
        .filter { filter == MonsterRewardFilter.ALL || it.routeFamily().toFilter() == filter }
        .toList()
    val originalIndexes = withIndex().associate { (index, reward) -> reward to index }

    return selected
        .groupByTo(linkedMapOf()) { reward ->
            requireNotNull(reward.gameItemId) {
                "Large-monster reward ${reward.relationId ?: reward.item} has no numeric gameItemId"
            }
        }
        .map { (gameItemId, itemRelations) ->
            val routes = itemRelations
                .groupByTo(linkedMapOf()) { it.displayRouteLabel() }
                .map { (label, routeRelations) ->
                    MonsterRewardRoute(label, routeRelations.first().routeFamily(), routeRelations)
                }
            MonsterRewardItemRow(
                gameItemId = gameItemId,
                itemName = itemRelations.first().item,
                firstRelationIndex = originalIndexes[itemRelations.first()] ?: Int.MAX_VALUE,
                routes = routes
            )
        }
}

fun MonsterReward.displayRouteLabel(): String = when (sourceType) {
    "Body Carve" -> "Body Carve"
    "Tail Carve" -> "Tail Carve"
    "Capture" -> "Capture"
    "Part Break" -> "${condition.requireMonsterRewardCondition("Part Break")} Break"
    "Shiny" -> "Shiny Drop"
    "Mining" -> when (condition.requireMonsterRewardCondition("Mining")) {
        "Mining Points" -> "Mining Points"
        "Tail (Mining)" -> "Tail Mining"
        else -> error("Unsupported mining route condition '$condition' on ${relationId ?: item}")
    }
    else -> error("Unsupported large-monster reward source type '$sourceType' on ${relationId ?: item}")
}

fun MonsterReward.routeFamily(): MonsterRewardRouteFamily = when (sourceType) {
    "Body Carve", "Tail Carve" -> MonsterRewardRouteFamily.CARVE
    "Capture" -> MonsterRewardRouteFamily.CAPTURE
    "Part Break" -> MonsterRewardRouteFamily.BREAK
    "Shiny" -> MonsterRewardRouteFamily.SHINY
    "Mining" -> MonsterRewardRouteFamily.MINING
    else -> error("Unsupported large-monster reward source type '$sourceType' on ${relationId ?: item}")
}

fun MonsterRewardRouteFamily.toFilter(): MonsterRewardFilter = when (this) {
    MonsterRewardRouteFamily.CARVE -> MonsterRewardFilter.CARVE
    MonsterRewardRouteFamily.CAPTURE -> MonsterRewardFilter.CAPTURE
    MonsterRewardRouteFamily.BREAK -> MonsterRewardFilter.BREAK
    MonsterRewardRouteFamily.SHINY -> MonsterRewardFilter.SHINY
    MonsterRewardRouteFamily.MINING -> MonsterRewardFilter.MINING
}

private fun String.requireMonsterRewardCondition(method: String): String = trim().takeIf { it.isNotEmpty() }
    ?: error("Large-monster reward $method is missing its accepted condition")
