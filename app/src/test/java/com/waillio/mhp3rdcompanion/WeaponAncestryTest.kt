package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.Weapon
import com.waillio.mhp3rdcompanion.data.WeaponRecipe
import com.waillio.mhp3rdcompanion.data.WeaponRecipeIngredient
import com.waillio.mhp3rdcompanion.data.WeaponSharpness
import com.waillio.mhp3rdcompanion.data.WeaponSharpnessProfile
import com.waillio.mhp3rdcompanion.data.WeaponUpgradeFrom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WeaponAncestryTest {
    @Test
    fun ancestryReversesParentChainAndKeepsRecipeOnChild() {
        val root = testWeapon("root", "Root", forge = recipe(100, 1))
        val middle = testWeapon("middle", "Middle", parent = upgrade("root", 200, 2))
        val current = testWeapon("current", "Current", parent = upgrade("middle", 300, 3))

        val result = buildWeaponAncestry(current, listOf(root, middle, current).associateBy { it.id })

        assertEquals(listOf("root", "middle", "current"), result.nodes.map { it.weapon.id })
        assertEquals(WeaponAncestryMethod.FORGE, result.nodes[0].method)
        assertEquals(100, result.nodes[0].recipe?.zenny)
        assertEquals(200, result.nodes[1].recipe?.zenny)
        assertEquals(300, result.nodes[2].recipe?.zenny)
        assertFalse(result.cycleDetected)
    }

    @Test
    fun childWithForgeAndUpgradeShowsBothRoutes() {
        val root = testWeapon("root")
        val child = testWeapon("child", parent = upgrade("root", 200, 2), forge = recipe(500, 5))
        val result = buildWeaponAncestry(child, mapOf(root.id to root, child.id to child))
        val node = result.nodes.last()

        assertEquals(WeaponAncestryMethod.UPGRADE, node.method)
        assertEquals(200, node.recipe?.zenny)
        assertEquals(500, node.directForgeRecipe?.zenny)
    }

    @Test
    fun rootWithoutForgeIsSafeAndDoesNotInventRecipe() {
        val root = testWeapon("root")
        val node = buildWeaponAncestry(root, mapOf(root.id to root)).nodes.single()
        assertEquals(WeaponAncestryMethod.BASE, node.method)
        assertNull(node.recipe)
        assertNull(node.directForgeRecipe)
    }

    @Test
    fun malformedGraphsAreReportedWithoutCrashing() {
        val a = testWeapon("a", parent = upgrade("b", 10, 1))
        val b = testWeapon("b", parent = upgrade("a", 20, 1))
        val cycle = buildWeaponAncestry(a, mapOf("a" to a, "b" to b))
        assertTrue(cycle.cycleDetected)
        assertEquals(listOf("b", "a"), cycle.nodes.map { it.weapon.id })

        val missing = buildWeaponAncestry(a, mapOf("a" to a))
        assertEquals("b", missing.missingParentId)
        assertEquals(1, missing.nodes.size)
    }

    @Test
    fun productionSpecialPathsUseStableRelationsAndResolveIngredients() {
        val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data
        val repository = WeaponRepository.fromProduction(data.weapons, data.materials)

        val dual = repository.catalog("DUAL_BLADES").ancestryFor("weapon_dual_blades_010")
        assertEquals(
            listOf("Old Yukumo Duo", "Yukumo Duo", "Matched Slicers", "Matched Slicers+", "Dual Slicers"),
            dual.nodes.takeLast(5).map { it.weapon.name }
        )
        assertEquals("weapon_dual_blades_010", dual.nodes.last().weapon.id)

        val eternal = repository.catalog("SWORD_AND_SHIELD").ancestryFor("weapon_sword_and_shield_079")
        assertEquals(listOf("Eternal Strife", "Eternal Hate"), eternal.nodes.takeLast(2).map { it.weapon.name })
        assertEquals(44444, eternal.nodes.last().recipe?.zenny)
        assertTrue(eternal.nodes.last().recipe!!.ingredients.all { repository.recipeItem(it.gameItemId) != null })

        // The accepted schema-29 corpus names this forge-only regression row
        // differently from older references; either identity still exercises
        // the same no-parent/direct-forge semantics.
        val forgeRoot = repository.catalog("SWORD_AND_SHIELD").weapons.firstOrNull { it.name == "Cruel Pain" }
            ?: repository.catalog("SWORD_AND_SHIELD").weapons.first { it.forgeRecipe != null && it.upgradeFrom == null }
        val rootResult = repository.catalog("SWORD_AND_SHIELD").ancestryFor(forgeRoot.id)
        assertEquals(WeaponAncestryMethod.FORGE, rootResult.nodes.single().method)
        assertNotNull(rootResult.nodes.single().recipe)
    }

    @Test
    fun everyProductionAncestryUsesOnlyIndexedParentsAndResolvesRecipeItems() {
        val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data
        val repository = WeaponRepository.fromProduction(data.weapons, data.materials)
        repository.catalogs.values.forEach { catalog ->
            catalog.weapons.forEach { weapon ->
                val result = catalog.ancestryFor(weapon.id)
                assertFalse(result.cycleDetected)
                assertNull(result.missingParentId)
                result.nodes.flatMap { listOfNotNull(it.recipe, it.directForgeRecipe) }
                    .flatMap { it.ingredients }
                    .forEach { assertNotNull(catalog.recipeItem(it.gameItemId)) }
            }
        }
    }

    private fun recipe(zenny: Int, gameItemId: Int): WeaponRecipe =
        WeaponRecipe(zenny, listOf(WeaponRecipeIngredient(gameItemId, 1)))

    private fun upgrade(parentId: String, zenny: Int, gameItemId: Int): WeaponUpgradeFrom =
        WeaponUpgradeFrom("edge-$parentId", parentId, recipe(zenny, gameItemId))

    private fun testWeapon(
        id: String,
        name: String = id,
        parent: WeaponUpgradeFrom? = null,
        forge: WeaponRecipe? = null
    ): Weapon = Weapon(
        stableWeaponId = id,
        weaponType = "GREAT_SWORD",
        sourceOrdinal = 1,
        sourceName = name,
        sourceKey = id,
        displayName = name,
        nameSource = "test",
        rarity = 1,
        attack = 100,
        affinity = 0,
        slots = 0,
        defenseBonus = null,
        special = null,
        sharpness = WeaponSharpnessProfile(WeaponSharpness(1, 1, 1, 1, 1, 1), null),
        forgeRecipe = forge,
        upgradeFrom = parent,
        upgradesTo = emptyList()
    )
}
