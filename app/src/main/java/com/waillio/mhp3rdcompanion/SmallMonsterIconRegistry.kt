package com.waillio.mhp3rdcompanion

import androidx.annotation.DrawableRes

internal sealed interface SmallMonsterIcon {
    data class Resource(
        @param:DrawableRes val resourceId: Int,
        val sourceFileName: String
    ) : SmallMonsterIcon
    data object Placeholder : SmallMonsterIcon
}

internal object SmallMonsterIconRegistry {
    private val iconsBySmallMonsterId = mapOf(
        "small_monster_altaroth" to SmallMonsterIcon.Resource(R.drawable.small_monster_altaroth, "Altaroth.png"),
        "small_monster_anteka" to SmallMonsterIcon.Resource(R.drawable.small_monster_anteka, "Anteka.png"),
        "small_monster_aptonoth" to SmallMonsterIcon.Resource(R.drawable.small_monster_aptonoth, "Aptanoth.png"),
        "small_monster_baggi" to SmallMonsterIcon.Resource(R.drawable.small_monster_baggi, "Baggi.png"),
        "small_monster_bnahabra" to SmallMonsterIcon.Resource(R.drawable.small_monster_bnahabra, "Bnahabra.png"),
        "small_monster_bullfango" to SmallMonsterIcon.Resource(R.drawable.small_monster_bullfango, "Bullfango.png"),
        "small_monster_delex" to SmallMonsterIcon.Resource(R.drawable.small_monster_delex, "Delex.png"),
        "small_monster_felyne" to SmallMonsterIcon.Resource(R.drawable.small_monster_felyne, "Felyne.png"),
        "small_monster_gargwa" to SmallMonsterIcon.Resource(R.drawable.small_monster_gargwa, "Gagua.png"),
        "small_monster_giggi" to SmallMonsterIcon.Resource(R.drawable.small_monster_giggi, "Giggi.png"),
        "small_monster_jaggi" to SmallMonsterIcon.Resource(R.drawable.small_monster_jaggi, "Jaggi.png"),
        "small_monster_jaggia" to SmallMonsterIcon.Resource(R.drawable.small_monster_jaggia, "Jaggia.png"),
        "small_monster_kelbi" to SmallMonsterIcon.Resource(R.drawable.small_monster_kelbi, "Kelbi.png"),
        "small_monster_ludroth" to SmallMonsterIcon.Resource(R.drawable.small_monster_ludroth, "Ludroth.png"),
        "small_monster_melynx" to SmallMonsterIcon.Resource(R.drawable.small_monster_melynx, "Melynx.png"),
        "small_monster_popo" to SmallMonsterIcon.Resource(R.drawable.small_monster_popo, "Popo.png"),
        "small_monster_rhenoplos" to SmallMonsterIcon.Resource(R.drawable.small_monster_rhenoplos, "Rhenoplos.png"),
        "small_monster_slagtoth" to SmallMonsterIcon.Resource(R.drawable.small_monster_slagtoth, "Zuwaroposu.png"),
        "small_monster_uroktor" to SmallMonsterIcon.Resource(R.drawable.small_monster_uroktor, "Uroktor.png"),
        "small_monster_wroggi" to SmallMonsterIcon.Resource(R.drawable.small_monster_wroggi, "Froggi.png")
    )

    val mappedSmallMonsterIds: Set<String> = iconsBySmallMonsterId.keys

    fun resolve(smallMonsterId: String): SmallMonsterIcon =
        iconsBySmallMonsterId[smallMonsterId] ?: SmallMonsterIcon.Placeholder
}
