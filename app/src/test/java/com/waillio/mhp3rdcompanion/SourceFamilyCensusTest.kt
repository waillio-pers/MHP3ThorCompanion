package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.GroupedMaterialSources
import com.waillio.mhp3rdcompanion.data.ItemUsageIndex
import com.waillio.mhp3rdcompanion.data.Material
import com.waillio.mhp3rdcompanion.data.groupForDisplay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class SourceFamilyCensusTest {
    private val fixtureData = CompanionRepository(ApplicationProvider.getApplicationContext()).data
    private val materials = fixtureData.materials
    private val prettyJson = Json { prettyPrint = true }

    companion object {
        private const val BASELINE_CENSUS_SHA256 = "5449020A884E4FD97B5840308978FB45027A79E4C40DE4CD5DC5595FC07E9B9D"
    }

    @Test
    fun allRuntimeItemsKeepTheAcceptedOrderedSourceFamilySet() {
        val records = materials.sortedBy(Material::id).map { material ->
            val groups = material.sources.groupForDisplay()
            CensusItem(
                id = material.id,
                gameItemId = material.gameItemId,
                name = material.name,
                familyIdsBefore = groups.orderedSourceFamilyIdsBeforeShell(),
                familyIdsAfter = groups.orderedSourceFamilyIdsAfterShell()
            )
        }
        val zeroSourceIds = records.filter { it.familyIdsBefore.isEmpty() }.map { it.id }
        val familyCounts = records.flatMap { it.familyIdsBefore }.groupingBy { it }.eachCount().toSortedMap()
        val maximumFamilyCount = records.maxOfOrNull { it.familyIdsBefore.size } ?: 0
        val maximumItems = records.filter { it.familyIdsBefore.size == maximumFamilyCount }.map { it.id }
        val membershipChanges = records.count { it.familyIdsBefore.toSet() != it.familyIdsAfter.toSet() }
        val orderChanges = records.count { it.familyIdsBefore != it.familyIdsAfter }
        val expectedOrderChanges = records.count {
            "monster_reward" in it.familyIdsBefore && "palico_expedition" in it.familyIdsBefore
        }
        val usageIndex = ItemUsageIndex.build(fixtureData)
        val baselineItems = buildJsonArray {
            records.forEach { item ->
                add(buildJsonObject {
                    put("stableItemId", item.id)
                    put("gameItemId", item.gameItemId?.let { kotlinx.serialization.json.JsonPrimitive(it) } ?: JsonNull)
                    put("displayName", item.name)
                    put("orderedSourceFamilyIds", JsonArray(item.familyIdsBefore.map { kotlinx.serialization.json.JsonPrimitive(it) }))
                })
            }
        }
        fun bodyWithItems(items: JsonArray) = buildJsonObject {
            put("schemaVersion", 31)
            put("sourceSha256", "657CFA9376FC8CBE56C67E67D4D1A27C455D21021F51E21B62427C8DDBB26155")
            put("itemCount", records.size)
            put("zeroSourceItemCount", zeroSourceIds.size)
            put("zeroSourceItemIds", JsonArray(zeroSourceIds.map { kotlinx.serialization.json.JsonPrimitive(it) }))
            put("familyCountsByItem", buildJsonObject { familyCounts.forEach { (family, count) -> put(family, count) } })
            put("maximumFamilyCount", maximumFamilyCount)
            put("itemsWithMaximumFamilyCount", JsonArray(maximumItems.map { kotlinx.serialization.json.JsonPrimitive(it) }))
            put("items", items)
        }
        fun censusDigest(value: kotlinx.serialization.json.JsonObject): String {
            val compact = Json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), value)
            return MessageDigest.getInstance("SHA-256").digest(compact.toByteArray()).toHex()
        }
        val baselineDigest = censusDigest(bodyWithItems(baselineItems))
        val currentItemsForDigest = buildJsonArray {
            records.forEach { item ->
                add(buildJsonObject {
                    put("stableItemId", item.id)
                    put("gameItemId", item.gameItemId?.let { kotlinx.serialization.json.JsonPrimitive(it) } ?: JsonNull)
                    put("displayName", item.name)
                    put("orderedSourceFamilyIds", JsonArray(item.familyIdsAfter.map { kotlinx.serialization.json.JsonPrimitive(it) }))
                })
            }
        }
        val currentDigest = censusDigest(bodyWithItems(currentItemsForDigest))
        val items = buildJsonArray {
            records.forEach { item ->
                add(buildJsonObject {
                    put("stableItemId", item.id)
                    put("gameItemId", item.gameItemId?.let { kotlinx.serialization.json.JsonPrimitive(it) } ?: JsonNull)
                    put("displayName", item.name)
                    put("orderedSourceFamilyIdsBefore", JsonArray(item.familyIdsBefore.map { kotlinx.serialization.json.JsonPrimitive(it) }))
                    put("orderedSourceFamilyIdsAfter", JsonArray(item.familyIdsAfter.map { kotlinx.serialization.json.JsonPrimitive(it) }))
                })
            }
        }
        val data = buildJsonObject {
            put("schemaVersion", 31)
            put("sourceSha256", "657CFA9376FC8CBE56C67E67D4D1A27C455D21021F51E21B62427C8DDBB26155")
            put("itemCount", records.size)
            put("zeroSourceItemCount", zeroSourceIds.size)
            put("zeroSourceItemIds", JsonArray(zeroSourceIds.map { kotlinx.serialization.json.JsonPrimitive(it) }))
            put("familyCountsByItem", buildJsonObject { familyCounts.forEach { (family, count) -> put(family, count) } })
            put("maximumFamilyCount", maximumFamilyCount)
            put("itemsWithMaximumFamilyCount", JsonArray(maximumItems.map { kotlinx.serialization.json.JsonPrimitive(it) }))
            put("baselineCensusSha256", baselineDigest)
            put("currentCensusSha256", currentDigest)
            put("membershipChanges", membershipChanges)
            put("orderChanges", orderChanges)
            put("items", items)
        }
        val output = File("build/outputs/source-family-census/SOURCE-FAMILY-CENSUS.json")
        output.parentFile?.mkdirs()
        output.writeText(prettyJson.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), data))

        println("SOURCE_FAMILY_CENSUS_BEFORE_SHA256=$baselineDigest")
        println("SOURCE_FAMILY_CENSUS_AFTER_SHA256=$currentDigest")
        println("SOURCE_FAMILY_ZERO_SOURCE_ITEMS=${zeroSourceIds.size}")
        println("SOURCE_FAMILY_COUNTS=$familyCounts")
        println("SOURCE_FAMILY_MAX_COUNT=$maximumFamilyCount ITEMS=$maximumItems")
        println("SOURCE_FAMILY_MEMBERSHIP_CHANGES=$membershipChanges ORDER_CHANGES=$orderChanges")
        println("SOURCE_FAMILY_SHORT_EXAMPLE=${materials.single { it.id == "item_absorber_jewel_1" }.name}")
        println("SOURCE_FAMILY_MULTI_COMPACT_CANDIDATES=" + materials.mapNotNull { item ->
            val families = item.sources.groupForDisplay().orderedSourceFamilyIdsBeforeShell()
            item.takeIf { families.size in 2..3 && item.sources.size <= 12 }?.let {
                "${it.id}|${it.name}|families=${families.joinToString(",")}|rows=${it.sources.size}|usage=${it.gameItemId?.let(usageIndex::forItem)?.families?.size ?: 0}"
            }
        }.take(20))
        println("SOURCE_FAMILY_MAX_WITH_USAGE=" + materials.mapNotNull { item ->
            val families = item.sources.groupForDisplay().orderedSourceFamilyIdsBeforeShell()
            val usages = item.gameItemId?.let(usageIndex::forItem)?.families?.size ?: 0
            item.takeIf { families.size == maximumFamilyCount && usages > 0 }?.let { "${it.id}|${it.name}|usageFamilies=$usages" }
        })
        println("SOURCE_FAMILY_CENSUS_OUTPUT=${output.absolutePath}")
        assertEquals("Production Item census row count", 978, records.size)
        assertEquals("Production zero-source Item count", 15, zeroSourceIds.size)
        assertEquals("Pre-shell family census baseline", BASELINE_CENSUS_SHA256, baselineDigest)
        assertEquals("Source family membership changes", 0, membershipChanges)
        assertEquals("Only items containing both Monster Rewards and Palico may change order", expectedOrderChanges, orderChanges)
        assertTrue("Every changed order places Palico after Monster Rewards", records.filter { it.familyIdsBefore != it.familyIdsAfter }.all {
            it.familyIdsAfter.indexOf("monster_reward") < it.familyIdsAfter.indexOf("palico_expedition")
        })
        assertTrue("Every census row has a stable identity", records.all { it.id.startsWith("item_") })
    }
}

private data class CensusItem(
    val id: String,
    val gameItemId: Int?,
    val name: String,
    val familyIdsBefore: List<String>,
    val familyIdsAfter: List<String>
)

private fun GroupedMaterialSources.orderedSourceFamilyIdsBeforeShell(): List<String> = buildList {
    if (field.isNotEmpty()) add("field")
    if (combinations.isNotEmpty() || combinationFailures.isNotEmpty()) add("combination")
    if (decorationCrafting.isNotEmpty()) add("decoration_crafting")
    if (roasting.isNotEmpty()) add("roasting")
    if (invasionRewards.isNotEmpty()) add("invasion_reward")
    if (palicoExpeditions.isNotEmpty()) add("palico_expedition")
    if (monsters.isNotEmpty()) add("monster_reward")
    if (smallMonsters.isNotEmpty()) add("small_monster")
    if (supplyBoxes.isNotEmpty()) add("supply_box")
    if (quests.isNotEmpty()) add("quest_reward")
    if (specialFree.isNotEmpty()) add("special")
    if (trainingRewards.isNotEmpty()) add("training_reward")
    if (farm.isNotEmpty()) add("farm")
    if (trade.isNotEmpty()) add("trading")
    if (shop.isNotEmpty()) add("shop")
    if (scrapConversions.isNotEmpty()) add("scrap_conversion")
}

/** Current SourceFamilyBlock dispatch sequence with Palico Expedition after Monster Rewards. */
private fun GroupedMaterialSources.orderedSourceFamilyIdsAfterShell(): List<String> = buildList {
    if (field.isNotEmpty()) add("field")
    if (combinations.isNotEmpty() || combinationFailures.isNotEmpty()) add("combination")
    if (decorationCrafting.isNotEmpty()) add("decoration_crafting")
    if (roasting.isNotEmpty()) add("roasting")
    if (invasionRewards.isNotEmpty()) add("invasion_reward")
    if (monsters.isNotEmpty()) add("monster_reward")
    if (palicoExpeditions.isNotEmpty()) add("palico_expedition")
    if (smallMonsters.isNotEmpty()) add("small_monster")
    if (supplyBoxes.isNotEmpty()) add("supply_box")
    if (quests.isNotEmpty()) add("quest_reward")
    if (specialFree.isNotEmpty()) add("special")
    if (trainingRewards.isNotEmpty()) add("training_reward")
    if (farm.isNotEmpty()) add("farm")
    if (trade.isNotEmpty()) add("trading")
    if (shop.isNotEmpty()) add("shop")
    if (scrapConversions.isNotEmpty()) add("scrap_conversion")
}

private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }
