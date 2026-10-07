package com.waillio.mhp3rdcompanion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.GeneratedDataset
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ItemShopProductionIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun productionShopArraysHaveAcceptedCensus() {
        val generated = context.assets.open("mhp3rd-data.json").bufferedReader().use { json.decodeFromString<GeneratedDataset>(it.readText()) }
        assertEquals(31, generated.schemaVersion)
        assertEquals(341, generated.itemShopPurchaseRelations.size)
        assertEquals(147, generated.itemShopPurchaseRelations.map { it.gameItemId }.distinct().size)
        assertEquals(5, generated.itemShopPeddlerProfiles.size)
        assertEquals(7, generated.itemShopUnlockConditions.size)
        assertEquals(25, generated.itemShopPurchaseRelations.single { it.gameItemId == 121 && it.shopId == "GENERAL_STORE" }.priceZenny)
        assertTrue(generated.itemShopPurchaseRelations.all { it.shopId in setOf("GENERAL_STORE", "HUNTERS_STORE", "PEDDLER") })
    }

    @Test fun repositoryProjectsShopReverseSourcesWithoutInventedRotation() {
        val data = CompanionRepository(context).data
        val herb = data.materials.single { it.gameItemId == 151 }
        val shop = herb.sources.filter { it.type == MaterialSourceType.SHOP_PURCHASE }
        assertTrue(shop.any { it.shopId == "GENERAL_STORE" })
        assertTrue(shop.any { it.shopId == "HUNTERS_STORE" })
        assertTrue(shop.any { it.shopInventoryProfileId == "PEDDLER_PATTERN_1" })
        assertTrue(shop.none { it.shopSelectionSemantics != null && it.shopSelectionSemantics != "SOURCE_UNSPECIFIED" })
        assertTrue(shop.all { it.priceZenny != null })
    }
}
