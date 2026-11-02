package com.waillio.mhp3rdcompanion

/** MHP3@Wiki Palico skills; the source was last updated 2012-01-03.
 * Ambiguous blank cells are kept unknown until checked in the game. */
internal enum class PalicoSkillCategory { OFFENSE, DEFENSE, SUPPORT, RECOVERY, GATHERING, SPECIAL }

internal sealed interface PalicoUnlock {
    data object InitiallyAvailable : PalicoUnlock
    data class Level(val minimum: Int) : PalicoUnlock
    data class Affection(val minimum: Int) : PalicoUnlock
    data class HuntAny(val monsters: List<String>) : PalicoUnlock
    data class All(val requirements: List<PalicoUnlock>) : PalicoUnlock
    data object Unknown : PalicoUnlock
}

internal data class PalicoSkill(
    val id: String, val japaneseName: String, val name: String, val effect: String,
    val points: Int, val memory: Int, val category: PalicoSkillCategory, val unlock: PalicoUnlock
)

private fun level(value: Int): PalicoUnlock = PalicoUnlock.Level(value)
private fun affection(value: Int): PalicoUnlock = PalicoUnlock.Affection(value)
private fun hunt(vararg names: String): PalicoUnlock = PalicoUnlock.HuntAny(names.toList())
private val predators = hunt("Nargacuga", "Barioth", "Duramboros")
private val powerful = hunt("Zinogre", "Tigrex", "Diablos")
private val explosive = hunt("Uragaan", "Rathalos", "Agnaktor")
private val unknown = PalicoUnlock.Unknown

internal val palicoSkills: List<PalicoSkill> = listOf(
    PalicoSkill("demon_flute", "鬼人笛の術", "Demon Flute", "Temporarily increases hunter and Palico attack.", 60, 2, PalicoSkillCategory.SUPPORT, level(2)),
    PalicoSkill("armor_flute", "硬化笛の術", "Armor Flute", "Temporarily increases hunter and Palico defense.", 60, 2, PalicoSkillCategory.SUPPORT, level(2)),
    PalicoSkill("antidote_flute", "解毒・消臭笛の術", "Antidote and Deodorant Flute", "Removes poison and odor.", 30, 1, PalicoSkillCategory.RECOVERY, level(2)),
    PalicoSkill("health_flute", "回復笛の術", "Health Flute", "Restores a little health to the hunter and Palico.", 70, 2, PalicoSkillCategory.RECOVERY, level(5)),
    PalicoSkill("true_health_flute", "真・回復笛の術", "True Health Flute", "Restores more health to the hunter and Palico.", 120, 3, PalicoSkillCategory.RECOVERY, affection(4)),
    PalicoSkill("element_attack", "属性攻撃強化の術", "Element Attack Up", "Strengthens elemental attacks.", 50, 2, PalicoSkillCategory.OFFENSE, level(10)),
    PalicoSkill("poison_attack", "状態異常攻撃〖毒〗", "Status Attack: Poison", "Adds poison to melee and boomerang attacks.", 80, 3, PalicoSkillCategory.OFFENSE, hunt("Great Wroggi", "Gigginox", "Purple Ludroth")),
    PalicoSkill("paralysis_attack", "状態異常攻撃〖麻痺〗", "Status Attack: Paralysis", "Adds paralysis to melee and boomerang attacks.", 80, 4, PalicoSkillCategory.OFFENSE, hunt("Volvidon", "Baleful Gigginox")),
    PalicoSkill("sleep_attack", "状態異常攻撃〖睡眠〗", "Status Attack: Sleep", "Adds sleep to melee and boomerang attacks.", 80, 3, PalicoSkillCategory.OFFENSE, hunt("Great Baggi", "Uragaan")),
    PalicoSkill("status_attack", "状態異常攻撃強化の術", "Status Attack Up", "Strengthens status attacks.", 120, 2, PalicoSkillCategory.OFFENSE, level(10)),
    PalicoSkill("mud_snow", "泥・雪だるま早割りの術", "Mud and Snow Escape", "Escapes mud and snowman status faster.", 30, 1, PalicoSkillCategory.DEFENSE, level(5)),
    PalicoSkill("poison_immunity", "毒無効の術", "Poison Immunity", "Prevents poison.", 30, 1, PalicoSkillCategory.DEFENSE, level(2)),
    PalicoSkill("wind_immunity", "風圧無効の術", "Wind Pressure Immunity", "Prevents flinching from most wind pressure.", 100, 2, PalicoSkillCategory.DEFENSE, level(10)),
    PalicoSkill("earplugs", "防音の術", "Earplugs", "Prevents flinching from monster roars.", 120, 2, PalicoSkillCategory.DEFENSE, level(10)),
    PalicoSkill("earplugs_wind", "防音・風圧無効の術", "Earplugs and Wind Immunity", "Protects against roars and most wind pressure.", 140, 3, PalicoSkillCategory.DEFENSE, level(15)),
    PalicoSkill("attack_small", "攻撃力強化術〖小〗", "Attack Up Small", "Raises attack slightly.", 30, 1, PalicoSkillCategory.OFFENSE, level(2)),
    PalicoSkill("attack_large", "攻撃力強化術〖大〗", "Attack Up Large", "Raises attack significantly.", 90, 2, PalicoSkillCategory.OFFENSE, powerful),
    PalicoSkill("defense_small", "防御力強化術〖小〗", "Defense Up Small", "Raises defense slightly.", 30, 1, PalicoSkillCategory.DEFENSE, level(2)),
    PalicoSkill("defense_large", "防御力強化術〖大〗", "Defense Up Large", "Raises defense significantly.", 90, 2, PalicoSkillCategory.DEFENSE, powerful),
    PalicoSkill("health_small", "体力強化術〖小〗", "Health Up Small", "Raises maximum health slightly.", 30, 1, PalicoSkillCategory.DEFENSE, level(5)),
    PalicoSkill("health_large", "体力強化術〖大〗", "Health Up Large", "Raises maximum health significantly.", 90, 2, PalicoSkillCategory.DEFENSE, powerful),
    PalicoSkill("guard_small", "ガード確率強化〖小〗", "Guard Chance Up Small", "Slightly increases guard chance.", 30, 1, PalicoSkillCategory.DEFENSE, level(5)),
    PalicoSkill("guard_large", "ガード確率強化〖大〗", "Guard Chance Up Large", "Greatly increases guard chance.", 90, 2, PalicoSkillCategory.DEFENSE, level(10)),
    PalicoSkill("critical", "会心攻撃強化の術", "Critical Attack Up", "Increases affinity by 10%.", 50, 1, PalicoSkillCategory.OFFENSE, level(5)),
    PalicoSkill("plunder", "ぶんどり強化の術", "Plunder Up", "Increases the chance of stealing items.", 100, 2, PalicoSkillCategory.GATHERING, affection(4)),
    PalicoSkill("shock_trap", "シビレ罠の術", "Shock Trap", "Occasionally places a shock trap.", 160, 3, PalicoSkillCategory.SUPPORT, unknown),
    PalicoSkill("felvine_bomb", "マタタビ爆弾の術", "Felvine Bomb", "Occasionally throws a felvine bomb.", 30, 1, PalicoSkillCategory.SUPPORT, level(2)),
    PalicoSkill("large_barrel_bomb", "大タル爆弾の術", "Large Barrel Bomb", "Occasionally charges with a large barrel bomb.", 140, 2, PalicoSkillCategory.OFFENSE, explosive),
    PalicoSkill("bomb_boost", "爆弾強化の術", "Bomb Boost", "Increases bomb damage.", 150, 3, PalicoSkillCategory.OFFENSE, unknown),
    PalicoSkill("bomb_resistance", "爆弾ダメージ軽減の術", "Bomb Damage Reduction", "Reduces damage from bombs.", 90, 1, PalicoSkillCategory.DEFENSE, level(5)),
    PalicoSkill("big_boomerang", "ビッグブーメランの術", "Big Boomerang", "Enlarges boomerangs and increases their power.", 100, 2, PalicoSkillCategory.OFFENSE, predators),
    PalicoSkill("piercing_boomerang", "貫通ブーメランの術", "Piercing Boomerang", "Boomerangs penetrate targets.", 80, 1, PalicoSkillCategory.OFFENSE, unknown),
    PalicoSkill("artillery", "大砲・バリスタ強化術", "Cannon and Ballista Boost", "Strengthens Palico cannon and ballista attacks.", 60, 1, PalicoSkillCategory.OFFENSE, explosive),
    PalicoSkill("quick_getup", "起き上がりこぼし術", "Quick Get-up", "Gets up quickly after taking damage on the ground.", 100, 2, PalicoSkillCategory.DEFENSE, predators),
    PalicoSkill("resilience", "七転び八起きの術", "Resilience", "Attack rises each time the Palico faints, up to eight times.", 120, 3, PalicoSkillCategory.OFFENSE, level(15)),
    PalicoSkill("last_stand", "背水の陣の術", "Last Stand", "Greatly raises attack and defense, but prevents revival after fainting.", 150, 2, PalicoSkillCategory.SPECIAL, level(15)),
    PalicoSkill("fire_res", "耐性強化〖火〗", "Fire Resistance Up", "Increases fire resistance.", 15, 1, PalicoSkillCategory.DEFENSE, level(3)),
    PalicoSkill("water_res", "耐性強化〖水〗", "Water Resistance Up", "Increases water resistance.", 15, 1, PalicoSkillCategory.DEFENSE, unknown),
    PalicoSkill("thunder_res", "耐性強化〖雷〗", "Thunder Resistance Up", "Increases thunder resistance.", 15, 1, PalicoSkillCategory.DEFENSE, level(5)),
    PalicoSkill("ice_res", "耐性強化〖氷〗", "Ice Resistance Up", "Increases ice resistance.", 15, 1, PalicoSkillCategory.DEFENSE, unknown),
    PalicoSkill("dragon_res", "耐性強化〖龍〗", "Dragon Resistance Up", "Increases dragon resistance.", 30, 1, PalicoSkillCategory.DEFENSE, level(10)),
    PalicoSkill("all_res", "全耐性強化の術", "All Resistances Up", "Slightly increases all elemental resistances.", 30, 1, PalicoSkillCategory.DEFENSE, unknown),
    PalicoSkill("psychic", "千里眼の術", "Psychic", "Periodically reveals the location of large monsters.", 70, 2, PalicoSkillCategory.SUPPORT, affection(4)),
    PalicoSkill("speed_gathering", "高速採取の術", "Speed Gathering", "Gathers more quickly.", 50, 1, PalicoSkillCategory.GATHERING, level(5)),
    PalicoSkill("speed_recovery", "高速回復の術", "Speed Recovery", "Recovers health more quickly underground.", 130, 2, PalicoSkillCategory.RECOVERY, level(15)),
    PalicoSkill("no_gathering", "採取封印の術", "No Gathering", "Stops the Palico from gathering.", 30, 1, PalicoSkillCategory.GATHERING, level(10)),
    PalicoSkill("no_slacking", "サボりなしの術", "No Slacking", "Prevents slacking, but reduces maximum health.", 60, 1, PalicoSkillCategory.SPECIAL, affection(4)),
    PalicoSkill("slacking_recovery", "サボり中・回復の術", "Slacking Recovery", "Restores health while slacking.", 60, 1, PalicoSkillCategory.RECOVERY, unknown),
    PalicoSkill("flute_echo", "オトモ同士の山彦笛術", "Palico Flute Echo", "Plays a flute when the other Palico plays one.", 100, 1, PalicoSkillCategory.SUPPORT, level(15)),
    PalicoSkill("trap_combo", "オトモ同士の罠連携術", "Palico Trap Combo", "Places a large barrel bomb on the other Palico's shock trap.", 60, 1, PalicoSkillCategory.SUPPORT, affection(4)),
    PalicoSkill("encouragement", "オトモ同士の注意術", "Palico Encouragement", "Interrupts the other Palico's slacking.", 20, 1, PalicoSkillCategory.SUPPORT, level(2)),
    PalicoSkill("bond", "オトモ同士のきずな術", "Palico Bond", "Raises attack and defense when the other Palico faints.", 80, 2, PalicoSkillCategory.SUPPORT, level(10))
)
