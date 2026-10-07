package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FarmSourcePresentationTest {
    private val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data
    private val rawFarm = data.materials.flatMap { material ->
        material.sources.filter { it.type == MaterialSourceType.FARM }
    }
    private val projectedByItem = data.materials.mapNotNull { material ->
        val rows = material.sources.filter { it.type == MaterialSourceType.FARM }
        material.gameItemId?.takeIf { rows.isNotEmpty() }?.let { it to rows.groupFarmSourcesForDisplay() }
    }.toMap()
    private val projected = projectedByItem.values.flatten()

    @Test
    fun completeRawCorpusProjectsToAcceptedStableMethodsWithoutSuppressingRows() {
        assertEquals(595, rawFarm.size)
        assertEquals(95, projectedByItem.size)
        assertEquals(76, projected.map { it.stableMethodId }.toSet().size)
        assertEquals(595, projected.sumOf { it.sourceRows.size })
        assertEquals(rawFarm.map { it.id }.toSet(), projected.flatMap { it.sourceRows }.map { it.id }.toSet())
        assertEquals(rawFarm.map { it.id }.toSet(), projected.flatMap { it.outcomes }.map { it.source.id }.toSet())
        assertTrue(projected.all { it.outcomes.isNotEmpty() })
        assertTrue(projected.all { it.sourceRows.size == it.outcomes.size })
        assertTrue(rawFarm.all { it.quantity != null })
        assertEquals(28, rawFarm.mapNotNull { it.facilityId }.toSet().size)
        assertEquals(28, FARM_FACILITY_DISPLAY_NAMES.size)
        assertTrue(rawFarm.mapNotNull { it.facilityId }.all(FARM_FACILITY_DISPLAY_NAMES::containsKey))
    }

    @Test
    fun methodIdsAndOrderingMatchAcceptedRuleAndKeepSourceOutcomeOrder() {
        rawFarm.forEach { source ->
            val expected = when {
                source.profileId != null -> "${source.facilityId}::profile:${source.profileId}"
                source.inputGameItemId != null -> "${source.facilityId}::inputGameItemId:${source.inputGameItemId}"
                source.condition != null && source.condition != "soil level changes harvest-slot count" ->
                    "${source.facilityId}::condition:${source.condition}"
                else -> "${source.facilityId}::default"
            }
            assertEquals(expected, source.farmStableMethodId())
        }
        projectedByItem.values.flatten().forEach { method ->
            assertEquals(method.stableMethodId, method.sourceRows.first().farmStableMethodId())
            assertEquals(method.sourceRows.map { it.sourceOrdinal }, method.outcomes.map { it.source.sourceOrdinal })
        }

        val armoredBeakfish = projectedByItem.getValue(199)
        assertEquals(19, armoredBeakfish.size)
        assertEquals(6, farmVisibleMethodCount(armoredBeakfish.size))
        assertEquals(13, armoredBeakfish.size - farmVisibleMethodCount(armoredBeakfish.size))
        assertEquals(
            (1..6).map { "farm_giant_fish_basket::profile:farm_fish_basket_profile_${it.toString().padStart(2, '0')}" },
            armoredBeakfish.take(6).map { it.stableMethodId }
        )
        assertEquals(armoredBeakfish.map { it.stableMethodId }.sorted(), armoredBeakfish.map { it.stableMethodId })
    }

    @Test
    fun blockedProfileSelectorsStayDistinctButDoNotLeakAuditWarnings() {
        val profileMethods = projected.filter { it.sourceRows.any { row -> row.profileId != null } }
        val productionProfiles = rawFarm.mapNotNull { it.profileId }.toSet()
        assertEquals(38, productionProfiles.size)
        assertEquals(FARM_TRIGGER_BLOCKED_PROFILE_IDS, productionProfiles)
        assertTrue(profileMethods.all { it.triggerStatus == FarmTriggerStatus.BLOCKED })
        assertTrue(profileMethods.all { it.triggerLabel.isBlank() })
        assertEquals(334, profileMethods.size)
        assertEquals(368, profileMethods.sumOf { it.outcomes.size })

        val rainbowCrystal = projectedByItem.getValue(234)
        assertEquals(3, rainbowCrystal.size)
        assertEquals(6, rainbowCrystal.sumOf { it.outcomes.size })
        assertTrue(rainbowCrystal.all { it.triggerLabel.isBlank() && it.outcomes.isNotEmpty() })
        val ancientFish = projectedByItem.getValue(196)
        assertEquals(6, ancientFish.sumOf { it.outcomes.size })
        assertTrue(ancientFish.all { it.outcomes.isNotEmpty() })
    }

    @Test
    fun genericFacilityResultMethodsAreClassifiedAndOmittedWithoutChangingRows() {
        val genericFacilityRoutes = projected.filter { it.triggerStatus == FarmTriggerStatus.FACILITY_ROUTE }
        assertEquals(164, genericFacilityRoutes.size)
        assertEquals(176, genericFacilityRoutes.sumOf { it.sourceRows.size })
        assertEquals(20, genericFacilityRoutes.map { it.facilityId }.toSet().size)
        assertEquals(20, genericFacilityRoutes.map { it.stableMethodId }.toSet().size)
        assertTrue(genericFacilityRoutes.all { it.triggerLabel.isBlank() })
        assertTrue(genericFacilityRoutes.all { group ->
            group.sourceRows.all { it.profileId == null && it.inputGameItemId == null && it.condition.isNullOrBlank() }
        })
        assertTrue(projected.none {
            it.triggerLabel.contains("Facility result", ignoreCase = true) ||
                it.triggerLabel.contains("Trigger details unresolved", ignoreCase = true) ||
                it.triggerLabel.contains("TRIGGER_BLOCKED")
        })
        val inputMethods = projected.filter { it.triggerStatus == FarmTriggerStatus.ITEM_INPUT }
        assertEquals(31, inputMethods.size)
        assertEquals(50, inputMethods.sumOf { it.sourceRows.size })

        val miningCartGroups = projectedByItem.values.flatten().groupFarmMethodsByFacility()
            .filter { it.facilityId == "farm_mining_cart" }
        assertTrue(miningCartGroups.isNotEmpty())
        assertTrue(miningCartGroups.all { it.mechanismNote == FARM_MINING_CART_MECHANISM_NOTE })
        assertEquals(
            listOf(84, 85, 86, 170),
            FARM_MINING_CART_MECHANISM_NOTE.sourceItemReferences.map { it.gameItemId }
        )
        assertTrue(FARM_MINING_CART_MECHANISM_NOTE.summary.contains("Up to 4 Palicoes"))
        assertTrue(FARM_MINING_CART_MECHANISM_NOTE.summary.contains("failure, normal, or success"))

        val cartMethods = projected.filter { it.facilityId == "farm_mining_cart" }
        assertEquals(51, cartMethods.size)
        assertEquals(62, cartMethods.sumOf { it.sourceRows.size })
        assertEquals((1..6).map { "farm_mining_cart_profile_${it.toString().padStart(2, '0')}" }.toSet(),
            cartMethods.flatMap { group -> group.sourceRows.mapNotNull { it.profileId } }.toSet())

        val provenCondition = projected.single { it.stableMethodId == "farm_mushroom_tree_pickaxe_shroom::condition:Mining Cart unlocked" }
        assertEquals(FarmTriggerStatus.SOURCE_CONDITION, provenCondition.triggerStatus)
        assertEquals("Mining Cart unlocked", provenCondition.triggerLabel)
    }

    @Test
    fun yieldsPreserveQuantitiesProbabilitiesDeterministicRowsAndPoolAnomalies() {
        val deterministic = rawFarm.filter { it.probabilitySemantics == "DETERMINISTIC_PER_HARVEST_SLOT" }
        assertEquals(12, deterministic.size)
        assertTrue(deterministic.all { it.chance == null && it.quantitySemantics == "PER_HARVEST_SLOT" })
        assertTrue(projected.flatMap { it.outcomes }
            .filter { it.probabilitySemantics == "DETERMINISTIC_PER_HARVEST_SLOT" }
            .all { it.displayLabel() == "Yield ×1 per harvest slot" && '%' !in it.displayLabel() })

        val huskberrySelfInput = projectedByItem.getValue(92)
            .single { it.stableMethodId == "farm_field_planted_item::inputGameItemId:92" }
        assertEquals(listOf("Yield ×1 · 10%", "Yield ×2 · 45%", "Yield ×3 · 40%", "Yield ×4 · 5%"),
            huskberrySelfInput.outcomes.map { it.displayLabel() })
        assertEquals(92, huskberrySelfInput.inputGameItemId)

        val caveats = projected.filter { it.publishedPoolTotalPercent != null }
            .associate { it.stableMethodId to it.publishedPoolTotalPercent }
        assertEquals(
            mapOf(
                "farm_bug_tree_seesaw::profile:farm_bug_tree_profile_4a" to 105,
                "farm_bug_tree_seesaw::profile:farm_bug_tree_profile_6a" to 90,
                "farm_mushroom_tree_supreme::default" to 95
            ),
            caveats
        )
        assertTrue(BUG_TREE_SEESAW_EXPLANATION.contains("Send 4 Palicoes"))
        assertTrue(BUG_TREE_SEESAW_EXPLANATION.contains("Better timing affects the haul"))
        assertTrue(BUG_TREE_SEESAW_EXPLANATION.contains("exact timing result") &&
            BUG_TREE_SEESAW_EXPLANATION.contains("not documented"))
        assertFalse(BUG_TREE_SEESAW_EXPLANATION.contains("Perfect", ignoreCase = true))
        assertFalse(BUG_TREE_SEESAW_EXPLANATION.contains("Success", ignoreCase = true))
        assertFalse(BUG_TREE_SEESAW_EXPLANATION.contains("Fail", ignoreCase = true))
        assertTrue(projected.all { method -> method.outcomes.all { it.source.quantity != null } })
        assertTrue(projected.all { method -> method.outcomes.all { outcome ->
            outcome.chance == outcome.source.chance && outcome.quantity == outcome.source.quantity
        } })
    }

    @Test
    fun optionalFarmInputUsageRemainsExactlyTheExistingFiftyRawRelations() {
        val farmInputs = rawFarm.filter { it.inputGameItemId != null }
        val usages = ItemUsageIndex.build(data).allRelations.filter { it.family == ItemUsageFamily.FARM }
        assertEquals(50, farmInputs.size)
        assertEquals(50, usages.size)
        assertEquals(farmInputs.map { it.id }.toSet(), usages.map { it.id }.toSet())
        val sourcesById = farmInputs.associateBy { it.id }
        usages.forEach { usage ->
            val source = requireNotNull(sourcesById[usage.id])
            val target = data.materials.single { material -> material.sources.any { it.id == source.id } }
            assertEquals(source.inputGameItemId, usage.currentItemGameItemId)
            assertEquals(target.gameItemId, usage.targetGameItemId)
            assertEquals(target.id, usage.targetId)
            assertEquals(source.quantity, usage.outputQuantity)
            assertEquals(source.chance, usage.chancePercent)
            assertEquals(source.facilityId, usage.context)
        }
    }

    @Test
    fun acceptedPresentationUsesHumanLabelsAndOnlyTheProvenSpecialCondition() {
        assertEquals(28, FARM_FACILITY_DISPLAY_NAMES.size)
        assertTrue(projected.none { it.facilityDisplayName.contains('_') })
        val pickaxe = projectedByItem.getValue(170).single()
        assertEquals("Mushroom Tree · Pickaxe Mushroom special slot", pickaxe.facilityDisplayName)
        assertEquals(FarmTriggerStatus.SOURCE_CONDITION, pickaxe.triggerStatus)
        assertEquals("Mining Cart unlocked", pickaxe.triggerLabel)
        assertFalse(pickaxe.triggerLabel.contains("profile", ignoreCase = true))
    }
}
