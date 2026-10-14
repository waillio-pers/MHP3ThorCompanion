package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.QuestProgressionData
import com.waillio.mhp3rdcompanion.data.SUPPORTED_QUEST_PROGRESSION_SCHEMA_VERSION
import com.waillio.mhp3rdcompanion.data.validateQuestProgressionSchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestProgressionSchemaTest {
    @Test
    fun supportedProgressionSchemaLoadsAndUnsupportedVersionFailsClearly() {
        val supported = QuestProgressionData.empty().copy(schemaVersion = SUPPORTED_QUEST_PROGRESSION_SCHEMA_VERSION)
        assertEquals(supported, validateQuestProgressionSchema(supported))

        val unsupported = QuestProgressionData.empty().copy(schemaVersion = SUPPORTED_QUEST_PROGRESSION_SCHEMA_VERSION + 1)
        val failure = runCatching { validateQuestProgressionSchema(unsupported) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure?.message.orEmpty().contains("Unsupported quest progression asset schema"))
    }
}
