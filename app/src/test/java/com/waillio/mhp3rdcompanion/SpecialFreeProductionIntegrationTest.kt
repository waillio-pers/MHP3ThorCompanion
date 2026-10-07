package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.GeneratedDataset
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import com.waillio.mhp3rdcompanion.data.projectSpecialSources
import kotlinx.serialization.json.Json as KotlinJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class SpecialFreeProductionIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun generated(): GeneratedDataset = context.assets.open("mhp3rd-data.json").bufferedReader().use {
        KotlinJson { ignoreUnknownKeys = true }.decodeFromString(it.readText())
    }

    @Test
    fun productionSpecialFreeLayerHasAcceptedCensusAndNoContamination() {
        val data = generated()
        assertEquals(31, data.schemaVersion)
        assertEquals(61, data.itemSpecialFreeAcquisitionRelations.size)
        assertEquals(39, data.itemSpecialFreeAcquisitionRelations.map { it.outputGameItemId }.distinct().size)
        assertEquals(mapOf(
            "NPC_PROGRESSION_GRANT" to 18,
            "INITIAL_FREE_GRANT" to 1,
            "VILLAGE_INTERACTION_GIFT" to 1,
            "DRINK_TICKET_GRANT" to 1,
            "HOT_SPRING_TICKET_GRANT" to 1,
            "PALICO_AFFECTION_TICKET_GRANT" to 8,
            "GUILD_FRIENDSHIP_TICKET_GRANT" to 5,
            "VEGGIE_ELDER_FREE_GIFT" to 25,
            "PROGRESSION_COMPLETION_GRANT" to 1,
        ), data.itemSpecialFreeAcquisitionRelations.groupingBy { it.mechanism }.eachCount())
        assertTrue(data.itemSpecialFreeAcquisitionRelations.none { it.mechanism == "FURATTO_HUNTER" })
        assertTrue(data.itemSpecialFreeAcquisitionRelations.none { it.mechanism == "SCRAP_CONVERSION" })
        assertTrue(data.itemSpecialFreeAcquisitionRelations.none { it.outputGameItemId == 0 })
    }

    @Test
    fun reverseProjectionPreservesExamplesAndSeparatesSpecialFree() {
        val repo = CompanionRepository(context).data
        val egg = repo.materials.single { it.gameItemId == 51 }
        assertTrue(egg.sources.any { it.type == MaterialSourceType.SPECIAL_FREE && it.id == "VILLAGE_EGG_INTERACTION" })
        assertTrue(repo.materials.single { it.gameItemId == 608 }.sources.any { it.id == "VALL_CHIEF_TICKET" && it.quantity == 10 })
        val hotSpring = repo.materials.single { it.gameItemId == 610 }
        val hot = hotSpring.sources.single { it.type == MaterialSourceType.SPECIAL_FREE }
        assertEquals(null, hot.chance)
        assertEquals("EXACT_TRIGGER_UNRESOLVED", hot.specialFreeOutcomeSelection)
        assertTrue(repo.materials.single { it.gameItemId == 611 }.sources.count { it.type == MaterialSourceType.SPECIAL_FREE } == 8)
        assertTrue(repo.materials.single { it.gameItemId == 612 }.sources.any { it.specialFreeMechanism == "GUILD_FRIENDSHIP_TICKET_GRANT" })
        assertTrue(repo.materials.single { it.gameItemId == 20 }.sources.any { it.type == MaterialSourceType.SPECIAL_FREE })
    }

    @Test
    fun specialPresentationProjectionPreservesEveryRelationAndDistinctMethodCount() {
        val materials = CompanionRepository(context).data.materials
        val special = materials.flatMap { it.sources }.filter { it.type == MaterialSourceType.SPECIAL_FREE }
        assertEquals(61, special.size)
        val palico = materials.single { it.gameItemId == 611 }.sources.projectSpecialSources()
        assertEquals(8, palico.rawRelationCount)
        assertEquals(1, palico.methodCount)
        assertEquals(8, palico.projectedRelationCount)
        assertEquals("PALICO_AFFECTION_TICKET_GRANT", palico.methods.single().mechanism)
        val egg = materials.single { it.gameItemId == 51 }.sources.projectSpecialSources()
        assertEquals(2, egg.methodCount)
        assertEquals(2, egg.projectedRelationCount)
        assertTrue(egg.methods.map { it.displayLabel }.none { it == "Special / Free" })
    }

    @Test
    fun embeddedJsonHasExpectedIntegratedSha() {
        val bytes = context.assets.open("mhp3rd-data.json").use { it.readBytes() }
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02X".format(it) }
        assertEquals("657CFA9376FC8CBE56C67E67D4D1A27C455D21021F51E21B62427C8DDBB26155", sha)
        assertFalse(String(bytes, Charsets.UTF_8).contains("SCRAP_CONVERSION"))
    }
}
