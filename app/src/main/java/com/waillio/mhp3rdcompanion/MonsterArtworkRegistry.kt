package com.waillio.mhp3rdcompanion

import androidx.annotation.DrawableRes

internal sealed interface MonsterArtwork {
    data class Resource(@param:DrawableRes val resourceId: Int) : MonsterArtwork
    data object Placeholder : MonsterArtwork
}

internal object MonsterArtworkRegistry {
    private val artworkByMonsterId = mapOf(
        "monster_agnaktor" to R.drawable.monster_agnaktor,
        "monster_akantor" to R.drawable.monster_akantor,
        "monster_alatreon" to R.drawable.monster_alatreon,
        "monster_amatsu" to R.drawable.monster_amatsu,
        "monster_arzuros" to R.drawable.monster_arzuros,
        "monster_baleful_gigginox" to R.drawable.monster_baleful_gigginox,
        "monster_barioth" to R.drawable.monster_barioth,
        "monster_barroth" to R.drawable.monster_barroth,
        "monster_black_diablos" to R.drawable.monster_black_diablos,
        "monster_brute_tigrex" to R.drawable.monster_brute_tigrex,
        "monster_bulldrome" to R.drawable.monster_bulldrome,
        "monster_crimson_qurupeco" to R.drawable.monster_crimson_qurupeco,
        "monster_deviljho" to R.drawable.monster_deviljho,
        "monster_diablos" to R.drawable.monster_diablos,
        "monster_duramboros" to R.drawable.monster_duramboros,
        "monster_gigginox" to R.drawable.monster_gigginox,
        "monster_glacial_agnaktor" to R.drawable.monster_glacial_agnaktor,
        "monster_gold_rathian" to R.drawable.monster_gold_rathian,
        "monster_great_baggi" to R.drawable.monster_great_baggi,
        "monster_great_jaggi" to R.drawable.monster_great_jaggi,
        "monster_great_wroggi" to R.drawable.monster_great_wroggi,
        "monster_green_nargacuga" to R.drawable.monster_green_nargacuga,
        "monster_jade_barroth" to R.drawable.monster_jade_barroth,
        "monster_jhen_mohran" to R.drawable.monster_jhen_mohran,
        "monster_lagombi" to R.drawable.monster_lagombi,
        "monster_nargacuga" to R.drawable.monster_nargacuga,
        "monster_nibelsnarf" to R.drawable.monster_nibelsnarf,
        "monster_purple_ludroth" to R.drawable.monster_purple_ludroth,
        "monster_qurupeco" to R.drawable.monster_qurupeco,
        "monster_rathalos" to R.drawable.monster_rathalos,
        "monster_rathian" to R.drawable.monster_rathian,
        "monster_royal_ludroth" to R.drawable.monster_royal_ludroth,
        "monster_sand_barioth" to R.drawable.monster_sand_barioth,
        "monster_silver_rathalos" to R.drawable.monster_silver_rathalos,
        "monster_steel_uragaan" to R.drawable.monster_steel_uragaan,
        "monster_tigrex" to R.drawable.monster_tigrex,
        "monster_ukanlos" to R.drawable.monster_ukanlos,
        "monster_uragaan" to R.drawable.monster_uragaan,
        "monster_volvidon" to R.drawable.monster_volvidon,
        "monster_zinogre" to R.drawable.monster_zinogre
    )

    val mappedMonsterIds: Set<String> = artworkByMonsterId.keys.toSet()

    fun resolve(monsterId: String): MonsterArtwork =
        artworkByMonsterId[monsterId]?.let(MonsterArtwork::Resource)
            ?: MonsterArtwork.Placeholder
}
