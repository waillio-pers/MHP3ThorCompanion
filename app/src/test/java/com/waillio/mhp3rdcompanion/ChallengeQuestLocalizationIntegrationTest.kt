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
class ChallengeQuestLocalizationIntegrationTest {
    private val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data
    private val cjk = Regex("[\\u3040-\\u30ff\\u3400-\\u4dbf\\u4e00-\\u9fff]")

    @Test
    fun allFourteenChallengeTitlesAndObjectivesAreEnglish() {
        val rows = data.trainingQuests.filter { it.trainingClass == "CHALLENGE" }
        assertEquals(14, rows.size)
        assertEquals(listOf("MH Fiesta 1", "MH Fiesta 2", "MH Fiesta 3", "MH Fiesta 4", "Challenge Quest 1", "Challenge Quest 2", "Challenge Quest 3", "Challenge Quest 4", "Challenge Quest 5", "Challenge Quest 6", "Challenge Quest 7", "Challenge Quest 8", "Challenge Quest 9", "Challenge Quest 10"), rows.map { it.canonicalDisplayName })
        assertTrue(rows.all { !cjk.containsMatchIn(it.canonicalDisplayName) && !cjk.containsMatchIn(it.objective.orEmpty()) })
    }

    @Test
    fun structuredChallengeObjectiveSemanticsRemainCanonical() {
        val rows = data.trainingQuests.filter { it.trainingClass == "CHALLENGE" }
        assertEquals("Slay 1 Great Wroggi", rows.single { it.id == "quest_challenge_05" }.objective)
        assertEquals("Slay 1 Arzuros · Slay 1 Lagombi", rows.single { it.id == "quest_challenge_06" }.objective)
        assertEquals("Slay 1 Brute Tigrex · Slay 1 Steel Uragaan", rows.single { it.id == "quest_challenge_14" }.objective)
        assertTrue(rows.all { it.objectiveTargets.isNotEmpty() })
    }

    @Test
    fun beginnerAndGroupTrainingRemainJapaneseSourceBackedAndUnchangedInCount() {
        assertEquals(22, data.trainingQuests.count { it.trainingClass == "BEGINNER" })
        assertEquals(6, data.trainingQuests.count { it.trainingClass == "GROUP" })
        assertFalse(data.trainingQuests.filter { it.trainingClass != "CHALLENGE" }.any { it.canonicalDisplayName.isBlank() })
    }

    @Test
    fun rewardGroupsAreLocalizedOnlyAtRuntime() {
        assertEquals("Guaranteed Rewards", trainingRewardGroupDisplayLabel("確定報酬"))
        assertEquals("Basic Rewards", trainingRewardGroupDisplayLabel("基本報酬"))
        assertEquals("Additional Rewards", trainingRewardGroupDisplayLabel("追加報酬"))
        assertEquals("Other", trainingRewardGroupDisplayLabel("Other"))
        val challenge = data.trainingQuests.single { it.id == "quest_challenge_01" }
        assertTrue(challenge.rewards.groupBy { it.rewardType }.keys.contains("確定報酬"))
    }
}
