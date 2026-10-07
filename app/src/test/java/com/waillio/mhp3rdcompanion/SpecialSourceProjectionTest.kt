package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.MaterialSource
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import com.waillio.mhp3rdcompanion.data.projectSpecialSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpecialSourceProjectionTest {
    private fun source(id: String, mechanism: String, quantity: Int? = null, quantitySemantics: String? = null) =
        MaterialSource(
            id = id,
            type = MaterialSourceType.SPECIAL_FREE,
            name = "Test",
            quantity = quantity,
            quantitySemantics = quantitySemantics,
            specialFreeMechanism = mechanism
        )

    @Test
    fun umbrellaCountsDistinctMethodsAndUsesStableOrder() {
        val projection = listOf(
            source("npc", "NPC_PROGRESSION_GRANT"),
            source("elder", "VEGGIE_ELDER_FREE_GIFT"),
            source("drink", "DRINK_TICKET_GRANT"),
            source("npc-2", "NPC_PROGRESSION_GRANT")
        ).projectSpecialSources()

        assertEquals(4, projection.rawRelationCount)
        assertEquals(3, projection.methodCount)
        assertEquals(4, projection.projectedRelationCount)
        assertEquals(
            listOf("VEGGIE_ELDER_FREE_GIFT", "NPC_PROGRESSION_GRANT", "DRINK_TICKET_GRANT"),
            projection.methods.map { it.mechanism }
        )
        assertEquals("Veggie Elder Gift", projection.methods[0].displayLabel)
        assertEquals("NPC Progression Reward", projection.methods[1].displayLabel)
        assertTrue(projection.methods.none { it.displayLabel.contains("Special / Free") })
    }

    @Test
    fun unpublishedQuantityIsNotProjectedAsOne() {
        val published = source("drink", "DRINK_TICKET_GRANT", quantity = 1, quantitySemantics = "PUBLISHED")
        val unpublished = source("elder", "VEGGIE_ELDER_FREE_GIFT", quantity = 1, quantitySemantics = "NOT_PUBLISHED")
        val projection = listOf(published, unpublished).projectSpecialSources()
        assertEquals(1, projection.methods.single { it.mechanism == "DRINK_TICKET_GRANT" }.entries.single().quantity)
        assertEquals(null, projection.methods.single { it.mechanism == "VEGGIE_ELDER_FREE_GIFT" }.entries.single().quantity)
        assertFalse(projection.rawRelationIds.isEmpty())
    }
}
