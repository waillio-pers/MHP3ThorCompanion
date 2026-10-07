package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.Weapon
import com.waillio.mhp3rdcompanion.data.WeaponSpecialType

/** Tree is the existing default; the other two modes are flat, ordered lists. */
internal enum class WeaponBrowseMode { TREE, ATTACK, RARITY }

internal data class WeaponBrowseFilters(
    val specialType: WeaponSpecialType? = null,
    val minimumAffinity: Int? = null,
    val rarity: Int? = null,
    val minimumSlots: Int? = null
) {
    val isActive: Boolean
        get() = specialType != null || minimumAffinity != null || rarity != null || minimumSlots != null

    val activeCount: Int
        get() = listOf(specialType, minimumAffinity, rarity, minimumSlots).count { it != null }
}

/** Options are scoped to the selected class and sourced only from its records. */
internal fun availableWeaponSpecialTypes(catalog: WeaponCatalog): List<WeaponSpecialType> {
    val present = catalog.weapons.mapNotNull { it.special?.type }.toSet()
    return WeaponSpecialType.entries.filter { it in present }
}

internal fun availableWeaponAffinities(catalog: WeaponCatalog): List<Int> =
    catalog.weapons.map { it.affinity }.distinct().sorted()

internal fun availableWeaponRarities(catalog: WeaponCatalog): List<Int> =
    catalog.weapons.map { it.rarity }.distinct().sorted()

/** Minimum slot thresholds are integers 1..the observed maximum. */
internal fun availableMinimumWeaponSlots(catalog: WeaponCatalog): List<Int> =
    (1..(catalog.weapons.maxOfOrNull { it.slots } ?: 0)).toList()

internal fun Weapon.matches(filters: WeaponBrowseFilters): Boolean =
    (filters.specialType == null || special?.type == filters.specialType) &&
        (filters.minimumAffinity == null || affinity >= filters.minimumAffinity) &&
        (filters.rarity == null || rarity == filters.rarity) &&
        (filters.minimumSlots == null || slots >= filters.minimumSlots)

/** Search, filters, then the selected flat ordering. Ties keep canonical corpus order. */
internal fun browseWeapons(
    catalog: WeaponCatalog,
    query: String,
    filters: WeaponBrowseFilters,
    mode: WeaponBrowseMode,
    descending: Boolean = true
): List<Weapon> {
    val normalizedQuery = query.trim()
    val matches = catalog.weapons.filter { weapon ->
        (normalizedQuery.isBlank() || weapon.name.contains(normalizedQuery, ignoreCase = true)) &&
            weapon.matches(filters)
    }
    return when (mode) {
        WeaponBrowseMode.TREE -> if (normalizedQuery.isBlank()) {
            matches.sortedBy { it.sourceOrdinal }
        } else {
            // Preserve the established alphabetic search-result order.
            matches.sortedWith(compareBy<Weapon>({ it.name.lowercase() }, { it.id }))
        }
        WeaponBrowseMode.ATTACK -> if (descending) {
            matches.sortedWith(compareByDescending<Weapon> { it.attack }.thenBy { it.sourceOrdinal }.thenBy { it.id })
        } else {
            matches.sortedWith(compareBy<Weapon> { it.attack }.thenBy { it.sourceOrdinal }.thenBy { it.id })
        }
        WeaponBrowseMode.RARITY -> if (descending) {
            matches.sortedWith(compareByDescending<Weapon> { it.rarity }.thenBy { it.sourceOrdinal }.thenBy { it.id })
        } else {
            matches.sortedWith(compareBy<Weapon> { it.rarity }.thenBy { it.sourceOrdinal }.thenBy { it.id })
        }
    }
}

internal fun WeaponBrowseFilters.summary(): String = buildList {
    specialType?.let { add(it.displayLabel()) }
    minimumAffinity?.let { add("Affinity ≥ $it%") }
    rarity?.let { add("Rarity R$it") }
    minimumSlots?.let { add("Slots ≥ $it") }
}.joinToString(" · ")

internal fun WeaponSpecialType.displayLabel(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }
