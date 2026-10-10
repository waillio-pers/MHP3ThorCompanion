package com.waillio.mhp3rdcompanion.data

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
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
}
