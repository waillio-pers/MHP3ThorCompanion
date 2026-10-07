package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.ItemUsageFamily
import com.waillio.mhp3rdcompanion.data.ItemUsageIndex
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
class TradeSourcesUiTest {
    @get:Rule val rule = createComposeRule()
    private val repository = CompanionRepository(ApplicationProvider.getApplicationContext())

    @Test
    fun uniqueMushroomShowsSixRoutePreviewAndFullFifteenRouteModal() {
        val material = repository.data.materials.single { it.gameItemId == 687 }
        val trade = material.sources.filter { it.type == MaterialSourceType.TRADE }
        rule.setContent {
            CompanionTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    TradeSourceRows(trade, {}, { gameItemId -> repository.data.materials.firstOrNull { it.gameItemId == gameItemId }?.id })
                }
            }
        }
        rule.onNodeWithText("Trading · 15 routes").assertExists()
        rule.onNodeWithText("Veggie Elder").assertExists()
        rule.onNodeWithTag("item-trading-view-all").assertExists().performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("item-trading-view-all-modal").assertExists()
        rule.onNodeWithText("Veggie Elder · 15 routes").assertExists()
        rule.onNodeWithTag("item-trading-view-all-list").assertExists()
        rule.onNodeWithTag("item-trading-view-all-close").performClick()
        rule.onNodeWithTag("item-trading-view-all-modal").assertDoesNotExist()
        rule.onNodeWithText("Trading · 15 routes").assertExists()
    }

    @Test
    fun farmManagerUsesCurrencyAndHumanAvailabilityCondition() {
        val material = repository.data.materials.single { it.gameItemId == 811 }
        val trade = material.sources.filter { it.type == MaterialSourceType.TRADE }
        rule.setContent { CompanionTheme { TradeSourceRows(trade, {}, { null }) } }
        rule.onNodeWithText("Farm Manager").assertExists()
        rule.onNodeWithText("Cost 500 pts · Receive ×1 · Initial exchange", substring = true).assertExists()
        rule.onNodeWithText("Cost ×500").assertDoesNotExist()
    }

    @Test
    fun usageTradingRendersBothMapDistinctRoutesWithSharedMetadata() {
        val index = ItemUsageIndex.build(repository.data)
        val current = repository.data.materials.single { it.gameItemId == 45 }
        val projection = index.forItem(45)!!.families.single { it.family == ItemUsageFamily.TRADING }
        val focusedTarget = projection.targets.single { it.targetGameItemId == 687 }
        val focused = projection.copy(targets = listOf(focusedTarget), rawRelationCount = focusedTarget.relations.size)
        rule.setContent { CompanionTheme { UsageFamilyScreen(current, focused, null, {}, {}) } }
        rule.onNodeWithText("Receive ×1 · Cost ×1").assertExists()
        rule.onNodeWithText("Flooded Forest · Base Camp · Gathering quests · before completion", substring = true).assertExists()
        rule.onNodeWithText("Deserted Island · Area 4 · Gathering quests · before completion", substring = true).assertExists()
        assertEquals(2, focusedTarget.relations.size)
    }
}
