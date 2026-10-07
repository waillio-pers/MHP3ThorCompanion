package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.BowMechanics
import com.waillio.mhp3rdcompanion.data.LightBowgunMechanics
import com.waillio.mhp3rdcompanion.data.HeavyBowgunMechanics
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class WeaponProductionUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun productionCorpusAndGraphInvariantsHold() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val sha = context.assets.open("mhp3rd-data.json").use { input ->
            MessageDigest.getInstance("SHA-256").digest(input.readBytes()).joinToString("") { "%02X".format(it) }
        }
        assertEquals("657CFA9376FC8CBE56C67E67D4D1A27C455D21021F51E21B62427C8DDBB26155", sha)
        val data = CompanionRepository(context).data
        assertEquals(978, data.materials.size)
        assertEquals(987, data.weapons.size)
        val repository = WeaponRepository.fromProduction(data.weapons, data.materials)
        assertEquals(91, repository.validation.weaponCount)
        assertEquals(75, repository.validation.edgeCount)
        assertEquals(16, repository.validation.rootIds.size)
        assertEquals(9, repository.validation.maxDepth)
        assertEquals(201, repository.validation.recipeMaterialCount)
        assertEquals(0, repository.validation.unresolvedMaterialCount)
        val longSword = repository.catalog("LONG_SWORD").validation
        assertEquals(85, longSword.weaponCount)
        assertEquals(71, longSword.edgeCount)
        assertEquals(14, longSword.rootIds.size)
        assertEquals(8, longSword.maxDepth)
        assertEquals(204, longSword.recipeMaterialCount)
        assertEquals(0, longSword.unresolvedMaterialCount)
    }

    @Test
    fun chooserUsesProductionCopyAndTreeStartsWithSixteenRoots() {
        openWeapons()
        rule.onNodeWithText("Great Sword").assertIsDisplayed()
        rule.onNodeWithText("91 · 16 trees").assertIsDisplayed()
        rule.onNodeWithText("Pilot").assertDoesNotExist()
        rule.onNodeWithTag("weapon-type-great-sword").performClick()
        rule.onNodeWithTag("weapon-tree").assertIsDisplayed()
        val repository = productionRepository()
        assertEquals(16, visibleTree(repository, emptySet()).size)
    }

    @Test
    fun chooserExposesExactlyTheTwelveIntegratedProductionWeaponTypes() {
        openWeapons()
        rule.onNodeWithTag("weapon-type-great-sword").assertIsDisplayed()
        rule.onNodeWithTag("weapon-type-long-sword").assertIsDisplayed()
        rule.onNodeWithTag("weapon-type-sword-and-shield").assertIsDisplayed()
        rule.onNodeWithTag("weapon-type-dual-blades").assertIsDisplayed()
        rule.onNodeWithTag("weapon-type-hammer").assertIsDisplayed()
        rule.onNodeWithTag("weapon-type-lance").assertIsDisplayed()
        rule.onNodeWithTag("weapon-type-hunting-horn").assertIsDisplayed()
        rule.onNodeWithTag("weapon-type-gunlance").assertIsDisplayed()
        rule.onNodeWithTag("weapon-type-switch-axe").assertIsDisplayed()
        rule.onNodeWithTag("weapon-type-bow").assertIsDisplayed()
        rule.onNodeWithTag("weapon-type-light-bowgun").assertIsDisplayed()
        rule.onNodeWithTag("weapon-type-heavy-bowgun").assertIsDisplayed()
        rule.onNodeWithText("91 · 16 trees").assertIsDisplayed()
        rule.onNodeWithText("85 · 14 trees").assertIsDisplayed()
        rule.onNodeWithText("94 · 14 trees").assertIsDisplayed()
        rule.onNodeWithText("82 · 10 trees").assertIsDisplayed()
        rule.onNodeWithText("93 · 15 trees").assertIsDisplayed()
        rule.onNodeWithText("96 · 14 trees").assertIsDisplayed()
        rule.onNodeWithText("85 · 11 trees").assertIsDisplayed()
        rule.onNodeWithText("77 · 13 trees").assertIsDisplayed()
        rule.onNodeWithText("71 · 10 trees").assertIsDisplayed()
        rule.onNodeWithText("67 · 15 trees").assertIsDisplayed()
        rule.onNodeWithText("66 · 18 trees").assertIsDisplayed()
    }

    @Test
    fun selectedWeaponTypeUsesDedicatedFullscreenShellAndBackReturnsChooser() {
        openWeapons()
        rule.onNodeWithTag("weapon-type-great-sword").performClick()

        rule.onNodeWithTag("weapon-type-toolbar").assertIsDisplayed()
        rule.onNodeWithTag("weapon-tree").assertIsDisplayed()
        rule.onNodeWithTag("global-search").assertDoesNotExist()
        rule.onNodeWithTag("field-filter-weapons").assertDoesNotExist()

        rule.onNodeWithTag("weapons-back").performClick()
        rule.onNodeWithTag("weapons-chooser").assertIsDisplayed()
        rule.onNodeWithTag("field-filter-weapons").assertIsDisplayed()
        rule.onNodeWithTag("global-search").assertIsDisplayed()
    }

    @Test
    fun chooserTitlesAndTreeCountsRemainCompleteAtThorProfile() {
        openWeapons()
        listOf(
            "Great Sword", "Long Sword", "Sword & Shield", "Dual Blades", "Hammer", "Lance",
            "Hunting Horn", "Gunlance", "Switch Axe", "Bow", "Light Bowgun", "Heavy Bowgun"
        ).forEach { rule.onNodeWithText(it).assertIsDisplayed() }
        listOf(
            "91 · 16 trees", "85 · 14 trees", "94 · 14 trees", "82 · 10 trees", "93 · 15 trees",
            "96 · 14 trees", "85 · 11 trees", "77 · 13 trees", "71 · 10 trees", "80 · 17 trees",
            "67 · 15 trees", "66 · 18 trees"
        ).forEach { rule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun everyIntegratedTypeHasValidatedCatalogAndSevenAuthenticRarityIcons() {
        val expected = mapOf(
            "GREAT_SWORD" to Triple(91, 16, 9),
            "LONG_SWORD" to Triple(85, 14, 8),
            "SWORD_AND_SHIELD" to Triple(94, 14, 8),
            "DUAL_BLADES" to Triple(82, 10, 8),
            "HAMMER" to Triple(93, 15, 8),
            "LANCE" to Triple(96, 14, 9),
            "HUNTING_HORN" to Triple(85, 11, 8),
            "GUNLANCE" to Triple(77, 13, 8),
            "SWITCH_AXE" to Triple(71, 10, 8)
        )
        val repository = productionRepository()
        val expectedWithBow = expected + ("BOW" to Triple(80, 17, 7)) +
            ("LIGHT_BOWGUN" to Triple(67, 15, 7)) +
            ("HEAVY_BOWGUN" to Triple(66, 18, 7))
        assertEquals(expectedWithBow.keys.toList(), WeaponRepository.PRODUCTION_WEAPON_TYPES)
        expectedWithBow.forEach { (type, metrics) ->
            val catalog = repository.catalog(type)
            assertEquals(metrics.first, catalog.validation.weaponCount)
            assertEquals(metrics.second, catalog.validation.rootIds.size)
            assertEquals(metrics.third, catalog.validation.maxDepth)
            assertEquals(7, (1..7).count { WeaponIconRegistry.resolve(type, it) != null })
            assertEquals(7, (1..7).map { WeaponRarityVisuals.resolve(it, type).icon.resourceId }.distinct().size)
            assertTrue(catalog.weapons.all { it.weaponType == type })
        }
    }

    @Test
    fun bowCatalogUsesExactCorpusGraphMechanicsAndNoSharpnessPlaceholder() {
        val catalog = productionRepository().catalog("BOW")
        assertEquals(80, catalog.weapons.size)
        assertEquals(63, catalog.validation.edgeCount)
        assertEquals(17, catalog.validation.rootIds.size)
        assertEquals(25, catalog.validation.leafCount)
        assertEquals(7, catalog.validation.maxDepth)
        assertEquals(209, catalog.validation.recipeMaterialCount)
        assertTrue(catalog.weapons.all { it.mechanics is BowMechanics && it.sharpness == null })
        val akantor = catalog.weaponById.getValue("weapon_bow_078")
        val mechanics = akantor.mechanics as BowMechanics
        assertEquals(3, mechanics.charges.size)
        assertEquals(1, mechanics.charges.count { it.availability.name == "REQUIRES_LOAD_UP" })
    }

    @Test
    fun bowChooserTreeAndDetailExposeFirstClassMechanics() {
        openWeapons()
        rule.onNodeWithTag("weapon-type-bow").performClick()
        rule.onNodeWithTag("weapon-row-weapon_bow_001").performClick()
        rule.onNodeWithTag("weapon-detail-title-weapon_bow_001").assertIsDisplayed()
        rule.onNodeWithTag("weapon-bow-charges").assertIsDisplayed()
        rule.onNodeWithTag("weapon-bow-arc-shot").assertIsDisplayed()
        rule.onNodeWithTag("weapon-bow-coatings").assertIsDisplayed()
        rule.onNodeWithTag("weapon-bow-coating-power").assertIsDisplayed()
        rule.onNodeWithTag("weapon-bow-coating-close_range").assertIsDisplayed()
        rule.onNodeWithTag("weapon-bow-coating-paralysis").assertIsDisplayed()
        rule.onNodeWithTag("weapon-bow-coating-sleep").assertIsDisplayed()
        rule.onNodeWithTag("weapon-bow-coating-row-0").assertExists()
        rule.onNodeWithTag("weapon-bow-coating-row-1").assertExists()
        rule.onNodeWithTag("weapon-bow-coating-row-2").assertExists()
        rule.onNodeWithTag("weapon-bow-coating-row-3").assertExists()
        rule.onNodeWithTag("weapon-bow-coating-row-4").assertDoesNotExist()
        rule.onNodeWithTag("weapon-detail-sharpness-normal-weapon_bow_001").assertDoesNotExist()
    }

    @Test
    fun bowgunCatalogsUseTypedMechanicsAndDetailShowsSharedAmmo() {
        val repository = productionRepository()
        val light = repository.catalog("LIGHT_BOWGUN")
        val heavy = repository.catalog("HEAVY_BOWGUN")
        assertEquals(67, light.weapons.size)
        assertEquals(66, heavy.weapons.size)
        assertTrue(light.weapons.all { it.mechanics is LightBowgunMechanics && it.sharpness == null })
        assertTrue(heavy.weapons.all { it.mechanics is HeavyBowgunMechanics && it.sharpness == null })
        assertEquals(85, light.weapons.sumOf { (it.mechanics as LightBowgunMechanics).rapidFire.size })
        assertEquals(162, heavy.weapons.sumOf { (it.mechanics as HeavyBowgunMechanics).crouchFire.size })
        assertEquals(20, AmmoIconRegistry.mappingCount)
    }

    @Test
    fun lightAndHeavyBowgunDetailsExposeVariantSpecificMechanics() {
        openWeapons()
        rule.onNodeWithTag("weapon-type-light-bowgun").performClick()
        rule.onNodeWithTag("weapon-row-weapon_light_bowgun_001").performClick()
        rule.onNodeWithTag("weapon-lbg-shared").assertIsDisplayed()
        rule.onNodeWithTag("weapon-lbg-ammo-table").assertIsDisplayed()
        rule.onNodeWithTag("weapon-detail-close").performClick()
        rule.onNodeWithTag("weapons-back").performClick()
        rule.onNodeWithTag("weapon-type-heavy-bowgun").performClick()
        rule.onNodeWithTag("weapon-row-weapon_heavy_bowgun_001").performClick()
        rule.onNodeWithTag("weapon-hbg-shared").assertIsDisplayed()
        rule.onNodeWithTag("weapon-hbg-crouch-fire").assertIsDisplayed()
    }

    @Test
    fun lanceUsesCorrectAuthenticShieldCellAcrossAllRarities() {
        val icons = (1..7).map { rarity -> requireNotNull(WeaponIconRegistry.resolve("LANCE", rarity)) }
        assertTrue(icons.all { it.sourceRectPx == "x=68,y=382,width=54,height=62" })
        assertFalse(icons.any { it.sourceRectPx == "x=324,y=382,width=54,height=62" })
        assertEquals("F561A1DA2E890F6523DB333EB5097E1CE5FD3AA7F0175DA2827B745A706B0022", icons.first().extractedSha256)
        assertEquals(7, icons.map { it.resourceId }.distinct().size)
    }

    @Test
    fun bowUsesAuthenticMhp3AtlasCellAcrossAllRarities() {
        val icons = (1..7).map { rarity -> requireNotNull(WeaponIconRegistry.resolve("BOW", rarity)) }
        assertTrue(icons.all { it.sourceRectPx == "x=324,y=382,width=54,height=62" })
        assertTrue(icons.all { it.extractedDimensionsPx == "54x62" })
        assertEquals("ECCBC7D619F033D60DD76F6B229BDE69ACF08E0FEABB4F6FC243126E04195B2D", icons.first().extractedSha256)
        assertEquals(7, icons.map { it.resourceId }.distinct().size)
    }

    @Test
    fun huntingHornDetailShowsSourceOrderedNotesAndSharedSongs() {
        openWeapons()
        rule.onNodeWithTag("weapon-type-hunting-horn").performClick()
        rule.onNodeWithTag("weapon-row-weapon_hunting_horn_001").performClick()
        rule.onNodeWithTag("weapon-hh-notes").assertIsDisplayed()
        rule.onNodeWithTag("weapon-hh-note-order").assertIsDisplayed()
        rule.onNodeWithTag("weapon-hh-note-0-WHITE").assertIsDisplayed()
        rule.onNodeWithTag("weapon-hh-note-1-BLUE").assertIsDisplayed()
        rule.onNodeWithTag("weapon-hh-note-2-RED").assertIsDisplayed()
        rule.onNodeWithTag("weapon-hh-songs").assertIsDisplayed()
        rule.onNodeWithTag("weapon-hh-song-list").assertIsDisplayed()
    }

    @Test
    fun gunlanceAndSwitchAxeDetailsShowTypedMechanicsWithoutReplacingCommonSpecial() {
        openWeapons()
        rule.onNodeWithTag("weapon-type-gunlance").performClick()
        rule.onNodeWithTag("weapon-row-weapon_gunlance_001").performClick()
        rule.onNodeWithTag("weapon-gl-shelling").assertIsDisplayed()
        rule.onNodeWithText("Normal Lv 1").assertIsDisplayed()
        rule.onNodeWithTag("weapon-detail-close").performClick()
        rule.onNodeWithTag("weapons-back").performClick()
        rule.onNodeWithTag("weapon-type-switch-axe").performClick()
        rule.onNodeWithTag("weapon-row-weapon_switch_axe_001").performClick()
        rule.onNodeWithTag("weapon-sa-phial").assertIsDisplayed()
        rule.onNodeWithText("Power").assertIsDisplayed()
    }

    @Test
    fun longSwordForestStartsWithFourteenRootsAndSearchIsFlat() {
        openWeapons()
        rule.onNodeWithTag("weapon-type-long-sword").performClick()
        rule.onNodeWithTag("weapon-production-long_sword").assertIsDisplayed()
        rule.onNodeWithTag("weapon-row-weapon_long_sword_001").assertIsDisplayed()
        rule.onNodeWithTag("weapon-row-weapon_long_sword_002").assertDoesNotExist()
        rule.onNodeWithTag("weapon-search").performTextInput("Tessaiga")
        rule.onNodeWithTag("weapon-search-result-weapon_long_sword_082").assertIsDisplayed()
        rule.onNodeWithText("Tessaiga ").assertDoesNotExist()
    }

    @Test
    fun longSwordForgeRecipeRetainsSourcePayloadWithoutOrphanUpgrade() {
        val anomaly = productionRepository().catalog("LONG_SWORD").weaponById.getValue("weapon_long_sword_064")
        assertEquals(null, anomaly.upgradeFrom)
        assertEquals(66666, anomaly.forgeRecipe?.zenny)
        assertTrue(anomaly.orphanUpgradeRecipe == null)
        assertEquals("Reaver \"Cruelty\"", anomaly.name)
    }

    @Test
    fun longSwordRegistryAndCanonicalNamesAreComplete() {
        val repository = productionRepository().catalog("LONG_SWORD")
        assertEquals(85, repository.weapons.size)
        assertEquals(7, (1..7).count { WeaponIconRegistry.resolve("LONG_SWORD", it) != null })
        assertEquals("Tigrine Edge", repository.weaponById.getValue("weapon_long_sword_018").name)
        assertEquals("Light Works", repository.weaponById.getValue("weapon_long_sword_084").name)
        assertEquals("Tessaiga", repository.weaponById.getValue("weapon_long_sword_082").name)
        assertEquals("Tessaiga ", repository.weaponById.getValue("weapon_long_sword_082").displayName)
    }

    @Test
    fun searchUsesTmoCanonicalNamesAndClearRestoresForest() {
        openTree()
        rule.onNodeWithTag("weapon-search").performTextInput("Hidden Blaze")
        rule.onNodeWithTag("weapon-search-results").assertIsDisplayed()
        rule.onNodeWithTag("weapon-search-result-weapon_great_sword_018").assertIsDisplayed()
        rule.onNodeWithText("Hidden Blade").assertDoesNotExist()
        rule.onNodeWithTag("weapon-search-clear").performClick()
        rule.onNodeWithTag("weapon-tree").assertIsDisplayed()
        rule.onNodeWithTag("weapon-row-weapon_great_sword_018").assertDoesNotExist()
    }

    @Test
    fun expansionAndRowTapAreSeparateAndDetailShowsRarityIcon() {
        openTree()
        rule.onNodeWithTag("weapon-expand-weapon_great_sword_001").performClick()
        rule.onNodeWithTag("weapon-row-weapon_great_sword_002").performClick()
        rule.onNodeWithTag("weapon-detail-overlay").assertIsDisplayed()
        rule.onNodeWithTag("weapon-detail-title-weapon_great_sword_002").assertIsDisplayed()
        rule.onNodeWithTag("weapon-detail-icon-weapon_great_sword_002").assertIsDisplayed()
        rule.onNodeWithTag("weapon-detail-close").performClick()
        rule.onNodeWithTag("weapon-tree").assertIsDisplayed()
    }

    @Test
    fun recipeIngredientOpensItemDetailAndBackRestoresWeaponDetail() {
        openTree()
        rule.onNodeWithTag("weapon-expand-weapon_great_sword_001").performClick()
        rule.onNodeWithTag("weapon-row-weapon_great_sword_002").performClick()
        rule.onNodeWithTag("weapon-forge-materials-item-183-0").performScrollTo().performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("weapon-detail-overlay").assertIsDisplayed()
        rule.onNodeWithTag("weapon-detail-title-weapon_great_sword_002").assertIsDisplayed()
    }

    @Test
    fun weaponDetailShowsDerivedUpgradePathAndKeepsForwardUpgrades() {
        openWeapons()
        rule.onNodeWithTag("weapon-type-dual-blades").performClick()
        rule.onNodeWithTag("weapon-search").performTextInput("Matched Slicers")
        rule.onNodeWithTag("weapon-search-result-weapon_dual_blades_008").performClick()
        rule.onNodeWithTag("weapon-upgrade-path").assertExists()
        // Upgrade path is deliberately lazy and collapsed on first open.
        rule.onNodeWithTag("weapon-ancestry-node-weapon_dual_blades_002").assertDoesNotExist()
        rule.onNodeWithTag("weapon-upgrade-path").performScrollTo().performClick()
        rule.onNodeWithTag("weapon-ancestry-node-weapon_dual_blades_002").assertExists()
        rule.onNodeWithTag("weapon-ancestry-node-weapon_dual_blades_008").assertExists()
        rule.onNodeWithTag("weapon-ancestry-current").assertExists()
        rule.onNodeWithTag("weapon-ancestry-direct-forge-weapon_dual_blades_008").assertExists()
        rule.onNodeWithTag("weapon-ancestry-recipe-weapon_dual_blades_008-materials-item-218-0").assertExists()
        rule.onNodeWithTag("weapon-upgrade-to-weapon_dual_blades_009").assertExists()
    }

    @Test
    fun progressionCardsUseCanonicalRanksAndSharedResponsiveDividers() {
        openTree()
        rule.onNodeWithTag("weapon-expand-weapon_great_sword_001").performClick()
        rule.onNodeWithTag("weapon-row-weapon_great_sword_002").performClick()

        // Yukumo Great Sword has both acquisition methods and two material
        // columns; both cards therefore share the same responsive/card internals.
        rule.onNodeWithTag("weapon-acquisition-sections").assertIsDisplayed()
        rule.onNodeWithTag("weapon-forge-column").assertIsDisplayed()
        rule.onNodeWithTag("weapon-upgrade-from-weapon_great_sword_002-column").assertIsDisplayed()
        rule.onNodeWithTag("weapon-forge-materials-header-divider").assertIsDisplayed()
        rule.onNodeWithTag("weapon-forge-materials-column-divider").assertIsDisplayed()
        rule.onNodeWithTag("weapon-upgrade-from-weapon_great_sword_002-materials-header-divider").assertIsDisplayed()
        rule.onNodeWithTag("weapon-upgrade-from-weapon_great_sword_002-materials-column-divider").assertIsDisplayed()

        val catalog = productionRepository().catalog("GREAT_SWORD")
        val parent = catalog.weaponById.getValue("weapon_great_sword_001")
        val current = catalog.weaponById.getValue("weapon_great_sword_002")
        assertEquals(1, parent.rarity)
        assertEquals(1, current.rarity)
        rule.onNodeWithTag("weapon-detail-title-weapon_great_sword_002").assertIsDisplayed()
    }

    @Test
    fun allRarityVisualsUseExactLocalMappings() {
        assertEquals(7, (1..7).count { WeaponRarityVisuals.resolve(it).icon.resourceId != 0 })
        assertTrue((1..7).map { WeaponRarityVisuals.resolve(it).icon.resourceId }.distinct().size == 7)
        assertEquals("#2B2118", rarityVisual(1).nameColor.toHexForTest())
        assertEquals("#802D34", rarityVisual(7).nameColor.toHexForTest())
    }

    private fun openWeapons() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-weapons").performClick()
    }

    private fun openTree() {
        openWeapons()
        rule.onNodeWithTag("weapon-type-great-sword").performClick()
    }

    private fun productionRepository(): WeaponRepository {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val data = CompanionRepository(context).data
        return WeaponRepository.fromProduction(data.weapons, data.materials)
    }
}

private fun androidx.compose.ui.graphics.Color.toHexForTest(): String =
    "#%02X%02X%02X".format((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
