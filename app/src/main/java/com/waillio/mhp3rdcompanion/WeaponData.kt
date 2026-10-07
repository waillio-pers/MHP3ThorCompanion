package com.waillio.mhp3rdcompanion

import androidx.compose.ui.graphics.Color
import com.waillio.mhp3rdcompanion.data.Material
import com.waillio.mhp3rdcompanion.data.GunlanceMechanics
import com.waillio.mhp3rdcompanion.data.HornNote
import com.waillio.mhp3rdcompanion.data.HuntingHornMechanics
import com.waillio.mhp3rdcompanion.data.HuntingHornSong
import com.waillio.mhp3rdcompanion.data.HuntingHornSongSet
import com.waillio.mhp3rdcompanion.data.BowMechanics
import com.waillio.mhp3rdcompanion.data.LightBowgunMechanics
import com.waillio.mhp3rdcompanion.data.HeavyBowgunMechanics
import com.waillio.mhp3rdcompanion.data.SwitchAxeMechanics
import com.waillio.mhp3rdcompanion.data.Weapon
import com.waillio.mhp3rdcompanion.data.WeaponRecipe
import com.waillio.mhp3rdcompanion.data.WeaponRecipeKind
import com.waillio.mhp3rdcompanion.data.WeaponRecipeUsage

data class WeaponValidation(
    val weaponType: String = "GREAT_SWORD",
    val weaponCount: Int,
    val edgeCount: Int,
    val recipeMaterialCount: Int,
    val unresolvedMaterialCount: Int,
    val maxDepth: Int,
    val rootIds: Set<String>,
    val leafCount: Int = 0
)

/** A type-scoped view over the shared schema-11 weapon graph. */
class WeaponCatalog internal constructor(
    val weaponType: String,
    val weapons: List<Weapon>,
    val weaponById: Map<String, Weapon>,
    val childrenByWeaponId: Map<String, List<Weapon>>,
    val parentEdgeByWeaponId: Map<String, WeaponUpgradeEdge>,
    val orphanUpgradeRecipeByWeaponId: Map<String, WeaponRecipe>,
    val depthByWeaponId: Map<String, Int>,
    val itemByGameItemId: Map<Int, Material>,
    val validation: WeaponValidation,
    private val huntingHornSongsByKey: Map<String, HuntingHornSongSet>
) {
    fun childrenOf(weaponId: String): List<Weapon> = childrenByWeaponId[weaponId].orEmpty()
    fun parentOf(weaponId: String): Weapon? = parentEdgeByWeaponId[weaponId]?.fromWeaponId?.let(weaponById::get)
    fun recipeItem(gameItemId: Int): Material? = itemByGameItemId[gameItemId]
    fun orphanUpgradeRecipeOf(weaponId: String): WeaponRecipe? = orphanUpgradeRecipeByWeaponId[weaponId]
    fun songsFor(weapon: Weapon): List<HuntingHornSong> = (weapon.mechanics as? HuntingHornMechanics)
        ?.let { huntingHornSongsByKey[it.noteSetKey]?.songs }.orEmpty()
}

/** Runtime indexes for the shared schema-11 production weapon domain. */
class WeaponRepository private constructor(
    val weapons: List<Weapon>,
    val weaponById: Map<String, Weapon>,
    val childrenByWeaponId: Map<String, List<Weapon>>,
    val parentEdgeByWeaponId: Map<String, WeaponUpgradeEdge>,
    val orphanUpgradeRecipeByWeaponId: Map<String, WeaponRecipe>,
    val depthByWeaponId: Map<String, Int>,
    val itemByGameItemId: Map<Int, Material>,
    /** Great Sword validation is retained for source compatibility. */
    val validation: WeaponValidation,
    val validationByType: Map<String, WeaponValidation>,
    val catalogs: Map<String, WeaponCatalog>,
    val huntingHornSongCatalog: List<HuntingHornSongSet>,
    val recipeUsageIndex: WeaponRecipeUsageIndex
) {
    val repositoryId: String = "production-schema-11-weapons"

    fun childrenOf(weaponId: String): List<Weapon> = childrenByWeaponId[weaponId].orEmpty()
    fun parentOf(weaponId: String): Weapon? = parentEdgeByWeaponId[weaponId]?.fromWeaponId?.let(weaponById::get)
    fun recipeItem(gameItemId: Int): Material? = itemByGameItemId[gameItemId]
    fun orphanUpgradeRecipeOf(weaponId: String): WeaponRecipe? = orphanUpgradeRecipeByWeaponId[weaponId]
    fun catalog(weaponType: String): WeaponCatalog = catalogs[weaponType]
        ?: error("Unsupported production weapon type: $weaponType")

    companion object {
        /** Production order is also the chooser order. */
        val PRODUCTION_WEAPON_TYPES: List<String> = listOf(
            "GREAT_SWORD", "LONG_SWORD", "SWORD_AND_SHIELD", "DUAL_BLADES", "HAMMER", "LANCE",
            "HUNTING_HORN", "GUNLANCE", "SWITCH_AXE", "BOW", "LIGHT_BOWGUN", "HEAVY_BOWGUN"
        )
        private val SUPPORTED_TYPES = PRODUCTION_WEAPON_TYPES.toSet()

        fun fromProduction(
            weapons: List<Weapon>,
            materials: List<Material>,
            huntingHornSongCatalog: List<HuntingHornSongSet> = emptyList()
        ): WeaponRepository {
            require(weapons.isNotEmpty()) { "Production weapon domain is empty" }
            require(weapons.all { it.weaponType in SUPPORTED_TYPES }) { "Unsupported production weapon type" }
            require(weapons.map { it.id }.distinct().size == weapons.size) { "Duplicate stableWeaponId" }
            val legacyMeleeTypes = setOf("GREAT_SWORD", "LONG_SWORD", "SWORD_AND_SHIELD", "DUAL_BLADES", "HAMMER", "LANCE")
            require(weapons.filter { it.weaponType in legacyMeleeTypes }.all { it.mechanics == null && it.sharpness != null }) {
                "Legacy weapon mechanics must be null"
            }
            require(weapons.filter { it.weaponType == "BOW" }.all { it.mechanics is BowMechanics && it.sharpness == null }) {
                "Bow rows must use typed mechanics and have no sharpness"
            }
            weapons.forEach { weapon ->
                when (val mechanics = weapon.mechanics) {
                    null -> require(weapon.weaponType !in setOf("HUNTING_HORN", "GUNLANCE", "SWITCH_AXE", "BOW", "LIGHT_BOWGUN", "HEAVY_BOWGUN")) { "Special weapon is missing typed mechanics" }
                    is HuntingHornMechanics -> require(weapon.weaponType == "HUNTING_HORN" && mechanics.notes.size == 3) { "Mechanics discriminator mismatch" }
                    is GunlanceMechanics -> require(weapon.weaponType == "GUNLANCE" && mechanics.shellingLevel in 1..4) { "Mechanics discriminator mismatch" }
                    is SwitchAxeMechanics -> require(weapon.weaponType == "SWITCH_AXE") { "Mechanics discriminator mismatch" }
                    is BowMechanics -> require(weapon.weaponType == "BOW" && mechanics.charges.size in 3..4 && mechanics.charges.all { it.shotLevel in 1..5 }) { "Mechanics discriminator mismatch" }
                    is LightBowgunMechanics -> require(weapon.weaponType == "LIGHT_BOWGUN" && weapon.sharpness == null) { "Mechanics discriminator mismatch" }
                    is HeavyBowgunMechanics -> require(weapon.weaponType == "HEAVY_BOWGUN" && weapon.sharpness == null) { "Mechanics discriminator mismatch" }
                }
            }
            val songsByKey = huntingHornSongCatalog.associateBy { it.noteSetKey }
            require(songsByKey.size == huntingHornSongCatalog.size) { "Duplicate Hunting Horn song set" }
            if (huntingHornSongCatalog.isNotEmpty()) {
                weapons.filter { it.weaponType == "HUNTING_HORN" }.forEach { weapon ->
                    val mechanics = weapon.mechanics as HuntingHornMechanics
                    require(songsByKey.containsKey(mechanics.noteSetKey)) { "Missing Hunting Horn song set ${mechanics.noteSetKey}" }
                }
            }
            val byId = weapons.associateBy { it.id }
            val itemById = materials.mapNotNull { item -> item.gameItemId?.let { it to item } }.toMap()
            val children = mutableMapOf<String, MutableList<Weapon>>()
            val parents = mutableMapOf<String, WeaponUpgradeEdge>()
            val orphanRecipes = weapons.mapNotNull { weapon -> weapon.orphanUpgradeRecipe?.let { weapon.id to it } }.toMap()

            weapons.forEach { weapon ->
                weapon.upgradesTo.forEach { childId ->
                    require(childId in byId && childId != weapon.id) { "Invalid upgrade endpoint ${weapon.id} -> $childId" }
                    val child = byId.getValue(childId)
                    require(child.weaponType == weapon.weaponType) { "Cross-type upgrade edge: ${weapon.id} -> $childId" }
                    require(parents[childId] == null) { "Weapon has multiple parents: $childId" }
                    val upgrade = child.upgradeFrom
                    require(upgrade != null && upgrade.fromWeaponId == weapon.id) { "Upgrade edge missing recipe for $childId" }
                    parents[childId] = WeaponUpgradeEdge(upgrade.edgeId, weapon.id, childId, upgrade.recipe)
                    children.getOrPut(weapon.id) { mutableListOf() } += child
                }
            }

            val depth = mutableMapOf<String, Int>()
            val visiting = mutableSetOf<String>()
            fun visit(id: String): Int {
                depth[id]?.let { return it }
                require(visiting.add(id)) { "Weapon graph contains a cycle at $id" }
                val value = parents[id]?.let { visit(it.fromWeaponId) + 1 } ?: 0
                visiting.remove(id)
                depth[id] = value
                return value
            }
            byId.keys.forEach(::visit)

            val validations = weapons.map { it.weaponType }.distinct().associateWith { type ->
                val typeRows = weapons.filter { it.weaponType == type }
                val typeIds = typeRows.map { it.id }.toSet()
                val typeEdges = children.filterKeys { it in typeIds }.values.sumOf { it.size }
                val typeRecipeItems = linkedSetOf<Int>()
                typeRows.forEach { weapon ->
                    weapon.forgeRecipe?.ingredients?.forEach { typeRecipeItems += it.gameItemId }
                    weapon.upgradeFrom?.recipe?.ingredients?.forEach { typeRecipeItems += it.gameItemId }
                    weapon.orphanUpgradeRecipe?.ingredients?.forEach { typeRecipeItems += it.gameItemId }
                }
                WeaponValidation(
                    weaponType = type,
                    weaponCount = typeIds.size,
                    edgeCount = typeEdges,
                    recipeMaterialCount = typeRecipeItems.size,
                    unresolvedMaterialCount = typeRecipeItems.count { it !in itemById },
                    maxDepth = typeIds.maxOfOrNull { depth[it] ?: 0 } ?: 0,
                    rootIds = typeIds.filter { it !in parents }.toSet(),
                    leafCount = typeIds.count { children[it].isNullOrEmpty() }
                )
            }
            require(validations["GREAT_SWORD"]?.let {
                it.weaponCount == 91 && it.edgeCount == 75 && it.rootIds.size == 16 &&
                    it.maxDepth == 9 && it.recipeMaterialCount == 201 && it.unresolvedMaterialCount == 0
            } == true) { "Unexpected production Great Sword graph or recipe closure" }
            validations["LONG_SWORD"]?.let {
                require(it.weaponCount == 85 && it.edgeCount == 71 && it.rootIds.size == 14 && it.maxDepth == 8) {
                    "Unexpected production Long Sword graph"
                }
                require(it.recipeMaterialCount == 204 && it.unresolvedMaterialCount == 0) { "Unexpected Long Sword recipe closure" }
            }
            validations["SWORD_AND_SHIELD"]?.let {
                require(it.weaponCount == 94 && it.edgeCount == 80 && it.rootIds.size == 14 && it.leafCount == 33 && it.maxDepth == 8 && it.recipeMaterialCount == 204 && it.unresolvedMaterialCount == 0) {
                    "Unexpected Sword & Shield graph or recipe closure"
                }
            }
            validations["DUAL_BLADES"]?.let {
                require(it.weaponCount == 82 && it.edgeCount == 72 && it.rootIds.size == 10 && it.leafCount == 30 && it.maxDepth == 8 && it.recipeMaterialCount == 201 && it.unresolvedMaterialCount == 0) {
                    "Unexpected Dual Blades graph or recipe closure"
                }
            }
            validations["HAMMER"]?.let {
                require(it.weaponCount == 93 && it.edgeCount == 78 && it.rootIds.size == 15 && it.leafCount == 31 && it.maxDepth == 8 && it.recipeMaterialCount == 192 && it.unresolvedMaterialCount == 0) {
                    "Unexpected Hammer graph or recipe closure"
                }
            }
            validations["LANCE"]?.let {
                require(it.weaponCount == 96 && it.edgeCount == 82 && it.rootIds.size == 14 && it.leafCount == 34 && it.maxDepth == 9 && it.recipeMaterialCount == 219 && it.unresolvedMaterialCount == 0) {
                    "Unexpected Lance graph or recipe closure"
                }
            }
            validations["HUNTING_HORN"]?.let {
                require(it.weaponCount == 85 && it.edgeCount == 74 && it.rootIds.size == 11 && it.leafCount == 31 && it.maxDepth == 8 && it.recipeMaterialCount == 208 && it.unresolvedMaterialCount == 0) { "Unexpected Hunting Horn graph or recipe closure" }
            }
            validations["GUNLANCE"]?.let {
                require(it.weaponCount == 77 && it.edgeCount == 64 && it.rootIds.size == 13 && it.leafCount == 27 && it.maxDepth == 8 && it.recipeMaterialCount == 180 && it.unresolvedMaterialCount == 0) { "Unexpected Gunlance graph or recipe closure" }
            }
            validations["SWITCH_AXE"]?.let {
                require(it.weaponCount == 71 && it.edgeCount == 61 && it.rootIds.size == 10 && it.leafCount == 25 && it.maxDepth == 8 && it.recipeMaterialCount == 187 && it.unresolvedMaterialCount == 0) { "Unexpected Switch Axe graph or recipe closure" }
            }
            validations["BOW"]?.let {
                require(it.weaponCount == 80 && it.edgeCount == 63 && it.rootIds.size == 17 && it.leafCount == 25 && it.maxDepth == 7 && it.recipeMaterialCount == 209 && it.unresolvedMaterialCount == 0) { "Unexpected Bow graph or recipe closure" }
            }
            validations["LIGHT_BOWGUN"]?.let {
                require(it.weaponCount == 67 && it.edgeCount == 52 && it.rootIds.size == 15 && it.leafCount == 22 && it.maxDepth == 7 && it.recipeMaterialCount == 192 && it.unresolvedMaterialCount == 0) { "Unexpected Light Bowgun graph or recipe closure" }
            }
            validations["HEAVY_BOWGUN"]?.let {
                require(it.weaponCount == 66 && it.edgeCount == 48 && it.rootIds.size == 18 && it.leafCount == 23 && it.maxDepth == 7 && it.recipeMaterialCount == 180 && it.unresolvedMaterialCount == 0) { "Unexpected Heavy Bowgun graph or recipe closure" }
            }

            val catalogMap = validations.mapValues { (type, typeValidation) ->
                val ids = weapons.filter { it.weaponType == type }.map { it.id }.toSet()
                WeaponCatalog(
                    weaponType = type,
                    weapons = weapons.filter { it.weaponType == type }.sortedBy { it.sourceOrdinal },
                    weaponById = byId.filterKeys { it in ids },
                    childrenByWeaponId = children.filterKeys { it in ids }.mapValues { (_, value) -> value.toList() },
                    parentEdgeByWeaponId = parents.filterKeys { it in ids },
                    orphanUpgradeRecipeByWeaponId = orphanRecipes.filterKeys { it in ids },
                    depthByWeaponId = depth.filterKeys { it in ids },
                    itemByGameItemId = itemById,
                    validation = typeValidation,
                    huntingHornSongsByKey = songsByKey
                )
            }
            return WeaponRepository(
                weapons = weapons.toList(),
                weaponById = byId,
                childrenByWeaponId = children.mapValues { (_, value) -> value.toList() },
                parentEdgeByWeaponId = parents,
                orphanUpgradeRecipeByWeaponId = orphanRecipes,
                depthByWeaponId = depth,
                itemByGameItemId = itemById,
                validation = requireNotNull(validations["GREAT_SWORD"]),
                validationByType = validations,
                catalogs = catalogMap,
                huntingHornSongCatalog = huntingHornSongCatalog,
                recipeUsageIndex = buildWeaponRecipeUsageIndex(weapons)
            )
        }
    }
}

data class WeaponUpgradeEdge(
    val id: String,
    val fromWeaponId: String,
    val toWeaponId: String,
    val recipe: WeaponRecipe
)

/** Deterministic reverse projection of numeric weapon recipes for Item Detail. */
data class WeaponRecipeUsageIndex(
    val byGameItemId: Map<Int, List<WeaponRecipeUsage>>,
    val all: List<WeaponRecipeUsage>
) {
    fun forItem(gameItemId: Int): List<WeaponRecipeUsage> = byGameItemId[gameItemId].orEmpty()
    fun uniqueWeaponCount(gameItemId: Int): Int = forItem(gameItemId).map { it.stableWeaponId }.distinct().size
}

internal fun buildWeaponRecipeUsageIndex(weapons: List<Weapon>): WeaponRecipeUsageIndex {
    val typeOrder = WeaponRepository.PRODUCTION_WEAPON_TYPES.withIndex().associate { it.value to it.index }
    val sourceOrdinalById = weapons.associate { it.stableWeaponId to it.sourceOrdinal }
    val rows = buildList {
        weapons.forEach { weapon ->
            weapon.forgeRecipe?.ingredients.orEmpty().forEach { ingredient ->
                add(WeaponRecipeUsage(ingredient.gameItemId, weapon.stableWeaponId, weapon.weaponType, WeaponRecipeKind.FORGE, ingredient.quantity))
            }
            weapon.upgradeFrom?.recipe?.ingredients.orEmpty().forEach { ingredient ->
                add(WeaponRecipeUsage(ingredient.gameItemId, weapon.stableWeaponId, weapon.weaponType, WeaponRecipeKind.UPGRADE, ingredient.quantity))
            }
            weapon.orphanUpgradeRecipe?.ingredients.orEmpty().forEach { ingredient ->
                add(WeaponRecipeUsage(ingredient.gameItemId, weapon.stableWeaponId, weapon.weaponType, WeaponRecipeKind.UPGRADE, ingredient.quantity))
            }
        }
    }.sortedWith(compareBy<WeaponRecipeUsage>({ typeOrder[it.weaponType] ?: Int.MAX_VALUE }, { sourceOrdinalById[it.stableWeaponId] ?: Int.MAX_VALUE }, { it.stableWeaponId }, { it.recipeKind.name }, { it.gameItemId }))
    return WeaponRecipeUsageIndex(rows.groupBy { it.gameItemId }, rows)
}

internal object SharpnessColors {
    val Red = Color(0xFFCC4A3A)
    val Orange = Color(0xFFE48942)
    val Yellow = Color(0xFFE2C849)
    val Green = Color(0xFF6BA866)
    val Blue = Color(0xFF5387C9)
    val White = Color(0xFFF4EEDC)
}
