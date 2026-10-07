package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.Weapon
import com.waillio.mhp3rdcompanion.data.WeaponRecipe

/** How a weapon node is obtained in the displayed ancestry. */
internal enum class WeaponAncestryMethod {
    BASE,
    FORGE,
    UPGRADE
}

/** A derived view node; the recipe belongs to this weapon, not its parent. */
internal data class WeaponAncestryNode(
    val weapon: Weapon,
    val method: WeaponAncestryMethod,
    val recipe: WeaponRecipe?,
    val directForgeRecipe: WeaponRecipe?
)

/** Safe result for malformed data. The UI can still render the partial chain. */
internal data class WeaponAncestryResult(
    val nodes: List<WeaponAncestryNode>,
    val cycleDetected: Boolean = false,
    val missingParentId: String? = null
)

/**
 * Build the path to [weaponId] in O(path length), using the catalog's stable-ID index.
 * No names, ordinals, list positions, or guessed forward links participate in traversal.
 */
internal fun WeaponCatalog.ancestryFor(weaponId: String): WeaponAncestryResult =
    buildWeaponAncestry(weaponById[weaponId], weaponById)

/** Kept separate from WeaponCatalog so malformed synthetic graphs can be tested safely. */
internal fun buildWeaponAncestry(
    current: Weapon?,
    byId: Map<String, Weapon>
): WeaponAncestryResult {
    if (current == null) return WeaponAncestryResult(emptyList())
    val collected = mutableListOf<Weapon>()
    val visited = mutableSetOf<String>()
    var cursor: Weapon? = current
    var cycleDetected = false
    var missingParentId: String? = null

    while (cursor != null) {
        if (!visited.add(cursor.id)) {
            cycleDetected = true
            break
        }
        collected += cursor
        val parentId = cursor.upgradeFrom?.fromWeaponId ?: break
        val parent = byId[parentId]
        if (parent == null) {
            missingParentId = parentId
            break
        }
        cursor = parent
    }

    collected.reverse()
    val nodes = collected.map { weapon ->
        val upgrade = weapon.upgradeFrom
        val method = when {
            upgrade != null -> WeaponAncestryMethod.UPGRADE
            weapon.orphanUpgradeRecipe != null -> WeaponAncestryMethod.UPGRADE
            weapon.forgeRecipe != null -> WeaponAncestryMethod.FORGE
            else -> WeaponAncestryMethod.BASE
        }
        WeaponAncestryNode(
            weapon = weapon,
            method = method,
            recipe = upgrade?.recipe ?: weapon.forgeRecipe ?: weapon.orphanUpgradeRecipe,
            directForgeRecipe = upgrade?.let { weapon.forgeRecipe }
        )
    }
    return WeaponAncestryResult(nodes, cycleDetected, missingParentId)
}
