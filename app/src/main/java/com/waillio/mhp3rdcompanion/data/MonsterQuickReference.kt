package com.waillio.mhp3rdcompanion.data

enum class PhysicalDamageType { CUT, IMPACT, SHOT }

data class BestHitArea(
    val damageType: PhysicalDamageType,
    val bodyPart: String,
    val value: Int
)

data class BreakablePartGroup(
    val name: String,
    val sourceRows: List<BreakablePart>
) {
    val actions: List<String> = sourceRows.map { it.action }.distinct()
}

data class HuntPrepCounterSummary(
    val threatType: HunterThreatType,
    val items: List<HuntPrepCounterItem>
) {
    val itemNames: List<String> get() = items.map { it.itemName }
}

data class RewardGroup(
    val method: String,
    val condition: String,
    val title: String,
    val rewards: List<MonsterReward>
)

val REWARD_CONTEXT_PROGRESSION = listOf(
    RewardContext.LOW,
    RewardContext.HIGH,
    RewardContext.GUILD_1_2,
    RewardContext.VILLAGE_2_SPECIAL
)

fun List<MonsterReward>.availableContexts(): List<RewardContext> {
    val available = map { it.rank }.toSet()
    return REWARD_CONTEXT_PROGRESSION.filter { it in available }
}

fun List<MonsterReward>.groupsFor(context: RewardContext): List<RewardGroup> =
    filter { it.rank == context }
        .groupByTo(linkedMapOf()) { it.sourceType to it.condition }
        .map { (key, rows) ->
            RewardGroup(key.first, key.second, rewardGroupTitle(key.first, key.second), rows)
        }

private fun rewardGroupTitle(method: String, condition: String): String = when {
    method == "Part Break" -> "$condition Break"
    method == "Body Carve" -> "Body Carve"
    method == "Tail Carve" -> "Tail Carve"
    method == "Capture" -> "Capture"
    method == "Shiny" && condition.isNotBlank() -> condition
    method == "Mining" && condition.isNotBlank() -> condition
    condition.isBlank() || method.contains(condition, ignoreCase = true) -> method
    else -> "$condition · $method"
}

fun RewardContext.displayLabel(): String = when (this) {
    RewardContext.GUILD_1_2 -> "Guild ★1–2"
    RewardContext.VILLAGE_2_SPECIAL -> "Village ★2"
    RewardContext.LOW -> "Low Rank"
    RewardContext.HIGH -> "High Rank"
}

fun Monster.bestHitAreas(): List<BestHitArea> = listOf(
    PhysicalDamageType.CUT to Hitzone::cut,
    PhysicalDamageType.IMPACT to Hitzone::impact,
    PhysicalDamageType.SHOT to Hitzone::shot
).mapNotNull { (damageType, valueOf) ->
    hitzones.maxByOrNull(valueOf)?.let { zone ->
        BestHitArea(damageType, zone.part, valueOf(zone))
    }
}

fun Monster.groupedBreakableParts(): List<BreakablePartGroup> =
    breakableParts.groupByTo(linkedMapOf()) { it.name }.map { (name, rows) ->
        BreakablePartGroup(name, rows)
    }

fun MonsterHuntPrep.counterSummaries(): List<HuntPrepCounterSummary> =
    threats.map { it.type }.distinct().mapNotNull { threatType ->
        counterItems
            .filter { threatType in it.counterFor }
            .distinctBy { it.itemGameId }
            .takeIf { it.isNotEmpty() }
            ?.let { HuntPrepCounterSummary(threatType, it) }
    }

fun HunterThreatType.displayLabel(): String = when (this) {
    HunterThreatType.ALL_RESISTANCE_DOWN_LARGE -> "All Resistance Down (Large)"
    HunterThreatType.DEFENSE_DOWN_LARGE -> "Defense Down (Large)"
    HunterThreatType.DEFENSE_DOWN_SMALL -> "Defense Down (Small)"
    HunterThreatType.FIREBLIGHT -> "Fireblight"
    HunterThreatType.ICEBLIGHT -> "Iceblight"
    HunterThreatType.MUD -> "Mud"
    HunterThreatType.PARALYSIS -> "Paralysis"
    HunterThreatType.POISON -> "Poison"
    HunterThreatType.SLEEP -> "Sleep"
    HunterThreatType.SNOWMAN -> "Snowman"
    HunterThreatType.STENCH -> "Stench"
    HunterThreatType.STUN -> "Stun"
    HunterThreatType.TERRAIN_ELEMENTAL_BLIGHT -> "Terrain Elemental Blight"
    HunterThreatType.THUNDERBLIGHT -> "Thunderblight"
    HunterThreatType.TREMOR -> "Tremor"
    HunterThreatType.WATERBLIGHT -> "Waterblight"
}

fun PhysicalDamageType.displayLabel(): String = when (this) {
    PhysicalDamageType.CUT -> "Cut"
    PhysicalDamageType.IMPACT -> "Impact"
    PhysicalDamageType.SHOT -> "Shot"
}

fun TacticalPriority.displayLabel(): String = when (this) {
    TacticalPriority.CORE -> "Core"
    TacticalPriority.RECOMMENDED -> "Recommended"
    TacticalPriority.CONDITIONAL -> "Conditional"
}
