package com.waillio.mhp3rdcompanion

import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserAdjudicatedItemIconsTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun allFortySevenExactUserAtlasSelectionsResolveToAuthenticRuntimeBitmaps() {
        val input = requireNotNull(javaClass.getResourceAsStream("/mhp3rd-item-icon-user-adjudication(1).json"))
            .bufferedReader().use { it.readText() }
        val export = Json.parseToJsonElement(input).jsonObject
        assertEquals(1, export.getValue("version").jsonPrimitive.int)
        assertEquals("USER_ADJUDICATED_PHYSICAL_GAME_REFERENCE", export.getValue("provenance").jsonPrimitive.content)
        assertEquals(47, export.getValue("selectedCount").jsonPrimitive.int)
        assertEquals(0, export.getValue("unresolvedCount").jsonPrimitive.int)

        val targets = export.getValue("targets").jsonArray.map { it.jsonObject }
        assertEquals(47, targets.size)
        val ids = targets.map { it.getValue("gameItemId").jsonPrimitive.int }
        assertEquals(47, ids.toSet().size)

        val dataBytes = context.assets.open("mhp3rd-data.json").use { it.readBytes() }
        val dataset = Json { ignoreUnknownKeys = true }.decodeFromString<com.waillio.mhp3rdcompanion.data.GeneratedDataset>(dataBytes.decodeToString())
        val itemIds = dataset.items.map { requireNotNull(it.gameItemId) }.toSet()
        assertEquals(978, itemIds.size)
        assertEquals(978, itemIds.count { ItemIconRegistry.resolve(it) != null })

        val selectedCells = mutableMapOf<String, MutableList<Int>>()
        targets.forEach { target ->
            val itemId = target.getValue("gameItemId").jsonPrimitive.int
            val cell = target.getValue("selectedCellId").jsonPrimitive.content
            val atlas = target.getValue("atlas").jsonPrimitive.content
            val x = target.getValue("x").jsonPrimitive.int
            val y = target.getValue("y").jsonPrimitive.int
            val selectedKey = "atlas:$atlas:$x:$y"
            val icon = ItemIconRegistry.resolve(itemId)
            assertNotNull("User-selected cell $cell did not resolve for Item $itemId", icon)
            assertEquals("Selection identity differs from $cell", selectedKey, icon?.iconKey)
            assertEquals(
                ItemIconRegistry.USER_ADJUDICATED_PHYSICAL_GAME_REFERENCE,
                ItemIconRegistry.provenanceFor(itemId)
            )
            val bitmap = BitmapFactory.decodeResource(context.resources, requireNotNull(icon).resourceId)
            assertNotNull("Missing compiled drawable for $cell", bitmap)
            assertEquals(16, requireNotNull(bitmap).width)
            assertEquals(16, bitmap.height)
            selectedCells.getOrPut(selectedKey) { mutableListOf() }.add(itemId)
        }

        assertEquals(41, selectedCells.size)
        val sharedCellGroups = selectedCells.filterValues { it.size > 1 }
        assertTrue("Selections that share a cell must reuse its runtime bitmap", sharedCellGroups.isNotEmpty())
        sharedCellGroups.forEach { (key, sharedIds) ->
            val resourceIds = sharedIds.map { ItemIconRegistry.resolve(it)?.resourceId }.toSet()
            assertEquals("Duplicate drawable resources were generated for $key", 1, resourceIds.size)
        }
        assertNull("Non-target mappings must not receive user visual provenance", ItemIconRegistry.provenanceFor(1))
    }
}
