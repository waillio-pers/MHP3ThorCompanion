package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
class ScrapConversionSourcePresentationTest {
    private val fixture = CompanionRepository(ApplicationProvider.getApplicationContext()).data
    private val materialsByGameId = fixture.materials.associateBy { it.gameItemId }

    @Test
    fun sourceProjectionPreservesAllRulesAndCanonicalOrdering() {
        val expected = fixture.itemScrapConversionRules.map { it.toScrapConversionSourceFact() }
        val actual = fixture.materials.flatMap { it.sources }
            .mapNotNull { it.toScrapConversionSourceFactOrNull() }
        val comparison = compareScrapConversionSourceFacts(expected, actual)

        assertTrue("328 source rows must exactly match the raw rules: $comparison", comparison.exact)
        assertEquals(328, actual.size)
        assertEquals(328, actual.map { it.inputGameItemId }.distinct().size)
        assertEquals(70, actual.map { it.outputScrapGameItemId }.distinct().size)
        assertTrue(fixture.itemScrapConversionRules.all { it.inputQuantity == 1 })
        assertTrue(fixture.itemScrapConversionRules.all {
            it.triggerModes == listOf(SCRAP_TRIGGER_DIRECT_HANDOFF, SCRAP_TRIGGER_HUNTER_GEAR)
        })

        fixture.itemScrapConversionRules.map { it.outputScrapGameItemId }.distinct().forEach { outputId ->
            val expectedIds = fixture.itemScrapConversionRules
                .filter { it.outputScrapGameItemId == outputId }
                .map { "scrap:${it.inputGameItemId}:${it.outputScrapGameItemId}" }
            val output = requireNotNull(materialsByGameId[outputId])
            val sourceRows = output.sources.filter { it.type == MaterialSourceType.SCRAP_CONVERSION }
            assertEquals("Scrap $outputId retains source order", expectedIds, sourceRows.map { it.id })
            assertTrue("Sources belong only to their current Scrap output", sourceRows.all {
                it.scrapOutputGameItemId == outputId
            })
        }
    }

    @Test
    fun sourceAndUsageParityMatchesAllRuleFacts() {
        val sourceFacts = fixture.materials.flatMap { it.sources }
            .mapNotNull { it.toScrapConversionSourceFactOrNull() }
            .associateBy { it.relationId }
        val usage = ItemUsageIndex.build(fixture).allRelations
            .filter { it.family == ItemUsageFamily.SCRAP_CONVERSION }
            .associateBy { it.id }
        assertEquals(328, sourceFacts.size)
        assertEquals(328, usage.size)

        fixture.itemScrapConversionRules.forEach { rule ->
            val id = "scrap:${rule.inputGameItemId}:${rule.outputScrapGameItemId}"
            val source = requireNotNull(sourceFacts[id])
            val inverse = requireNotNull(usage[id])
            assertEquals(rule.inputGameItemId, source.inputGameItemId)
            assertEquals(rule.inputQuantity, source.inputQuantity)
            assertEquals(rule.outputScrapGameItemId, source.outputScrapGameItemId)
            assertEquals(rule.outputQuantity, source.outputQuantity)
            assertEquals(rule.quantitySemantics, source.quantitySemantics)
            assertEquals(rule.triggerModes, source.triggerModes)

            assertEquals(rule.inputGameItemId, inverse.currentItemGameItemId)
            assertEquals(rule.outputScrapGameItemId, inverse.targetGameItemId)
            assertEquals(rule.inputQuantity, inverse.quantity)
            assertEquals(rule.outputQuantity, inverse.outputQuantity)
            assertEquals(rule.quantitySemantics, inverse.quantitySemantics)
            assertEquals(rule.triggerModes.joinToString(" · "), inverse.context)
        }
    }

    @Test
    fun focusedPoisonMutationsAreDetectedByTheSourceComparator() {
        val expected = fixture.itemScrapConversionRules.map { it.toScrapConversionSourceFact() }
        val actual = fixture.materials.flatMap { it.sources }
            .mapNotNull { it.toScrapConversionSourceFactOrNull() }
        assertTrue(compareScrapConversionSourceFacts(expected, actual).exact)

        val published = actual.first { it.outputQuantity != null }
        assertPoisonDetected(expected, actual.map {
            if (it.relationId == published.relationId) it.copy(inputGameItemId = it.inputGameItemId + 1) else it
        }, published.relationId, "wrong input Item")
        assertPoisonDetected(expected, actual.map {
            if (it.relationId == published.relationId) it.copy(outputScrapGameItemId = it.outputScrapGameItemId + 1) else it
        }, published.relationId, "wrong Scrap output")
        assertPoisonDetected(expected, actual.map {
            if (it.relationId == published.relationId) it.copy(inputQuantity = it.inputQuantity!! + 1) else it
        }, published.relationId, "wrong input quantity")
        assertPoisonDetected(expected, actual.map {
            if (it.relationId == published.relationId) it.copy(outputQuantity = it.outputQuantity!! + 1) else it
        }, published.relationId, "wrong published yield")

        val courage = actual.single { it.inputGameItemId == 605 }
        assertNull(courage.outputQuantity)
        assertPoisonDetected(expected, actual.map {
            if (it.relationId == courage.relationId) it.copy(outputQuantity = 1) else it
        }, courage.relationId, "fake Courage yield")
        assertPoisonDetected(expected, actual.map {
            if (it.relationId == courage.relationId) it.copy(triggerModes = it.triggerModes.dropLast(1)) else it
        }, courage.relationId, "missing trigger mode")
    }

    @Test
    fun disclosureKeepsMaximumScrapInputsDistinctAndInSourceOrder() {
        val output = requireNotNull(materialsByGameId[793])
        val projection = scrapConversionSourcePresentation(793, output.sources)
        val expectedIds = fixture.itemScrapConversionRules
            .filter { it.outputScrapGameItemId == 793 }
            .map { "scrap:${it.inputGameItemId}:793" }

        assertEquals("Agnaktor Scraps+", output.name)
        assertEquals(12, projection.materialCount)
        assertEquals(12, projection.rows.size)
        assertEquals(12, projection.rows.mapNotNull { it.inputGameItemId }.distinct().size)
        assertEquals(expectedIds, projection.rows.map { it.id })
        assertEquals(expectedIds.take(6), projection.inlineRows.map { it.id })
        assertEquals(6, projection.hiddenMaterialCount)
        assertEquals("Palico Armory handoff · Also generated when used in hunter gear", projection.triggerSummary)
    }

    @Test
    fun exactYieldsCourageOmissionAndInitialWoodGrantStayDistinct() {
        assertEquals(326, fixture.itemScrapConversionRules.count { it.quantitySemantics == "EXACT_PUBLISHED" })
        assertEquals(2, fixture.itemScrapConversionRules.count { it.quantitySemantics == "NOT_PUBLISHED" })
        val distribution = fixture.itemScrapConversionRules.filter { it.quantitySemantics == "EXACT_PUBLISHED" }
            .groupingBy { it.outputQuantity }
            .eachCount()
        assertEquals(mapOf(1 to 163, 2 to 91, 3 to 36, 4 to 12, 5 to 2, 6 to 8, 8 to 1, 10 to 13), distribution)

        val courageScraps = requireNotNull(materialsByGameId[801])
        val courageSource = courageScraps.sources.single { it.type == MaterialSourceType.SCRAP_CONVERSION }
        assertEquals(605, courageSource.inputGameItemId)
        assertEquals(1, courageSource.scrapInputQuantity)
        assertNull(courageSource.quantity)
        assertEquals("NOT_PUBLISHED", courageSource.quantitySemantics)
        assertEquals("Use ×1", scrapConversionQuantityLabel(courageSource))

        val woodScraps = requireNotNull(materialsByGameId[733])
        val repeatableWood = woodScraps.sources.filter { it.type == MaterialSourceType.SCRAP_CONVERSION }
        assertEquals(1, repeatableWood.size)
        assertEquals(183, repeatableWood.single().inputGameItemId)
        assertEquals(1, repeatableWood.single().quantity)
        val initialGrant = woodScraps.sources.single {
            it.id == "INITIAL_WOOD_SCRAP" && it.type == MaterialSourceType.SPECIAL_FREE
        }
        assertEquals("INITIAL_FREE_GRANT", initialGrant.specialFreeMechanism)
        assertEquals("INITIAL_PALICO_ARMORY_ACCESS", initialGrant.specialFreeConditionType)
        assertEquals(4, initialGrant.quantity)
        assertTrue(repeatableWood.none { it.quantity == initialGrant.quantity })
    }

    @Test
    fun triggerAndQuantityVocabularyIsHumanFacingAndUnknownModesAreOmitted() {
        val modes = listOf(SCRAP_TRIGGER_DIRECT_HANDOFF, SCRAP_TRIGGER_HUNTER_GEAR)
        val summary = requireNotNull(scrapConversionTriggerSummary(modes))
        assertEquals("Palico Armory handoff · Also generated when used in hunter gear", summary)
        assertEquals(1, Regex("Palico Armory handoff").findAll(summary).count())
        assertFalse(summary.contains(SCRAP_TRIGGER_DIRECT_HANDOFF))
        assertFalse(summary.contains(SCRAP_TRIGGER_HUNTER_GEAR))
        assertNull(scrapConversionTriggerSummary(listOf("FUTURE_INTERNAL_TRIGGER")))

        val pumpkin = requireNotNull(materialsByGameId[735]).sources.single { it.type == MaterialSourceType.SCRAP_CONVERSION }
        assertEquals("Use ×1 · Produces ×3", scrapConversionQuantityLabel(pumpkin))
        assertFalse(scrapConversionQuantityLabel(pumpkin).contains("EXACT_PUBLISHED"))
    }

    private fun assertPoisonDetected(
        expected: List<ScrapConversionSourceFact>,
        poisoned: List<ScrapConversionSourceFact>,
        relationId: String,
        case: String
    ) {
        val result = compareScrapConversionSourceFacts(expected, poisoned)
        assertFalse("Comparator must detect $case", result.exact)
        assertTrue("$case must be attributed to $relationId: $result", relationId in result.mismatchedRelationIds)
    }
}
