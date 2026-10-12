package com.waillio.mhp3rdcompanion.data

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CharmTablePersistenceTest {
    @Test
    fun currentTablePersistsAcrossRepositoryRecreationAndCanBeCleared() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val firstRepository = CompanionRepository(context)
        val restoredRepository = CompanionRepository(context)
        try {
            firstRepository.saveCurrentCharmTable(7)
            assertEquals(7, withTimeout(5_000) { restoredRepository.currentCharmTable.first() })
            restoredRepository.saveCurrentCharmTable(null)
            assertEquals(null, withTimeout(5_000) { firstRepository.currentCharmTable.first() })
        } finally {
            firstRepository.saveCurrentCharmTable(null)
        }
    }

    @Test
    fun savedSearchPersistsExactOperatorsNegativeValuesAndCurrentTableModeAndCanBeDeleted() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CompanionRepository(context)
        val restoredRepository = CompanionRepository(context)
        val skillId = "saved-search-test-${System.nanoTime()}"
        val criteria = CharmSearchCriteria(
            skill1Id = skillId,
            skill1Points = -7,
            skill1Mode = CharmPointOperator.LESS_OR_EQUAL,
            skill2Id = "second-$skillId",
            skill2Points = 0,
            skill2Mode = CharmPointOperator.GREATER_THAN,
            slots = 2,
            slotMode = CharmSlotMode.EXACT,
            tableMode = CharmTableMode.CURRENT,
            talismanClass = TalismanClass.DRAGON
        )
        try {
            repository.saveCharmSearch("Negative Current", criteria)
            val saved = withTimeout(5_000) {
                restoredRepository.charmSavedSearches.first { entries -> entries.any { it.criteria == criteria } }
            }.single { it.criteria == criteria }
            assertEquals("Negative Current", saved.name)
            assertEquals(criteria, saved.criteria)

            restoredRepository.deleteCharmSearch(saved.id)
            assertTrue(withTimeout(5_000) { repository.charmSavedSearches.first { entries -> entries.none { it.id == saved.id } } }.none { it.id == saved.id })
        } finally {
            val existing = repository.charmSavedSearches.first()
            existing.filter { it.criteria == criteria }.forEach { repository.deleteCharmSearch(it.id) }
        }
    }
}
