package com.waillio.mhp3rdcompanion

/** Names: TMO PSP v6.1 r754, language table 0, entries 1782–1833.
 * Effects: MHP3@Wiki (updated 2012-01-03) and MHP3 Kouryaku Tsushin.
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
    PalicoSkill("demon_flute", "鬼人笛の術", "Demon Flute", "Temporarily increases the hunter's and Palico's attack by 10.", 60, 2, PalicoSkillCategory.SUPPORT, level(2)),
    PalicoSkill("armor_flute", "硬化笛の術", "Armor Flute", "Temporarily increases the hunter's and Palico's defense by 20.", 60, 2, PalicoSkillCategory.SUPPORT, level(2)),
    PalicoSkill("antidote_flute", "解毒・消臭笛の術", "Cure Flute", "Removes poison and odor from the hunter and Palico.", 30, 1, PalicoSkillCategory.RECOVERY, level(2)),
    PalicoSkill("health_flute", "回復笛の術", "Health Flute", "Restores 30 health to the hunter and Palico.", 70, 2, PalicoSkillCategory.RECOVERY, level(5)),
    PalicoSkill("true_health_flute", "真・回復笛の術", "Health Flute (L)", "Restores 50 health to the hunter and Palico.", 120, 3, PalicoSkillCategory.RECOVERY, affection(4)),
    PalicoSkill("element_attack", "属性攻撃強化の術", "Elemental Attack Up", "Multiplies the Palico's elemental attack by 1.3.", 50, 2, PalicoSkillCategory.OFFENSE, level(10)),
    PalicoSkill("poison_attack", "状態異常攻撃〖毒〗", "Status Atk [Poison]", "Adds poison to the Palico's melee and boomerang attacks.", 80, 3, PalicoSkillCategory.OFFENSE, hunt("Great Wroggi", "Gigginox", "Purple Ludroth")),
    PalicoSkill("paralysis_attack", "状態異常攻撃〖麻痺〗", "StatusAtk [Paralysis]", "Adds paralysis to the Palico's melee and boomerang attacks.", 80, 4, PalicoSkillCategory.OFFENSE, hunt("Volvidon", "Baleful Gigginox")),
    PalicoSkill("sleep_attack", "状態異常攻撃〖睡眠〗", "Status Atk [Sleep]", "Adds sleep to the Palico's melee and boomerang attacks.", 80, 3, PalicoSkillCategory.OFFENSE, hunt("Great Baggi", "Uragaan")),
    PalicoSkill("status_attack", "状態異常攻撃強化の術", "Abn Status Atk Up", "Multiplies the Palico's poison, paralysis and sleep attack strength by 1.4.", 120, 2, PalicoSkillCategory.OFFENSE, level(10)),
    PalicoSkill("mud_snow", "泥・雪だるま早割りの術", "Mud/Snow Quick Rec", "Lets the Palico escape mud and snowman status faster.", 30, 1, PalicoSkillCategory.DEFENSE, level(5)),
    PalicoSkill("poison_immunity", "毒無効の術", "Poison Negate", "Prevents the Palico from being poisoned.", 30, 1, PalicoSkillCategory.DEFENSE, level(2)),
    PalicoSkill("wind_immunity", "風圧無効の術", "Wind Negate", "Prevents the Palico from flinching from most monster wind pressure.", 100, 2, PalicoSkillCategory.DEFENSE, level(10)),
    PalicoSkill("earplugs", "防音の術", "Earplug", "Prevents the Palico from flinching from monster roars.", 120, 2, PalicoSkillCategory.DEFENSE, level(10)),
    PalicoSkill("earplugs_wind", "防音・風圧無効の術", "Earplugs/Wind Negate", "Protects the Palico from monster roars and most wind pressure.", 140, 3, PalicoSkillCategory.DEFENSE, level(15)),
    PalicoSkill("attack_small", "攻撃力強化術〖小〗", "Attack Up（S）", "Increases the Palico's attack by 5.", 30, 1, PalicoSkillCategory.OFFENSE, level(2)),
    PalicoSkill("attack_large", "攻撃力強化術〖大〗", "Attack Up (L)", "Increases the Palico's attack by 20.", 90, 2, PalicoSkillCategory.OFFENSE, powerful),
    PalicoSkill("defense_small", "防御力強化術〖小〗", "Defense Up（S）", "Increases the Palico's defense by 10.", 30, 1, PalicoSkillCategory.DEFENSE, level(2)),
    PalicoSkill("defense_large", "防御力強化術〖大〗", "Defense Up (L)", "Increases the Palico's defense by 30.", 90, 2, PalicoSkillCategory.DEFENSE, powerful),
    PalicoSkill("health_small", "体力強化術〖小〗", "Health Up（S）", "Multiplies the Palico's maximum health by 1.1.", 30, 1, PalicoSkillCategory.DEFENSE, level(5)),
    PalicoSkill("health_large", "体力強化術〖大〗", "Health Up (L)", "Greatly increases the Palico's maximum health.", 90, 2, PalicoSkillCategory.DEFENSE, powerful),
    PalicoSkill("guard_small", "ガード確率強化〖小〗", "Guard Up（S）", "Slightly increases the Palico's guard chance.", 30, 1, PalicoSkillCategory.DEFENSE, level(5)),
    PalicoSkill("guard_large", "ガード確率強化〖大〗", "Guard Up (L)", "Greatly increases the Palico's guard chance.", 90, 2, PalicoSkillCategory.DEFENSE, level(10)),
    PalicoSkill("critical", "会心攻撃強化の術", "Affinity Boost", "Increases the Palico's affinity by 10%.", 50, 1, PalicoSkillCategory.OFFENSE, level(5)),
    PalicoSkill("plunder", "ぶんどり強化の術", "Rob'em Blind", "Increases the Palico's chance of stealing items from monsters.", 100, 2, PalicoSkillCategory.GATHERING, affection(4)),
    PalicoSkill("shock_trap", "シビレ罠の術", "Shock Trap", "The Palico occasionally places a shock trap.", 160, 3, PalicoSkillCategory.SUPPORT, unknown),
    PalicoSkill("felvine_bomb", "マタタビ爆弾の術", "Felvine Bomb", "The Palico occasionally throws a felvine bomb.", 30, 1, PalicoSkillCategory.SUPPORT, level(2)),
    PalicoSkill("large_barrel_bomb", "大タル爆弾の術", "Large Barrel Bomb", "The Palico occasionally charges with a large barrel bomb.", 140, 2, PalicoSkillCategory.OFFENSE, explosive),
    PalicoSkill("bomb_boost", "爆弾強化の術", "Bomb Strength Up", "Increases the Palico's bomb damage.", 150, 3, PalicoSkillCategory.OFFENSE, unknown),
    PalicoSkill("bomb_resistance", "爆弾ダメージ軽減の術", "Bomb Dmg Reduction", "Halves the bomb damage the Palico receives.", 90, 1, PalicoSkillCategory.DEFENSE, level(5)),
    PalicoSkill("big_boomerang", "ビッグブーメランの術", "Boomerang Up", "Enlarges the Palico's boomerangs and increases their power.", 100, 2, PalicoSkillCategory.OFFENSE, predators),
    PalicoSkill("piercing_boomerang", "貫通ブーメランの術", "Pierce Boomerang", "The Palico's boomerangs penetrate targets and can hit twice.", 80, 1, PalicoSkillCategory.OFFENSE, unknown),
    PalicoSkill("artillery", "大砲・バリスタ強化術", "Cannon/Ballista Up", "Strengthens Palico cannon and ballista attacks.", 60, 1, PalicoSkillCategory.OFFENSE, explosive),
    PalicoSkill("quick_getup", "起き上がりこぼし術", "Quick Riser", "Lets the Palico get up quickly after taking damage on the ground.", 100, 2, PalicoSkillCategory.DEFENSE, predators),
    PalicoSkill("resilience", "七転び八起きの術", "9 Lives", "Each faint raises the Palico's attack by 10, up to eight times (+80).", 120, 3, PalicoSkillCategory.OFFENSE, level(15)),
    PalicoSkill("last_stand", "背水の陣の術", "Last Stand", "Raises the Palico's attack and defense by 50 each, but prevents revival after fainting.", 150, 2, PalicoSkillCategory.SPECIAL, level(15)),
    PalicoSkill("fire_res", "耐性強化〖火〗", "Resist Fire", "Increases the Palico's fire resistance by 20.", 15, 1, PalicoSkillCategory.DEFENSE, level(3)),
    PalicoSkill("water_res", "耐性強化〖水〗", "Resist Water", "Increases the Palico's water resistance by 20.", 15, 1, PalicoSkillCategory.DEFENSE, unknown),
    PalicoSkill("thunder_res", "耐性強化〖雷〗", "Resist Thunder", "Increases the Palico's thunder resistance by 20.", 15, 1, PalicoSkillCategory.DEFENSE, level(5)),
    PalicoSkill("ice_res", "耐性強化〖氷〗", "Resist Ice", "Increases the Palico's ice resistance by 20.", 15, 1, PalicoSkillCategory.DEFENSE, unknown),
    PalicoSkill("dragon_res", "耐性強化〖龍〗", "Resist Dragon", "Increases the Palico's dragon resistance by 20.", 30, 1, PalicoSkillCategory.DEFENSE, level(10)),
    PalicoSkill("all_res", "全耐性強化の術", "Resist all", "Increases each of the Palico's elemental resistances by 10.", 30, 1, PalicoSkillCategory.DEFENSE, unknown),
    PalicoSkill("psychic", "千里眼の術", "Clairvoyance", "Reveals large monsters' locations at the start of a quest and periodically afterward.", 70, 2, PalicoSkillCategory.SUPPORT, affection(4)),
    PalicoSkill("speed_gathering", "高速採取の術", "Speedy Gathering", "Multiplies the Palico's gathering speed by 1.2.", 50, 1, PalicoSkillCategory.GATHERING, level(5)),
    PalicoSkill("speed_recovery", "高速回復の術", "Speedy Recovery", "Lets the Palico recover its own health more quickly underground.", 130, 2, PalicoSkillCategory.RECOVERY, level(15)),
    PalicoSkill("no_gathering", "採取封印の術", "Hunter", "Stops the Palico from gathering.", 30, 1, PalicoSkillCategory.GATHERING, level(10)),
    PalicoSkill("no_slacking", "サボりなしの術", "Workaholic", "Prevents the Palico from slacking, but reduces its maximum health.", 60, 1, PalicoSkillCategory.SPECIAL, affection(4)),
    PalicoSkill("slacking_recovery", "サボり中・回復の術", "Take a Break", "The Palico restores its own health while slacking.", 60, 1, PalicoSkillCategory.RECOVERY, unknown),
    PalicoSkill("flute_echo", "オトモ同士の山彦笛術", "Copycat", "This Palico plays a flute when the other Palico plays one.", 100, 1, PalicoSkillCategory.SUPPORT, level(15)),
    PalicoSkill("trap_combo", "オトモ同士の罠連携術", "Trap Bombing", "This Palico places a large barrel bomb on the other Palico's shock trap.", 60, 1, PalicoSkillCategory.SUPPORT, affection(4)),
    PalicoSkill("encouragement", "オトモ同士の注意術", "Buddy's Concern", "This Palico interrupts the other Palico's slacking.", 20, 1, PalicoSkillCategory.SUPPORT, level(2)),
    PalicoSkill("bond", "オトモ同士のきずな術", "Friendship", "Raises this Palico's attack and defense by 30 each while the other Palico is down.", 80, 2, PalicoSkillCategory.SUPPORT, level(10))
)
