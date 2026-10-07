package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.MaterialSource
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import com.waillio.mhp3rdcompanion.data.MaterialSourceConditionDetails
import com.waillio.mhp3rdcompanion.data.RewardContext
import com.waillio.mhp3rdcompanion.data.chanceForDisplay
import com.waillio.mhp3rdcompanion.data.groupForDisplay
import com.waillio.mhp3rdcompanion.data.smallMonsterConditionLabel
import com.waillio.mhp3rdcompanion.data.smallMonsterPresentationProjection
import com.waillio.mhp3rdcompanion.data.smallMonsterVisibleGroupCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class MaterialSourceGroupingTest {
    @Test
    fun lowAndHighAvailabilityOfOnePhysicalNodeAreGroupedWithoutLosingRows() {
        val sources = listOf(
            field("low", "node-a", RewardContext.LOW),
            field("high", "node-a", RewardContext.HIGH)
        )

        val groups = sources.groupForDisplay().field

        assertEquals(1, groups.size)
        assertEquals(listOf(RewardContext.LOW, RewardContext.HIGH), groups.single().availability.map { it.rank })
        assertEquals(sources, groups.single().sourceRows)
    }

    @Test
    fun distinctPhysicalNodesAreNeverMerged() {
        val sources = listOf(
            field("node-a-low", "node-a", RewardContext.LOW, "Area 4 · Point 2"),
            field("node-b-low", "node-b", RewardContext.LOW, "Area 4 · Point 3")
        )

        val groups = sources.groupForDisplay().field

        assertEquals(listOf("node-a", "node-b"), groups.map { it.nodeId })
        assertEquals(sources.size, groups.sumOf { it.sourceRows.size })
    }

    @Test
    fun unpublishedGatheringProbabilityNeverBecomesFakePercentage() {
        val unpublished = field("unknown", "node-a", RewardContext.LOW).copy(chance = null)
        val published = unpublished.copy(chance = 18)
        val quest = MaterialSource("quest", MaterialSourceType.QUEST_REWARD, "Quest", chance = 13)
        val farm = MaterialSource("farm", MaterialSourceType.FARM, "Yukumo Farm", chance = 24)

        assertNull(unpublished.chanceForDisplay())
        assertEquals("18%", published.chanceForDisplay())
        assertEquals("13% reward slot", quest.chanceForDisplay())
        assertEquals("24% farm output", farm.chanceForDisplay())
    }

    @Test
    fun fieldMapSummaryUsesGroupedPointsAndRankUnion() {
        val groups = listOf(
            field("a-low", "node-a", RewardContext.LOW),
            field("a-high", "node-a", RewardContext.HIGH),
            field("b-high", "node-b", RewardContext.HIGH)
        ).groupForDisplay().field

        assertEquals("2 points · LR · HR", fieldMapSummary(groups))
        assertEquals(listOf("LR", "HR"), fieldMapRankLabels(groups))
    }

    @Test
    fun rankQuantityPreservesExplicitAndNonDefaultValues() {
        val groups = listOf(
            field("low", "node-a", RewardContext.LOW).copy(quantity = 1, quantityExplicit = false),
            field("high", "node-a", RewardContext.HIGH).copy(quantity = 2, quantityExplicit = true)
        ).groupForDisplay().field

        val displays = fieldRankDisplays(groups.single())
        assertEquals(listOf(RewardContext.LOW, RewardContext.HIGH), displays.map { it.rank })
        assertTrue(displays[0].showQuantity)
        assertEquals(listOf(1), displays[0].quantities)
        assertTrue(displays[1].showQuantity)
        assertEquals(listOf(2), displays[1].quantities)
    }

    @Test
    fun smallMonsterProjectionPreservesMethodsConditionsContextsAndRawRows() {
        val sources = listOf(
            small("body-guild", "bnahabra", "Bnahabra", RewardContext.GUILD_1_2, "BODY_CARVE", 40, condition = "WING_COLOR_LOCATION_VARIANT", details = MaterialSourceConditionDetails(listOf("Misty Peaks"), "PALE_WHITE")),
            small("body-lr", "bnahabra", "Bnahabra", RewardContext.LOW, "BODY_CARVE", 30, condition = "WING_COLOR_LOCATION_VARIANT", details = MaterialSourceConditionDetails(listOf("Misty Peaks"), "PALE_WHITE")),
            small("body-orange", "bnahabra", "Bnahabra", RewardContext.LOW, "BODY_CARVE", 20, condition = "WING_COLOR_LOCATION_VARIANT", details = MaterialSourceConditionDetails(listOf("Tundra"), "ORANGE")),
            small("body-null", "bnahabra", "Bnahabra", RewardContext.HIGH, "BODY_CARVE", 10),
            small("shiny", "bnahabra", "Bnahabra", RewardContext.LOW, "SHINY_DROP", 15, condition = "NORMAL_DROP"),
            small("stunned", "kelbi", "Kelbi", RewardContext.LOW, "BODY_CARVE", 2, condition = "STUNNED", quantity = 2)
        )

        val projection = sources.smallMonsterPresentationProjection()
        assertEquals(2, projection.monsterCount)
        assertEquals(sources.size, projection.rawRelationCount)
        assertEquals(sources.map { it.id }.toSet(), projection.rawRelationIds.toSet())
        val bnahabra = projection.monsters.first()
        assertEquals(listOf("BODY_CARVE", "SHINY_DROP"), bnahabra.methods.map { it.method })
        assertEquals(listOf("body-null"), bnahabra.methods.first().directEntries.map { it.rawRelationIdentity })
        assertEquals(listOf("Pale White · Misty Peaks", "Orange · Tundra"), bnahabra.methods.first().conditionGroups.map { it.label })
        assertEquals(listOf("body-guild", "body-lr"), bnahabra.methods.first().conditionGroups.first().entries.map { it.rawRelationIdentity })
        assertEquals("Normal Drop", bnahabra.methods[1].conditionGroups.single().label)
        assertEquals(2, projection.monsters.last().methods.single().conditionGroups.single().entries.single().quantity)
    }

    @Test
    fun smallMonsterConditionLabelsNeverExposeRawEnumTokens() {
        assertEquals("Stunned", smallMonsterConditionLabel("STUNNED", null))
        assertEquals("Blue abdomen", smallMonsterConditionLabel("ABDOMEN_COLOR", MaterialSourceConditionDetails(color = "BLUE")))
        assertEquals("White Egg", smallMonsterConditionLabel("WHITE_EGG", null))
        assertEquals("Gold Egg", smallMonsterConditionLabel("GOLD_EGG", null))
        assertEquals("Special condition", smallMonsterConditionLabel("UNEXPECTED_INTERNAL_VALUE", null))
    }

    @Test
    fun smallMonsterOutlierCountsAndInlineLimitAreDeterministic() {
        val rawMeat = (1..8).flatMap { monsterIndex ->
            (1..3).map { row ->
                small("raw-$monsterIndex-$row", "raw-$monsterIndex", "Monster $monsterIndex", RewardContext.LOW, "BODY_CARVE", 10 + row)
            }
        }
        val projection = rawMeat.smallMonsterPresentationProjection()
        assertEquals(8, projection.monsterCount)
        assertEquals(24, projection.rawRelationCount)
        assertEquals(6, smallMonsterVisibleGroupCount(projection.monsterCount))
        assertEquals((1..8).map { "raw-$it" }, projection.monsters.map { it.smallMonsterId })
    }

    @Test
    fun monsterFluidRetainsAllTwentyTwoRelationsWithoutMergingEqualChance() {
        val rows = (1..22).map { index ->
            small("fluid-$index", "monster-fluid-$index", "Monster Fluid $index", RewardContext.LOW, if (index % 2 == 0) "SHINY_DROP" else "BODY_CARVE", 25)
        }
        val projection = rows.smallMonsterPresentationProjection()
        assertEquals(22, projection.rawRelationCount)
        assertEquals(22, projection.monsterCount)
        assertEquals(rows.map { it.id }, projection.rawRelationIds)
    }

    private fun field(
        id: String,
        nodeId: String,
        rank: RewardContext,
        context: String = "Area 4 · Point 2"
    ) = MaterialSource(
        id = id,
        type = MaterialSourceType.MINING,
        name = "Misty Peaks",
        locationId = "misty_peaks",
        nodeId = nodeId,
        rank = rank,
        context = context,
        method = "MINING"
    )

    private fun small(
        id: String,
        smallMonsterId: String,
        name: String,
        rank: RewardContext,
        method: String,
        chance: Int,
        condition: String? = null,
        details: MaterialSourceConditionDetails? = null,
        quantity: Int = 1
    ) = MaterialSource(
        id = id,
        type = MaterialSourceType.SMALL_MONSTER,
        name = name,
        smallMonsterId = smallMonsterId,
        rank = rank,
        method = method,
        condition = null,
        conditionKey = condition,
        conditionDetails = details,
        chance = chance,
        quantity = quantity,
        rolls = 1,
        probabilitySemantics = "SOURCE_TABLE_PERCENT"
    )
}
