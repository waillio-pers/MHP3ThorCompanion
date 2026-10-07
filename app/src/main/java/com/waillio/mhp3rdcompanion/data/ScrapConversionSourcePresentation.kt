package com.waillio.mhp3rdcompanion.data

internal const val SCRAP_SOURCE_INLINE_LIMIT = 6

internal const val SCRAP_TRIGGER_DIRECT_HANDOFF = "DIRECT_PALICO_ARMORY_HANDOFF"
internal const val SCRAP_TRIGGER_HUNTER_GEAR = "HUNTER_GEAR_MATERIAL_CONSUMPTION"

internal fun scrapConversionRelationId(inputGameItemId: Int, outputScrapGameItemId: Int): String =
    "scrap:$inputGameItemId:$outputScrapGameItemId"

internal data class ScrapConversionSourceFact(
    val relationId: String,
    val inputGameItemId: Int,
    val inputQuantity: Int?,
    val outputScrapGameItemId: Int,
    val outputQuantity: Int?,
    val quantitySemantics: String,
    val triggerModes: List<String>
)

internal data class ScrapConversionSourceComparison(
    val expectedCount: Int,
    val actualCount: Int,
    val missingRelationIds: List<String>,
    val extraRelationIds: List<String>,
    val mismatchedRelationIds: List<String>
) {
    val exact: Boolean
        get() = expectedCount == actualCount && missingRelationIds.isEmpty() &&
            extraRelationIds.isEmpty() && mismatchedRelationIds.isEmpty()
}

internal data class ScrapConversionSourcePresentation(
    val outputScrapGameItemId: Int,
    /** Raw source order is retained; equal output/yield never merges distinct materials. */
    val rows: List<MaterialSource>,
    val materialCount: Int,
    val inlineRows: List<MaterialSource>,
    val hiddenMaterialCount: Int,
    val triggerSummary: String?
)

internal fun GeneratedItemScrapConversionRule.toScrapConversionSourceFact() =
    ScrapConversionSourceFact(
        relationId = scrapConversionRelationId(inputGameItemId, outputScrapGameItemId),
        inputGameItemId = inputGameItemId,
        inputQuantity = inputQuantity,
        outputScrapGameItemId = outputScrapGameItemId,
        outputQuantity = outputQuantity,
        quantitySemantics = quantitySemantics,
        triggerModes = triggerModes
    )

internal fun MaterialSource.toScrapConversionSourceFactOrNull(): ScrapConversionSourceFact? {
    if (type != MaterialSourceType.SCRAP_CONVERSION) return null
    val inputId = inputGameItemId ?: return null
    val outputId = scrapOutputGameItemId ?: return null
    return ScrapConversionSourceFact(
        relationId = id,
        inputGameItemId = inputId,
        inputQuantity = scrapInputQuantity,
        outputScrapGameItemId = outputId,
        outputQuantity = quantity,
        quantitySemantics = quantitySemantics.orEmpty(),
        triggerModes = scrapTriggerModes
    )
}

internal fun compareScrapConversionSourceFacts(
    expected: List<ScrapConversionSourceFact>,
    actual: List<ScrapConversionSourceFact>
): ScrapConversionSourceComparison {
    val expectedById = expected.groupBy { it.relationId }
    val actualById = actual.groupBy { it.relationId }
    val missing = expectedById.keys.filter { it !in actualById }.sorted()
    val extra = actualById.keys.filter { it !in expectedById }.sorted()
    val mismatched = expectedById.keys.intersect(actualById.keys).filter { id ->
        expectedById.getValue(id) != actualById.getValue(id)
    }.sorted()
    return ScrapConversionSourceComparison(
        expectedCount = expected.size,
        actualCount = actual.size,
        missingRelationIds = missing,
        extraRelationIds = extra,
        mismatchedRelationIds = mismatched
    )
}

internal fun scrapConversionSourcePresentation(
    outputScrapGameItemId: Int,
    sources: List<MaterialSource>
): ScrapConversionSourcePresentation {
    val rows = sources.filter {
        it.type == MaterialSourceType.SCRAP_CONVERSION &&
            it.scrapOutputGameItemId == outputScrapGameItemId
    }
    val materialCount = rows.mapNotNull { it.inputGameItemId }.distinct().size
    val sharedModes = rows.map { it.scrapTriggerModes }.distinct().singleOrNull()
    return ScrapConversionSourcePresentation(
        outputScrapGameItemId = outputScrapGameItemId,
        rows = rows,
        materialCount = materialCount,
        inlineRows = rows.take(SCRAP_SOURCE_INLINE_LIMIT),
        hiddenMaterialCount = (materialCount - SCRAP_SOURCE_INLINE_LIMIT).coerceAtLeast(0),
        triggerSummary = sharedModes?.let(::scrapConversionTriggerSummary)
    )
}

/** Explicit vocabulary mapping: unknown mechanic enums are never user-facing. */
internal fun scrapConversionTriggerSummary(triggerModes: List<String>): String? {
    val labels = triggerModes.distinct().mapNotNull { mode ->
        when (mode) {
            SCRAP_TRIGGER_DIRECT_HANDOFF -> "Palico Armory handoff"
            SCRAP_TRIGGER_HUNTER_GEAR -> "Also generated when used in hunter gear"
            else -> null
        }
    }
    return labels.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/** Exact yields are shown only under the published semantics; null output is omitted. */
internal fun scrapConversionQuantityLabel(source: MaterialSource): String = buildList {
    source.scrapInputQuantity?.let { add("Use ×$it") }
    if (source.quantitySemantics == "EXACT_PUBLISHED") {
        source.quantity?.let { add("Produces ×$it") }
    }
}.joinToString(" · ")
