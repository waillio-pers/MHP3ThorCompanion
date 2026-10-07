package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JewelSkillCrossReferenceTest {
    private val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data

    @Test
    fun acceptedJewelCorpusAndSignedRelationsResolve() {
        assertEquals(164, data.decorationSkillRelations.map { it.stableDecorationItemId }.distinct().size)
        assertEquals(284, data.decorationSkillRelations.size)
        assertEquals(164, data.decorationSkillRelations.count { it.points > 0 })
        assertEquals(120, data.decorationSkillRelations.count { it.points < 0 })
        assertEquals(164, data.decorationSkillRelations.groupBy { it.stableDecorationItemId }.count { it.value.size >= 1 })
        assertTrue(data.decorationSkillRelations.all { relation ->
            data.materials.any { it.id == relation.stableDecorationItemId } &&
                data.skillTrees.any { it.stableSkillTreeId == relation.stableSkillTreeId } &&
                relation.slotCost in 1..3
        })
    }

    @Test
    fun bidirectionalIndexesAreExactInverses() {
        val jewelToSkill = data.decorationSkillRelations
            .groupBy { it.stableDecorationItemId }
            .mapValues { (_, rows) -> rows.map { Triple(it.stableSkillTreeId, it.points, it.slotCost) }.toSet() }
        val skillToJewel = data.decorationSkillRelations
            .groupBy { it.stableSkillTreeId }
            .flatMap { (skillId, rows) -> rows.map { skillId to Triple(it.stableDecorationItemId, it.points, it.slotCost) } }
            .groupBy({ it.first }, { it.second })
        assertEquals(
            data.decorationSkillRelations.map { Triple(it.stableDecorationItemId, it.stableSkillTreeId, it.points to it.slotCost) }.toSet().size,
            data.decorationSkillRelations.size
        )
        assertFalse(jewelToSkill.isEmpty())
        assertEquals(
            data.decorationSkillRelations.map { it.stableSkillTreeId to Triple(it.stableDecorationItemId, it.points, it.slotCost) }
                .groupBy({ it.first }, { it.second }),
            skillToJewel
        )
    }

    @Test
    fun criticalAndPunishingDrawRemainDistinct() {
        val critical = data.decorationSkillRelations.filter { it.stableSkillTreeId == "mhp3_skill_tree_066" }
        val punishing = data.decorationSkillRelations.filter { it.stableSkillTreeId == "mhp3_skill_tree_072" }
        assertEquals(setOf(1, 3), critical.map { it.points }.toSet())
        assertEquals(setOf(1, 4), punishing.map { it.points }.toSet())
        assertTrue(critical.map { it.stableDecorationItemId }.none { id -> punishing.any { it.stableDecorationItemId == id } })
    }
}
