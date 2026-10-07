package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.BowgunAmmoFamily

/**
 * Typed lookup for the 20 accepted MHP3 bowgun ammo cells.  The PNGs are
 * exact 16x16 crops from the pinned local palette assets; keeping the mapping
 * here makes UI independent of source filenames and gives every row one
 * stable icon identity.
 */
object AmmoIconRegistry {
    private val resources = mapOf(
        BowgunAmmoFamily.NORMAL_S to R.drawable.ammo_icon_normal_s,
        BowgunAmmoFamily.PIERCE_S to R.drawable.ammo_icon_pierce_s,
        BowgunAmmoFamily.PELLET_S to R.drawable.ammo_icon_pellet_s,
        BowgunAmmoFamily.CRAG_S to R.drawable.ammo_icon_crag_s,
        BowgunAmmoFamily.CLUST_S to R.drawable.ammo_icon_clust_s,
        BowgunAmmoFamily.RECOV_S to R.drawable.ammo_icon_recov_s,
        BowgunAmmoFamily.POISON_S to R.drawable.ammo_icon_poison_s,
        BowgunAmmoFamily.PARA_S to R.drawable.ammo_icon_para_s,
        BowgunAmmoFamily.SLEEP_S to R.drawable.ammo_icon_sleep_s,
        BowgunAmmoFamily.EXHAUST_S to R.drawable.ammo_icon_exhaust_s,
        BowgunAmmoFamily.FLAMING_S to R.drawable.ammo_icon_flaming_s,
        BowgunAmmoFamily.WATER_S to R.drawable.ammo_icon_water_s,
        BowgunAmmoFamily.THUNDER_S to R.drawable.ammo_icon_thunder_s,
        BowgunAmmoFamily.FREEZE_S to R.drawable.ammo_icon_freeze_s,
        BowgunAmmoFamily.DRAGON_S to R.drawable.ammo_icon_dragon_s,
        BowgunAmmoFamily.TRANQ_S to R.drawable.ammo_icon_tranq_s,
        BowgunAmmoFamily.PAINT_S to R.drawable.ammo_icon_paint_s,
        BowgunAmmoFamily.DEMON_S to R.drawable.ammo_icon_demon_s,
        BowgunAmmoFamily.ARMOR_S to R.drawable.ammo_icon_armor_s,
        BowgunAmmoFamily.SLICING_S to R.drawable.ammo_icon_slicing_s,
    )

    val mappingCount: Int get() = resources.size
    fun resolve(family: BowgunAmmoFamily): ItemIconRef? = resources[family]?.let {
        ItemIconRef("ammo:${family.name}", it)
    }
}
