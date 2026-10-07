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
class QuestLocalizationIntegrationTest {
    private val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data
    private val cjk = Regex("[\\u3040-\\u30ff\\u3400-\\u4dbf\\u4e00-\\u9fff]")

    @Test
    fun allRegularQuestRuntimeDisplayFieldsAreEnglish() {
        assertEquals(354, data.quests.size)
        assertTrue(data.quests.all { !cjk.containsMatchIn(it.name) })
        assertTrue(data.quests.all { !cjk.containsMatchIn(it.objective.orEmpty()) })
        assertEquals(0, data.quests.count { cjk.containsMatchIn(it.name) || cjk.containsMatchIn(it.objective.orEmpty()) })
    }

    @Test
    fun eventAndAcceptedCorrectionCoverageIsPresent() {
        assertEquals(52, data.quests.count { it.category == "Event" })
        assertEquals(7, data.quests.count { it.category == "Hot Spring" })
        assertEquals(20, data.quests.count { it.category == "Drink" })
        val event603 = data.quests.single { it.id == "quest_event_603" }
        assertEquals("Survive until Time Over or deliver the Paw Pass Ticket after hunting at least 2 Gigginox; automatic clear after hunting 10 Gigginox.", event603.objective)
        assertFalse(event603.objective.orEmpty().contains("less than 2"))
        assertEquals("Deliver 3 Wyvern Eggs", data.quests.single { it.id == "quest_event_622" }.objective)
        val event635 = data.quests.single { it.id == "quest_event_635" }
        assertEquals("Gargwa, Eggs, and Streams", event635.name)
        assertEquals("Deliver 10 Gargwa Eggs", event635.objective)
    }

    @Test
    fun eventTerminologyAndFilterCoverageRemainStable() {
        val text = data.quests.filter { it.category == "Event" }.joinToString(" ") { "${it.name} ${it.objective}" }
        listOf("Jinouga", "Rangurotora", "Hapurubokka", "Aoashira", "Urcusis", "Doboruberuku", "Amatsumagatsuchi", "Thunder Gigginox", "Black Tigrex", "Ice Barroth", "Ice Agnaktor", "Red Qurupeco", "Gagua").forEach { assertFalse(text.contains(it)) }
        listOf("Zinogre", "Volvidon", "Nibelsnarf", "Arzuros", "Lagombi", "Duramboros", "Amatsu", "Baleful Gigginox", "Brute Tigrex", "Jade Barroth", "Glacial Agnaktor", "Crimson Qurupeco", "Gargwa").forEach { assertTrue(text.contains(it)) }
        assertEquals(52, filterQuests(data.quests, "", null, QuestCategoryFilter.EVENT).size)
        assertEquals(354, filterQuests(data.quests, "", null, QuestCategoryFilter.ALL).size)
    }
}
