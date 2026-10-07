package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionLogic
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.EntityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SkillReferenceIntegrationTest {
    private val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data

    @Test
    fun acceptedCorpusIsProjectedWithoutDroppingNegativeOrSpecialRows() {
        assertEquals(100, data.skillTrees.size)
        assertEquals(209, data.skillTrees.sumOf { it.thresholds.size })
        assertEquals(1, data.skillTrees.sumOf { it.specialMechanics.size })
        assertEquals(100, data.skillTrees.count { it.inGameDescription.isNotBlank() })
        assertEquals(100, data.skillTrees.map { it.stableSkillTreeId }.distinct().size)
        assertTrue(data.skillTrees.flatMap { it.thresholds }.all { it.activatedSkillName.isNotBlank() && it.effectSummary.isNotBlank() })
        assertTrue(data.skillTrees.flatMap { it.thresholds }.any { it.points < 0 })
        val torso = data.skillTrees.single { it.displayName == "Torso Up" }
        assertTrue(torso.thresholds.isEmpty())
        assertEquals("TORSO_COPY", torso.specialMechanics.single().type)
    }

    @Test
    fun skillSearchMatchesDisplayTmoAndActivatedNames() {
        assertEquals("mhp3_skill_tree_072", CompanionLogic.search(data, "Punishing", EntityType.SKILL).single().id)
        assertEquals("mhp3_skill_tree_066", CompanionLogic.search(data, "Critical Draw", EntityType.SKILL).single().id)
        assertEquals("mhp3_skill_tree_040", CompanionLogic.search(data, "Fire", EntityType.SKILL).single { it.id == "mhp3_skill_tree_040" }.id)
        assertEquals(100, CompanionLogic.search(data, "", EntityType.SKILL).size)
    }

    @Test
    fun keyThresholdSemanticsRemainDistinct() {
        val punishing = data.skillTrees.single { it.displayName == "Punishing Draw" }
        assertEquals(listOf(10), punishing.thresholds.map { it.points })
        assertTrue(punishing.thresholds.single().effectSummary.contains("stun", ignoreCase = true))
        assertTrue(punishing.thresholds.single().effectSummary.contains("exhaust", ignoreCase = true))
        assertTrue(punishing.thresholds.single().effectSummary.contains("head hit", ignoreCase = true))

        val critical = data.skillTrees.single { it.displayName == "Critical Draw" }
        assertEquals("Critical Draw", critical.thresholds.single().activatedSkillName)
        assertFalse(critical.thresholds.single().activatedSkillName == punishing.thresholds.single().activatedSkillName)

        val fire = data.skillTrees.single { it.displayName == "Fire Res" }
        assertEquals(setOf(15, 10, -10), fire.thresholds.map { it.points }.toSet())
        assertEquals(3, fire.thresholds.map { it.effectSummary }.distinct().size)

        val attack = data.skillTrees.single { it.displayName == "Attack" }
        assertEquals(setOf(20, 15, 10, -10, -15, -20), attack.thresholds.map { it.points }.toSet())
        val guard = data.skillTrees.single { it.displayName == "Guard" }
        assertTrue(guard.thresholds.any { it.points > 0 })
        assertTrue(guard.thresholds.any { it.points < 0 })
        assertNotNull(data.skillTrees.singleOrNull { it.displayName == "Torso Up" })
    }

    @Test
    fun hardeningRemovesPlaceholdersCjkAndPspMarkup() {
        val thresholds = data.skillTrees.flatMap { it.thresholds }
        assertTrue(thresholds.all { !it.effectSummary.contains("see the preserved", ignoreCase = true) })
        assertTrue(thresholds.all { !it.effectSummary.contains("Published MHP3 effect for", ignoreCase = true) })
        assertTrue(thresholds.all { Regex("[\\u3040-\\u30ff\\u3400-\\u9fff]").containsMatchIn(it.activatedSkillName + it.effectSummary).not() })
        assertTrue(data.skillTrees.all { !it.inGameDescription.contains("~C") })
        assertEquals("Transporter", data.skillTrees.single { it.stableSkillTreeId.endsWith("039") }.displayName)
        assertEquals("Item Duration", data.skillTrees.single { it.stableSkillTreeId.endsWith("058") }.displayName)
        assertEquals("Thunder Attack", data.skillTrees.single { it.stableSkillTreeId.endsWith("090") }.displayName)
        assertEquals("Stun Halved", data.skillTrees.single { it.stableSkillTreeId.endsWith("005") }.thresholds.single { it.points == 10 }.activatedSkillName)
    }
}
