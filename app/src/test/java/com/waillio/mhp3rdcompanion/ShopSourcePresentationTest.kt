package com.waillio.mhp3rdcompanion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.GeneratedDataset
import com.waillio.mhp3rdcompanion.data.GeneratedItemShopPurchaseRelation
import com.waillio.mhp3rdcompanion.data.MaterialSource
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShopSourcePresentationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun allRawPurchaseRelationsMatchTheRepositoryProjectionExactly() {
        val generated = generatedDataset()
        val expected = generated.itemShopPurchaseRelations.map { it.toFact() }
        val actual = CompanionRepository(context).data.materials.flatMap { material ->
            material.sources.filter { it.type == MaterialSourceType.SHOP_PURCHASE }.map { it.toFact() }
        }
        val expectedById = expected.associateBy { it.id }
        val actualById = actual.associateBy { it.id }
        val missing = expectedById.keys - actualById.keys
        val extra = actualById.keys - expectedById.keys
        val mismatches = expectedById.keys.intersect(actualById.keys).filter { expectedById[it] != actualById[it] }

        assertEquals(341, expected.size)
        assertEquals(341, actual.size)
        assertEquals(341, expectedById.size)
        assertEquals(147, generated.itemShopPurchaseRelations.map { it.gameItemId }.distinct().size)
        assertTrue("missing=${missing.take(5)}", missing.isEmpty())
        assertTrue("extra=${extra.take(5)}", extra.isEmpty())
        assertTrue("mismatches=${mismatches.take(5)}", mismatches.isEmpty())
        assertTrue(generated.itemShopPurchaseRelations.all { it.priceZenny > 0 })

        val counts = generated.itemShopPurchaseRelations.groupingBy { it.shopId }.eachCount()
        assertEquals(mapOf("GENERAL_STORE" to 86, "HUNTERS_STORE" to 86, "PEDDLER" to 169), counts)
        val peddlerCounts = generated.itemShopPurchaseRelations.filter { it.shopId == "PEDDLER" }
            .groupingBy { it.inventoryProfileId }.eachCount()
        assertEquals(
            mapOf(
                "PEDDLER_PATTERN_1" to 29,
                "PEDDLER_PATTERN_2" to 27,
                "PEDDLER_PATTERN_3" to 25,
                "PEDDLER_PATTERN_4" to 34,
                "PEDDLER_PATTERN_5" to 54
            ),
            peddlerCounts
        )
        assertEquals(25, generated.itemShopPurchaseRelations.single { it.gameItemId == 121 && it.shopId == "GENERAL_STORE" }.priceZenny)
        println("SHOP_COMPARATOR expected=341 actual=341 missing=0 extra=0 mismatches=0 items=147")
    }

    @Test
    fun generalAndHuntersAggregateOnlyWhenAllPurchaseFactsMatch() {
        val herb = CompanionRepository(context).data.materials.single { it.gameItemId == 151 }
        val purchaseRows = herb.sources.filter { it.type == MaterialSourceType.SHOP_PURCHASE }
        val projection = shopSourcesPresentation(purchaseRows)
        val combined = projection.storeRows.single()

        assertEquals(4, projection.relationCount)
        assertEquals("General Store / Hunter's Store", combined.label)
        assertEquals(listOf("item_shop_purchase_001", "item_shop_purchase_087"), combined.relationIds)
        assertEquals("20z", combined.priceLabel)
        assertEquals("Initial stock", combined.availabilityLabel)
        assertEquals(2, projection.peddlerRows.size)

        val storeRows = purchaseRows.filter { it.shopId != "PEDDLER" }
        val pricePoison = storeRows.mapIndexed { index, source ->
            if (index == 0) source.copy(priceZenny = requireNotNull(source.priceZenny) + 1) else source
        }
        assertEquals(2, shopSourcesPresentation(pricePoison).storeRows.size)

        val conditionPoison = storeRows.mapIndexed { index, source ->
            if (index == 0) source.copy(
                shopAvailabilityConditionId = "UNLOCK_A",
                shopAvailabilityConditionSemantics = "OR",
                condition = "Village ★4 / Guild ★4 shop expansion"
            ) else source
        }
        assertEquals(2, shopSourcesPresentation(conditionPoison).storeRows.size)

        val itemIdentityPoison = storeRows.mapIndexed { index, source ->
            if (index == 0) source.copy(shopGameItemId = 152) else source
        }
        assertEquals(2, shopSourcesPresentation(itemIdentityPoison).storeRows.size)
    }

    @Test
    fun unlockConditionSemanticsRemainHumanReadableAndDistinct() {
        val data = CompanionRepository(context).data
        val sourceByCondition = data.materials.flatMap { it.sources }
            .filter { it.type == MaterialSourceType.SHOP_PURCHASE && it.shopAvailabilityConditionId != null }
            .associateBy { it.shopAvailabilityConditionId }

        assertEquals("Either: Village ★4 or Guild ★4 shop expansion", shopAvailabilityLabel(requireNotNull(sourceByCondition["UNLOCK_A"])))
        assertEquals("After clearing: Guild ★5 progression", shopAvailabilityLabel(requireNotNull(sourceByCondition["UNLOCK_D"])))
        val appears = shopAvailabilityLabel(requireNotNull(sourceByCondition["UNLOCK_G"]))
        assertTrue(appears, appears.contains("appearance"))
        assertFalse(appears, appears.contains("After clearing"))
        assertTrue(shopAvailabilityLabel(requireNotNull(sourceByCondition["UNLOCK_C"])).startsWith("Clear or appearance:"))
        assertTrue(shopAvailabilityLabel(requireNotNull(sourceByCondition["UNLOCK_F"])).startsWith("Clear or appearance:"))
    }

    @Test
    fun peddlerProfilesKeepIdentityPriceAndOnlyPublishedQualifiers() {
        val generated = generatedDataset()
        val data = CompanionRepository(context).data
        assertEquals(
            mapOf(
                "PEDDLER_PATTERN_1" to "HALF_PRICE_PROFILE",
                "PEDDLER_PATTERN_2" to "NORMAL_SPECIALTY_PROFILE",
                "PEDDLER_PATTERN_3" to "HALF_PRICE_PROFILE",
                "PEDDLER_PATTERN_4" to "NORMAL_SPECIALTY_PROFILE",
                "PEDDLER_PATTERN_5" to "DOWNLOAD_BONUS_SPECIAL_INVENTORY"
            ),
            generated.itemShopPeddlerProfiles.associate { it.profileId to it.semantics }
        )
        assertTrue(generated.itemShopPeddlerProfiles.all { it.selectionSemantics == "SOURCE_UNSPECIFIED" })
        val expected = mapOf(
            "PEDDLER_PATTERN_1" to ("Inventory 1" to "Half-price inventory"),
            "PEDDLER_PATTERN_2" to ("Inventory 2" to null),
            "PEDDLER_PATTERN_3" to ("Inventory 3" to "Half-price inventory"),
            "PEDDLER_PATTERN_4" to ("Inventory 4" to null),
            "PEDDLER_PATTERN_5" to ("Download bonus inventory" to null)
        )

        expected.forEach { (profileId, expectedLabels) ->
            val relation = generated.itemShopPurchaseRelations.first { it.inventoryProfileId == profileId }
            val material = data.materials.single { it.gameItemId == relation.gameItemId }
            val row = shopSourcesPresentation(material.sources).peddlerRows.single { relation.id in it.relationIds }
            assertEquals(profileId, expectedLabels.first, row.label)
            assertEquals(profileId, expectedLabels.second, row.qualifier)
            assertEquals(profileId, "${formatShopZenny(relation.priceZenny)}z", row.priceLabel)
            if (profileId == "PEDDLER_PATTERN_2" || profileId == "PEDDLER_PATTERN_4") assertEquals(null, row.qualifier)
        }
    }

    @Test
    fun disclosureAndVisibleTextDoNotLeakInternalShopEnums() {
        val data = CompanionRepository(context).data
        val projections = data.materials.map { shopSourcesPresentation(it.sources) }.filter { it.relationCount > 0 }
        assertTrue(projections.maxOf { it.relationCount } <= 6)
        val visibleText = projections.flatMap { projection ->
            (projection.storeRows + projection.peddlerRows).flatMap { row ->
                listOfNotNull(row.label, row.priceLabel, row.availabilityLabel, row.qualifier)
            }
        }
        val rawEnum = Regex("\\b[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)+\\b|\\b[a-z][a-z0-9]*(?:_[a-z0-9]+)+\\b")
        assertTrue(visibleText.none { rawEnum.containsMatchIn(it) })
        assertFalse(visibleText.joinToString(" ").contains("SOURCE_UNSPECIFIED"))
        assertTrue(projections.all { it.relationCount == it.storeRows.sumOf { row -> row.relationIds.size } + it.peddlerRows.sumOf { row -> row.relationIds.size } })
    }

    private fun generatedDataset(): GeneratedDataset = context.assets.open("mhp3rd-data.json")
        .bufferedReader().use { json.decodeFromString(it.readText()) }

    private data class ShopFact(
        val id: String,
        val shopId: String,
        val inventoryProfileId: String?,
        val gameItemId: Int,
        val priceZenny: Int,
        val availabilityConditionId: String?
    )

    private fun GeneratedItemShopPurchaseRelation.toFact() = ShopFact(
        id, shopId, inventoryProfileId, gameItemId, priceZenny, availabilityConditionId
    )

    private fun MaterialSource.toFact() = ShopFact(
        id = id,
        shopId = requireNotNull(shopId),
        inventoryProfileId = shopInventoryProfileId,
        gameItemId = requireNotNull(shopGameItemId),
        priceZenny = requireNotNull(priceZenny),
        availabilityConditionId = shopAvailabilityConditionId
    )
}
