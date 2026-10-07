package com.waillio.mhp3rdcompanion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.GeneratedDataset
import com.waillio.mhp3rdcompanion.data.ItemUsageFamily
import com.waillio.mhp3rdcompanion.data.ItemUsageIndex
import com.waillio.mhp3rdcompanion.data.MaterialSource
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoastingSourcePresentationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = CompanionRepository(context)

    @Test
    fun sourceComparatorPreservesAllThirtyForwardRowsAndMatchesInverseUsage() {
        val expected = frozenDataset().roastingConversions.map { conversion ->
            RoastingRouteFact(
                relationId = conversion.id,
                inputGameItemId = conversion.inputGameItemId,
                inputQuantity = conversion.inputQuantity,
                outputGameItemId = conversion.outputGameItemId,
                outputQuantity = conversion.outputQuantity,
                context = conversion.context,
                resultState = conversion.resultState
            )
        }
        val sourceProjection = repository.data.materials.flatMap { it.sources }
            .filter { it.type == MaterialSourceType.ROASTING && it.id.endsWith("_output") }
            .map { source -> source.toRouteFact() }
        val usageProjection = ItemUsageIndex.build(repository.data).allRelations
            .filter { it.family == ItemUsageFamily.ROASTING }
            .map { relation ->
                RoastingRouteFact(
                    relationId = relation.id,
                    inputGameItemId = relation.currentItemGameItemId,
                    inputQuantity = relation.quantity,
                    outputGameItemId = requireNotNull(relation.targetGameItemId),
                    outputQuantity = relation.outputQuantity,
                    context = relation.context.orEmpty(),
                    resultState = relation.resultState.orEmpty()
                )
            }

        assertEquals(30, expected.size)
        assertEquals(30, sourceProjection.size)
        assertEquals(30, usageProjection.size)
        assertEquals(emptyList<RoastingRouteDifference>(), compareRoastingRoutes(expected, sourceProjection))
        assertEquals(emptyList<RoastingRouteDifference>(), compareRoastingRoutes(expected, usageProjection))
    }

    @Test
    fun focusedComparatorPoisonsInputOutputQuantityAndContextState() {
        val expected = frozenDataset().roastingConversions.map { conversion ->
            RoastingRouteFact(
                conversion.id,
                conversion.inputGameItemId,
                conversion.inputQuantity,
                conversion.outputGameItemId,
                conversion.outputQuantity,
                conversion.context,
                conversion.resultState
            )
        }
        val original = repository.data.materials.flatMap { it.sources }
            .filter { it.type == MaterialSourceType.ROASTING && it.id.endsWith("_output") }
            .map { it.toRouteFact() }
        val targetId = expected.first().relationId

        val wrongInput = original.map { if (it.relationId == targetId) it.copy(inputGameItemId = 186) else it }
        assertEquals(listOf("inputGameItemId"), compareRoastingRoutes(expected, wrongInput).map { it.field }.distinct())

        val wrongOutputQuantity = original.map { if (it.relationId == targetId) it.copy(outputQuantity = 7) else it }
        assertEquals(listOf("outputQuantity"), compareRoastingRoutes(expected, wrongOutputQuantity).map { it.field }.distinct())

        val wrongContextAndState = original.map {
            if (it.relationId == targetId) it.copy(context = "FARM_CUSTOM_ROASTER", resultState = "BURNT") else it
        }
        assertEquals(
            setOf("context", "resultState"),
            compareRoastingRoutes(expected, wrongContextAndState).map { it.field }.toSet()
        )
    }

    @Test
    fun outputFortyThreeGroupsRawMeatOnceButKeepsBothContextRoutes() {
        val routes = routesForOutput(43)
        val groups = roastingInputDisplayGroups(routes)

        assertEquals(2, routes.size)
        assertEquals(1, groups.size)
        assertEquals(39, groups.single().inputGameItemId)
        assertEquals("item_raw_meat", repository.data.materials.single { it.gameItemId == 39 }.id)
        assertEquals(1, groups.single().inputQuantity)
        assertEquals(
            listOf("FIELD_BBQ", "FARM_CUSTOM_ROASTER"),
            groups.single().routes.mapNotNull { it.context }
        )
        assertEquals(
            listOf(
                "Field BBQ · Undercooked · Produces ×1",
                "Farm Custom Roaster · Undercooked · Produces ×1"
            ),
            groups.single().routes.map(::roastingRouteSummary)
        )
        assertTrue(groups.single().routes.all { source ->
            val summary = roastingRouteSummary(source)
            !summary.contains("%") && !summary.contains("FIELD_BBQ") && !summary.contains("FARM_CUSTOM_ROASTER")
        })
    }

    @Test
    fun maximumRouteOutputUsesSixInlineRoutesAndKeepsEightDistinctInputs() {
        val routes = routesForOutput(48)
        val groups = roastingInputDisplayGroups(routes)

        assertEquals(8, routes.size)
        assertEquals(8, groups.size)
        assertEquals(8, groups.map { it.inputGameItemId }.distinct().size)
        assertEquals(6, roastingInputDisplayGroups(routes.take(ROASTING_INLINE_ROUTE_LIMIT)).sumOf { it.routes.size })
        assertEquals("Farm Custom Roaster · Undercooked · Produces ×1", roastingRouteSummary(routes.first()))
    }

    private fun routesForOutput(outputGameItemId: Int): List<MaterialSource> =
        roastingRoutesForOutput(repository.data.materials.flatMap { it.sources }, outputGameItemId)

    private fun frozenDataset(): GeneratedDataset = context.assets.open("mhp3rd-data.json")
        .bufferedReader()
        .use { Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(it.readText()) }

    private fun MaterialSource.toRouteFact() = RoastingRouteFact(
        relationId = roastingRawRelationId(this),
        inputGameItemId = requireNotNull(inputGameItemId),
        inputQuantity = roastingInputQuantity,
        outputGameItemId = requireNotNull(outputGameItemId),
        outputQuantity = quantity,
        context = context.orEmpty(),
        resultState = resultState.orEmpty()
    )
}

private data class RoastingRouteFact(
    val relationId: String,
    val inputGameItemId: Int,
    val inputQuantity: Int?,
    val outputGameItemId: Int,
    val outputQuantity: Int?,
    val context: String,
    val resultState: String
)

private data class RoastingRouteDifference(
    val relationId: String,
    val field: String,
    val expected: Any?,
    val actual: Any?
)

/** Same exact fact comparator is used for source parity and its three poisons. */
private fun compareRoastingRoutes(
    expected: List<RoastingRouteFact>,
    actual: List<RoastingRouteFact>
): List<RoastingRouteDifference> {
    val differences = mutableListOf<RoastingRouteDifference>()
    val expectedById = expected.groupBy { it.relationId }
    val actualById = actual.groupBy { it.relationId }
    (expectedById.keys + actualById.keys).toSortedSet().forEach { relationId ->
        val expectedRows = expectedById[relationId].orEmpty()
        val actualRows = actualById[relationId].orEmpty()
        if (expectedRows.size != 1 || actualRows.size != 1) {
            differences += RoastingRouteDifference(relationId, "relationIdentity", expectedRows.size, actualRows.size)
        } else {
            val expectedRow = expectedRows.single()
            val actualRow = actualRows.single()
            listOf(
                "inputGameItemId" to (expectedRow.inputGameItemId to actualRow.inputGameItemId),
                "inputQuantity" to (expectedRow.inputQuantity to actualRow.inputQuantity),
                "outputGameItemId" to (expectedRow.outputGameItemId to actualRow.outputGameItemId),
                "outputQuantity" to (expectedRow.outputQuantity to actualRow.outputQuantity),
                "context" to (expectedRow.context to actualRow.context),
                "resultState" to (expectedRow.resultState to actualRow.resultState)
            ).forEach { (field, values) ->
                if (values.first != values.second) {
                    differences += RoastingRouteDifference(relationId, field, values.first, values.second)
                }
            }
        }
    }
    return differences
}
