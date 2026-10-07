package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.ItemUsageIndex
import com.waillio.mhp3rdcompanion.data.ItemUsageFamily
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
class ItemUsageUiTest {
    @get:Rule val rule = createComposeRule()
    private val repository = CompanionRepository(ApplicationProvider.getApplicationContext())
    private val index = ItemUsageIndex.build(repository.data)
    private val weaponRepository = WeaponRepository.fromProduction(repository.data.weapons, repository.data.materials, repository.data.huntingHornSongCatalog)

    @Before fun reset() { }

    @Test fun ironOreStartsCollapsedWithAllFiveFamilies() {
        rule.setContent { CompanionTheme { ItemUsageSection(index.forItem(218)!!, weaponRepository, {}, {}) } }
        rule.onNodeWithTag("item-usage-summary-218").assertExists()
        rule.onNodeWithTag("item-usage-family-toggle-WEAPONS").assertExists()
        rule.onNodeWithTag("item-usage-target-WEAPONS-${index.forItem(218)!!.families.first { it.family.name == "WEAPONS" }.targets.first().targetId}").assertDoesNotExist()
    }

    @Test fun largeFamilyExpansionShowsPreviewAndFooter() {
        rule.setContent { CompanionTheme { ItemUsageSection(index.forItem(812)!!, weaponRepository, {}, {}) } }
        rule.onNodeWithTag("item-usage-family-toggle-DECORATIONS").performClick()
        rule.onNodeWithTag("item-usage-view-all-DECORATIONS").assertExists()
        rule.onAllNodesWithText("LR", useUnmergedTree = true).assertCountEquals(6)
    }

    @Test fun smallFamilyIsExpandedByDefaultAndRendersRankQuantity() {
        rule.setContent { CompanionTheme { ItemUsageSection(index.forItem(30)!!, weaponRepository, {}, {}) } }
        rule.onNodeWithTag("item-usage-target-DECORATIONS-item_medicine_jewel_2").assertExists()
        rule.onNodeWithTag("item-usage-rank-chip-HR", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("×1", useUnmergedTree = true).assertExists()
    }

    @Test fun scrapUsageDoesNotExposeInternalQuantityEnums() {
        rule.setContent { CompanionTheme { ItemUsageSection(index.forItem(592)!!, weaponRepository, {}, {}) } }
        rule.onNodeWithText("EXACT_PUBLISHED").assertDoesNotExist()
        rule.onNodeWithText("NOT_PUBLISHED").assertDoesNotExist()
    }

    @Test fun weaponAndScrapFamilyIconsRemainDistinct() {
        assertNotEquals(
            usageFamilyIconName(ItemUsageFamily.WEAPONS),
            usageFamilyIconName(ItemUsageFamily.SCRAP_CONVERSION)
        )
    }

    @Test fun roastingAndFarmRowsHideInternalLabels() {
        val roasting = index.forItem(186)!!.families.first { it.family == ItemUsageFamily.ROASTING }
        val roastingSummaries = roasting.targets.flatMap { it.relations }.map { usageRelationSummary(ItemUsageFamily.ROASTING, it) }
        assert(roastingSummaries.any { it.contains("Undercooked") })
        assert(roastingSummaries.any { it.contains("Farm Custom Roaster") })
        assert(roastingSummaries.none { it.contains("UNDERCOOKED") || it.contains("FARM_CUSTOM_ROASTER") })

        val farm = index.forItem(92)!!.families.first { it.family == ItemUsageFamily.FARM }
        val farmSummaries = farm.targets.flatMap { it.relations }.map { usageRelationSummary(ItemUsageFamily.FARM, it) }
        assert(farmSummaries.any { it == "Yield ×1 · 10%" })
        assert(farmSummaries.none { it.contains("farm_field_planted_item") })
    }

    @Test fun roastingVocabularyIsExplicitAndUnknownValuesAreOmitted() {
        assert(roastingResultStateLabel("UNDERCOOKED") == "Undercooked")
        assert(roastingResultStateLabel("WELL_DONE") == "Well Done")
        assert(roastingResultStateLabel("BURNT") == "Burnt")
        assert(roastingContextLabel("FARM_CUSTOM_ROASTER") == "Farm Custom Roaster")
        assert(roastingContextLabel("FIELD_BBQ") == "Field BBQ")
        assert(roastingResultStateLabel("FUTURE_INTERNAL_STATE") == null)
        assert(roastingContextLabel("FUTURE_INTERNAL_CONTEXT") == null)
    }

    @Test fun farmSemanticsUsePublishedYieldRows() {
        val spikeberry = index.forItem(178)!!.families.first { it.family == ItemUsageFamily.FARM }
        assertEquals(listOf("Yield ×1"), spikeberry.targets.flatMap { it.relations }.map { usageRelationSummary(ItemUsageFamily.FARM, it) })
        val huskberry = index.forItem(92)!!.families.first { it.family == ItemUsageFamily.FARM }
        assertTrue(huskberry.targets.flatMap { it.relations }.map { usageRelationSummary(ItemUsageFamily.FARM, it) }.containsAll(listOf("Yield ×1 · 10%", "Yield ×2 · 45%", "Yield ×3 · 40%", "Yield ×4 · 5%")))
        assertTrue(huskberry.targets.all { target -> target.relations.all { it.currentItemGameItemId == 92 && it.outputQuantity != null } })
    }

    @Test fun tradingUsageIsExclusivelyVeggieElder() {
        val usageRelations = repository.data.itemTradeExchangeRelations.filter { it.inputGameItemId != null }
        assertEquals(116, usageRelations.size)
        assertTrue(usageRelations.all { it.mechanism == "VEGGIE_ELDER_ITEM_EXCHANGE" })
        assertEquals("Trading with Veggie Elder", ItemUsageFamily.TRADING.displayLabel)
    }

    @Test fun viewAllOpensModalAndKeepsParentSurface() {
        rule.setContent { CompanionTheme { ItemUsageSection(index.forItem(218)!!, weaponRepository, {}, {}) } }
        rule.onNodeWithTag("item-usage-family-toggle-WEAPONS").performClick()
        rule.onNodeWithTag("item-usage-view-all-WEAPONS").performClick()
        rule.onNodeWithTag("item-usage-view-all-modal").assertExists()
        rule.onNodeWithTag("item-usage-section-218").assertExists()
        rule.onNodeWithTag("item-usage-view-all-modal-list").assertExists()
        rule.onNodeWithTag("item-usage-view-all-modal-close").performClick()
        rule.onNodeWithTag("item-usage-view-all-modal").assertDoesNotExist()
    }
}
