package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MonsterQuickReferenceTest {
    @Test
    fun bestHitAreasUseNormalHitzonesAndAllowDifferentBodyParts() {
        val monster = Monster(
            id = "test",
            name = "Test Monster",
            subtitle = "",
            hitzones = listOf(
                Hitzone("Head", cut = 60, impact = 40, shot = 20, fire = 0, water = 0, thunder = 0, ice = 0, dragon = 0),
                Hitzone("Back", cut = 30, impact = 65, shot = 25, fire = 0, water = 0, thunder = 0, ice = 0, dragon = 0),
                Hitzone("Tail", cut = 45, impact = 35, shot = 70, fire = 0, water = 0, thunder = 0, ice = 0, dragon = 0)
            ),
            alternateHitzones = listOf(
                AlternateHitzoneState(
                    "alternate",
                    "Enraged",
                    listOf(Hitzone("Wing", cut = 99, impact = 99, shot = 99, fire = 0, water = 0, thunder = 0, ice = 0, dragon = 0))
                )
            )
        )

        assertEquals(
            listOf(
                BestHitArea(PhysicalDamageType.CUT, "Head", 60),
                BestHitArea(PhysicalDamageType.IMPACT, "Back", 65),
                BestHitArea(PhysicalDamageType.SHOT, "Tail", 70)
            ),
            monster.bestHitAreas()
        )
    }

    @Test
    fun bestHitAreaTieUsesStableSourceOrder() {
        val monster = Monster(
            id = "tie", name = "Tie", subtitle = "",
            hitzones = listOf(
                Hitzone("First", 50, 50, 50, 0, 0, 0, 0, 0),
                Hitzone("Second", 50, 50, 50, 0, 0, 0, 0, 0)
            )
        )
        assertEquals(listOf("First", "First", "First"), monster.bestHitAreas().map { it.bodyPart })
    }

    @Test
    fun breakSeverGroupingPreservesEverySourceRow() {
        val rows = listOf(
            BreakablePart("Tail", "Break"),
            BreakablePart("Tail", "Sever"),
            BreakablePart("Head", "Break", 2)
        )
        val groups = Monster("parts", "Parts", "", breakableParts = rows).groupedBreakableParts()

        assertEquals(listOf("Tail", "Head"), groups.map { it.name })
        assertEquals(listOf("Break", "Sever"), groups.first().actions)
        assertEquals(rows, groups.flatMap { it.sourceRows })
    }

    @Test
    fun hunterThreatLabelsAreHumanReadableAndComplete() {
        val labels = HunterThreatType.entries.associateWith { it.displayLabel() }
        assertEquals("Poison", labels[HunterThreatType.POISON])
        assertEquals("Defense Down (Large)", labels[HunterThreatType.DEFENSE_DOWN_LARGE])
        assertEquals("Terrain Elemental Blight", labels[HunterThreatType.TERRAIN_ELEMENTAL_BLIGHT])
        assertFalse(labels.values.any { '_' in it })
    }

    @Test
    fun counterSummariesOnlyUseExplicitCounterLinks() {
        val prep = MonsterHuntPrep(
            coverageStatus = ThreatCoverageStatus.LISTED,
            threats = listOf(
                HunterThreat("poison", HunterThreatType.POISON),
                HunterThreat("stun", HunterThreatType.STUN)
            ),
            counterItems = listOf(
                HuntPrepCounterItem(12, "Antidote", listOf(HunterThreatType.POISON)),
                HuntPrepCounterItem(13, "Herbal Medicine", listOf(HunterThreatType.POISON))
            ),
            tacticalTools = emptyList()
        )
        assertEquals(listOf("Antidote", "Herbal Medicine"), prep.counterSummaries().single().itemNames)
        assertFalse(prep.counterSummaries().any { it.threatType == HunterThreatType.STUN })
    }

    @Test
    fun rewardContextsFollowProgressionAndNeverMixRows() {
        val rewards = listOf(
            MonsterReward("Guild Body", "Body Carve", "Body", "50%", RewardContext.GUILD_1_2),
            MonsterReward("Village Body", "Body Carve", "Body", "60%", RewardContext.VILLAGE_2_SPECIAL),
            MonsterReward("Low Body", "Body Carve", "Body", "40%", RewardContext.LOW),
            MonsterReward("Low Head", "Part Break", "Head", "30%", RewardContext.LOW),
            MonsterReward("High Body+", "Body Carve", "Body", "35%", RewardContext.HIGH)
        )

        assertEquals(
            listOf(RewardContext.LOW, RewardContext.HIGH, RewardContext.GUILD_1_2, RewardContext.VILLAGE_2_SPECIAL),
            rewards.availableContexts()
        )
        assertEquals(listOf("Low Body", "Low Head"), rewards.groupsFor(RewardContext.LOW).flatMap { it.rewards }.map { it.item })
        assertEquals(listOf("Body Carve", "Head Break"), rewards.groupsFor(RewardContext.LOW).map { it.title })
        assertFalse(rewards.groupsFor(RewardContext.LOW).flatMap { it.rewards }.any {
            it.rank == RewardContext.GUILD_1_2 || it.rank == RewardContext.VILLAGE_2_SPECIAL
        })
    }
}
