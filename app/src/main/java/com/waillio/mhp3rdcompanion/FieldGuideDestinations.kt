package com.waillio.mhp3rdcompanion

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.waillio.mhp3rdcompanion.data.EntityType

/** The single ordered source of truth for Field Guide root pages and shortcut choices. */
internal enum class FieldGuideRootPage(
    val id: String,
    val entityType: EntityType? = null,
    @param:StringRes val labelResource: Int? = null
) {
    ALL("all", labelResource = R.string.filter_all),
    LARGE_MONSTERS("large_monsters", EntityType.MONSTER),
    SMALL_MONSTERS("small_monsters", EntityType.SMALL_MONSTER),
    ITEMS("items", EntityType.MATERIAL),
    MAPS("maps", EntityType.MAP),
    WEAPONS("weapons", EntityType.WEAPON),
    SKILLS("skills", EntityType.SKILL),
    QUESTS("quests", EntityType.QUEST),
    CHARM_TABLES("charm_tables", labelResource = R.string.category_charm_tables),
    TRAINING("training", EntityType.TRAINING)
}

internal val fieldGuideCategoryOrder: List<FieldGuideRootPage> = FieldGuideRootPage.entries
internal val fieldGuideShortcutChoices: List<FieldGuideRootPage> =
    fieldGuideCategoryOrder.filterNot { it == FieldGuideRootPage.ALL }

internal const val HOME_SHORTCUT_SLOT_COUNT = 4
internal const val HOME_SHORTCUTS_KEY = "home_shortcut_slots_v1"

/** Preserves the four destinations shown by the pre-customization Home cards. */
internal val DEFAULT_HOME_SHORTCUT_SLOTS: List<String?> = listOf(
    FieldGuideRootPage.LARGE_MONSTERS.id,
    FieldGuideRootPage.ITEMS.id,
    FieldGuideRootPage.MAPS.id,
    FieldGuideRootPage.CHARM_TABLES.id
)

internal fun normalizeHomeShortcutSlots(slots: List<String?>): List<String?> {
    val validIds = fieldGuideShortcutChoices.mapTo(hashSetOf()) { it.id }
    val used = hashSetOf<String>()
    return List(HOME_SHORTCUT_SLOT_COUNT) { index ->
        slots.getOrNull(index)?.takeIf { it in validIds && used.add(it) }
    }
}

internal fun decodeHomeShortcutSlots(encoded: String?): List<String?> {
    if (encoded == null) return DEFAULT_HOME_SHORTCUT_SLOTS
    return normalizeHomeShortcutSlots(encoded.split('|', limit = HOME_SHORTCUT_SLOT_COUNT))
}

internal fun readHomeShortcutSlots(preferences: SharedPreferences): List<String?> =
    if (preferences.contains(HOME_SHORTCUTS_KEY)) decodeHomeShortcutSlots(preferences.getString(HOME_SHORTCUTS_KEY, ""))
    else DEFAULT_HOME_SHORTCUT_SLOTS

internal fun persistHomeShortcutSlots(preferences: SharedPreferences, slots: List<String?>) {
    preferences.edit { putString(HOME_SHORTCUTS_KEY, encodeHomeShortcutSlots(slots)) }
}

internal fun encodeHomeShortcutSlots(slots: List<String?>): String =
    normalizeHomeShortcutSlots(slots).joinToString("|") { it.orEmpty() }

/** Duplicate selections are rejected; None is represented by a null slot. */
internal fun selectHomeShortcut(
    current: List<String?>,
    slotIndex: Int,
    destinationId: String?
): List<String?> {
    if (slotIndex !in 0 until HOME_SHORTCUT_SLOT_COUNT) return normalizeHomeShortcutSlots(current)
    val normalized = normalizeHomeShortcutSlots(current).toMutableList()
    if (destinationId != null && normalized.indices.any { it != slotIndex && normalized[it] == destinationId }) {
        return normalized
    }
    normalized[slotIndex] = destinationId
    return normalizeHomeShortcutSlots(normalized)
}

internal fun fieldGuidePageForCategory(category: EntityType?): FieldGuideRootPage =
    fieldGuideCategoryOrder.firstOrNull { it.entityType == category && it != FieldGuideRootPage.CHARM_TABLES }
        ?: FieldGuideRootPage.ALL
