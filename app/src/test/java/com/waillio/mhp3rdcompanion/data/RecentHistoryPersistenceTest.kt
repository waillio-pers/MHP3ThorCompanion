package com.waillio.mhp3rdcompanion.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
class RecentHistoryPersistenceTest {
    @Test
    fun sixRecentItemsRestoreAcrossRepositoryRecreation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val writer = CompanionRepository(context)
        val restored = CompanionRepository(context)
        val expected = (1..6).map { RecentEntry(ReferenceEntityType.MONSTER, "monster_$it", it.toLong()) }
        try {
            writer.saveRecent(expected)
            assertEquals(expected, withTimeout(5_000) { restored.recent.first() })
        } finally {
            writer.saveRecent(emptyList())
        }
    }
}
