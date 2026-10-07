package com.waillio.mhp3rdcompanion.data

private val FIELD_SOURCE_TYPES = setOf(
    MaterialSourceType.GATHERING,
    MaterialSourceType.MINING,
    MaterialSourceType.BUG,
    MaterialSourceType.FISHING
)

data class FieldSourceAvailability(
    val rank: RewardContext?,
    val chance: Int?,
    val quantity: Int?
)

data class FieldSourceGroup(
    val nodeId: String,
    val location: String,
    val context: String?,
    val method: String?,
    val availability: List<FieldSourceAvailability>,
    val sourceRows: List<MaterialSource>
)

data class FarmSourceOutcome(
    val quantity: Int?,
    val chance: Int?,
    val probabilitySemantics: String?,
    val quantitySemantics: String?,
    val source: MaterialSource
)

data class FarmSourceGroup(
    /** Stable identity accepted in FARM-METHOD-DISTRIBUTION.json. */
    val stableMethodId: String,
    val facilityId: String,
    val facilityDisplayName: String,
    val action: String,
    val inputGameItemId: Int?,
    val inputItemName: String?,
    val condition: String?,
    val triggerStatus: FarmTriggerStatus,
    val triggerLabel: String,
    val outcomes: List<FarmSourceOutcome>,
    val sourceRows: List<MaterialSource>,
    val publishedPoolTotalPercent: Int? = null
)

enum class FarmTriggerStatus { ITEM_INPUT, BLOCKED, FACILITY_ROUTE, SOURCE_CONDITION }

data class FarmSourceFacilityGroup(
    val facilityId: String,
    val displayName: String,
    val methods: List<FarmSourceGroup>,
    val mechanismNote: FarmFacilityMechanismNote? = null
)

data class FarmFacilityMechanismNote(
    val summary: String,
    val sourceItemReferences: List<FarmMechanismItemReference>
)

data class FarmMechanismItemReference(val gameItemId: Int, val displayName: String)

/** Broad Mining Cart behavior is source-backed; no numbered profile is mapped to a selector. */
internal val FARM_MINING_CART_MECHANISM_NOTE = FarmFacilityMechanismNote(
    summary = "Up to 4 Palicoes go mining; the supplied tool affects failure, normal, or success rates.",
    sourceItemReferences = listOf(
        FarmMechanismItemReference(84, "Old Pickaxe"),
        FarmMechanismItemReference(85, "Iron Pickaxe"),
        FarmMechanismItemReference(86, "Mega Pickaxe"),
        FarmMechanismItemReference(170, "Pickaxe Mushroom")
    )
)

/** Trigger classification frozen by FARM-PROFILE-TRIGGER-MATRIX.json. */
internal val FARM_TRIGGER_BLOCKED_PROFILE_IDS = setOf(
    "farm_bug_tree_profile_1a", "farm_bug_tree_profile_1b",
    "farm_bug_tree_profile_2a", "farm_bug_tree_profile_2b",
    "farm_bug_tree_profile_3a", "farm_bug_tree_profile_3b",
    "farm_bug_tree_profile_4a", "farm_bug_tree_profile_4b",
    "farm_bug_tree_profile_5a", "farm_bug_tree_profile_5b",
    "farm_bug_tree_profile_6a", "farm_bug_tree_profile_6b",
    "farm_custom_roaster_profile_01_03", "farm_custom_roaster_profile_04_06",
    "farm_custom_roaster_profile_07_09", "farm_custom_roaster_profile_10",
    "farm_fish_basket_profile_01", "farm_fish_basket_profile_02",
    "farm_fish_basket_profile_03", "farm_fish_basket_profile_04",
    "farm_fish_basket_profile_05", "farm_fish_basket_profile_06",
    "farm_fish_basket_profile_07", "farm_fish_basket_profile_08",
    "farm_fish_basket_profile_09", "farm_fish_basket_profile_10",
    "farm_fish_basket_profile_11", "farm_fish_basket_profile_12",
    "farm_fish_basket_profile_13", "farm_fish_basket_profile_14",
    "farm_fish_basket_profile_15", "farm_fish_basket_profile_16",
    "farm_mining_cart_profile_01", "farm_mining_cart_profile_02",
    "farm_mining_cart_profile_03", "farm_mining_cart_profile_04",
    "farm_mining_cart_profile_05", "farm_mining_cart_profile_06"
)

/** Human-facing names copied from the accepted Farm facility matrix. */
internal val FARM_FACILITY_DISPLAY_NAMES = mapOf(
    "farm_beehive_improved" to "Beehive · Improved",
    "farm_beehive_prototype" to "Beehive · Prototype",
    "farm_beehive_standard" to "Beehive · Standard",
    "farm_bug_cage_beetle_nectar" to "Insect Cage · Beetle Insect Lure",
    "farm_bug_cage_black_nectar" to "Insect Cage · Black Insect Lure",
    "farm_bug_cage_initial" to "Insect Cage · Insect Lure",
    "farm_bug_cage_premium_nectar" to "Insect Cage · Insect Lure+",
    "farm_bug_cage_rare_nectar" to "Insect Cage · Rare Insect Lure",
    "farm_bug_cage_royal_nectar" to "Insect Cage · Royal Insect Lure",
    "farm_bug_tree_seesaw" to "Bug Tree Seesaw",
    "farm_custom_roaster" to "Custom Roaster",
    "farm_field_additional_slot" to "Field · bonus harvest slot",
    "farm_field_green_seed" to "Field · Green Seed",
    "farm_field_planted_item" to "Field · planted item",
    "farm_field_red_seed" to "Field · Red Seed",
    "farm_giant_fish_basket" to "Giant Fish Basket",
    "farm_mining_cart" to "Palico Mining Cart",
    "farm_mining_point_base" to "Mining Point · initial",
    "farm_mining_point_plus_1" to "Mining Point · +1",
    "farm_mining_point_plus_2" to "Mining Point · +2",
    "farm_mining_point_plus_3" to "Mining Point · +3",
    "farm_mushroom_tree_base" to "Mushroom Tree · initial",
    "farm_mushroom_tree_deluxe" to "Mushroom Tree · Deluxe",
    "farm_mushroom_tree_pickaxe_shroom" to "Mushroom Tree · Pickaxe Mushroom special slot",
    "farm_mushroom_tree_supreme" to "Mushroom Tree · Supreme",
    "farm_net_and_pier_initial" to "Net & Pier · initial",
    "farm_net_and_pier_plus_1" to "Net & Pier · +1",
    "farm_net_and_pier_plus_2" to "Net & Pier · +2"
)

/** Whole-source-pool totals from the accepted method distribution, not item-level sums. */
private val FARM_PUBLISHED_POOL_TOTALS = mapOf(
    "farm_bug_tree_seesaw::profile:farm_bug_tree_profile_4a" to 105,
    "farm_bug_tree_seesaw::profile:farm_bug_tree_profile_6a" to 90,
    "farm_mushroom_tree_supreme::default" to 95
)

private const val FARM_GENERIC_SOIL_CONDITION = "soil level changes harvest-slot count"
private const val FARM_PICKAXE_CART_CONDITION = "Mining Cart unlocked"

/** Stable-key rule and the generic soil note exclusion are frozen by the accepted method distribution. */
fun MaterialSource.farmStableMethodId(): String {
    val facility = requireNotNull(facilityId) { "Farm source ${id} has no facility id" }
    val identity = when {
        profileId != null -> "profile:$profileId"
        inputGameItemId != null -> "inputGameItemId:$inputGameItemId"
        condition != null && condition.isNotBlank() && condition != FARM_GENERIC_SOIL_CONDITION -> "condition:$condition"
        else -> "default"
    }
    return "$facility::$identity"
}

/** Returns the source-ordered facility groups used both inline and in View All. */
fun List<FarmSourceGroup>.groupFarmMethodsByFacility(): List<FarmSourceFacilityGroup> =
    groupBy { it.facilityId }.map { (facilityId, methods) ->
        FarmSourceFacilityGroup(
            facilityId = facilityId,
            displayName = methods.first().facilityDisplayName,
            methods = methods,
            mechanismNote = FARM_MINING_CART_MECHANISM_NOTE.takeIf { facilityId == "farm_mining_cart" }
        )
    }

fun farmVisibleMethodCount(methodCount: Int): Int = minOf(methodCount, 6)

fun FarmSourceOutcome.displayLabel(): String = when {
    probabilitySemantics == "DETERMINISTIC_PER_HARVEST_SLOT" ->
        "Yield ×${quantity ?: "?"} per harvest slot"
    else -> listOfNotNull(
        quantity?.let { "Yield ×$it" },
        chance?.takeIf { probabilitySemantics == "FARM_OUTPUT" }?.let { "$it%" }
    ).joinToString(" · ").ifBlank { "Yield details unavailable" }
}

/** Presentation-only umbrella for the nine factual Special mechanisms. */
data class SpecialSourceEntryProjection(
    val rawRelationId: String,
    val source: MaterialSource
) {
    val mechanism: String get() = source.specialFreeMechanism.orEmpty()
    val quantity: Int? get() = source.quantity.takeIf { source.quantitySemantics == "PUBLISHED" }
}

data class SpecialSourceMethodProjection(
    val mechanism: String,
    val displayLabel: String,
    val entries: List<SpecialSourceEntryProjection>
)

data class SpecialItemSourceProjection(
    val rawRelationCount: Int,
    val methods: List<SpecialSourceMethodProjection>
) {
    val methodCount: Int get() = methods.size
    val projectedRelationCount: Int get() = methods.sumOf { it.entries.size }
    val rawRelationIds: List<String> get() = methods.flatMap { it.entries }.map { it.rawRelationId }
}

private val SPECIAL_METHOD_ORDER = listOf(
    "VEGGIE_ELDER_FREE_GIFT",
    "NPC_PROGRESSION_GRANT",
    "PROGRESSION_COMPLETION_GRANT",
    "PALICO_AFFECTION_TICKET_GRANT",
    "GUILD_FRIENDSHIP_TICKET_GRANT",
    "DRINK_TICKET_GRANT",
    "HOT_SPRING_TICKET_GRANT",
    "VILLAGE_INTERACTION_GIFT",
    "INITIAL_FREE_GRANT"
)

internal fun specialMethodDisplayLabel(mechanism: String): String = when (mechanism) {
    "VEGGIE_ELDER_FREE_GIFT" -> "Veggie Elder Gift"
    "NPC_PROGRESSION_GRANT" -> "NPC Progression Reward"
    "PROGRESSION_COMPLETION_GRANT" -> "Progression Completion"
    "PALICO_AFFECTION_TICKET_GRANT" -> "Palico Affection"
    "GUILD_FRIENDSHIP_TICKET_GRANT" -> "Guild Friendship"
    "DRINK_TICKET_GRANT" -> "Drink Shop Reward"
    "HOT_SPRING_TICKET_GRANT" -> "Bathhouse Reward"
    "VILLAGE_INTERACTION_GIFT" -> "Village Interaction"
    "INITIAL_FREE_GRANT" -> "Initial Grant"
    else -> "Special"
}

fun List<MaterialSource>.projectSpecialSources(): SpecialItemSourceProjection {
    val rows = filter { it.type == MaterialSourceType.SPECIAL_FREE }
    val methods = rows.groupBy { it.specialFreeMechanism.orEmpty() }
        .entries
        .sortedWith(compareBy({ SPECIAL_METHOD_ORDER.indexOf(it.key).let { index -> if (index < 0) Int.MAX_VALUE else index } }, { it.key }))
        .map { (mechanism, entries) ->
            SpecialSourceMethodProjection(
                mechanism = mechanism,
                displayLabel = specialMethodDisplayLabel(mechanism),
                entries = entries.sortedBy { it.id }.map { SpecialSourceEntryProjection(it.id, it) }
            )
        }
    return SpecialItemSourceProjection(rows.size, methods)
}

/** Presentation-only Training Rewards hierarchy. Raw rows remain attached to every entry. */
data class TrainingRewardEntryProjection(
    val rawRelationId: String,
    val quantity: Int?,
    val quantitySemantics: String,
    val probabilitySemantics: String,
    val probabilityValuePercent: Int?,
    val sourceOrdinal: Int,
    val source: MaterialSource
)

data class TrainingRewardGroupProjection(
    val rawRewardType: String,
    val displayLabel: String?,
    val entries: List<TrainingRewardEntryProjection>
)

data class TrainingMissionSourceProjection(
    val trainingQuestId: String,
    val trainingClass: String,
    val displayTitle: String,
    val navigationTarget: String,
    val rewardGroups: List<TrainingRewardGroupProjection>
)

data class TrainingRewardItemProjection(
    val rawRelationCount: Int,
    val missions: List<TrainingMissionSourceProjection>
) {
    val missionCount: Int get() = missions.size
}

private val TRAINING_CLASS_ORDER = listOf("BEGINNER", "GROUP", "CHALLENGE")
private val TRAINING_REWARD_TYPE_ORDER = listOf("確定報酬", "基本報酬", "追加報酬")

internal fun trainingClassDisplayLabel(trainingClass: String): String = when (trainingClass) {
    "BEGINNER" -> "Beginner"
    "GROUP" -> "Group"
    "CHALLENGE" -> "Challenge"
    else -> trainingClass
}

/** Null is intentional for ATWIKI_CANDIDATE and any unproven marker. */
internal fun trainingRewardPoolDisplayLabel(rawRewardType: String): String? = when (rawRewardType) {
    "確定報酬" -> "Guaranteed Rewards"
    "基本報酬" -> "Basic Rewards"
    "追加報酬" -> "Additional Rewards"
    else -> null
}

private fun trainingClassSortIndex(trainingClass: String?): Int =
    TRAINING_CLASS_ORDER.indexOf(trainingClass).let { if (it < 0) Int.MAX_VALUE else it }

private fun trainingRewardTypeSortIndex(rawRewardType: String): Int =
    TRAINING_REWARD_TYPE_ORDER.indexOf(rawRewardType).let { if (it < 0) Int.MAX_VALUE else it }

fun List<MaterialSource>.projectTrainingRewards(): TrainingRewardItemProjection {
    val rows = filter { it.type == MaterialSourceType.TRAINING_REWARD && it.trainingQuestId != null }
    val missions = rows
        .groupBy { it.trainingQuestId!! }
        .values
        .map { missionRows ->
            val first = missionRows.first()
            val groups = missionRows
                .groupBy { it.method.orEmpty() }
                .entries
                .sortedWith(
                    compareBy<Map.Entry<String, List<MaterialSource>>> { trainingRewardTypeSortIndex(it.key) }
                        .thenBy { it.value.minOfOrNull { row -> row.sourceOrdinal ?: Int.MAX_VALUE } ?: Int.MAX_VALUE }
                )
                .map { (rawRewardType, rewardRows) ->
                    TrainingRewardGroupProjection(
                        rawRewardType = rawRewardType,
                        displayLabel = trainingRewardPoolDisplayLabel(rawRewardType),
                        entries = rewardRows
                            .sortedWith(compareBy<MaterialSource> { it.sourceOrdinal ?: Int.MAX_VALUE }.thenBy { it.id })
                            .map { row ->
                                TrainingRewardEntryProjection(
                                    rawRelationId = row.id,
                                    quantity = row.quantity,
                                    quantitySemantics = row.quantitySemantics ?: "NOT_STATED",
                                    probabilitySemantics = row.probabilitySemantics ?: "SOURCE_UNSUPPORTED",
                                    probabilityValuePercent = row.chance,
                                    sourceOrdinal = row.sourceOrdinal ?: Int.MAX_VALUE,
                                    source = row
                                )
                            }
                    )
                }
            TrainingMissionSourceProjection(
                trainingQuestId = first.trainingQuestId!!,
                trainingClass = first.trainingClass.orEmpty(),
                displayTitle = first.name,
                navigationTarget = first.trainingQuestId!!,
                rewardGroups = groups
            )
        }
        .sortedWith(
            compareBy<TrainingMissionSourceProjection> { trainingClassSortIndex(it.trainingClass) }
                .thenBy { missionOrderFor(it.trainingQuestId, rows) }
                .thenBy { it.trainingQuestId }
        )
    return TrainingRewardItemProjection(rows.size, missions)
}

private fun missionOrderFor(trainingQuestId: String, rows: List<MaterialSource>): Int =
    rows.firstOrNull { it.trainingQuestId == trainingQuestId }?.trainingMissionOrder ?: Int.MAX_VALUE

data class PalicoRewardProjection(
    val category: String,
    val quantity: Int?,
    val sourceIds: List<String>
)

data class PalicoExpeditionProjectionEntry(
    val expeditionIds: List<String>,
    val rewards: List<PalicoRewardProjection>
)

data class PalicoTierProjection(
    val starRank: Int,
    val costPerPalico: Int?,
    val costConsistent: Boolean,
    val entries: List<PalicoExpeditionProjectionEntry>
) {
    val isSimple: Boolean
        get() = entries.size == 1 && entries.single().rewards.size == 1
}

data class PalicoItemSourceProjection(
    val rawRelationCount: Int,
    val uniqueExpeditionCount: Int,
    val tiers: List<PalicoTierProjection>
) {
    val projectedExpeditionIds: Set<String>
        get() = tiers.flatMap { it.entries }.flatMap { it.expeditionIds }.toSet()
}

/**
 * Presentation-only Monster-first projection.  The source-native rows remain
 * attached to every entry; only the hierarchy and explicit display order are
 * changed here.
 */
data class MonsterRewardEntryProjection(
    val source: MaterialSource
) {
    val rawRelationIdentity: String get() = source.id
    val rank: RewardContext? get() = source.rank
    val chance: Int? get() = source.chance
    val quantity: Int? get() = source.quantity
}

data class MonsterRewardMethodProjection(
    val method: String,
    val condition: String?,
    val label: String,
    val entries: List<MonsterRewardEntryProjection>
)

data class MonsterRewardMonsterProjection(
    val monsterId: String,
    val monsterName: String,
    val methods: List<MonsterRewardMethodProjection>
) {
    val entries: List<MonsterRewardEntryProjection>
        get() = methods.flatMap { it.entries }
}

data class MonsterItemSourceProjection(
    val monsterCount: Int,
    val rawRelationCount: Int,
    val monsters: List<MonsterRewardMonsterProjection>
) {
    val rawRelationIds: List<String>
        get() = monsters.flatMap { it.entries }.map { it.rawRelationIdentity }
}

/**
 * Compact, presentation-only projection for Item → Sources → Invasion Reward.
 *
 * Invasion relations are intentionally kept as individual entries.  The
 * projection only introduces the accepted Monster → Context → Reward
 * hierarchy; it never merges duplicate same-context relations or rewrites the
 * source-native identity.
 */
data class InvasionRewardEntryProjection(
    val source: MaterialSource
) {
    val rawRelationIdentity: String get() = source.id
    val quantity: Int? get() = source.quantity
    val probabilityBand: InvasionProbabilityBand? get() = source.probabilityBand
    val probabilitySemantics: String? get() = source.probabilitySemantics
    val displayBand: String get() = invasionProbabilityBandLabel(probabilityBand)
}

data class InvasionRewardContextProjection(
    val context: String,
    val entries: List<InvasionRewardEntryProjection>
) {
    val label: String get() = invasionContextDisplayLabel(context)
}

data class InvasionRewardMonsterProjection(
    val monsterId: String,
    val monsterName: String,
    val contexts: List<InvasionRewardContextProjection>
) {
    val entries: List<InvasionRewardEntryProjection>
        get() = contexts.flatMap { it.entries }
}

data class InvasionItemSourceProjection(
    val monsterCount: Int,
    val rawRelationCount: Int,
    val monsters: List<InvasionRewardMonsterProjection>
) {
    val rawRelationIds: List<String>
        get() = monsters.flatMap { it.entries }.map { it.rawRelationIdentity }
}

private val INVASION_CONTEXT_PRIORITY = mapOf(
    "GUILD_1_2" to 0,
    "LOW" to 1,
    "HIGH" to 2
)

/** Explicit user-facing context labels; raw source enum values never leak. */
fun invasionContextDisplayLabel(context: String): String = when (context) {
    "GUILD_1_2" -> "Guild ★1–2"
    "LOW" -> "Village / Guild Low"
    "HIGH" -> "Guild High"
    else -> "Other invasion context"
}

/** Explicit user-facing probability-band labels. */
fun invasionProbabilityBandLabel(band: InvasionProbabilityBand?): String = when (band) {
    InvasionProbabilityBand.AT_LEAST_20 -> "≥20%"
    InvasionProbabilityBand.TEN_TO_UNDER_20 -> "10–<20%"
    InvasionProbabilityBand.FIVE_TO_UNDER_10 -> "5–<10%"
    InvasionProbabilityBand.TWO_TO_UNDER_5 -> "2–<5%"
    InvasionProbabilityBand.UNDER_2 -> "<2%"
    null -> "Chance not published"
}

/**
 * Builds the accepted Monster-first invasion hierarchy.  Monster and entry
 * order follows the canonical source row order (with stable identity/name
 * tie-breakers); contexts use the explicit Guild 1–2 → Low → High order.
 */
fun List<MaterialSource>.invasionRewardPresentationProjection(): InvasionItemSourceProjection {
    val rows = filter { it.type == MaterialSourceType.INVASION_REWARD && it.monsterId != null }
    val monsterGroups = rows.groupBy { it.monsterId!! }
    val monsters = monsterGroups.entries
        .withIndex()
        .sortedWith(
            compareBy<IndexedValue<Map.Entry<String, List<MaterialSource>>>> { it.index }
                .thenBy { it.value.key }
        )
        .map { (_, monsterEntry) ->
            val monsterRows = monsterEntry.value
            val contexts = monsterRows.groupBy { it.context.orEmpty() }
                .entries
                .withIndex()
                .sortedWith(
                    compareBy<IndexedValue<Map.Entry<String, List<MaterialSource>>>> {
                        INVASION_CONTEXT_PRIORITY[it.value.key] ?: 99
                    }.thenBy { it.index }
                        .thenBy { it.value.key }
                )
                .map { (_, contextEntry) ->
                    InvasionRewardContextProjection(
                        context = contextEntry.key,
                        entries = contextEntry.value.map(::InvasionRewardEntryProjection)
                    )
                }
            InvasionRewardMonsterProjection(
                monsterId = monsterEntry.key,
                monsterName = monsterRows.first().name,
                contexts = contexts
            )
        }
    return InvasionItemSourceProjection(
        monsterCount = monsters.size,
        rawRelationCount = rows.size,
        monsters = monsters
    )
}

private val MONSTER_METHOD_PRIORITY = mapOf(
    "BODY_CARVE" to 0,
    "TAIL_CARVE" to 1,
    "CAPTURE" to 2,
    "PART_BREAK" to 3,
    "MINING" to 4,
    "SHINY" to 5
)

private val MONSTER_CONTEXT_PRIORITY = mapOf(
    RewardContext.LOW to 0,
    RewardContext.HIGH to 1,
    RewardContext.GUILD_1_2 to 2,
    RewardContext.VILLAGE_2_SPECIAL to 3
)

/** Stable, explicit user-facing priority; it intentionally does not use enum ordinals. */
fun monsterRewardMethodPriority(method: String?): Int = MONSTER_METHOD_PRIORITY[method] ?: 99

fun monsterRewardMethodLabel(method: String?, condition: String?): String = when (method) {
    "BODY_CARVE" -> "Body Carve"
    "TAIL_CARVE" -> "Tail Carve"
    "CAPTURE" -> "Capture"
    "PART_BREAK" -> "Break — ${condition?.takeIf { it.isNotBlank() } ?: "Unknown part"}"
    "MINING" -> "Mining"
    "SHINY" -> "Shiny Drop"
    else -> method.orEmpty().lowercase().split('_').joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }.ifBlank { "Reward" }
}

private fun monsterConditionSortKey(method: String, condition: String?): String =
    if (method == "PART_BREAK") condition.orEmpty().lowercase() else ""

/**
 * Builds the accepted Monster-first hierarchy. Monster order follows the
 * existing Monster index convention (display name, then stable id), while
 * methods use the explicit factual UX priority above. Every input relation is
 * retained exactly once, including same-method/rank duplicates.
 */
fun List<MaterialSource>.monsterPresentationProjection(): MonsterItemSourceProjection {
    val rows = filter { it.type == MaterialSourceType.MONSTER_REWARD && it.monsterId != null }
    val monsters = rows.groupBy { it.monsterId!! }
        .map { (monsterId, monsterRows) ->
            val methods = monsterRows.groupBy { it.method.orEmpty() to it.condition }
                .entries
                .sortedWith(
                    compareBy<Map.Entry<Pair<String, String?>, List<MaterialSource>>>
                        { monsterRewardMethodPriority(it.key.first) }
                        .thenBy { monsterConditionSortKey(it.key.first, it.key.second) }
                        .thenBy { it.key.first }
                )
                .map { (key, methodRows) ->
                    val sortedRows = methodRows.sortedWith(
                        compareBy<MaterialSource> { MONSTER_CONTEXT_PRIORITY[it.rank] ?: 99 }
                            .thenBy { it.rank?.name.orEmpty() }
                            .thenBy { it.id }
                    )
                    MonsterRewardMethodProjection(
                        method = key.first,
                        condition = key.second,
                        label = monsterRewardMethodLabel(key.first, key.second),
                        entries = sortedRows.map(::MonsterRewardEntryProjection)
                    )
                }
            val name = monsterRows.first().name
            MonsterRewardMonsterProjection(monsterId, name, methods)
        }
        .sortedWith(compareBy<MonsterRewardMonsterProjection> { it.monsterName.lowercase() }.thenBy { it.monsterId })
    return MonsterItemSourceProjection(
        monsterCount = monsters.size,
        rawRelationCount = rows.size,
        monsters = monsters
    )
}

/** One source-table row in the Small Monster Item Sources presentation. */
data class SmallMonsterSourceContextEntryProjection(
    val source: MaterialSource
) {
    val rawRelationIdentity: String get() = source.id
    val context: RewardContext? get() = source.rank
    val chance: Int? get() = source.chance
    val quantity: Int? get() = source.quantity
}

/** Exact source condition identity.  Details are part of the key so two
 * variants of the same condition enum can never be collapsed. */
data class SmallMonsterSourceConditionProjection(
    val conditionKey: String?,
    val conditionDetails: MaterialSourceConditionDetails?,
    val label: String?,
    val entries: List<SmallMonsterSourceContextEntryProjection>
)

data class SmallMonsterSourceMethodProjection(
    val method: String,
    val label: String,
    val directEntries: List<SmallMonsterSourceContextEntryProjection>,
    val conditionGroups: List<SmallMonsterSourceConditionProjection>
) {
    val entries: List<SmallMonsterSourceContextEntryProjection>
        get() = directEntries + conditionGroups.flatMap { it.entries }
}

data class SmallMonsterSourceMonsterProjection(
    val smallMonsterId: String,
    val monsterName: String,
    val methods: List<SmallMonsterSourceMethodProjection>
) {
    val entries: List<SmallMonsterSourceContextEntryProjection>
        get() = methods.flatMap { it.entries }
}

data class SmallMonsterItemSourceProjection(
    val monsterCount: Int,
    val rawRelationCount: Int,
    val monsters: List<SmallMonsterSourceMonsterProjection>
) {
    val rawRelationIds: List<String>
        get() = monsters.flatMap { it.entries }.map { it.rawRelationIdentity }
}

private val SMALL_MONSTER_METHOD_PRIORITY = mapOf(
    "BODY_CARVE" to 0,
    "SHINY_DROP" to 1
)

private data class SmallMonsterConditionKey(
    val conditionKey: String?,
    val locations: List<String>,
    val wingColor: String?,
    val color: String?
)

private fun String?.smallMonsterSafeMethodLabel(): String = when (this) {
    "BODY_CARVE" -> "Body Carve"
    "SHINY_DROP" -> "Shiny Drop"
    else -> "Other source"
}

/** Explicit user-facing context labels; source enum values never leak into UI. */
fun smallMonsterContextLabel(context: RewardContext?): String = when (context) {
    RewardContext.GUILD_1_2 -> "Guild ★1–2"
    RewardContext.LOW -> "LR"
    RewardContext.HIGH -> "HR"
    RewardContext.VILLAGE_2_SPECIAL -> "Village 2★ Special"
    null -> ""
}

private fun String?.smallMonsterColorLabel(): String? = when (this) {
    null, "" -> null
    "PALE_WHITE" -> "Pale White"
    "PALE_BROWN" -> "Pale Brown"
    "PALE_BLUE" -> "Pale Blue"
    "PALE_RED" -> "Pale Red"
    "WHITE" -> "White"
    "BLACK" -> "Black"
    "GRAY", "GREY" -> "Gray"
    "RED" -> "Red"
    "ORANGE" -> "Orange"
    "GREEN" -> "Green"
    "BLUE" -> "Blue"
    "GOLD", "YELLOW" -> "Gold"
    else -> null
}

private fun String.smallMonsterLocationLabel(): String = when (this) {
    "MISTY_PEAKS" -> "Misty Peaks"
    "SANDY_PLAINS" -> "Sandy Plains"
    "FLOODED_FOREST" -> "Flooded Forest"
    "FROST_ISLANDS" -> "Frost Islands"
    "TUNDRA" -> "Tundra"
    "VOLCANO" -> "Volcano"
    else -> this
}

/** Human label for the accepted source condition vocabulary.  Unknown keys
 * intentionally receive a generic label instead of exposing raw identifiers. */
fun smallMonsterConditionLabel(
    conditionKey: String?,
    details: MaterialSourceConditionDetails?
): String? = when (conditionKey) {
    null -> null
    "STUNNED" -> "Stunned"
    "NORMAL_DROP" -> "Normal Drop"
    "WHITE_EGG" -> "White Egg"
    "GOLD_EGG" -> "Gold Egg"
    "ABDOMEN_COLOR" -> details?.color.smallMonsterColorLabel()?.let { "$it abdomen" } ?: "Special condition"
    "WING_COLOR_LOCATION_VARIANT" -> buildList {
        details?.wingColor.smallMonsterColorLabel()?.let(::add)
        details?.locations.orEmpty().map(String::smallMonsterLocationLabel).takeIf { it.isNotEmpty() }?.joinToString(", ")?.let(::add)
    }.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "Special condition"
    else -> "Special condition"
}

/** The accepted UI shows six Small Monster groups inline before opening the
 * full list dialog. */
const val SMALL_MONSTER_INLINE_LIMIT: Int = 6

fun smallMonsterVisibleGroupCount(monsterCount: Int): Int = monsterCount.coerceAtMost(SMALL_MONSTER_INLINE_LIMIT)

/**
 * Builds the accepted Small Monster hierarchy without changing source rows:
 * Small Monster → method → exact condition → context/chance entry.  Group and
 * condition order follows first appearance in production source order; method
 * order is the explicit Body Carve then Shiny Drop order.
 */
fun List<MaterialSource>.smallMonsterPresentationProjection(): SmallMonsterItemSourceProjection {
    val rows = filter { it.type == MaterialSourceType.SMALL_MONSTER && it.smallMonsterId != null }
    val monsters = rows.groupBy { it.smallMonsterId!! }.map { (monsterId, monsterRows) ->
        val methodGroups = monsterRows.groupBy { it.method.orEmpty() }
        val methods = methodGroups.entries
            .withIndex()
            .sortedWith(compareBy<IndexedValue<Map.Entry<String, List<MaterialSource>>>> {
                SMALL_MONSTER_METHOD_PRIORITY[it.value.key] ?: 99
            }.thenBy { it.index })
            .map { (_, methodEntry) ->
                val methodRows = methodEntry.value
                val directRows = methodRows.filter {
                    it.conditionKey == null && it.condition == null && it.conditionDetails == null
                }
                val conditionRows = methodRows.filter {
                    it.conditionKey != null || it.condition != null || it.conditionDetails != null
                }
                val conditionGroups = conditionRows.groupBy { row ->
                    SmallMonsterConditionKey(
                        conditionKey = row.conditionKey ?: row.condition,
                        locations = row.conditionDetails?.locations.orEmpty(),
                        wingColor = row.conditionDetails?.wingColor,
                        color = row.conditionDetails?.color
                    )
                }.map { (key, groupedRows) ->
                    SmallMonsterSourceConditionProjection(
                        conditionKey = key.conditionKey,
                        conditionDetails = MaterialSourceConditionDetails(
                            locations = key.locations,
                            wingColor = key.wingColor,
                            color = key.color
                        ).takeUnless { it.locations.isEmpty() && it.wingColor == null && it.color == null },
                        label = smallMonsterConditionLabel(key.conditionKey, groupedRows.first().conditionDetails?.let {
                            MaterialSourceConditionDetails(it.locations, it.wingColor, it.color)
                        }),
                        entries = groupedRows.map(::SmallMonsterSourceContextEntryProjection)
                    )
                }
                SmallMonsterSourceMethodProjection(
                    method = methodEntry.key,
                    label = methodEntry.key.smallMonsterSafeMethodLabel(),
                    directEntries = directRows.map(::SmallMonsterSourceContextEntryProjection),
                    conditionGroups = conditionGroups
                )
            }
        SmallMonsterSourceMonsterProjection(
            smallMonsterId = monsterId,
            monsterName = monsterRows.first().name,
            methods = methods
        )
    }
    return SmallMonsterItemSourceProjection(
        monsterCount = monsters.size,
        rawRelationCount = rows.size,
        monsters = monsters
    )
}

/**
 * Presentation-only Palico projection. Each expedition is first reduced to
 * its ordered reward signature; only expeditions with identical complete
 * signatures are then aggregated. Raw source IDs remain attached to every
 * reward line.
 */
fun List<MaterialSource>.palicoPresentationProjection(): PalicoItemSourceProjection {
    val expeditionRows = asSequence()
        .filter { it.type == MaterialSourceType.PALICO_EXPEDITION && it.expeditionId != null }
        .groupBy { it.expeditionId!! }
    val tiers = expeditionRows.values
        .groupBy { rows -> rows.first().expeditionStarRank ?: parsePalicoStar(rows.first().context) ?: 0 }
        .toSortedMap()
        .map { (starRank, rowsByExpedition) ->
            val costs = rowsByExpedition.flatMap { rows -> rows.mapNotNull { it.expeditionCostPerPalico ?: parsePalicoCost(it.context) } }.distinct()
            val cost = costs.singleOrNull()
            val entries = rowsByExpedition
                .map { rows ->
                    val rewards = rows.map { row ->
                        PalicoRewardProjection(
                            category = row.expeditionRewardCategory ?: row.method.orEmpty(),
                            quantity = row.quantity,
                            sourceIds = listOf(row.id)
                        )
                    }
                    val signature = rewards.map { it.category to it.quantity }
                    signature to (rows.first().expeditionId!! to rewards)
                }
                .groupBy({ it.first }, { it.second })
                .values
                .map { grouped ->
                    val representative = grouped.first().second
                    val mergedRewards = representative.mapIndexed { rewardIndex, reward ->
                        reward.copy(
                            sourceIds = grouped.flatMap { (_, rewards) ->
                                rewards.getOrNull(rewardIndex)?.sourceIds.orEmpty()
                            }
                        )
                    }
                    PalicoExpeditionProjectionEntry(
                        expeditionIds = grouped.map { it.first }.sortedWith(Comparator(::compareExpeditionIds)),
                        rewards = mergedRewards
                    )
                }
                .sortedWith(compareBy({ it.expeditionIds.firstOrNull()?.let(::expeditionSortKey) ?: Int.MAX_VALUE }))
            PalicoTierProjection(
                starRank = starRank,
                costPerPalico = cost,
                costConsistent = costs.size <= 1,
                entries = entries
            )
        }
    return PalicoItemSourceProjection(
        rawRelationCount = count { it.type == MaterialSourceType.PALICO_EXPEDITION },
        uniqueExpeditionCount = expeditionRows.size,
        tiers = tiers
    )
}

private fun parsePalicoStar(context: String?): Int? =
    Regex("★(\\d+)").find(context.orEmpty())?.groupValues?.getOrNull(1)?.toIntOrNull()

private fun parsePalicoCost(context: String?): Int? =
    Regex("★\\d+ · (\\d+) pts").find(context.orEmpty())?.groupValues?.getOrNull(1)?.toIntOrNull()

private fun expeditionSortKey(id: String): Int {
    val match = Regex("palico_expedition_(\\d+)_(\\d+)").find(id)
    return if (match == null) Int.MAX_VALUE else match.groupValues[1].toInt() * 100 + match.groupValues[2].toInt()
}

private fun compareExpeditionIds(left: String, right: String): Int =
    expeditionSortKey(left).compareTo(expeditionSortKey(right))

data class GroupedMaterialSources(
    val field: List<FieldSourceGroup>,
    val monsters: List<MaterialSource>,
    val smallMonsters: List<MaterialSource>,
    val quests: List<MaterialSource>,
    val supplyBoxes: List<MaterialSource>,
    val specialFree: List<MaterialSource>,
    val trainingRewards: List<MaterialSource>,
    val farm: List<FarmSourceGroup>,
    val trade: List<MaterialSource>,
    val shop: List<MaterialSource>,
    val combinations: List<MaterialSource>,
    val combinationFailures: List<MaterialSource>,
    val scrapConversions: List<MaterialSource>,
    val decorationCrafting: List<MaterialSource>,
    val roasting: List<MaterialSource>,
    val invasionRewards: List<MaterialSource>,
    val palicoExpeditions: List<MaterialSource>
)

/** Groups rank variants of one physical gathering node without merging distinct nodes. */
fun List<MaterialSource>.groupForDisplay(): GroupedMaterialSources {
    val fieldRows = filter { it.type in FIELD_SOURCE_TYPES }
    val field = fieldRows
        .groupBy { source ->
            source.nodeId ?: listOf(source.locationId, source.context, source.method, source.id).joinToString("|")
        }
        .map { (nodeId, rows) ->
            val first = rows.first()
            FieldSourceGroup(
                nodeId = nodeId,
                location = first.name,
                context = first.context,
                method = first.method,
                availability = rows
                    .map { FieldSourceAvailability(it.rank, it.chance, it.quantity) }
                    .distinct()
                    .sortedBy { REWARD_CONTEXT_PROGRESSION.indexOf(it.rank).let { index -> if (index < 0) Int.MAX_VALUE else index } },
                sourceRows = rows
            )
        }

    return GroupedMaterialSources(
        field = field,
        monsters = filter { it.type == MaterialSourceType.MONSTER_REWARD },
        smallMonsters = filter { it.type == MaterialSourceType.SMALL_MONSTER },
        quests = filter { it.type == MaterialSourceType.QUEST_REWARD },
        supplyBoxes = filter { it.type == MaterialSourceType.SUPPLY_BOX },
        specialFree = filter { it.type == MaterialSourceType.SPECIAL_FREE },
        trainingRewards = filter { it.type == MaterialSourceType.TRAINING_REWARD },
        farm = filter { it.type == MaterialSourceType.FARM }.groupFarmSourcesForDisplay(),
        trade = filter { it.type == MaterialSourceType.TRADE },
        shop = filter { it.type == MaterialSourceType.SHOP_PURCHASE },
        combinations = filter { it.type == MaterialSourceType.COMBINATION },
        combinationFailures = filter { it.type == MaterialSourceType.COMBINATION_FAILURE },
        scrapConversions = filter { it.type == MaterialSourceType.SCRAP_CONVERSION },
        decorationCrafting = filter { it.type == MaterialSourceType.DECORATION_CRAFTING },
        roasting = filter { it.type == MaterialSourceType.ROASTING },
        invasionRewards = filter { it.type == MaterialSourceType.INVASION_REWARD },
        palicoExpeditions = filter { it.type == MaterialSourceType.PALICO_EXPEDITION }
    )
}

/** Preserves every raw yield row and groups only by the accepted stable method identity. */
fun List<MaterialSource>.groupFarmSourcesForDisplay(): List<FarmSourceGroup> =
    groupBy(MaterialSource::farmStableMethodId)
        .toSortedMap()
        .map { (stableMethodId, unsortedRows) ->
            val rows = unsortedRows.sortedBy { it.sourceOrdinal ?: Int.MAX_VALUE }
            val first = rows.first()
            val facilityId = requireNotNull(first.facilityId)
            val triggerStatus = when {
                first.profileId != null -> {
                    require(first.profileId in FARM_TRIGGER_BLOCKED_PROFILE_IDS) {
                        "Farm profile ${first.profileId} has no accepted trigger adjudication"
                    }
                    FarmTriggerStatus.BLOCKED
                }
                first.inputGameItemId != null -> FarmTriggerStatus.ITEM_INPUT
                first.condition == FARM_PICKAXE_CART_CONDITION -> FarmTriggerStatus.SOURCE_CONDITION
                else -> FarmTriggerStatus.FACILITY_ROUTE
            }
            val triggerLabel = when (triggerStatus) {
                FarmTriggerStatus.BLOCKED -> ""
                FarmTriggerStatus.ITEM_INPUT -> first.farmAction ?: "Input Item"
                FarmTriggerStatus.SOURCE_CONDITION -> requireNotNull(first.condition)
                FarmTriggerStatus.FACILITY_ROUTE -> ""
            }
            FarmSourceGroup(
                stableMethodId = stableMethodId,
                facilityId = facilityId,
                facilityDisplayName = FARM_FACILITY_DISPLAY_NAMES[facilityId]
                    ?: error("Farm facility $facilityId has no accepted display name"),
                action = first.farmAction.orEmpty(),
                inputGameItemId = first.inputGameItemId,
                inputItemName = first.inputItemName,
                condition = first.condition,
                triggerStatus = triggerStatus,
                triggerLabel = triggerLabel,
                outcomes = rows.map {
                    FarmSourceOutcome(it.quantity, it.chance, it.probabilitySemantics, it.quantitySemantics, it)
                },
                sourceRows = rows,
                publishedPoolTotalPercent = FARM_PUBLISHED_POOL_TOTALS[stableMethodId]
            )
        }

/** Null means that no percentage should be rendered; it never invents unpublished odds. */
fun MaterialSource.chanceForDisplay(): String? = chance?.let { value ->
    when (type) {
        MaterialSourceType.QUEST_REWARD -> "$value% reward slot"
        MaterialSourceType.TRAINING_REWARD -> "$value% training reward"
        MaterialSourceType.SMALL_MONSTER -> "$value% source table"
        MaterialSourceType.FARM -> "$value% farm output"
        MaterialSourceType.GATHERING,
        MaterialSourceType.MINING,
        MaterialSourceType.BUG,
        MaterialSourceType.FISHING -> "$value%"
        else -> "$value%"
    }
}

fun MaterialSource.invasionChanceForDisplay(): String = when (probabilityBand) {
    InvasionProbabilityBand.AT_LEAST_20 -> "≥20%"
    InvasionProbabilityBand.TEN_TO_UNDER_20 -> "10–<20%"
    InvasionProbabilityBand.FIVE_TO_UNDER_10 -> "5–<10%"
    InvasionProbabilityBand.TWO_TO_UNDER_5 -> "2–<5%"
    InvasionProbabilityBand.UNDER_2 -> "<2%"
    null -> "Chance not published"
}
