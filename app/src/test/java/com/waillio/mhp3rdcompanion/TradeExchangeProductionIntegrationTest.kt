package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import com.waillio.mhp3rdcompanion.data.ItemUsageIndex
import com.waillio.mhp3rdcompanion.data.ItemUsageFamily
import com.waillio.mhp3rdcompanion.data.tradeRoutePreview
import com.waillio.mhp3rdcompanion.data.tradeSourcesPresentation
import com.waillio.mhp3rdcompanion.data.tradeSourceRouteMetadata
import com.waillio.mhp3rdcompanion.data.tradeUsageRouteMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TradeExchangeProductionIntegrationTest {
    private lateinit var repository: CompanionRepository

    @Before
    fun setUp() {
        repository = CompanionRepository(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun generatedTradeCorpusHasAcceptedCountsAndContexts() {
        val generated = repository.data
        assertEquals(9, generated.itemTradeExchangeRelations.count { it.mechanism == "FARM_MANAGER_POINT_EXCHANGE" })
        assertEquals(116, generated.itemTradeExchangeRelations.count { it.mechanism == "VEGGIE_ELDER_ITEM_EXCHANGE" })
        assertEquals(6, generated.itemTradeExchangeConditions.size)
        assertEquals(6, generated.itemVeggieElderLocations.size)
        val elder = generated.itemTradeExchangeRelations.filter { it.mechanism == "VEGGIE_ELDER_ITEM_EXCHANGE" }
        assertEquals(43, elder.count { it.scopeType == "ALL_SIX_GATHERING_MAPS" })
        assertEquals(73, elder.count { it.scopeType == "MAP_SPECIFIC" })
        assertTrue(elder.all { it.inputQuantity == 1 && it.questContext == "GATHERING_QUESTS_ONLY" && it.interactionWindow == "BEFORE_QUEST_COMPLETION" })
        assertEquals(111, elder.count { it.outputQuantity == 1 })
        assertEquals(4, elder.count { it.outputQuantity == 2 })
        assertEquals(1, elder.count { it.outputQuantity == 3 })
    }

    @Test
    fun outputItemsExposeTradeSourcesWithoutGiftRows() {
        val materials = repository.data.materials
        val tradeSources = materials.flatMap { it.sources }.filter { it.type == MaterialSourceType.TRADE }
        assertEquals(125, tradeSources.size)
        assertEquals(66, materials.count { material -> material.sources.any { it.type == MaterialSourceType.TRADE } })
        val aquaglow = materials.single { it.gameItemId == 811 }
        val farm = aquaglow.sources.single { it.type == MaterialSourceType.TRADE && it.tradeMechanism == "FARM_MANAGER_POINT_EXCHANGE" }
        assertEquals(500, farm.tradeCurrencyCost)
        assertEquals("YUKUMO_POINTS", farm.tradeCurrencyType)
        assertEquals("Initial exchange", farm.condition)
        val yukumoTicket = materials.single { it.gameItemId == 608 }
        val ticketFarm = yukumoTicket.sources.single { it.type == MaterialSourceType.TRADE && it.tradeMechanism == "FARM_MANAGER_POINT_EXCHANGE" }
        assertEquals(5000, ticketFarm.tradeCurrencyCost)
        assertEquals("Guild ★5 progression", ticketFarm.condition)
        val egg = materials.single { it.gameItemId == 51 }
        assertTrue(egg.sources.any { it.type == MaterialSourceType.TRADE && it.inputGameItemId in setOf(608, 609, 610) })
        val multiple = materials.first { it.sources.count { source -> source.type == MaterialSourceType.TRADE && source.tradeMechanism == "VEGGIE_ELDER_ITEM_EXCHANGE" } > 1 }
        assertTrue(multiple.sources.filter { it.type == MaterialSourceType.TRADE }.map { it.inputGameItemId }.distinct().size > 1)
        assertFalse(tradeSources.any { it.inputGameItemId == null && it.tradeMechanism == "VEGGIE_ELDER_ITEM_EXCHANGE" })
    }

    @Test
    fun mapSpecificTradeHasSafeLocationContextAndCommonIsNotDuplicated() {
        val materials = repository.data.materials
        val mapSpecific = materials.flatMap { it.sources }.filter { it.type == MaterialSourceType.TRADE && it.tradeScopeType == "MAP_SPECIFIC" }
        assertEquals(73, mapSpecific.size)
        assertTrue(mapSpecific.all { it.context?.contains("Misty Peaks") == true || it.context?.contains("Sandy Plains") == true || it.context?.contains("Flooded Forest") == true || it.context?.contains("Deserted Island") == true || it.context?.contains("Tundra") == true || it.context?.contains("Volcano") == true })
        val common = materials.flatMap { it.sources }.filter { it.type == MaterialSourceType.TRADE && it.tradeScopeType == "ALL_SIX_GATHERING_MAPS" }
        assertEquals(43, common.size)
        assertTrue(common.all { it.context == "All gathering maps" })
    }

    @Test
    fun exactForwardSourceProjectionCoversAll125RelationsAndExcludesGifts() {
        val generated = repository.data
        val owners = generated.materials.flatMap { material ->
            material.sources.filter { it.type == MaterialSourceType.TRADE }.map { material.gameItemId to it }
        }
        assertEquals(125, owners.size)
        assertEquals(125, owners.map { it.second.id }.toSet().size)
        val actualById = owners.associate { it.second.id to (it.first to it.second) }
        assertEquals(generated.itemTradeExchangeRelations.map { it.id }.toSet(), actualById.keys)
        generated.itemTradeExchangeRelations.forEach { expected ->
            val (outputId, source) = actualById.getValue(expected.id)
            assertEquals(expected.outputGameItemId, outputId)
            assertEquals(expected.mechanism, source.tradeMechanism)
            assertEquals(expected.inputGameItemId, source.inputGameItemId)
            assertEquals(expected.inputQuantity, source.tradeInputQuantity)
            assertEquals(expected.outputQuantity, source.quantity)
            assertEquals(expected.currencyType, source.tradeCurrencyType)
            assertEquals(expected.currencyCost, source.tradeCurrencyCost)
            assertEquals(expected.mapId, source.tradeMapId)
            assertEquals(expected.scopeType, source.tradeScopeType)
            assertEquals(expected.availabilityConditionId, source.tradeAvailabilityConditionId)
            assertEquals(expected.questContext, source.tradeQuestContext)
            assertEquals(expected.interactionWindow, source.tradeInteractionWindow)
            assertEquals(expected.sourceId, source.tradeSourceId)
            assertEquals(expected.sourceUrl, source.sourceUrl)
        }
        val giftIds = generated.materials.flatMap { it.sources }
            .filter { it.type == MaterialSourceType.SPECIAL_FREE && it.specialFreeMechanism == "VEGGIE_ELDER_FREE_GIFT" }
            .map { it.id }.toSet()
        assertEquals(25, giftIds.size)
        assertTrue(giftIds.intersect(actualById.keys).isEmpty())
    }

    @Test
    fun inverseUsageTradingCarriesExactRouteParityAndDoesNotInventCurrencyItems() {
        val generated = repository.data
        val index = ItemUsageIndex.build(generated)
        assertEquals(5125, index.allRelations.size)
        val usage = index.allRelations.filter { it.family == ItemUsageFamily.TRADING }
        val elder = generated.itemTradeExchangeRelations.filter { it.mechanism == "VEGGIE_ELDER_ITEM_EXCHANGE" }
        assertEquals(116, usage.size)
        assertEquals(elder.map { it.id }.toSet(), usage.map { it.id }.toSet())
        val materialsByGameId = generated.materials.mapNotNull { it.gameItemId?.let { id -> id to it } }.toMap()
        val usageById = usage.associateBy { it.id }
        elder.forEach { expected ->
            val actual = usageById.getValue(expected.id)
            assertEquals(expected.inputGameItemId, actual.currentItemGameItemId)
            assertEquals(expected.outputGameItemId, actual.targetGameItemId)
            assertEquals(materialsByGameId.getValue(expected.outputGameItemId).id, actual.targetId)
            assertEquals(expected.inputQuantity, actual.quantity)
            assertEquals(expected.outputQuantity, actual.outputQuantity)
            assertEquals(expected.mapId, actual.mapId)
            assertEquals(expected.scopeType, actual.scopeType)
            assertEquals(expected.availabilityConditionId, actual.availabilityConditionId)
            assertEquals(expected.questContext, actual.questContext)
            assertEquals(expected.interactionWindow, actual.interactionWindow)
            assertEquals(expected.sourceId, actual.sourceId)
            assertEquals(expected.sourceUrl, actual.sourceUrl)
            val outputSource = materialsByGameId.getValue(expected.outputGameItemId).sources.single { it.id == expected.id && it.type == MaterialSourceType.TRADE }
            assertEquals(tradeSourceRouteMetadata(outputSource), tradeUsageRouteMetadata(actual))
        }
        assertTrue(usage.none { it.id in generated.itemTradeExchangeRelations.filter { row -> row.mechanism == "FARM_MANAGER_POINT_EXCHANGE" }.map { row -> row.id } })
        assertTrue(usage.all { it.mapId != null || it.scopeType == "ALL_SIX_GATHERING_MAPS" })
    }

    @Test
    fun tradingPresentationPreservesMapDistinctRoutesAndUniqueMushroomModalCount() {
        val generated = repository.data
        val uniqueMushroom = generated.materials.single { it.gameItemId == 687 }
        val sources = uniqueMushroom.sources.filter { it.type == MaterialSourceType.TRADE }
        val presentation = tradeSourcesPresentation(sources)
        assertEquals(15, presentation.routeCount)
        assertEquals(15, presentation.veggieElderRoutes.size)
        assertEquals(9, presentation.veggieElderInputs.size)
        assertEquals(6, tradeRoutePreview(presentation.veggieElderRoutes).size)
        assertEquals(9, presentation.routeCount - tradeRoutePreview(presentation.veggieElderRoutes).size)
        val burntMeat = sources.filter { it.inputItemName == "Burnt Meat" }
        assertEquals(2, burntMeat.size)
        assertEquals(setOf("FLOODED_FOREST", "DESERTED_ISLAND"), burntMeat.map { it.tradeMapId }.toSet())
        assertEquals(2, burntMeat.map { it.id }.toSet().size)
        assertTrue(presentation.veggieElderRoutes.all { it.tradeInputQuantity != null && it.quantity != null })
    }
}
