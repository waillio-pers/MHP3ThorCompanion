package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.MaterialSource
import com.waillio.mhp3rdcompanion.data.MaterialSourceType

internal const val ROASTING_INLINE_ROUTE_LIMIT = 6

internal data class RoastingInputDisplayGroup(
    val inputGameItemId: Int,
    val inputItemName: String,
    val inputQuantity: Int,
    val routes: List<MaterialSource>
)

/** The forward Source projection is selected by output Item, never by input. */
internal fun roastingRoutesForOutput(
    sources: List<MaterialSource>,
    outputGameItemId: Int
): List<MaterialSource> = sources
    .asSequence()
    .filter { it.type == MaterialSourceType.ROASTING && it.outputGameItemId == outputGameItemId }
    .filter { it.id.endsWith("_output") }
    .filter { it.inputGameItemId != null && it.inputItemName != null }
    .sortedWith(
        compareBy<MaterialSource> { it.inputItemName.orEmpty().lowercase() }
            .thenBy { roastingContextOrder(it.context) }
            .thenBy { it.resultState.orEmpty() }
            .thenBy(::roastingRawRelationId)
    )
    .toList()

/** Groups a consumed Item once while retaining every distinct raw route beneath it. */
internal fun roastingInputDisplayGroups(routes: List<MaterialSource>): List<RoastingInputDisplayGroup> =
    routes.groupBy { requireNotNull(it.inputGameItemId) }
        .map { (inputId, inputRoutes) ->
            val first = inputRoutes.first()
            RoastingInputDisplayGroup(
                inputGameItemId = inputId,
                inputItemName = requireNotNull(first.inputItemName),
                inputQuantity = requireNotNull(first.roastingInputQuantity),
                routes = inputRoutes
            )
        }
        .sortedWith(compareBy<RoastingInputDisplayGroup> { it.inputItemName.lowercase() }.thenBy { it.inputGameItemId })

internal fun roastingRawRelationId(source: MaterialSource): String = source.id.removeSuffix("_output")

internal fun roastingRouteSummary(source: MaterialSource): String = listOfNotNull(
    roastingContextLabel(source.context),
    roastingResultStateLabel(source.resultState),
    source.quantity?.let { "Produces ×$it" }
).joinToString(" · ")

private fun roastingContextOrder(context: String?): Int = when (context) {
    "FIELD_BBQ" -> 0
    "FARM_CUSTOM_ROASTER" -> 1
    else -> Int.MAX_VALUE
}
