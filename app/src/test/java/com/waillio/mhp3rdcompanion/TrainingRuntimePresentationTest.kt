package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingRuntimePresentationTest {
    private val lagombi = Monster("monster_lagombi", "Lagombi", "", type = "Large Monster")
    private val gargwa = SmallMonster("small_monster_gargwa", "Gargwa", "Bird Wyvern", emptyList(), emptyList(), emptyList())
    private val steak = Material("item_steak", "Well-done Steak", 1, "", emptyList(), emptyList(), gameItemId = 44)

    @Test
    fun objectiveEntitiesResolveToCanonicalEnglishNamesWithoutInternalIds() {
        val targets = listOf(
            TrainingObjectiveTarget("LARGE_MONSTER", "monster_lagombi", 1, "EXACT", "SLAY"),
            TrainingObjectiveTarget("SMALL_MONSTER", "small_monster_gargwa", 2, "EXACT", "CAPTURE"),
            TrainingObjectiveTarget("ITEM", "game_item_44", 1, "EXACT", "DELIVER", gameItemId = 44)
        )
        val display = trainingObjectiveDisplayText(targets, listOf(lagombi), listOf(gargwa), listOf(steak))
        assertEquals("Slay 1 Lagombi · Capture 2 Gargwa · Deliver 1 Well-done Steak", display)
        assertFalse(display.orEmpty().contains("monster_"))
        assertFalse(display.orEmpty().contains("small_monster_"))
        assertFalse(display.orEmpty().contains("game_item_"))
    }

    @Test
    fun unknownEntityDoesNotLeakItsGeneratedId() {
        val target = TrainingObjectiveTarget("LARGE_MONSTER", "monster_unknown", 1, "EXACT", "SLAY")
        assertEquals(null, trainingObjectiveDisplayText(listOf(target), emptyList(), emptyList(), emptyList()))
        assertEquals(null, trainingEntityDisplayName(target, emptyList(), emptyList(), emptyList()))
    }

    @Test
    fun structuredObjectiveVerbsAndCountModesAreHumanReadable() {
        val targets = listOf(
            TrainingObjectiveTarget("LARGE_MONSTER", lagombi.id, 1, "EXACT", "CAPTURE"),
            TrainingObjectiveTarget("ITEM", "game_item_44", 3, "UP_TO", "DELIVER", gameItemId = 44)
        )
        assertEquals("Capture 1 Lagombi · Deliver up to 3 Well-done Steak", trainingObjectiveDisplayText(targets, listOf(lagombi), emptyList(), listOf(steak)))
    }

    @Test
    fun appearingMonsterNamesResolveLargeAndSmallMasters() {
        assertEquals(listOf("Lagombi", "Gargwa"), trainingAppearingMonsterDisplayNames(listOf(lagombi.id, gargwa.id), listOf(lagombi), listOf(gargwa)))
    }

    @Test
    fun timesAndConditionsUseHumanReadableLabels() {
        assertEquals("50 min", trainingTimeLimitLabel(3000))
        assertEquals("30 min", trainingTimeLimitLabel(1800))
        assertEquals("4:00", trainingGradeThresholdLabel(240))
        assertEquals("9:00", trainingGradeThresholdLabel(540))
        assertEquals("Within time limit", trainingConditionLabel("WITHIN_TIME_LIMIT"))
    }

    @Test
    fun allProductionWeaponClassesHaveCanonicalLabels() {
        val labels = mapOf(
            "GREAT_SWORD" to "Great Sword",
            "LONG_SWORD" to "Long Sword",
            "SWORD_AND_SHIELD" to "Sword & Shield",
            "DUAL_BLADES" to "Dual Blades",
            "HAMMER" to "Hammer",
            "LANCE" to "Lance",
            "HUNTING_HORN" to "Hunting Horn",
            "GUNLANCE" to "Gunlance",
            "SWITCH_AXE" to "Switch Axe",
            "BOW" to "Bow",
            "LIGHT_BOWGUN" to "Light Bowgun",
            "HEAVY_BOWGUN" to "Heavy Bowgun"
        )
        assertEquals(labels, labels.mapValues { canonicalWeaponTypeLabel(it.key) })
        assertTrue(labels.values.none { it.contains('_') })
    }
}
