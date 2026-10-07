package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.GeneratedDataset
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TmoTerminologyIntegrationTest {
    private val repository = CompanionRepository(ApplicationProvider.getApplicationContext())

    @Test
    fun allTrainingVocabularyUsesCanonicalOverlayWithEnumeratedAliases() {
        val terminology = repository.displayTerminology
        assertEquals(151, terminology.trainingArmorByRaw.size)
        assertEquals(49, terminology.trainingSkillByRaw.size)
        assertEquals(6, terminology.trainingCharmByRaw.size)
        assertEquals("Arzuros Vambraces S", terminology.trainingArmorByRaw["アシラSアーム"])
        assertEquals("Jaggi Greaves", terminology.trainingArmorByRaw["ジャギィグリーブ"])
        assertEquals("Blast Earring", terminology.trainingArmorByRaw["爆砕のピアス"])
        assertEquals("Cannon Earring", terminology.trainingArmorByRaw["大砲のピアス"])
        assertEquals("Wyvern Earring", terminology.trainingArmorByRaw["幻獣のピアス"])
        assertEquals("Sheathe Earring", terminology.trainingArmorByRaw["早納のピアス"])
        assertEquals("No Equipment", terminology.trainingArmorByRaw["装備なし"])
        assertEquals("No Equipment", terminology.trainingCharmByRaw["装備無し"])
        assertEquals("Bishop Talisman", terminology.trainingCharmByRaw["闘士の護石"])
        assertEquals("Critical Draw", terminology.trainingSkillByRaw["抜刀術【技】"])
        assertFalse(terminology.trainingSkillByRaw["抜刀術【技】"] == "Punishing Draw")
        assertEquals("Status Atk +1", terminology.trainingSkillByRaw["状態異常+1"])
    }

    @Test
    fun specialObjectivesAreReusableAndFactualCorpusIsUnchanged() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val generated = context.assets.open("mhp3rd-data.json").bufferedReader().use {
            Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(it.readText())
        }
        assertEquals(31, generated.schemaVersion)
        assertEquals(55, generated.displayTerminology?.objectiveByStableId?.size)
        assertEquals(978, generated.items.size)
        assertEquals(987, generated.weapons.size)
        assertEquals(40, generated.monsters.size)
        assertEquals(20, generated.smallMonsters.size)
        assertEquals(354, generated.quests.size)
        assertEquals(42, generated.trainingQuests.size)
        assertEquals(112, generated.trainingQuests.sumOf { it.loadouts.size })
        assertEquals(35, generated.trainingQuests.sumOf { q -> q.loadouts.count { it.weaponStableId != null } })
        assertTrue(repository.trainingObjectiveDisplay("quest_group_training_20201", "fallback").isNullOrBlank().not())
    }
}
