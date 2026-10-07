package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import com.waillio.mhp3rdcompanion.data.palicoPresentationProjection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PalicoExpeditionPresentationTest {
    private val repository = CompanionRepository(
        ApplicationProvider.getApplicationContext()
    )

    private fun sources(gameItemId: Int) = repository.data.materials
        .single { it.gameItemId == gameItemId }
        .sources
        .filter { it.type == MaterialSourceType.PALICO_EXPEDITION }

    @Test fun corpusAndProjectionPreserveEveryRawRelation() {
        val all = repository.data.materials.flatMap { it.sources }
            .filter { it.type == MaterialSourceType.PALICO_EXPEDITION }
        assertEquals(662, all.size)
        assertEquals(248, all.mapNotNull { it.expeditionId }.let { ids ->
            repository.data.materials.count { material -> material.sources.any { it.type == MaterialSourceType.PALICO_EXPEDITION } }
        })
        assertEquals(24, repository.palicoExpeditions.size)
        assertEquals(24, all.flatMap { listOfNotNull(it.expeditionId) }.toSet().size)
        assertEquals(setOf("GATHERING", "SMALL_MONSTER", "LARGE_MONSTER"), all.mapNotNull { it.expeditionRewardCategory }.toSet())
        assertTrue(all.all { it.expeditionId != null && it.expeditionStarRank != null && it.expeditionCostPerPalico != null })
        repository.data.materials
            .filter { material -> material.sources.any { it.type == MaterialSourceType.PALICO_EXPEDITION } }
            .forEach { material ->
                val rows = sources(material.gameItemId!!)
                val projection = rows.palicoPresentationProjection()
                assertEquals(rows.size, projection.rawRelationCount)
                assertEquals(rows.mapNotNull { it.expeditionId }.toSet(), projection.projectedExpeditionIds)
            }
    }

    @Test fun armorSphereAndRustedFragmentUseCompleteTierRanges() {
        val armor = sources(804).palicoPresentationProjection()
        assertEquals(14, armor.rawRelationCount)
        assertEquals(14, armor.uniqueExpeditionCount)
        assertEquals(listOf("06"), armor.tiers[0].entries.map { formatPalicoExpeditionIds(it.expeditionIds) })
        assertEquals(listOf("06"), armor.tiers[1].entries.map { formatPalicoExpeditionIds(it.expeditionIds) })
        assertEquals(listOf("01–06"), armor.tiers[2].entries.map { formatPalicoExpeditionIds(it.expeditionIds) })
        assertEquals(listOf("01–06"), armor.tiers[3].entries.map { formatPalicoExpeditionIds(it.expeditionIds) })
        assertEquals(50, armor.tiers[0].costPerPalico)
        assertEquals(400, armor.tiers[3].costPerPalico)

        val rusted = sources(598).palicoPresentationProjection()
        assertEquals(24, rusted.rawRelationCount)
        assertEquals(List(4) { "01–06" }, rusted.tiers.map { formatPalicoExpeditionIds(it.entries.single().expeditionIds) })
        assertTrue(rusted.tiers.all { it.entries.single().rewards.single().quantity == 1 })
    }

    @Test fun huskberryKeepsNonContiguousAndSameExpeditionQuantitiesSeparate() {
        val huskberry = sources(92).palicoPresentationProjection()
        val tierOne = huskberry.tiers.single { it.starRank == 1 }
        assertEquals(setOf("01, 03–04, 06", "05"), tierOne.entries.map { formatPalicoExpeditionIds(it.expeditionIds) }.toSet())
        val five = tierOne.entries.single { formatPalicoExpeditionIds(it.expeditionIds) == "05" }
        assertEquals(listOf(5, 10), five.rewards.map { it.quantity })
        assertEquals(listOf("Gathering", "Gathering"), five.rewards.map { it.category.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase) })
    }

    @Test fun mixedCategoryAndSingleItemRemainDistinct() {
        val mixed = sources(237).palicoPresentationProjection()
        assertEquals(setOf("GATHERING", "SMALL_MONSTER"), mixed.tiers.flatMap { it.entries }.flatMap { it.rewards }.map { it.category }.toSet())
        assertTrue(mixed.tiers.flatMap { it.entries }.all { entry -> entry.rewards.map { it.category }.distinct().size <= entry.rewards.size })

        val single = sources(934).palicoPresentationProjection()
        assertEquals(1, single.rawRelationCount)
        assertEquals(1, single.uniqueExpeditionCount)
        assertEquals(1, single.tiers.single().entries.single().rewards.size)
    }

    @Test fun thorCardsUseFourColumnsWhenSpaceAllowsAndNarrowUsesOne() {
        assertEquals(4, palicoSimpleTierColumnCount(720.dp))
        assertEquals(2, palicoSimpleTierColumnCount(500.dp))
        assertEquals(1, palicoSimpleTierColumnCount(300.dp))
        assertEquals("01–06", formatPalicoExpeditionIds(listOf("palico_expedition_1_1", "palico_expedition_1_2", "palico_expedition_1_3", "palico_expedition_1_4", "palico_expedition_1_5", "palico_expedition_1_6")))
        assertEquals("01, 03–04, 06", formatPalicoExpeditionIds(listOf("palico_expedition_1_1", "palico_expedition_1_3", "palico_expedition_1_4", "palico_expedition_1_6")))
    }

    @Test fun expeditionIdentityAndRewardStayPrimaryWhileFeeIsSecondary() {
        assertEquals("Expedition 06", formatPalicoExpeditionLabel(listOf("palico_expedition_3_6")))
        assertEquals("Expeditions 02, 06", formatPalicoExpeditionLabel(listOf("palico_expedition_3_2", "palico_expedition_3_6")))
        assertEquals("Expeditions 01–06", formatPalicoExpeditionLabel((1..6).map { "palico_expedition_3_$it" }))
        assertEquals("Gathering · ×5", palicoRewardLabel("GATHERING", 5))
        assertEquals("Small Monster · ×10", palicoRewardLabel("SMALL_MONSTER", 10))

        val huskberry = sources(92).palicoPresentationProjection().tiers.single { it.starRank == 1 }
        val fifthExpedition = huskberry.entries.single { formatPalicoExpeditionIds(it.expeditionIds) == "05" }
        assertEquals(listOf("Gathering · ×5", "Gathering · ×10"), fifthExpedition.rewards.map {
            palicoRewardLabel(it.category, it.quantity)
        })
    }
}
