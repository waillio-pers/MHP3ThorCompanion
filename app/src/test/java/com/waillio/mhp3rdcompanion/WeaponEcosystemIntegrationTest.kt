package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeaponEcosystemIntegrationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun productionReverseIndexIsCompleteAndRecipeKindsArePreserved() {
        val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data
        val repository = WeaponRepository.fromProduction(data.weapons, data.materials)
        assertEquals(987, repository.weapons.size)
        assertEquals(3834, repository.recipeUsageIndex.all.size)
        assertEquals(436, repository.recipeUsageIndex.byGameItemId.size)
        assertEquals(87, repository.recipeUsageIndex.uniqueWeaponCount(220))
        assertTrue(repository.recipeUsageIndex.all.any { it.recipeKind == WeaponRecipeKind.FORGE })
        assertTrue(repository.recipeUsageIndex.all.any { it.recipeKind == WeaponRecipeKind.UPGRADE })
        assertTrue(repository.recipeUsageIndex.all.all { repository.weaponById.containsKey(it.stableWeaponId) })

        // Compare the reverse projection as a multiset, so repeated ingredients
        // remain meaningful while ordering remains an implementation detail.
        val forwardRows = repository.weapons.flatMap { weapon ->
            buildList {
                weapon.forgeRecipe?.ingredients.orEmpty().forEach { ingredient ->
                    add(WeaponRecipeUsage(ingredient.gameItemId, weapon.id, weapon.weaponType, WeaponRecipeKind.FORGE, ingredient.quantity))
                }
                weapon.upgradeFrom?.recipe?.ingredients.orEmpty().forEach { ingredient ->
                    add(WeaponRecipeUsage(ingredient.gameItemId, weapon.id, weapon.weaponType, WeaponRecipeKind.UPGRADE, ingredient.quantity))
                }
                weapon.orphanUpgradeRecipe?.ingredients.orEmpty().forEach { ingredient ->
                    add(WeaponRecipeUsage(ingredient.gameItemId, weapon.id, weapon.weaponType, WeaponRecipeKind.UPGRADE, ingredient.quantity))
                }
            }
        }
        assertEquals(forwardRows.groupingBy { it }.eachCount(), repository.recipeUsageIndex.all.groupingBy { it }.eachCount())
    }

    @Test
    fun globalWeaponIndexCoversEveryStableIdAndAllTwelveTypes() {
        val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data
        val results = CompanionLogic.search(data, "", com.waillio.mhp3rdcompanion.data.EntityType.WEAPON)
        assertEquals(987, results.size)
        assertEquals(987, results.map { it.id }.distinct().size)
        assertEquals(WeaponRepository.PRODUCTION_WEAPON_TYPES.toSet(), data.weapons.map { it.weaponType }.toSet())
        val canonical = data.weapons.first { it.displayName == "Yukumo Edge" }
        assertTrue(CompanionLogic.search(data, "Yukumo Edge", com.waillio.mhp3rdcompanion.data.EntityType.WEAPON)
            .any { it.id == canonical.stableWeaponId && it.name == canonical.name })
    }

    @Test
    fun favoriteAndRecentPoliciesKeepStableWeaponIdentityForAllTypes() {
        val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data
        val weaponsByType = data.weapons.associateBy { it.weaponType }
        var favorites = listOf(FavoriteEntry(ReferenceEntityType.MATERIAL, "item_iron_ore", 1L))
        WeaponRepository.PRODUCTION_WEAPON_TYPES.forEachIndexed { index, type ->
            favorites = com.waillio.mhp3rdcompanion.data.ReferenceHistoryLogic.toggleFavorite(
                favorites, ReferenceEntityType.WEAPON, weaponsByType.getValue(type).id, (index + 2).toLong()
            )
        }
        assertEquals(13, favorites.size)
        assertTrue(favorites.any { it.type == ReferenceEntityType.MATERIAL })
        assertEquals(12, favorites.count { it.type == ReferenceEntityType.WEAPON })

        var recent = emptyList<RecentEntry>()
        WeaponRepository.PRODUCTION_WEAPON_TYPES.forEachIndexed { index, type ->
            recent = com.waillio.mhp3rdcompanion.data.ReferenceHistoryLogic.recordRecent(
                recent, ReferenceEntityType.WEAPON, weaponsByType.getValue(type).id, (index + 1).toLong()
            )
        }
        assertEquals(5, recent.size)
        assertEquals(5, recent.map { it.entityId }.distinct().size)
        assertTrue(recent.all { it.type == ReferenceEntityType.WEAPON })
    }

    @Test
    fun globalWeaponResultDeepLinksAndBackRestoresSearchContext() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-all").performClick()
        rule.onNodeWithTag("global-search").performTextInput("Old Yukumo L. Bowgun")
        rule.onNodeWithTag("result-weapon-weapon_light_bowgun_001").assertIsDisplayed().performClick()
        rule.onNodeWithTag("weapon-detail-title-weapon_light_bowgun_001").assertIsDisplayed()
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.waitForIdle()
        rule.onNodeWithTag("global-search").assertTextContains("Old Yukumo L. Bowgun")
        rule.onNodeWithTag("result-weapon-weapon_light_bowgun_001").assertIsDisplayed()
    }

    @Test
    fun materialDetailExposesUsedInWeaponsAndDedicatedList() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-all").performClick()
        rule.onNodeWithTag("global-search").performTextInput("Iron Ore")
        rule.onNodeWithTag("result-material-item_iron_ore").performClick()
        rule.onNodeWithTag("material-used-in-weapons").assertExists()
        rule.onNodeWithTag("material-used-in-weapons-view-all").performScrollTo().performClick()
        rule.onNodeWithTag("screen-used-in-weapons").assertIsDisplayed()
    }

    @Test
    fun weaponFavoriteUsesStableIdentityAcrossDetailAndFavorites() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-all").performClick()
        rule.onNodeWithTag("global-search").performTextInput("Old Yukumo L. Bowgun")
        rule.onNodeWithTag("result-weapon-weapon_light_bowgun_001").performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithContentDescription("Add to favorites").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithContentDescription("Remove from favorites").fetchSemanticsNodes().isNotEmpty()
        }
        if (rule.onAllNodesWithContentDescription("Add to favorites").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithTag("weapon-favorite").performClick()
            rule.waitUntil(timeoutMillis = 5_000) {
                rule.onAllNodesWithContentDescription("Remove from favorites").fetchSemanticsNodes().isNotEmpty()
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("nav-favorites").performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag("favorite-weapon-weapon_light_bowgun_001").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("favorite-weapon-weapon_light_bowgun_001").assertIsDisplayed()
        rule.onNodeWithTag("favorite-weapon-weapon_light_bowgun_001").performClick()
        rule.onNodeWithTag("weapon-detail-title-weapon_light_bowgun_001").assertIsDisplayed()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithContentDescription("Remove from favorites").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Remove from favorites").assertIsDisplayed()
    }

    @Test
    fun bowgunAmmoUsesSharedMatrixAndNeverShowsUnsupportedZero() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-all").performClick()
        rule.onNodeWithTag("global-search").performTextInput("Old Yukumo L. Bowgun")
        rule.onNodeWithTag("result-weapon-weapon_light_bowgun_001").performClick()
        rule.onNodeWithTag("weapon-lbg-ammo-table").assertExists()
        rule.onNodeWithTag("weapon-lbg-ammo-group-physical").assertExists()
        rule.onNodeWithTag("ammo-matrix-cell-normal_s-3").assertTextContains("4")
        rule.onNodeWithTag("weapon-lbg-ammo-group-utility").assertExists()
        rule.onNodeWithText("×0").assertDoesNotExist()
        assertEquals("—", ammoCapacityLabel(0))
        assertEquals("3", ammoCapacityLabel(3))
    }

    @Test
    fun bowgunMatrixHeadersFollowTheirTypedSourceShapes() {
        assertEquals(listOf("Lv1", "Lv2", "Lv3"), ammoMatrixColumns(BowgunAmmoSourceShape.THREE_LEVEL).map { it.label })
        assertEquals(listOf("Lv1", "Lv2"), ammoMatrixColumns(BowgunAmmoSourceShape.TWO_LEVEL).map { it.label })
        assertEquals(listOf("Load"), ammoMatrixColumns(BowgunAmmoSourceShape.SINGLE_CAPACITY).map { it.label })
        assertEquals(listOf(1, 2, 3), ammoMatrixColumns(BowgunAmmoSourceShape.THREE_LEVEL).map { it.level })
        assertEquals(listOf(1, 2), ammoMatrixColumns(BowgunAmmoSourceShape.TWO_LEVEL).map { it.level })
        assertEquals(listOf(null), ammoMatrixColumns(BowgunAmmoSourceShape.SINGLE_CAPACITY).map { it.level })
        assertTrue(ammoMatrixGroups.single { it.key == "physical" }.sourceShape == BowgunAmmoSourceShape.THREE_LEVEL)
        assertTrue(ammoMatrixGroups.single { it.key == "status" }.sourceShape == BowgunAmmoSourceShape.TWO_LEVEL)
        assertTrue(ammoMatrixGroups.filter { it.key == "element" || it.key == "utility" }.all { it.sourceShape == BowgunAmmoSourceShape.SINGLE_CAPACITY })
    }

    @Test
    fun productionBowgunGroupsAreHomogeneousAndFitWithoutHorizontalScroll() {
        val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data
        data.weapons.filter { it.weaponType == "LIGHT_BOWGUN" || it.weaponType == "HEAVY_BOWGUN" }
            .mapNotNull { weapon ->
                when (val mechanics = weapon.mechanics) {
                    is LightBowgunMechanics -> mechanics.shared
                    is HeavyBowgunMechanics -> mechanics.shared
                    else -> null
                }
            }
            .forEach { shared ->
                ammoMatrixGroups.forEach { group ->
                    val shapes = shared.ammoLoads
                        .filter { it.round.ammoType in group.families }
                        .map { it.sourceShape }
                        .distinct()
                    if (shapes.isNotEmpty()) assertEquals(listOf(group.sourceShape), shapes)
                    assertTrue(ammoMatrixColumns(group.sourceShape).size <= 3)
                }
            }
    }

    @Test
    fun specialFireKeepsTypedRoundsAndAuthenticIcons() {
        val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data
        val light = data.weapons.mapNotNull { it.mechanics as? LightBowgunMechanics }
        val heavy = data.weapons.mapNotNull { it.mechanics as? HeavyBowgunMechanics }
        assertEquals(85, light.sumOf { it.rapidFire.size })
        assertEquals(162, heavy.sumOf { it.crouchFire.size })
        assertTrue(light.flatMap { it.rapidFire }.all { AmmoIconRegistry.resolve(it.round.ammoType) != null })
        assertTrue(heavy.flatMap { it.crouchFire }.all { AmmoIconRegistry.resolve(it.round.ammoType) != null })
        assertTrue(light.flatMap { it.rapidFire }.all { it.burstCount > 0 })
    }

    @Test
    fun globalWeaponSearchUsesCanonicalHumanReadableTypeLabels() {
        val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data
        val results = CompanionLogic.search(data, "Yukumo L. Bowgun", EntityType.WEAPON)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.subtitle.contains("Light Bowgun") })
        assertTrue(results.none { it.subtitle.contains("LIGHT_BOWGUN") || it.subtitle.contains("LIGHT BOWGUN") })
        assertEquals("Light Bowgun", canonicalWeaponTypeLabel("LIGHT_BOWGUN"))
    }
}
