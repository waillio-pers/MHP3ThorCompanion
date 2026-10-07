package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionLogic
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.EntityType
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import com.waillio.mhp3rdcompanion.data.projectTrainingRewards
import kotlinx.serialization.json.Json
import com.waillio.mhp3rdcompanion.data.GeneratedDataset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrainingProductionIntegrationTest {
    private val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data

    @Test
    fun trainingBrowserUniverseAndClassFiltersUseProductionRows() {
        assertEquals(42, data.trainingQuests.size)
        assertEquals(22, data.trainingQuests.count { it.trainingClass == "BEGINNER" })
        assertEquals(6, data.trainingQuests.count { it.trainingClass == "GROUP" })
        assertEquals(14, data.trainingQuests.count { it.trainingClass == "CHALLENGE" })
        assertEquals(42, CompanionLogic.search(data, "", EntityType.TRAINING).size)
    }

    @Test
    fun trainingGlobalSearchMatchesEnglishAndJapaneseIdentity() {
        val beginner = data.trainingQuests.first { it.trainingClass == "BEGINNER" }
        assertTrue(CompanionLogic.search(data, beginner.canonicalDisplayName).any { it.id == beginner.id && it.type == EntityType.TRAINING })
        val challenge = data.trainingQuests.first { it.trainingClass == "CHALLENGE" }
        assertTrue(CompanionLogic.search(data, challenge.japaneseName).any { it.id == challenge.id && it.type == EntityType.TRAINING })
    }

    @Test
    fun trainingRewardSourcesAreSeparateFromSuppliedItems() {
        val trainingSources = data.materials.flatMap { it.sources }.filter { it.type == MaterialSourceType.TRAINING_REWARD }
        assertEquals(179, trainingSources.size)
        assertEquals(24, data.materials.count { material -> material.sources.any { it.type == MaterialSourceType.TRAINING_REWARD } })
        assertTrue(data.trainingQuests.flatMap { it.suppliedItems }.isNotEmpty())
    }

    @Test
    fun trainingRewardProjectionPreservesMissionFirstHierarchyAndRawRows() {
        val trainingSources = data.materials.flatMap { it.sources }.filter { it.type == MaterialSourceType.TRAINING_REWARD }
        val projected = trainingSources.projectTrainingRewards()
        assertEquals(179, projected.rawRelationCount)
        assertEquals(34, projected.missions.size)
        assertEquals(0, projected.missions.flatMap { it.rewardGroups }.flatMap { it.entries }
            .count { it.source.type != MaterialSourceType.TRAINING_REWARD })
        val classOrder = listOf("BEGINNER", "GROUP", "CHALLENGE")
        assertTrue(projected.missions.zipWithNext().all { (left, right) ->
            classOrder.indexOf(left.trainingClass) <= classOrder.indexOf(right.trainingClass)
        })
        assertTrue(projected.missions.flatMap { it.rewardGroups }.all { group ->
            group.displayLabel == null || group.displayLabel in setOf("Guaranteed Rewards", "Basic Rewards", "Additional Rewards")
        })
        assertTrue(projected.missions.flatMap { it.rewardGroups }
            .filter { it.rawRewardType == "ATWIKI_CANDIDATE" }
            .all { it.displayLabel == null })
    }

    @Test
    fun trainingRewardOutliersAndQuantityProbabilitySemanticsAreProjectedExactly() {
        fun item(name: String) = data.materials.single { it.name == name }
        val wellDone = item("Well-done Steak").sources.projectTrainingRewards()
        val rawMeat = item("Raw Meat").sources.projectTrainingRewards()
        val oldPickaxe = item("Old Pickaxe").sources.projectTrainingRewards()
        val armorSpherePlus = item("Armor Sphere+").sources.projectTrainingRewards()

        assertEquals(23, wellDone.missionCount)
        assertEquals(23, wellDone.rawRelationCount)
        assertEquals(25, rawMeat.rawRelationCount)
        assertEquals(13, rawMeat.missionCount)
        assertEquals(1, oldPickaxe.missionCount)
        assertEquals(2, armorSpherePlus.missionCount)
        assertTrue(oldPickaxe.missions.flatMap { it.rewardGroups }.all { it.displayLabel == null })
        assertTrue(rawMeat.missions.flatMap { it.rewardGroups }.any { it.displayLabel == "Guaranteed Rewards" })
        assertTrue(rawMeat.missions.flatMap { it.rewardGroups }.any { it.displayLabel == "Basic Rewards" })
        assertEquals(0, rawMeat.missions.flatMap { it.rewardGroups }.flatMap { it.entries }
            .count { it.quantitySemantics == "NOT_STATED" && it.quantity != null })
        assertEquals(91, data.materials.flatMap { it.sources }.count { it.type == MaterialSourceType.TRAINING_REWARD && it.quantitySemantics == "EXPLICIT" })
        assertEquals(88, data.materials.flatMap { it.sources }.count { it.type == MaterialSourceType.TRAINING_REWARD && it.quantitySemantics == "NOT_STATED" })
        assertEquals(88, data.materials.flatMap { it.sources }.count { it.type == MaterialSourceType.TRAINING_REWARD && it.probabilitySemantics == "SOURCE_UNSUPPORTED" && it.chance == null })
        assertEquals(12, data.materials.flatMap { it.sources }.count { it.type == MaterialSourceType.TRAINING_REWARD && it.probabilitySemantics == "GUARANTEED_OUTPUT" && it.chance == 100 })
        assertEquals(79, data.materials.flatMap { it.sources }.count { it.type == MaterialSourceType.TRAINING_REWARD && it.probabilitySemantics == "REWARD_SLOT_CHANCE" })
    }

    @Test
    fun generatedProductionSchemaIsThirteenAndTrainingBacklinksStaySeparate() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val generated = context.assets.open("mhp3rd-data.json").bufferedReader().use { Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(it.readText()) }
        assertEquals(31, generated.schemaVersion)
        assertEquals(42, generated.trainingQuests.size)
        assertEquals(438, generated.monsters.sumOf { it.questIds.size })
        assertEquals(82, generated.smallMonsters.sumOf { it.questIds.size })
        assertEquals(82, generated.monsters.sumOf { it.trainingQuestIds.size } + generated.smallMonsters.sumOf { it.trainingQuestIds.size })
    }
}
