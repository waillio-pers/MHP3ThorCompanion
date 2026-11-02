package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.Weapon
import com.waillio.mhp3rdcompanion.data.WeaponRecipeKind
import com.waillio.mhp3rdcompanion.data.WeaponRecipeUsage
import org.junit.Assert.assertEquals
import org.junit.Test

class WeaponUsageOrderingTest {
    private fun weapon(id: String, type: String, name: String) = Weapon(
        stableWeaponId = id, weaponType = type, sourceOrdinal = 1, sourceName = name,
        sourceKey = id, displayName = name, nameSource = "test", rarity = 1, attack = 100,
        affinity = 0, slots = 0, defenseBonus = null, special = null,
        forgeRecipe = null, upgradeFrom = null, upgradesTo = emptyList()
    )

    @Test fun orderingIsStableAndPreservesAllRelations() {
        val weapons = (0..8).map { weapon("l$it", "LANCE", "Lance $it") } +
            listOf(weapon("ga", "GUNLANCE", "Alpha"), weapon("lb", "LANCE", "lance 0"))
        val byId = weapons.associateBy { it.id }
        val relations = weapons.map { WeaponRecipeUsage(1, it.id, it.weaponType, WeaponRecipeKind.FORGE, 2) } +
            listOf(WeaponRecipeUsage(1, "l0", "LANCE", WeaponRecipeKind.UPGRADE, 4),
                WeaponRecipeUsage(1, "missing", "LANCE", WeaponRecipeKind.FORGE, 3))
        val ordered = orderedWeaponUsages(relations, byId)
        val shuffled = orderedWeaponUsages(relations.reversed(), byId)
        assertEquals(ordered.map { it.key }, shuffled.map { it.key })
        assertEquals(listOf("l0", "lb"), ordered.take(2).map { it.key })
        assertEquals("ga", ordered[10].key)
        assertEquals("missing", ordered.last().key)
        assertEquals(relations.filter { it.stableWeaponId == "l0" }.toSet(), ordered.first().value.toSet())
        assertEquals(ordered.map { it.key }.take(8), shuffled.map { it.key }.take(8))
    }
}
