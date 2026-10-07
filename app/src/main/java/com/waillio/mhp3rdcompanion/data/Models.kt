package com.waillio.mhp3rdcompanion.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.Json

data class FixtureData(
    val monsters: List<Monster>,
    val materials: List<Material>,
    val quests: List<Quest>,
    val smallMonsters: List<SmallMonster> = emptyList(),
    val weapons: List<Weapon> = emptyList(),
    val huntingHornSongCatalog: List<HuntingHornSongSet> = emptyList(),
    val trainingQuests: List<TrainingQuest> = emptyList(),
    val itemCombinationRecipes: List<ItemCombinationRecipe> = emptyList(),
    val itemCombinationFailureRelations: List<ItemCombinationFailureRelation> = emptyList(),
    val itemShopPurchaseRelations: List<ItemShopPurchaseRelation> = emptyList(),
    val itemShopPeddlerProfiles: List<ItemShopPeddlerProfile> = emptyList(),
    val itemShopUnlockConditions: List<ItemShopUnlockCondition> = emptyList(),
    val itemTradeExchangeRelations: List<ItemTradeExchangeRelation> = emptyList(),
    val itemTradeExchangeConditions: List<ItemTradeExchangeCondition> = emptyList(),
    val itemVeggieElderLocations: List<ItemVeggieElderLocation> = emptyList(),
    val decorationCraftingRecipes: List<DecorationCraftingRecipe> = emptyList(),
    val itemScrapConversionRules: List<GeneratedItemScrapConversionRule> = emptyList(),
    val roastingConversions: List<RoastingConversion> = emptyList(),
    val invasionRewardDrops: List<InvasionReward> = emptyList(),
    val regularQuestSupplyItems: List<RegularQuestSupplyItem> = emptyList(),
    val regularQuestSupplyCoverage: List<RegularQuestSupplyCoverage> = emptyList(),
    val palicoExpeditions: List<PalicoExpedition> = emptyList(),
    val palicoExpeditionRewardDrops: List<PalicoExpeditionRewardDrop> = emptyList(),
    /** Accepted S1a skill-tree reference corpus used by the Field Guide. */
    val skillTrees: List<SkillTree> = emptyList(),
    /** Accepted Decoration/Jewel ↔ Skill Tree signed point relations. */
    val decorationSkillRelations: List<DecorationSkillRelation> = emptyList(),
    /** Canonical TMO terminology kept separate from source/raw fields. */
    val displayTerminology: DisplayTerminology = DisplayTerminology()
)

/** Compact runtime representation of one canonical Skill Tree. Evidence and
 * source transport metadata remain in the audit package. */
data class SkillTree(
    val stableSkillTreeId: String,
    val displayName: String,
    val tmoDisplayName: String,
    val jpName: String,
    val inGameDescription: String,
    val thresholds: List<SkillThreshold> = emptyList(),
    val specialMechanics: List<SkillSpecialMechanic> = emptyList()
) {
    /** Alias retained for callers that use the projection field name. */
    val uiDisplayName: String get() = displayName
}

data class SkillThreshold(
    val points: Int,
    val activatedSkillName: String,
    val polarity: String,
    val effectSummary: String
)

data class SkillSpecialMechanic(
    val type: String,
    val name: String,
    val effectSummary: String
)

/** Canonical bidirectional relation between a Jewel Item and a Skill Tree. */
data class DecorationSkillRelation(
    val stableDecorationItemId: String,
    val stableSkillTreeId: String,
    val points: Int,
    val slotCost: Int
)

data class DisplayTerminology(
    val trainingArmorByRaw: Map<String, String> = emptyMap(),
    val trainingSkillByRaw: Map<String, String> = emptyMap(),
    val trainingCharmByRaw: Map<String, String> = emptyMap(),
    val objectiveByStableId: Map<String, String> = emptyMap()
)

/** Neutral runtime name for the accepted Palico expedition corpus. */
data class PalicoExpedition(
    val id: String,
    val starRank: Int,
    val canonicalName: String,
    val dispatchPointsPerPalico: Int,
    val unlockAnyOf: List<PalicoExpeditionUnlockChoice>,
    val facilityUnlockAnyOf: List<PalicoExpeditionUnlockChoice> = emptyList()
)

data class PalicoExpeditionUnlockChoice(
    val kind: String,
    val questId: String? = null,
    val facilityId: String? = null
)

data class PalicoExpeditionRewardDrop(
    val id: String,
    val expeditionId: String,
    val gameItemId: Int,
    val rewardCategory: String,
    val quantity: Int
)

enum class RegularQuestSupplyQuantitySemantics { FINITE_SINGLE_BUNDLE, FINITE_MULTI_BUNDLE, INFINITE }
enum class RegularQuestSupplyLifecycle { QUEST_LOCAL_SUPPLY_ONLY, PERSISTENT_NORMAL_SUPPLY }
enum class RegularQuestSupplyCoverageStatus { DATA_PRESENT, EXPLICIT_NO_SUPPLY, SOURCE_UNAVAILABLE }
data class RegularQuestSupplyItem(
    val id: String,
    val questId: String,
    val gameItemId: Int,
    val availabilityTiming: String,
    val quantityValue: Int? = null,
    val quantityNotation: String? = null,
    val bundleCount: Int? = null,
    val quantitySemantics: RegularQuestSupplyQuantitySemantics,
    val distributionSemantics: String,
    val lifecycle: RegularQuestSupplyLifecycle,
    val persistentAcquisition: Boolean
)
data class RegularQuestSupplyCoverage(
    val questId: String,
    val status: RegularQuestSupplyCoverageStatus
)

/** Presentation-only copy of every source-native supply relation retained
 * under its quest parent.  This deliberately keeps quantity dimensions
 * separate: a bundle count is not an item quantity. */
@Serializable data class SupplyBoxEntry(
    val id: String,
    val gameItemId: Int,
    val quantityValue: Int? = null,
    val quantityNotation: String? = null,
    val bundleCount: Int? = null,
    val quantitySemantics: RegularQuestSupplyQuantitySemantics,
    val distributionSemantics: String,
    val availabilityTiming: String,
    val lifecycle: RegularQuestSupplyLifecycle,
    val persistentAcquisition: Boolean
)

/** Production weapon records. Recipe identity is always numeric. */
data class Weapon(
    val stableWeaponId: String,
    val weaponType: String,
    val sourceOrdinal: Int,
    val sourceName: String,
    val sourceKey: String,
    val displayName: String,
    val nameSource: String,
    val rarity: Int,
    val attack: Int,
    val affinity: Int,
    val slots: Int,
    val defenseBonus: Int?,
    val special: WeaponSpecial?,
    /** Null for ranged weapons (Bow has no melee sharpness bar). */
    val sharpness: WeaponSharpnessProfile? = null,
    val forgeRecipe: WeaponRecipe?,
    val upgradeFrom: WeaponUpgradeFrom?,
    val upgradesTo: List<String>,
    /** A source-preserved Upgrade recipe whose parent is not in the corpus. */
    val orphanUpgradeRecipe: WeaponRecipe? = null,
    /** Schema-11 typed weapon mechanics; null for the legacy six weapon types. */
    val mechanics: WeaponMechanics? = null
) {
    val id: String get() = stableWeaponId
    /** User-facing boundary normalization; raw TMO text remains in displayName. */
    val name: String get() = displayName.trim()
    val affinityPercent: Int get() = affinity
    /** Existing accepted UI displays the derived Attack +15 value. */
    val attackBoosted: Int? get() = (attack + 15).takeIf { attack > 0 }
}

sealed interface WeaponMechanics { val kind: WeaponMechanicsKind }
enum class WeaponMechanicsKind { HUNTING_HORN, GUNLANCE, SWITCH_AXE, BOW, LIGHT_BOWGUN, HEAVY_BOWGUN }
enum class HornNote { WHITE, PURPLE, RED, BLUE, GREEN, CYAN, YELLOW, ORANGE }
data class HuntingHornMechanics(val notes: List<HornNote>) : WeaponMechanics {
    override val kind: WeaponMechanicsKind = WeaponMechanicsKind.HUNTING_HORN
    val noteSetKey: String get() = notes.map { it.name.first() }.sorted().joinToString("")
}
enum class GunlanceShellingType { NORMAL, LONG, WIDE }
data class GunlanceMechanics(val shellingType: GunlanceShellingType, val shellingLevel: Int) : WeaponMechanics {
    override val kind: WeaponMechanicsKind = WeaponMechanicsKind.GUNLANCE
}
enum class SwitchAxePhialType { POWER, ELEMENTAL, EXHAUST, PARALYSIS, DRAGON, POISON }
data class SwitchAxeMechanics(val phialType: SwitchAxePhialType) : WeaponMechanics {
    override val kind: WeaponMechanicsKind = WeaponMechanicsKind.SWITCH_AXE
}

enum class BowArcShot { WIDE, FOCUS, BLAST }
enum class BowShotType { RAPID, PIERCE, SPREAD }
enum class BowChargeAvailability { NORMAL, REQUIRES_LOAD_UP }
data class BowCharge(val shotType: BowShotType, val shotLevel: Int, val availability: BowChargeAvailability)
enum class BowCoatingType { POWER, CLOSE_RANGE, POISON, PAINT, PARALYSIS, EXHAUST, SLEEP }
enum class BowCoatingSupportLevel { NORMAL, ENHANCED }
data class BowCoatingSupport(val coatingType: BowCoatingType, val supportLevel: BowCoatingSupportLevel)
data class BowMechanics(
    val arcShot: BowArcShot,
    val charges: List<BowCharge>,
    val coatings: List<BowCoatingSupport>
) : WeaponMechanics {
    override val kind: WeaponMechanicsKind = WeaponMechanicsKind.BOW
}

enum class BowgunAmmoFamily {
    @SerialName("Normal S") NORMAL_S,
    @SerialName("Pierce S") PIERCE_S,
    @SerialName("Pellet S") PELLET_S,
    @SerialName("Crag S") CRAG_S,
    @SerialName("Clust S") CLUST_S,
    @SerialName("Recov S") RECOV_S,
    @SerialName("Poison S") POISON_S,
    @SerialName("Para S") PARA_S,
    @SerialName("Sleep S") SLEEP_S,
    @SerialName("Exhaust S") EXHAUST_S,
    @SerialName("Flaming S") FLAMING_S,
    @SerialName("Water S") WATER_S,
    @SerialName("Thunder S") THUNDER_S,
    @SerialName("Freeze S") FREEZE_S,
    @SerialName("Dragon S") DRAGON_S,
    @SerialName("Tranq S") TRANQ_S,
    @SerialName("Paint S") PAINT_S,
    @SerialName("Demon S") DEMON_S,
    @SerialName("Armor S") ARMOR_S,
    @SerialName("Slicing S") SLICING_S
}
enum class BowgunReloadValue { VERY_FAST, FAST, NORMAL, SLOW, VERY_SLOW }
enum class BowgunRecoilValue { LIGHT, WEAK, MODERATE, STRONG, STRONGEST }
enum class BowgunDeviationMagnitude { NONE, SMALL, LARGE }
enum class BowgunDeviationDirection { NONE, LEFT, RIGHT, BOTH }
enum class BowgunAmmoSourceShape { THREE_LEVEL, TWO_LEVEL, SINGLE_CAPACITY }
data class BowgunRound(val ammoType: BowgunAmmoFamily, val level: Int?)
data class BowgunAmmoLoad(val round: BowgunRound, val capacity: Int, val sourceShape: BowgunAmmoSourceShape)
data class BowgunReload(val value: BowgunReloadValue, val semanticOrder: Int, val rawSourceValue: String)
data class BowgunRecoil(val value: BowgunRecoilValue, val semanticOrder: Int, val rawSourceValue: String, val spellingAnomaly: Boolean? = null)
data class BowgunDeviation(val magnitude: BowgunDeviationMagnitude, val direction: BowgunDeviationDirection, val rawDrift: String)
data class BowgunSharedMechanics(
    val reload: BowgunReload,
    val recoil: BowgunRecoil,
    val deviation: BowgunDeviation,
    val ammoLoads: List<BowgunAmmoLoad>
)
data class BowgunRapidFire(val round: BowgunRound, val burstCount: Int, val recoil: BowgunRecoil)
data class BowgunCrouchFire(val round: BowgunRound, val rawSourceToken: String)
data class LightBowgunMechanics(
    val shared: BowgunSharedMechanics,
    val rapidFire: List<BowgunRapidFire>
) : WeaponMechanics { override val kind: WeaponMechanicsKind = WeaponMechanicsKind.LIGHT_BOWGUN }
data class HeavyBowgunMechanics(
    val shared: BowgunSharedMechanics,
    val crouchFire: List<BowgunCrouchFire>
) : WeaponMechanics { override val kind: WeaponMechanicsKind = WeaponMechanicsKind.HEAVY_BOWGUN }

data class HuntingHornSongSet(val noteSetKey: String, val sourceKey: String?, val songs: List<HuntingHornSong>)
data class HuntingHornSong(val sequence: List<String>, val noteIdentities: List<HornNote>, val effectName: String)

data class WeaponSpecial(val kind: WeaponSpecialKind, val type: WeaponSpecialType, val value: Int)
enum class WeaponSpecialKind { ELEMENT, STATUS }
enum class WeaponSpecialType { FIRE, WATER, THUNDER, ICE, DRAGON, POISON, PARALYSIS, SLEEP }
data class WeaponSharpnessProfile(val normal: WeaponSharpness, val plusOne: WeaponSharpness?)
data class WeaponSharpness(
    val red: Int,
    val orange: Int,
    val yellow: Int,
    val green: Int,
    val blue: Int,
    val white: Int
) {
    fun totalUnits(): Int = red + orange + yellow + green + blue + white
    fun values(): List<Int> = listOf(red, orange, yellow, green, blue, white)
}
data class WeaponRecipe(val zenny: Int?, val ingredients: List<WeaponRecipeIngredient>)
data class WeaponRecipeIngredient(val gameItemId: Int, val quantity: Int)
data class WeaponUpgradeFrom(val edgeId: String, val fromWeaponId: String, val recipe: WeaponRecipe)
enum class WeaponRecipeKind { FORGE, UPGRADE }
data class WeaponRecipeUsage(
    val gameItemId: Int,
    val stableWeaponId: String,
    val weaponType: String,
    val recipeKind: WeaponRecipeKind,
    val quantity: Int
)
@Serializable data class ElementValues(
    val fire: Int = 0,
    val water: Int = 0,
    val thunder: Int = 0,
    val ice: Int = 0,
    val dragon: Int = 0
)
@Serializable data class BreakablePart(
    val name: String,
    val action: String,
    val count: Int? = null
)
@Serializable enum class RewardContext { GUILD_1_2, VILLAGE_2_SPECIAL, LOW, HIGH }
@Serializable data class MonsterReward(
    val item: String,
    val sourceType: String,
    val condition: String = "",
    val chance: String,
    val rank: RewardContext = RewardContext.LOW,
    val quantity: Int? = null,
    val gameItemId: Int? = null,
    val relationId: String? = null
)
@Serializable data class Hitzone(
    val part: String,
    val cut: Int,
    val impact: Int,
    val shot: Int,
    val fire: Int,
    val water: Int,
    val thunder: Int,
    val ice: Int,
    val dragon: Int
)
@Serializable data class AlternateHitzoneState(
    val stateId: String,
    val label: String,
    val hitzones: List<Hitzone>
)
@Serializable data class HitzoneStateSemantic(
    val sourceState: String,
    val semanticState: String,
    val label: String
)
@Serializable enum class ThreatCoverageStatus { LISTED, NONE_LISTED_IN_MASTER_TABLE }
@Serializable enum class HunterThreatType {
    ALL_RESISTANCE_DOWN_LARGE,
    DEFENSE_DOWN_LARGE,
    DEFENSE_DOWN_SMALL,
    FIREBLIGHT,
    ICEBLIGHT,
    MUD,
    PARALYSIS,
    POISON,
    SLEEP,
    SNOWMAN,
    STENCH,
    STUN,
    TERRAIN_ELEMENTAL_BLIGHT,
    THUNDERBLIGHT,
    TREMOR,
    WATERBLIGHT
}
@Serializable enum class TacticalPriority { CORE, RECOMMENDED, CONDITIONAL }
@Serializable data class HunterThreat(
    val id: String,
    val type: HunterThreatType,
    val context: String? = null
)
@Serializable data class HuntPrepCounterItem(
    val itemGameId: Int,
    val itemName: String,
    val counterFor: List<HunterThreatType>
)
@Serializable data class TacticalTool(
    val itemGameId: Int,
    val itemName: String,
    val priority: TacticalPriority,
    val reason: String
)
@Serializable data class MonsterHuntPrep(
    val coverageStatus: ThreatCoverageStatus,
    val threats: List<HunterThreat>,
    val counterItems: List<HuntPrepCounterItem>,
    val tacticalTools: List<TacticalTool>
)
@Serializable data class Monster(
    val id: String,
    val name: String,
    val subtitle: String,
    val type: String = "",
    val threatLevel: Int? = null,
    val weaknesses: ElementValues = ElementValues(),
    val recommendedParts: List<String> = emptyList(),
    val breakableParts: List<BreakablePart> = emptyList(),
    val rewards: List<MonsterReward> = emptyList(),
    val hitzones: List<Hitzone> = emptyList(),
    val alternateHitzones: List<AlternateHitzoneState> = emptyList(),
    val hitzoneStateSemantics: List<HitzoneStateSemantic> = emptyList(),
    val questIds: List<String> = emptyList(),
    val huntPrep: MonsterHuntPrep? = null,
    val trainingQuestIds: List<String> = emptyList(),
    val invasionRewards: List<InvasionReward> = emptyList()
)

@Serializable enum class InvasionProbabilityBand {
    AT_LEAST_20,
    TEN_TO_UNDER_20,
    FIVE_TO_UNDER_10,
    TWO_TO_UNDER_5,
    UNDER_2
}

@Serializable data class InvasionReward(
    val id: String,
    val monsterId: String,
    val context: String,
    val gameItemId: Int,
    val quantity: Int,
    val probabilityValuePercent: Int? = null,
    val probabilitySemantics: String = "QUALITATIVE_SOURCE_BAND",
    val probabilityBand: InvasionProbabilityBand
)
@Serializable data class Quest(
    val id: String,
    val name: String,
    val subtitle: String,
    val rank: String = "",
    val location: String = "",
    val target: String = "",
    val relevantReward: String = "",
    val objective: String = "",
    val stars: Int = 0,
    val category: String = "Guild",
    val objectiveTargetMonsterIds: List<String> = emptyList(),
    val objectiveTargetSmallMonsterIds: List<String> = emptyList(),
    val canonicalNumber: Int? = null
)

data class TrainingObjectiveTarget(
    val entityKind: String,
    val entityId: String,
    val count: Int,
    val countMode: String,
    val objectiveType: String,
    val gameItemId: Int? = null
)
data class TrainingSuppliedItem(
    val gameItemId: Int,
    val quantity: Int? = null,
    val quantitySemantics: String = "NOT_STATED"
)
data class TrainingLoadout(
    val weaponType: String,
    val weaponStableId: String? = null,
    val armorRaw: List<String> = emptyList(),
    val charmRaw: String? = null,
    val skillsRaw: String? = null,
    val suppliedItems: List<TrainingSuppliedItem> = emptyList()
)
data class TrainingGrade(
    val grade: String,
    val thresholdSeconds: Int? = null,
    val condition: String? = null
)
data class TrainingResultGrades(
    val provisional: Boolean = false,
    val rows: List<TrainingGrade> = emptyList()
)
data class TrainingReward(
    val id: String,
    val gameItemId: Int,
    val quantity: Int? = null,
    val quantitySemantics: String = "NOT_STATED",
    val probabilityValuePercent: Int? = null,
    val probabilitySemantics: String = "SOURCE_UNSUPPORTED",
    val rewardType: String = ""
)
data class TrainingQuest(
    val id: String,
    val trainingClass: String,
    val challengeSeries: String? = null,
    val canonicalDisplayName: String,
    val japaneseName: String,
    val tmoName: String? = null,
    val stars: Int? = null,
    val location: String = "",
    val objective: String = "",
    val objectiveTargets: List<TrainingObjectiveTarget> = emptyList(),
    val appearingMonsterIds: List<String> = emptyList(),
    val participantLimit: Int? = null,
    val timeLimitSeconds: Int? = null,
    val suppliedItems: List<TrainingSuppliedItem> = emptyList(),
    val loadouts: List<TrainingLoadout> = emptyList(),
    val resultGrades: TrainingResultGrades = TrainingResultGrades(),
    val rewards: List<TrainingReward> = emptyList()
)

/** Runtime projection of one accepted normal Item Combination recipe. */
data class ItemCombinationRecipe(
    val recipeNumber: Int,
    val outputGameItemId: Int,
    val ingredientAGameItemId: Int,
    val ingredientBGameItemId: Int,
    val baseSuccessPercent: Int,
    val outputQuantityMin: Int,
    val outputQuantityMax: Int,
    val ingredientAvailability: String? = null,
    val successOverrideSourceNote: String? = null,
    val sourceNote: String? = null,
    val sourceId: String,
    val sourceUrl: String
)

/** Failed combinations produce Garbage without a recipe number or fixed odds. */
data class ItemCombinationFailureRelation(
    val mechanism: String,
    val outputGameItemId: Int,
    val probabilitySemantics: String,
    val sourceId: String,
    val sourceUrl: String,
    val sourceNote: String? = null
)

/** Runtime projection of one accepted Item Shop purchase relation. */
data class ItemShopPurchaseRelation(
    val id: String,
    val shopId: String,
    val inventoryProfileId: String? = null,
    val gameItemId: Int,
    val priceZenny: Int,
    val availabilityConditionId: String? = null,
    val sourceId: String,
    val sourceUrl: String
)
data class ItemShopPeddlerProfile(
    val profileId: String,
    val semantics: String,
    val selectionSemantics: String,
    val availabilityConditionId: String? = null
)
data class ItemShopUnlockCondition(
    val conditionId: String,
    val sourceText: String,
    val displayLabel: String,
    val semantics: String,
    val sourceUrl: String
)
data class ItemTradeExchangeRelation(
    val id: String,
    val mechanism: String,
    val scopeType: String,
    val mapId: String? = null,
    val inputGameItemId: Int? = null,
    val inputQuantity: Int? = null,
    val currencyType: String? = null,
    val currencyCost: Int? = null,
    val outputGameItemId: Int,
    val outputQuantity: Int,
    val availabilityConditionId: String? = null,
    val questContext: String? = null,
    val interactionWindow: String? = null,
    val sourceId: String,
    val sourceUrl: String
)
data class ItemTradeExchangeCondition(
    val conditionId: String,
    val sourceText: String,
    val displayLabel: String,
    val semantics: String,
    val sourceId: String,
    val sourceUrl: String
)
data class ItemVeggieElderLocation(
    val mapId: String,
    val sourceJapaneseMapName: String,
    val elderLocationRaw: String,
    val locationType: String? = null,
    val areaNumber: Int? = null,
    val sourceId: String,
    val sourceUrl: String
)

@Serializable enum class SmallMonsterRewardContext { LOW, HIGH, GUILD_1_2 }
@Serializable enum class SmallMonsterRewardMethod { BODY_CARVE, SHINY_DROP }
@Serializable enum class SmallMonsterTipKind { ACQUISITION, DROP_CONDITION, BEHAVIOR }
@Serializable data class SmallMonsterRewardConditionDetails(
    val locations: List<String> = emptyList(),
    val wingColor: String? = null,
    val color: String? = null
)
@Serializable data class SmallMonsterReward(
    val id: String,
    val context: SmallMonsterRewardContext,
    val method: SmallMonsterRewardMethod,
    val condition: String? = null,
    val conditionDetails: SmallMonsterRewardConditionDetails? = null,
    val rolls: Int,
    val gameItemId: Int,
    val itemName: String,
    val chancePercent: Int,
    val quantity: Int
)
@Serializable data class SmallMonsterTip(
    val id: String,
    val kind: SmallMonsterTipKind,
    val text: String
)
@Serializable data class SmallMonster(
    val id: String,
    val name: String,
    val monsterClass: String,
    val rewards: List<SmallMonsterReward>,
    val tips: List<SmallMonsterTip>,
    val relatedQuestIds: List<String>,
    val trainingQuestIds: List<String> = emptyList()
)
@Serializable enum class MaterialSourceType {
    MONSTER_REWARD,
    SMALL_MONSTER,
    QUEST_REWARD,
    GATHERING,
    MINING,
    BUG,
    FISHING,
    FARM,
    TRADE,
    TRAINING_REWARD,
    COMBINATION,
    COMBINATION_FAILURE,
    SHOP_PURCHASE,
    SPECIAL_FREE,
    SCRAP_CONVERSION,
    DECORATION_CRAFTING,
    ROASTING,
    INVASION_REWARD,
    SUPPLY_BOX,
    PALICO_EXPEDITION
}

/** Source-native Small Monster condition identity retained for the Item
 * Sources presentation projection.  The existing `condition` string remains
 * a backwards-compatible summary for other source families. */
@Serializable data class MaterialSourceConditionDetails(
    val locations: List<String> = emptyList(),
    val wingColor: String? = null,
    val color: String? = null
)

/**
 * UI/domain representation of a material source.
 *
 * A reverse projection over source-native generated arrays. Nullable origin fields
 * are meaningful only for the source type that owns them.
 */
@Serializable data class MaterialSource(
    val id: String,
    val type: MaterialSourceType,
    val name: String,
    val monsterId: String? = null,
    val smallMonsterId: String? = null,
    val questId: String? = null,
    val trainingQuestId: String? = null,
    /** Explicit Training mission ordering/context for the presentation-only projection. */
    val trainingClass: String? = null,
    val trainingMissionOrder: Int? = null,
    val sourceOrdinal: Int? = null,
    val locationId: String? = null,
    val nodeId: String? = null,
    val facilityId: String? = null,
    val profileId: String? = null,
    val profileTriggerKnown: Boolean? = null,
    val inputGameItemId: Int? = null,
    val inputItemName: String? = null,
    /** Scrap-conversion source facts; input and output sides are kept explicit. */
    val scrapOutputGameItemId: Int? = null,
    val scrapInputQuantity: Int? = null,
    val scrapTriggerModes: List<String> = emptyList(),
    val farmAction: String? = null,
    val rank: RewardContext? = null,
    val context: String? = null,
    val method: String? = null,
    val condition: String? = null,
    val conditionKey: String? = null,
    val conditionDetails: MaterialSourceConditionDetails? = null,
    val chance: Int? = null,
    val quantity: Int? = null,
    /** Source-native Field quantity flag; null for source families that do not publish it. */
    val quantityExplicit: Boolean? = null,
    val rolls: Int? = null,
    val probabilitySemantics: String? = null,
    val quantitySemantics: String? = null,
    val probabilityBand: InvasionProbabilityBand? = null,
    val invasionContext: String? = null,
    val recipeNumber: Int? = null,
    val ingredientAGameItemId: Int? = null,
    val ingredientAName: String? = null,
    val ingredientBGameItemId: Int? = null,
    val ingredientBName: String? = null,
    val baseSuccessPercent: Int? = null,
    val outputQuantityMin: Int? = null,
    val outputQuantityMax: Int? = null,
    val ingredientAvailability: String? = null,
    val successOverrideSourceNote: String? = null,
    val sourceNote: String? = null,
    val shopId: String? = null,
    val shopGameItemId: Int? = null,
    val shopInventoryProfileId: String? = null,
    val shopProfileSemantics: String? = null,
    val shopSelectionSemantics: String? = null,
    val priceZenny: Int? = null,
    val shopAvailabilityConditionId: String? = null,
    val shopAvailabilityConditionSemantics: String? = null,
    val sourceUrl: String? = null,
    val tradeMechanism: String? = null,
    val tradeScopeType: String? = null,
    val tradeMapId: String? = null,
    val tradeInputQuantity: Int? = null,
    val tradeCurrencyType: String? = null,
    val tradeCurrencyCost: Int? = null,
    val tradeAvailabilityConditionId: String? = null,
    val tradeQuestContext: String? = null,
    val tradeInteractionWindow: String? = null,
    val tradeSourceId: String? = null,
    /** Source-native fields for Special/Free acquisition relations. */
    val specialFreeMechanism: String? = null,
    val specialFreeGiverId: String? = null,
    val specialFreeTrigger: String? = null,
    val specialFreeOutcomeSelection: String? = null,
    val specialFreeScopeType: String? = null,
    val specialFreeMapId: String? = null,
    val specialFreeCompletionSetId: String? = null,
    /** Full source-native Special projection fields; null means unpublished/not applicable. */
    val specialFreeConditionType: String? = null,
    val specialFreeCounterType: String? = null,
    val specialFreeInterval: Int? = null,
    val specialFreeThresholdMin: Int? = null,
    val specialFreeThresholdMax: Int? = null,
    val specialFreeQuestContext: String? = null,
    val specialFreeInteractionWindow: String? = null,
    val specialFreePalicoOrigin: String? = null,
    val specialFreeAffectionFrom: Int? = null,
    val specialFreeAffectionTo: Int? = null,
    val specialFreeSourceDisagreement: Boolean = false,
    val specialFreeAccuracyReviewRequired: Boolean = false,
    /** Source-native decoration recipe identity and graph endpoints. */
    val decorationRecipeId: String? = null,
    val decorationOutputGameItemId: Int? = null,
    val decorationIngredientGameItemId: Int? = null,
    /** Roasting source graph fields. Output rows and reverse usage rows share this type. */
    val outputGameItemId: Int? = null,
    val outputItemName: String? = null,
    val roastingInputQuantity: Int? = null,
    val resultState: String? = null,
    val resultSemantics: String? = null,
    val supportedToolGameItemIds: List<Int> = emptyList(),
    val batchCapacity: Int? = null,
    /** Quest Supply layout fields; these are not guaranteed take-home totals. */
    val supplyQuantityValue: Int? = null,
    val supplyQuantityNotation: String? = null,
    val supplyBundleCount: Int? = null,
    val supplyDistributionSemantics: String? = null,
    val supplyAvailabilityTiming: String? = null,
    val supplyLifecycle: RegularQuestSupplyLifecycle? = null,
    val supplyPersistentAcquisition: Boolean? = null,
    /** All persistent rows for this quest/item, retained for multi-entry UI. */
    val supplyEntries: List<SupplyBoxEntry> = emptyList(),
    /** Raw Quest Reward identity retained for exact quest-first projection. */
    val rewardPool: String? = null,
    val rewardRelationId: String? = null,
    val expeditionId: String? = null,
    val expeditionRewardCategory: String? = null,
    /** Canonical Palico expedition metadata retained for presentation projection. */
    val expeditionStarRank: Int? = null,
    val expeditionCostPerPalico: Int? = null
)

data class RoastingConversion(
    val id: String,
    val context: String,
    val inputGameItemId: Int,
    val inputQuantity: Int,
    val outputGameItemId: Int,
    val outputQuantity: Int,
    val resultState: String,
    val resultSemantics: String,
    val probabilityValuePercent: Int? = null,
    val supportedToolGameItemIds: List<Int> = emptyList(),
    val facilityId: String? = null,
    val batchCapacity: Int? = null
)

/** Source-censused decoration recipe projected into the runtime graph. */
data class DecorationCraftingIngredient(
    val gameItemId: Int,
    val quantity: Int?,
    val quantitySemantics: String
)

data class DecorationCraftingRecipe(
    val id: String,
    val outputGameItemId: Int,
    val rankContext: String,
    val ingredients: List<DecorationCraftingIngredient>
)

@Serializable data class Material(
    val id: String,
    val name: String,
    val rarity: Int? = null,
    val description: String,
    val sources: List<MaterialSource>,
    val uses: List<String>,
    val aliases: List<String> = emptyList(),
    val gameItemId: Int? = null
)

@Serializable data class GeneratedDataset(
    val schemaVersion: Int,
    val enrichmentVersion: Int? = null,
    val huntPrepVersion: Int,
    val materialSourcesVersion: Int,
    val monsters: List<GeneratedMonster>,
    val hitzones: List<GeneratedHitzone>,
    val rewards: List<GeneratedReward>,
    val breakableParts: List<GeneratedPart>,
    val quests: List<GeneratedQuest>,
    val items: List<GeneratedItem>,
    val sourceCatalog: List<GeneratedSourceCatalogEntry> = emptyList(),
    val hitzoneStateSemantics: List<GeneratedHitzoneStateSemantic> = emptyList(),
    val statusEffects: List<GeneratedStatusEffect> = emptyList(),
    val itemEffects: List<GeneratedItemEffect> = emptyList(),
    val monsterBehavior: List<GeneratedMonsterBehavior> = emptyList(),
    val huntPrepItemCatalog: List<GeneratedHuntPrepItem> = emptyList(),
    val monsterHuntPrep: List<GeneratedMonsterHuntPrep> = emptyList(),
    val gatheringNodes: List<GeneratedGatheringNode> = emptyList(),
    val gatheringDrops: List<GeneratedGatheringDrop> = emptyList(),
    val farmFacilities: List<GeneratedFarmFacility> = emptyList(),
    val farmProfiles: List<GeneratedFarmProfile> = emptyList(),
    val itemCombinationRecipes: List<GeneratedItemCombinationRecipe> = emptyList(),
    val itemCombinationFailureRelations: List<GeneratedItemCombinationFailureRelation> = emptyList(),
    val farmDrops: List<GeneratedFarmDrop> = emptyList(),
    val questRewardDrops: List<GeneratedQuestRewardDrop> = emptyList(),
    val smallMonsterDataVersion: Int? = null,
    val smallMonsterNameSource: GeneratedSmallMonsterNameSource? = null,
    val smallMonsters: List<GeneratedSmallMonster> = emptyList(),
    val smallMonsterItemCatalog: List<GeneratedSmallMonsterItem> = emptyList(),
    val smallMonsterRewards: List<GeneratedSmallMonsterReward> = emptyList(),
    val smallMonsterTips: List<GeneratedSmallMonsterTip> = emptyList(),
    val itemNameSource: GeneratedItemNameSource? = null,
    val itemMasterVersion: Int? = null,
    val itemMasterSource: GeneratedItemMasterSource? = null,
    val sourceCorrections: List<GeneratedSourceCorrection> = emptyList(),
    val questNameSource: GeneratedQuestNameSource? = null,
    val questLocationSource: GeneratedQuestLocationSource? = null,
    val weapons: List<GeneratedWeapon> = emptyList(),
    val huntingHornSongCatalog: List<GeneratedHuntingHornSongSet> = emptyList(),
    val trainingQuests: List<GeneratedTrainingQuest> = emptyList(),
    val itemShopPurchaseRelations: List<GeneratedItemShopPurchaseRelation> = emptyList(),
    val itemShopPeddlerProfiles: List<GeneratedItemShopPeddlerProfile> = emptyList(),
    val itemShopUnlockConditions: List<GeneratedItemShopUnlockCondition> = emptyList(),
    val itemTradeExchangeRelations: List<GeneratedItemTradeExchangeRelation> = emptyList(),
    val itemTradeExchangeConditions: List<GeneratedItemTradeExchangeCondition> = emptyList(),
    val itemVeggieElderLocations: List<GeneratedItemVeggieElderLocation> = emptyList(),
    val itemSpecialFreeAcquisitionRelations: List<GeneratedItemSpecialFreeAcquisitionRelation> = emptyList(),
    val decorationCraftingRecipes: List<GeneratedDecorationCraftingRecipe> = emptyList(),
    val itemScrapConversionRules: List<GeneratedItemScrapConversionRule> = emptyList(),
    val roastingConversions: List<GeneratedRoastingConversion> = emptyList(),
    val invasionRewardDrops: List<GeneratedInvasionRewardDrop> = emptyList(),
    /** Schema-24 regular Quest supply-box layout relations. */
    val regularQuestSupplyItems: List<GeneratedRegularQuestSupplyItem> = emptyList(),
    /** One coverage status for every regular Quest, including source gaps. */
    val regularQuestSupplyCoverage: List<GeneratedRegularQuestSupplyCoverage> = emptyList(),
    val palicoExpeditions: List<GeneratedPalicoExpedition> = emptyList(),
    val palicoExpeditionRewardDrops: List<GeneratedPalicoExpeditionRewardDrop> = emptyList(),
    val skillTrees: List<GeneratedSkillTree> = emptyList(),
    /** Schema-31 accepted Decoration/Jewel ↔ Skill Tree relations. */
    val decorationSkillRelations: List<GeneratedDecorationSkillRelation> = emptyList(),
    /** Schema-28 display-only TMO terminology overlay. Source/raw fields remain authoritative. */
    val displayTerminology: GeneratedDisplayTerminology? = null
)

@Serializable data class GeneratedSkillTree(
    val stableSkillTreeId: String,
    val displayName: String,
    val uiDisplayName: String? = null,
    val tmoDisplayName: String,
    val jpName: String,
    val inGameDescription: String,
    val thresholds: List<GeneratedSkillThreshold> = emptyList(),
    val specialMechanics: List<GeneratedSkillSpecialMechanic> = emptyList()
)

@Serializable data class GeneratedSkillThreshold(
    val points: Int,
    val activatedSkillName: String,
    val polarity: String,
    val effectSummary: String
)

@Serializable data class GeneratedSkillSpecialMechanic(
    val type: String,
    val name: String,
    val effectSummary: String
)

@Serializable data class GeneratedDecorationSkillRelation(
    val stableDecorationItemId: String,
    val stableSkillTreeId: String,
    val points: Int,
    val slotCost: Int
)

@Serializable data class GeneratedDisplayTerminology(
    val trainingArmorByRaw: Map<String, String> = emptyMap(),
    val trainingSkillByRaw: Map<String, String> = emptyMap(),
    val trainingCharmByRaw: Map<String, String> = emptyMap(),
    val objectiveByStableId: Map<String, String> = emptyMap()
)

@Serializable data class GeneratedPalicoExpeditionUnlockChoice(
    val kind: String,
    val questId: String? = null,
    val facilityId: String? = null
)
@Serializable data class GeneratedPalicoExpeditionUnlockCondition(
    val unlockAnyOf: List<GeneratedPalicoExpeditionUnlockChoice>,
    val facilityUnlockAnyOf: List<GeneratedPalicoExpeditionUnlockChoice> = emptyList()
)
@Serializable data class GeneratedPalicoExpedition(
    val id: String,
    val starRank: Int,
    val canonicalName: String,
    val dispatchPointsPerPalico: Int,
    val unlockCondition: GeneratedPalicoExpeditionUnlockCondition
)
@Serializable data class GeneratedPalicoExpeditionRewardDrop(
    val id: String,
    val expeditionId: String,
    val gameItemId: Int,
    val rewardCategory: String,
    val quantity: Int
)

@Serializable enum class GeneratedRegularQuestSupplyAvailabilityTiming {
    SOURCE_UNSPECIFIED
}

@Serializable enum class GeneratedRegularQuestSupplyQuantitySemantics {
    FINITE_SINGLE_BUNDLE,
    FINITE_MULTI_BUNDLE,
    INFINITE
}

@Serializable enum class GeneratedRegularQuestSupplyDistributionSemantics {
    SOURCE_LAYOUT,
    SOURCE_LAYOUT_INFINITE
}

@Serializable enum class GeneratedRegularQuestSupplyLifecycle {
    QUEST_LOCAL_SUPPLY_ONLY,
    PERSISTENT_NORMAL_SUPPLY
}

@Serializable data class GeneratedRegularQuestSupplyItem(
    val id: String,
    val questId: String,
    val gameItemId: Int,
    val availabilityTiming: GeneratedRegularQuestSupplyAvailabilityTiming,
    val quantityValue: Int? = null,
    val quantityNotation: String? = null,
    val bundleCount: Int? = null,
    val quantitySemantics: GeneratedRegularQuestSupplyQuantitySemantics,
    val distributionSemantics: String,
    val lifecycle: GeneratedRegularQuestSupplyLifecycle,
    val persistentAcquisition: Boolean
)

@Serializable enum class GeneratedRegularQuestSupplyCoverageStatus {
    DATA_PRESENT,
    EXPLICIT_NO_SUPPLY,
    SOURCE_UNAVAILABLE
}

@Serializable data class GeneratedRegularQuestSupplyCoverage(
    val questId: String,
    val status: GeneratedRegularQuestSupplyCoverageStatus
)

@Serializable enum class GeneratedWeaponSpecialKind { ELEMENT, STATUS }
@Serializable enum class GeneratedWeaponSpecialType { FIRE, WATER, THUNDER, ICE, DRAGON, POISON, PARALYSIS, SLEEP }
@Serializable data class GeneratedWeaponSpecial(
    val kind: GeneratedWeaponSpecialKind,
    val type: GeneratedWeaponSpecialType,
    val value: Int
)
@Serializable data class GeneratedWeaponSharpness(
    val red: Int = 0,
    val orange: Int = 0,
    val yellow: Int = 0,
    val green: Int = 0,
    val blue: Int = 0,
    val white: Int = 0
) {
    fun toRuntime() = WeaponSharpness(red, orange, yellow, green, blue, white)
}
@Serializable data class GeneratedWeaponSharpnessProfile(
    val normal: GeneratedWeaponSharpness,
    val plusOne: GeneratedWeaponSharpness? = null
)
@Serializable data class GeneratedWeaponRecipeIngredient(val gameItemId: Int, val quantity: Int)
@Serializable data class GeneratedWeaponRecipe(
    val zenny: Int? = null,
    val ingredients: List<GeneratedWeaponRecipeIngredient>
) {
    fun toRuntime() = WeaponRecipe(zenny, ingredients.map { WeaponRecipeIngredient(it.gameItemId, it.quantity) })
}
@Serializable data class GeneratedWeaponUpgradeFrom(
    val edgeId: String,
    val fromWeaponId: String,
    val recipe: GeneratedWeaponRecipe
)
@Serializable enum class GeneratedWeaponMechanicsKind { HUNTING_HORN, GUNLANCE, SWITCH_AXE, BOW, LIGHT_BOWGUN, HEAVY_BOWGUN }
@Serializable enum class GeneratedHornNote { WHITE, PURPLE, RED, BLUE, GREEN, CYAN, YELLOW, ORANGE }
@Serializable enum class GeneratedGunlanceShellingType { NORMAL, LONG, WIDE }
@Serializable enum class GeneratedSwitchAxePhialType { POWER, ELEMENTAL, EXHAUST, PARALYSIS, DRAGON, POISON }
@Serializable enum class GeneratedBowArcShot { WIDE, FOCUS, BLAST }
@Serializable enum class GeneratedBowShotType { RAPID, PIERCE, SPREAD }
@Serializable enum class GeneratedBowChargeAvailability { NORMAL, REQUIRES_LOAD_UP }
@Serializable data class GeneratedBowCharge(
    val shotType: GeneratedBowShotType,
    val shotLevel: Int,
    val availability: GeneratedBowChargeAvailability
)
@Serializable enum class GeneratedBowCoatingType { POWER, CLOSE_RANGE, POISON, PAINT, PARALYSIS, EXHAUST, SLEEP }
@Serializable enum class GeneratedBowCoatingSupportLevel { NORMAL, ENHANCED }
@Serializable data class GeneratedBowCoating(
    val coatingType: GeneratedBowCoatingType,
    val supportLevel: GeneratedBowCoatingSupportLevel
)
@Serializable enum class GeneratedBowgunAmmoFamily {
    @SerialName("Normal S") NORMAL_S,
    @SerialName("Pierce S") PIERCE_S,
    @SerialName("Pellet S") PELLET_S,
    @SerialName("Crag S") CRAG_S,
    @SerialName("Clust S") CLUST_S,
    @SerialName("Recov S") RECOV_S,
    @SerialName("Poison S") POISON_S,
    @SerialName("Para S") PARA_S,
    @SerialName("Sleep S") SLEEP_S,
    @SerialName("Exhaust S") EXHAUST_S,
    @SerialName("Flaming S") FLAMING_S,
    @SerialName("Water S") WATER_S,
    @SerialName("Thunder S") THUNDER_S,
    @SerialName("Freeze S") FREEZE_S,
    @SerialName("Dragon S") DRAGON_S,
    @SerialName("Tranq S") TRANQ_S,
    @SerialName("Paint S") PAINT_S,
    @SerialName("Demon S") DEMON_S,
    @SerialName("Armor S") ARMOR_S,
    @SerialName("Slicing S") SLICING_S
}
@Serializable enum class GeneratedBowgunReloadValue {
    @SerialName("VeryFast") VERY_FAST,
    @SerialName("Fast") FAST,
    @SerialName("Normal") NORMAL,
    @SerialName("Slow") SLOW,
    @SerialName("VerySlow") VERY_SLOW
}
@Serializable enum class GeneratedBowgunRecoilValue {
    @SerialName("Light") LIGHT,
    @SerialName("Weak") WEAK,
    @SerialName("Moderate") MODERATE,
    @SerialName("Strong") STRONG,
    @SerialName("STRONGEST") STRONGEST
}
@Serializable enum class GeneratedBowgunDeviationMagnitude { NONE, SMALL, LARGE }
@Serializable enum class GeneratedBowgunDeviationDirection { NONE, LEFT, RIGHT, BOTH }
@Serializable enum class GeneratedBowgunAmmoSourceShape { THREE_LEVEL, TWO_LEVEL, SINGLE_CAPACITY }
@Serializable data class GeneratedBowgunRound(val ammoType: GeneratedBowgunAmmoFamily, val level: Int? = null)
@Serializable data class GeneratedBowgunAmmoLoad(val round: GeneratedBowgunRound, val capacity: Int, val sourceShape: GeneratedBowgunAmmoSourceShape)
@Serializable data class GeneratedBowgunReload(val value: GeneratedBowgunReloadValue, val semanticOrder: Int, val rawSourceValue: String)
@Serializable data class GeneratedBowgunRecoil(val value: GeneratedBowgunRecoilValue, val semanticOrder: Int, val rawSourceValue: String, val spellingAnomaly: Boolean? = null)
@Serializable data class GeneratedBowgunDeviation(val magnitude: GeneratedBowgunDeviationMagnitude, val direction: GeneratedBowgunDeviationDirection, val rawDrift: String)
@Serializable data class GeneratedBowgunSharedMechanics(
    val reload: GeneratedBowgunReload,
    val recoil: GeneratedBowgunRecoil,
    val deviation: GeneratedBowgunDeviation,
    val ammoLoads: List<GeneratedBowgunAmmoLoad>
)
@Serializable data class GeneratedBowgunRapidFire(val round: GeneratedBowgunRound, val burstCount: Int, val recoil: GeneratedBowgunRecoil)
@Serializable data class GeneratedBowgunCrouchFire(val round: GeneratedBowgunRound, val rawSourceToken: String)
@Serializable data class GeneratedWeaponMechanics(
    val kind: GeneratedWeaponMechanicsKind,
    val notes: List<GeneratedHornNote> = emptyList(),
    val shellingType: GeneratedGunlanceShellingType? = null,
    val shellingLevel: Int? = null,
    val phialType: GeneratedSwitchAxePhialType? = null,
    val arcShot: GeneratedBowArcShot? = null,
    val charges: List<GeneratedBowCharge> = emptyList(),
    val coatings: List<GeneratedBowCoating> = emptyList(),
    val shared: GeneratedBowgunSharedMechanics? = null,
    val rapidFire: List<GeneratedBowgunRapidFire> = emptyList(),
    val crouchFire: List<GeneratedBowgunCrouchFire> = emptyList()
) {
    private fun GeneratedBowgunRound.toRuntime() = BowgunRound(BowgunAmmoFamily.valueOf(ammoType.name), level)
    private fun GeneratedBowgunReload.toRuntime() = BowgunReload(BowgunReloadValue.valueOf(value.name), semanticOrder, rawSourceValue)
    private fun GeneratedBowgunRecoil.toRuntime() = BowgunRecoil(BowgunRecoilValue.valueOf(value.name), semanticOrder, rawSourceValue, spellingAnomaly)
    private fun GeneratedBowgunSharedMechanics.toRuntime() = BowgunSharedMechanics(
        reload.toRuntime(), recoil.toRuntime(),
        BowgunDeviation(BowgunDeviationMagnitude.valueOf(deviation.magnitude.name), BowgunDeviationDirection.valueOf(deviation.direction.name), deviation.rawDrift),
        ammoLoads.map { BowgunAmmoLoad(it.round.toRuntime(), it.capacity, BowgunAmmoSourceShape.valueOf(it.sourceShape.name)) }
    )
    fun toRuntime(): WeaponMechanics = when (kind) {
        GeneratedWeaponMechanicsKind.HUNTING_HORN -> {
            require(notes.size == 3 && shellingType == null && shellingLevel == null && phialType == null) { "Invalid Hunting Horn mechanics payload" }
            HuntingHornMechanics(notes.map { HornNote.valueOf(it.name) })
        }
        GeneratedWeaponMechanicsKind.GUNLANCE -> {
            require(notes.isEmpty() && shellingType != null && shellingLevel != null && shellingLevel in 1..4 && phialType == null) { "Invalid Gunlance mechanics payload" }
            GunlanceMechanics(GunlanceShellingType.valueOf(shellingType.name), shellingLevel)
        }
        GeneratedWeaponMechanicsKind.SWITCH_AXE -> {
            require(notes.isEmpty() && shellingType == null && shellingLevel == null && phialType != null) { "Invalid Switch Axe mechanics payload" }
            SwitchAxeMechanics(SwitchAxePhialType.valueOf(phialType.name))
        }
        GeneratedWeaponMechanicsKind.BOW -> {
            require(notes.isEmpty() && shellingType == null && shellingLevel == null && phialType == null && arcShot != null && charges.size in 3..4) { "Invalid Bow mechanics payload" }
            require(charges.all { it.shotLevel in 1..5 }) { "Invalid Bow charge level" }
            require(coatings.map { it.coatingType }.distinct().size == coatings.size) { "Duplicate Bow coating family" }
            BowMechanics(
                arcShot = BowArcShot.valueOf(arcShot.name),
                charges = charges.map { BowCharge(BowShotType.valueOf(it.shotType.name), it.shotLevel, BowChargeAvailability.valueOf(it.availability.name)) },
                coatings = coatings.map { BowCoatingSupport(BowCoatingType.valueOf(it.coatingType.name), BowCoatingSupportLevel.valueOf(it.supportLevel.name)) }
            )
        }
        GeneratedWeaponMechanicsKind.LIGHT_BOWGUN -> {
            require(notes.isEmpty() && shellingType == null && shellingLevel == null && phialType == null && arcShot == null && charges.isEmpty() && coatings.isEmpty() && shared != null && crouchFire.isEmpty()) { "Invalid Light Bowgun mechanics payload" }
            LightBowgunMechanics(shared.toRuntime(), rapidFire.map { BowgunRapidFire(it.round.toRuntime(), it.burstCount, it.recoil.toRuntime()) })
        }
        GeneratedWeaponMechanicsKind.HEAVY_BOWGUN -> {
            require(notes.isEmpty() && shellingType == null && shellingLevel == null && phialType == null && arcShot == null && charges.isEmpty() && coatings.isEmpty() && shared != null && rapidFire.isEmpty()) { "Invalid Heavy Bowgun mechanics payload" }
            HeavyBowgunMechanics(shared.toRuntime(), crouchFire.map { BowgunCrouchFire(it.round.toRuntime(), it.rawSourceToken) })
        }
    }
}
@Serializable data class GeneratedHuntingHornSong(
    val sequence: List<String>,
    val noteIdentities: List<GeneratedHornNote>,
    val effectName: String
) {
    fun toRuntime() = HuntingHornSong(sequence, noteIdentities.map { HornNote.valueOf(it.name) }, effectName)
}
@Serializable data class GeneratedHuntingHornSongSet(
    val noteSetKey: String,
    val sourceKey: String? = null,
    val songs: List<GeneratedHuntingHornSong>
) {
    fun toRuntime() = HuntingHornSongSet(noteSetKey, sourceKey, songs.map { it.toRuntime() })
}
@Serializable data class GeneratedWeapon(
    val stableWeaponId: String,
    val weaponType: String,
    val sourceOrdinal: Int,
    val sourceName: String,
    val sourceKey: String,
    val displayName: String,
    val nameSource: String,
    val rarity: Int,
    val attack: Int,
    val affinity: Int,
    val slots: Int,
    val defenseBonus: Int? = null,
    val special: GeneratedWeaponSpecial? = null,
    val sharpness: GeneratedWeaponSharpnessProfile? = null,
    val forgeRecipe: GeneratedWeaponRecipe? = null,
    val upgradeFrom: GeneratedWeaponUpgradeFrom? = null,
    val orphanUpgradeRecipe: GeneratedWeaponRecipe? = null,
    val upgradesTo: List<String> = emptyList(),
    val mechanics: GeneratedWeaponMechanics? = null
) {
    fun toRuntime() = Weapon(
        stableWeaponId = stableWeaponId,
        weaponType = weaponType,
        sourceOrdinal = sourceOrdinal,
        sourceName = sourceName,
        sourceKey = sourceKey,
        displayName = displayName,
        nameSource = nameSource,
        rarity = rarity,
        attack = attack,
        affinity = affinity,
        slots = slots,
        defenseBonus = defenseBonus,
        special = special?.let { WeaponSpecial(WeaponSpecialKind.valueOf(it.kind.name), WeaponSpecialType.valueOf(it.type.name), it.value) },
        sharpness = sharpness?.let { WeaponSharpnessProfile(it.normal.toRuntime(), it.plusOne?.toRuntime()) },
        forgeRecipe = forgeRecipe?.toRuntime(),
        upgradeFrom = upgradeFrom?.let { WeaponUpgradeFrom(it.edgeId, it.fromWeaponId, it.recipe.toRuntime()) },
        upgradesTo = upgradesTo,
        orphanUpgradeRecipe = orphanUpgradeRecipe?.toRuntime(),
        mechanics = mechanics?.toRuntime()
    )
}
@Serializable data class GeneratedSmallMonsterNameSource(
    val id: String, val name: String, val language: String, val build: String, val sourceFile: String
)
@Serializable data class GeneratedSmallMonsterSourcePointer(
    val sourceId: String, val entry: Int, val offset: Int
)
@Serializable data class GeneratedSmallMonster(
    val id: String,
    val name: String,
    val sourceJapaneseName: String,
    val monsterClass: String,
    val questIds: List<String>,
    val nameSource: GeneratedSmallMonsterSourcePointer,
    val classSource: GeneratedSmallMonsterSourcePointer,
    val trainingQuestIds: List<String> = emptyList()
)
@Serializable enum class GeneratedSmallMonsterItemCatalogStatus { CURRENT_ITEM, PENDING_ITEM_PROMOTION }
@Serializable data class GeneratedSmallMonsterItem(
    val gameItemId: Int,
    val name: String,
    val nameSourceId: String,
    val existingItemId: String? = null,
    val catalogStatus: GeneratedSmallMonsterItemCatalogStatus
)
@Serializable enum class GeneratedSmallMonsterRewardContext { GUILD_1_2, LOW, HIGH }
@Serializable enum class GeneratedSmallMonsterRewardMethod { BODY_CARVE, SHINY_DROP }
@Serializable enum class GeneratedSmallMonsterProbabilitySemantics { SOURCE_TABLE_PERCENT }
@Serializable data class GeneratedSmallMonsterConditionDetails(
    val locations: List<String> = emptyList(),
    val wingColor: String? = null,
    val color: String? = null
)
@Serializable data class GeneratedSmallMonsterReward(
    val id: String,
    val smallMonsterId: String,
    val context: GeneratedSmallMonsterRewardContext,
    val method: GeneratedSmallMonsterRewardMethod,
    val condition: String? = null,
    val conditionDetails: GeneratedSmallMonsterConditionDetails? = null,
    val rolls: Int,
    val gameItemId: Int,
    val itemName: String,
    val chancePercent: Int,
    val quantity: Int,
    val probabilitySemantics: GeneratedSmallMonsterProbabilitySemantics,
    val sourceId: String,
    val sourcePage: String,
    val sourceLines: String
)
@Serializable enum class GeneratedSmallMonsterTipKind { ACQUISITION, DROP_CONDITION, BEHAVIOR }
@Serializable data class GeneratedSmallMonsterTip(
    val id: String,
    val smallMonsterId: String,
    val kind: GeneratedSmallMonsterTipKind,
    val text: String,
    val sourceId: String,
    val sourcePage: String,
    val sourceLines: String
)
@Serializable enum class GeneratedGatheringMethod { GATHERING, MINING, BUGNET, FISHING }
@Serializable enum class GeneratedMaterialRankContext { LOW, HIGH }
@Serializable enum class GeneratedSourceProbabilitySemantics { NOT_PUBLISHED, FARM_OUTPUT, QUEST_REWARD_SLOT, DETERMINISTIC_PER_HARVEST_SLOT }
@Serializable data class GeneratedSourceProbability(
    val valuePercent: Int? = null,
    val semantics: GeneratedSourceProbabilitySemantics
)
@Serializable data class GeneratedGatheringNode(
    val id: String,
    val locationId: String,
    val locationName: String,
    val area: String,
    val pointIndex: String,
    val method: GeneratedGatheringMethod,
    val sourceId: String,
    val sourceUrl: String
)
@Serializable data class GeneratedGatheringDrop(
    val nodeId: String,
    val rankContext: GeneratedMaterialRankContext,
    val gameItemId: Int,
    val quantity: Int,
    val quantityExplicit: Boolean,
    val probability: GeneratedSourceProbability,
    val sourceId: String
)
@Serializable data class GeneratedFarmFacility(
    val id: String,
    val facilityType: String,
    val tier: String,
    val unlockCondition: String,
    val sourceId: String,
    val sourceUrl: String,
    /** Complete source-backed OR unlock routes; absent on unaffected facilities. */
    val unlockAnyOf: List<String> = emptyList(),
    /** Sequential facility prerequisite that is additionally required. */
    val prerequisiteFacilityId: String? = null
)
@Serializable data class GeneratedFarmProfile(
    val id: String,
    val facilityId: String,
    val triggerKnown: Boolean,
    val triggerDescription: String? = null
)
@Serializable data class GeneratedFarmDrop(
    val facilityId: String,
    val gameItemId: Int,
    val quantity: Int,
    val quantityExplicit: Boolean,
    val probability: GeneratedSourceProbability,
    val sourceId: String,
    val inputGameItemId: Int? = null,
    val profileId: String? = null,
    val quantitySemantics: String? = null,
    val condition: String? = null
)
@Serializable data class GeneratedRoastingConversion(
    val id: String,
    val context: String,
    val inputGameItemId: Int,
    val inputQuantity: Int,
    val outputGameItemId: Int,
    val outputQuantity: Int,
    val resultState: String,
    val resultSemantics: String,
    val probability: GeneratedSourceProbability? = null,
    val supportedToolGameItemIds: List<Int> = emptyList(),
    val facilityId: String? = null,
    val batchCapacity: Int? = null
)
@Serializable data class GeneratedItemCombinationRecipe(
    val recipeNumber: Int,
    val outputGameItemId: Int,
    val ingredientAGameItemId: Int,
    val ingredientBGameItemId: Int,
    val baseSuccessPercent: Int,
    val outputQuantityMin: Int,
    val outputQuantityMax: Int,
    val ingredientAvailability: String? = null,
    val successOverrideSourceNote: String? = null,
    val sourceNote: String? = null,
    val sourceId: String,
    val sourceUrl: String
)
@Serializable data class GeneratedItemCombinationFailureRelation(
    val mechanism: String,
    val outputGameItemId: Int,
    val probabilitySemantics: String,
    val sourceId: String,
    val sourceUrl: String,
    val sourceNote: String? = null
)
@Serializable data class GeneratedItemShopPurchaseRelation(
    val id: String,
    val shopId: String,
    val inventoryProfileId: String? = null,
    val gameItemId: Int,
    val priceZenny: Int,
    val availabilityConditionId: String? = null,
    val sourceId: String,
    val sourceUrl: String
)
@Serializable data class GeneratedItemShopPeddlerProfile(
    val profileId: String,
    val semantics: String,
    val selectionSemantics: String,
    val availabilityConditionId: String? = null
)
@Serializable data class GeneratedItemShopUnlockCondition(
    val conditionId: String,
    val sourceText: String,
    val displayLabel: String,
    val semantics: String,
    val sourceUrl: String
)
@Serializable data class GeneratedItemTradeExchangeRelation(
    val id: String,
    val mechanism: String,
    val scopeType: String,
    val mapId: String? = null,
    val inputGameItemId: Int? = null,
    val inputQuantity: Int? = null,
    val currencyType: String? = null,
    val currencyCost: Int? = null,
    val outputGameItemId: Int,
    val outputQuantity: Int,
    val availabilityConditionId: String? = null,
    val questContext: String? = null,
    val interactionWindow: String? = null,
    val sourceId: String,
    val sourceUrl: String
)
@Serializable data class GeneratedItemTradeExchangeCondition(
    val conditionId: String,
    val sourceText: String,
    val displayLabel: String,
    val semantics: String,
    val sourceId: String,
    val sourceUrl: String
)
@Serializable data class GeneratedItemVeggieElderLocation(
    val mapId: String,
    val sourceJapaneseMapName: String,
    val elderLocationRaw: String,
    val locationType: String? = null,
    val areaNumber: Int? = null,
    val sourceId: String,
    val sourceUrl: String
)

@Serializable data class GeneratedDecorationCraftingIngredient(
    val gameItemId: Int,
    val quantity: Int? = null,
    val quantitySemantics: String
)

@Serializable data class GeneratedDecorationCraftingRecipe(
    val id: String,
    val outputGameItemId: Int,
    val rankContext: String,
    val ingredients: List<GeneratedDecorationCraftingIngredient>
)

@Serializable data class GeneratedItemSpecialFreeAcquisitionRelation(
    val relationId: String,
    val mechanism: String,
    val giverId: String? = null,
    val outputGameItemId: Int,
    val outputQuantity: Int? = null,
    val quantitySemantics: String? = null,
    val conditionType: String? = null,
    val questId: String? = null,
    val completionSetId: String? = null,
    val recurring: Boolean? = null,
    val counterType: String? = null,
    val interval: Int? = null,
    val thresholdMin: Int? = null,
    val thresholdMax: Int? = null,
    val probabilityPercent: Int? = null,
    val probabilitySemantics: String? = null,
    val scopeType: String? = null,
    val mapId: String? = null,
    val questContext: String? = null,
    val interactionWindow: String? = null,
    val triggerSemantics: String? = null,
    val outcomeSelectionSemantics: String? = null,
    val sourceDisagreement: Boolean = false,
    val accuracyReviewRequired: Boolean = false,
    val sourceId: String,
    val sourceUrl: String? = null
)
@Serializable data class GeneratedItemScrapConversionRule(
    val inputGameItemId: Int,
    val inputQuantity: Int,
    val outputScrapGameItemId: Int,
    val outputQuantity: Int? = null,
    val quantitySemantics: String,
    val triggerModes: List<String>,
    val sourceId: String,
    val sourceUrl: String,
    val sourceRowOrdinal: Int? = null
)
@Serializable enum class GeneratedQuestRewardPool { FIXED, BASIC, ADDITIONAL }
@Serializable data class GeneratedQuestRewardDrop(
    val questId: String? = null,
    val sourceQuestKey: String,
    val rewardPool: GeneratedQuestRewardPool,
    val gameItemId: Int,
    val quantity: Int,
    val quantityExplicit: Boolean,
    val probability: GeneratedSourceProbability,
    val sourceId: String,
    val sourceUrl: String,
    val condition: String? = null
)
@Serializable data class GeneratedHuntPrepItem(
    val gameItemId: Int, val name: String, val nameSourceId: String
)
@Serializable enum class GeneratedThreatCoverageStatus { LISTED, NONE_LISTED_IN_MASTER_TABLE }
@Serializable enum class GeneratedHunterThreatType {
    ALL_RESISTANCE_DOWN_LARGE,
    DEFENSE_DOWN_LARGE,
    DEFENSE_DOWN_SMALL,
    FIREBLIGHT,
    ICEBLIGHT,
    MUD,
    PARALYSIS,
    POISON,
    SLEEP,
    SNOWMAN,
    STENCH,
    STUN,
    TERRAIN_ELEMENTAL_BLIGHT,
    THUNDERBLIGHT,
    TREMOR,
    WATERBLIGHT
}
@Serializable enum class GeneratedTacticalPriority { CORE, RECOMMENDED, CONDITIONAL }
@Serializable data class GeneratedHunterThreat(
    val id: String,
    val type: GeneratedHunterThreatType,
    val confidence: String,
    val sourceId: String,
    val sourceUrl: String,
    val context: String? = null
)
@Serializable data class GeneratedHuntPrepCounterItem(
    val itemGameId: Int, val counterFor: List<GeneratedHunterThreatType>
)
@Serializable data class GeneratedHuntPrepCounterNote(
    val threat: GeneratedHunterThreatType, val note: String
)
@Serializable data class GeneratedTacticalTool(
    val itemGameId: Int,
    val priority: GeneratedTacticalPriority,
    val reason: String,
    val sourceUrl: String,
    val sourceLocator: String
)
@Serializable data class GeneratedMonsterHuntPrep(
    val monsterId: String,
    val monsterName: String,
    val threatCoverageStatus: GeneratedThreatCoverageStatus,
    val threats: List<GeneratedHunterThreat>,
    val counterItems: List<GeneratedHuntPrepCounterItem>,
    val counterNotes: List<GeneratedHuntPrepCounterNote>,
    val tacticalTools: List<GeneratedTacticalTool>,
    val masterSourceUrl: String,
    val strategySourceUrl: String
)
@Serializable data class GeneratedMonster(
    val id: String, val name: String, val rawName: String, val monsterClass: String? = null,
    val threat: Int? = null, val hitzoneStates: List<String>, val breakablePartIds: List<String>, val questIds: List<String>,
    val trainingQuestIds: List<String> = emptyList()
)
@Serializable data class GeneratedHitzone(
    val id: String, val monsterId: String, val bodyPart: String, val state: String,
    val cut: Int, val impact: Int, val shot: Int, val fire: Int, val water: Int,
    val thunder: Int, val ice: Int, val dragon: Int
)
@Serializable enum class GeneratedRewardRank { GUILD_1_2, LOW, HIGH, VILLAGE_2_SPECIAL }
@Serializable data class GeneratedReward(
    val id: String, val monsterId: String, val itemId: String, val rank: GeneratedRewardRank,
    val method: String, val condition: String, val chance: Int, val quantity: Int? = null
)
@Serializable enum class GeneratedInvasionProbabilityBand {
    AT_LEAST_20,
    TEN_TO_UNDER_20,
    FIVE_TO_UNDER_10,
    TWO_TO_UNDER_5,
    UNDER_2
}
@Serializable data class GeneratedInvasionRewardDrop(
    val id: String,
    val monsterId: String,
    val context: String,
    val gameItemId: Int,
    val quantity: Int,
    val probabilityValuePercent: Int? = null,
    val probabilitySemantics: String = "QUALITATIVE_SOURCE_BAND",
    val probabilityBand: GeneratedInvasionProbabilityBand
)
@Serializable data class GeneratedPart(
    val id: String, val monsterId: String, val name: String, val kind: String
)
@Serializable enum class QuestTargetCountMode { EXACT, AT_LEAST }
@Serializable enum class QuestObjectiveType { HUNT, SLAY, CAPTURE, SLAY_OR_REPEL, OTHER }
@Serializable enum class QuestTargetEntityKind { LARGE_MONSTER, SMALL_MONSTER }
@Serializable data class GeneratedQuestObjectiveTarget(
    val entityKind: QuestTargetEntityKind,
    val entityId: String,
    val count: Int,
    val countMode: QuestTargetCountMode,
    val objectiveType: QuestObjectiveType
)
@Serializable data class GeneratedQuest(
    val id: String, val name: String, val category: String, val rank: String? = null,
    val stars: Int, val location: String? = null,
    val objectiveTargets: List<GeneratedQuestObjectiveTarget> = emptyList(),
    val appearingMonsterIds: List<String> = emptyList(),
    val objective: String? = null, val tmoName: String? = null,
    val canonicalNumber: Int? = null, val questKind: String? = null,
    val nameSource: String? = null, val locationSource: String? = null
)
@Serializable data class GeneratedTrainingObjectiveTarget(
    val entityKind: String,
    val entityId: String,
    val count: Int,
    val countMode: String,
    val objectiveType: String,
    val gameItemId: Int? = null
)
@Serializable data class GeneratedTrainingSuppliedItem(
    val gameItemId: Int,
    val quantity: Int? = null,
    val quantitySemantics: String = "NOT_STATED"
)
@Serializable data class GeneratedTrainingLoadout(
    val weaponType: String,
    val weaponStableId: String? = null,
    val armorRaw: List<String> = emptyList(),
    val charmRaw: String? = null,
    val skillsRaw: String? = null,
    val suppliedItems: List<GeneratedTrainingSuppliedItem> = emptyList()
)
@Serializable data class GeneratedTrainingGrade(
    val grade: String,
    val thresholdSeconds: Int? = null,
    val condition: String? = null
)
@Serializable data class GeneratedTrainingResultGrades(
    val provisional: Boolean = false,
    val rows: List<GeneratedTrainingGrade> = emptyList()
)
@Serializable data class GeneratedTrainingProbability(
    val valuePercent: Int? = null,
    val semantics: String = "SOURCE_UNSUPPORTED"
)
@Serializable data class GeneratedTrainingReward(
    val id: String,
    val gameItemId: Int,
    val quantity: Int? = null,
    val quantitySemantics: String = "NOT_STATED",
    val probability: GeneratedTrainingProbability = GeneratedTrainingProbability(),
    val rewardType: String = ""
)
@Serializable data class GeneratedTrainingQuest(
    val id: String,
    val trainingClass: String,
    val challengeSeries: String? = null,
    val canonicalDisplayName: String,
    val japaneseName: String,
    val tmoName: String? = null,
    val stars: Int? = null,
    val location: String = "",
    val objective: String = "",
    val objectiveTargets: List<GeneratedTrainingObjectiveTarget> = emptyList(),
    val appearingMonsterIds: List<String> = emptyList(),
    val participantLimit: Int? = null,
    val timeLimitSeconds: Int? = null,
    val suppliedItems: List<GeneratedTrainingSuppliedItem> = emptyList(),
    val loadouts: List<GeneratedTrainingLoadout> = emptyList(),
    val resultGrades: GeneratedTrainingResultGrades = GeneratedTrainingResultGrades(),
    val rewards: List<GeneratedTrainingReward> = emptyList()
)
@Serializable data class GeneratedItem(
    val id: String, val name: String, val rawName: String,
    val gameItemId: Int? = null, val tmoName: String? = null,
    /** Optional audited icon identity; absent means the UI reserves icon space without guessing. */
    val iconKey: String? = null,
    val aliases: List<String> = emptyList()
)
@Serializable data class GeneratedSourceCatalogEntry(
    val id: String, val name: String, val url: String,
    val usage: List<String> = emptyList(), val notes: String = ""
)
@Serializable data class GeneratedHitzoneStateSemantic(
    val id: String, val monsterId: String, val sourceState: String,
    val semanticState: String? = null, val confidence: String, val sourceId: String
)
@Serializable data class GeneratedStatusEffect(
    val id: String, val monsterId: String, val status: String,
    val initialThreshold: Int? = null,
    val thresholdIncrease: Int? = null,
    val maximumThreshold: Int? = null,
    val decayAmount: Int? = null,
    val decayIntervalSec: Int? = null,
    val durationSec: Double? = null,
    val damage: Int? = null,
    val staminaDamage: Int? = null,
    val immune: Boolean = false, val sourceId: String
)
@Serializable data class GeneratedItemEffect(
    val id: String, val monsterId: String, val item: String, val effective: Boolean,
    val durationsSec: List<Int> = emptyList(), val fatigueBonusSec: Int? = null,
    val sourceId: String
)
@Serializable data class GeneratedMonsterBehavior(
    val id: String, val monsterId: String,
    val rageDurationSec: Int? = null, val rageAttackMultiplier: Double? = null,
    val rageSpeedMultiplier: Double? = null, val fatigueDurationSec: Int? = null,
    val fatigueSpeedMultiplier: Double? = null, val fatigueImmune: Boolean = false,
    val acceptsMeatWhenFatigued: Boolean? = null, val sourceId: String
)
@Serializable data class GeneratedItemNameSource(
    val source: String, val patchVersion: String, val extractedItemStringCount: Int,
    val resolvedCurrentItems: Int, val renamedCurrentItems: Int,
    val unresolvedCurrentItems: Int, val canonicalCurrentItemEntities: Int,
    val sourceItemEntitiesRemovedAsErrors: Int, val sourceRewardReferencesCorrected: Int
)
@Serializable data class GeneratedItemMasterSource(
    val source: String,
    val masterGameItemIdRange: String,
    val identityPolicy: String,
    val recordsAdded: Int,
    val factualPayloadAdded: Boolean,
    val iconMaster: String
)
@Serializable data class GeneratedSourceCorrection(
    val id: String, val scope: String,
    val rewardIds: List<String> = emptyList(),
    val previousItemId: String? = null, val correctedItemId: String? = null,
    val reason: String, val confidence: String,
    val monsterId: String? = null, val sourceIds: List<String> = emptyList(),
    val addedRewardIds: List<String> = emptyList(),
    val removedRewardIds: List<String> = emptyList(),
    val addedPartId: String? = null, val removedPartId: String? = null,
    val addedBehaviorId: String? = null,
    val addedStatusEffectIds: List<String> = emptyList(),
    val statusId: String? = null, val stateId: String? = null,
    val monsterIds: List<String> = emptyList(),
    val previousRank: String? = null, val correctedRank: String? = null,
    val previous: JsonElement? = null, val corrected: JsonElement? = null
)
@Serializable data class GeneratedQuestNameSource(
    val id: String, val name: String, val language: String,
    val build: String, val sourceFile: String
)
@Serializable data class GeneratedQuestLocationSource(
    val id: String, val name: String, val url: String, val note: String
)

enum class EntityType { MATERIAL, MONSTER, SMALL_MONSTER, QUEST, TRAINING, MAP, WEAPON, SKILL }
@Serializable enum class ReferenceEntityType { MONSTER, SMALL_MONSTER, MATERIAL, WEAPON, MAP }
@Serializable data class FavoriteEntry(
    val type: ReferenceEntityType,
    val entityId: String,
    val addedAt: Long
)
@Serializable data class RecentEntry(
    val type: ReferenceEntityType,
    val entityId: String,
    val openedAt: Long
)
data class SearchResult(
    val id: String, val name: String, val subtitle: String, val type: EntityType,
    val aliases: List<String> = emptyList(), val gameItemId: Int? = null
)

/** One canonical user-facing label shared by search, chooser and detail UI. */
fun canonicalWeaponTypeLabel(weaponType: String): String = when (weaponType) {
    "GREAT_SWORD" -> "Great Sword"
    "LONG_SWORD" -> "Long Sword"
    "SWORD_AND_SHIELD" -> "Sword & Shield"
    "DUAL_BLADES" -> "Dual Blades"
    "HAMMER" -> "Hammer"
    "LANCE" -> "Lance"
    "HUNTING_HORN" -> "Hunting Horn"
    "GUNLANCE" -> "Gunlance"
    "SWITCH_AXE" -> "Switch Axe"
    "BOW" -> "Bow"
    "LIGHT_BOWGUN" -> "Light Bowgun"
    "HEAVY_BOWGUN" -> "Heavy Bowgun"
    else -> weaponType.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

/** User-facing Training presentation helpers. These resolve only against the
 * already-loaded runtime masters; unresolved values are deliberately omitted
 * instead of exposing generated entity IDs. */
internal fun trainingEntityDisplayName(
    target: TrainingObjectiveTarget,
    monsters: List<Monster>,
    smallMonsters: List<SmallMonster>,
    materials: List<Material>
): String? = when (target.entityKind.uppercase()) {
    "LARGE_MONSTER" -> monsters.firstOrNull { it.id == target.entityId }?.name
    "SMALL_MONSTER" -> smallMonsters.firstOrNull { it.id == target.entityId }?.name
    "ITEM" -> target.gameItemId?.let { gameItemId -> materials.firstOrNull { it.gameItemId == gameItemId }?.name }
    else -> null
}

internal fun trainingObjectiveDisplayText(
    targets: List<TrainingObjectiveTarget>,
    monsters: List<Monster>,
    smallMonsters: List<SmallMonster>,
    materials: List<Material>
): String? {
    if (targets.isEmpty()) return null
    val labels = targets.map { target ->
        val entityName = trainingEntityDisplayName(target, monsters, smallMonsters, materials) ?: return null
        val verb = when (target.objectiveType.uppercase()) {
            "SLAY" -> "Slay"
            "DELIVER" -> "Deliver"
            "CAPTURE" -> "Capture"
            else -> return null
        }
        val count = when (target.countMode.uppercase()) {
            "EXACT", "AT_LEAST", "MINIMUM" -> target.count.toString()
            "UP_TO", "MAXIMUM" -> "up to ${target.count}"
            else -> target.count.toString()
        }
        "$verb $count $entityName"
    }
    return labels.joinToString(" · ")
}

internal fun trainingAppearingMonsterDisplayNames(
    ids: List<String>,
    monsters: List<Monster>,
    smallMonsters: List<SmallMonster>
): List<String> = ids.mapNotNull { id ->
    monsters.firstOrNull { it.id == id }?.name
        ?: smallMonsters.firstOrNull { it.id == id }?.name
}.distinct()

internal fun trainingTimeLimitLabel(seconds: Int?): String? {
    if (seconds == null || seconds < 0) return null
    val minutes = seconds / 60
    val remainder = seconds % 60
    return if (remainder == 0) "$minutes min" else "$minutes:${remainder.toString().padStart(2, '0')}"
}

internal fun trainingGradeThresholdLabel(seconds: Int?): String? {
    if (seconds == null || seconds < 0) return null
    val minutes = seconds / 60
    val remainder = seconds % 60
    return "$minutes:${remainder.toString().padStart(2, '0')}"
}

internal fun trainingConditionLabel(condition: String?): String? = condition
    ?.takeIf { it.isNotBlank() }
    ?.let { raw ->
        when (raw.uppercase()) {
            "WITHIN_TIME_LIMIT" -> "Within time limit"
            else -> raw.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
    }

object CompanionLogic {
    fun search(data: FixtureData, query: String, filter: EntityType? = null): List<SearchResult> {
        val q = query.trim()
        if (q.isEmpty() && filter !in setOf(EntityType.MATERIAL, EntityType.SMALL_MONSTER, EntityType.QUEST, EntityType.TRAINING, EntityType.WEAPON, EntityType.SKILL)) return emptyList()
        val candidates = buildList {
            if (filter == null || filter == EntityType.MATERIAL) addAll(data.materials.map { SearchResult(it.id, it.name, it.description, EntityType.MATERIAL, it.aliases, it.gameItemId) })
            if (filter == null || filter == EntityType.MONSTER) addAll(data.monsters.map { SearchResult(it.id, it.name, it.subtitle, EntityType.MONSTER) })
            if (filter == null || filter == EntityType.SMALL_MONSTER) addAll(data.smallMonsters.map {
                SearchResult(it.id, it.name, "${it.monsterClass} · Small Monster", EntityType.SMALL_MONSTER)
            })
            if (filter == null || filter == EntityType.QUEST) addAll(data.quests.map {
                SearchResult(it.id, it.name, it.subtitle, EntityType.QUEST, listOf(it.objective, it.location).filter { value -> value.isNotBlank() })
            })
            if (filter == null || filter == EntityType.TRAINING) addAll(data.trainingQuests.map {
                SearchResult(
                    it.id,
                    it.canonicalDisplayName,
                    listOfNotNull(
                        it.trainingClass,
                        it.stars?.let { stars -> "★$stars" },
                        it.location.takeIf(String::isNotBlank)
                    ).joinToString(" · "),
                    EntityType.TRAINING,
                    listOf(it.japaneseName, it.objective, it.location).filter { value -> value.isNotBlank() }
                )
            })
            if (filter == null || filter == EntityType.WEAPON) addAll(data.weapons.map { weapon ->
                SearchResult(
                    id = weapon.stableWeaponId,
                    name = weapon.name,
                    subtitle = "${canonicalWeaponTypeLabel(weapon.weaponType)} · R${weapon.rarity} · ${weapon.attack}",
                    type = EntityType.WEAPON
                )
            })
            if (filter == null || filter == EntityType.SKILL) addAll(data.skillTrees.map { skill ->
                SearchResult(
                    id = skill.stableSkillTreeId,
                    name = skill.displayName,
                    subtitle = "${skill.tmoDisplayName} · Skill",
                    type = EntityType.SKILL,
                    aliases = listOf(skill.tmoDisplayName, skill.jpName) + skill.thresholds.map { it.activatedSkillName }
                )
            })
        }
        return candidates.filter { result ->
            q.isEmpty() || result.name.contains(q, ignoreCase = true) ||
                result.subtitle.contains(q, ignoreCase = true) ||
                result.aliases.any { it.contains(q, ignoreCase = true) }
        }
            .sortedWith(compareBy<SearchResult>(
                { !it.name.equals(q, ignoreCase = true) },
                { !it.name.startsWith(q, ignoreCase = true) },
                { it.name.lowercase() }
            ))
    }

}

object ReferenceHistoryLogic {
    fun toggleFavorite(
        entries: List<FavoriteEntry>,
        type: ReferenceEntityType,
        entityId: String,
        addedAt: Long
    ): List<FavoriteEntry> {
        val existing = entries.any { it.type == type && it.entityId == entityId }
        return if (existing) {
            entries.filterNot { it.type == type && it.entityId == entityId }
        } else {
            listOf(FavoriteEntry(type, entityId, addedAt)) + entries
        }
    }

    fun recordRecent(
        entries: List<RecentEntry>,
        type: ReferenceEntityType,
        entityId: String,
        openedAt: Long
    ): List<RecentEntry> =
        (listOf(RecentEntry(type, entityId, openedAt)) +
            entries.filterNot { it.type == type && it.entityId == entityId })
            .take(5)
}

object ReferenceEntryCodec {
    private val json = Json { ignoreUnknownKeys = true }
    fun encodeFavorites(entries: List<FavoriteEntry>): String = json.encodeToString(entries)
    fun decodeFavorites(value: String?): List<FavoriteEntry> = decode(value)
    fun encodeRecent(entries: List<RecentEntry>): String = json.encodeToString(entries)
    fun decodeRecent(value: String?): List<RecentEntry> = decode(value)

    private inline fun <reified T> decode(value: String?): List<T> =
        runCatching { json.decodeFromString<List<T>>(value.orEmpty()) }.getOrDefault(emptyList())
}
