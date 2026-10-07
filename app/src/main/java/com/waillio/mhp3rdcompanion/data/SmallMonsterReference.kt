package com.waillio.mhp3rdcompanion.data

private val smallMonsterContextPriority = listOf(
    SmallMonsterRewardContext.LOW,
    SmallMonsterRewardContext.HIGH,
    SmallMonsterRewardContext.GUILD_1_2
)

fun List<SmallMonsterReward>.availableSmallMonsterContexts(): List<SmallMonsterRewardContext> =
    smallMonsterContextPriority.filter { context -> any { it.context == context } }

data class SmallMonsterRewardGroup(
    val method: SmallMonsterRewardMethod,
    val condition: String?,
    val conditionDetails: SmallMonsterRewardConditionDetails?,
    val rewards: List<SmallMonsterReward>
) {
    val title: String
        get() = buildList {
            add(method.displayLabel())
            conditionDetails?.wingColor?.let { add(it.displayLabel()) }
            conditionDetails?.locations?.takeIf { it.isNotEmpty() }?.let { add(it.joinToString(" / ")) }
            conditionDetails?.color?.let { add("${it.displayLabel()} abdomen") }
            condition?.takeUnless { it == "WING_COLOR_LOCATION_VARIANT" || it == "ABDOMEN_COLOR" }
                ?.let { add(it.displayLabel()) }
        }.joinToString(" · ")
}

private data class SmallMonsterRewardGroupKey(
    val method: SmallMonsterRewardMethod,
    val condition: String?,
    val locations: List<String>,
    val wingColor: String?,
    val color: String?
)

fun List<SmallMonsterReward>.groupForSmallMonsterDisplay(
    context: SmallMonsterRewardContext
): List<SmallMonsterRewardGroup> =
    filter { it.context == context }
        .groupBy { reward ->
            SmallMonsterRewardGroupKey(
                reward.method,
                reward.condition,
                reward.conditionDetails?.locations.orEmpty(),
                reward.conditionDetails?.wingColor,
                reward.conditionDetails?.color
            )
        }
        .map { (key, rows) ->
            SmallMonsterRewardGroup(
                key.method,
                key.condition,
                rows.first().conditionDetails,
                rows
            )
        }

private fun SmallMonsterRewardMethod.displayLabel(): String = when (this) {
    SmallMonsterRewardMethod.BODY_CARVE -> "Body Carve"
    SmallMonsterRewardMethod.SHINY_DROP -> "Shiny Drop"
}

private fun String.displayLabel(): String = lowercase().split('_').joinToString(" ") { word ->
    word.replaceFirstChar { it.uppercase() }
}
