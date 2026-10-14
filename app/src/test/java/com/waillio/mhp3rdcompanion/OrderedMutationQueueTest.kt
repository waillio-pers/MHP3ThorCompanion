package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.OrderedMutationQueue
import com.waillio.mhp3rdcompanion.data.FavoriteEntry
import com.waillio.mhp3rdcompanion.data.RecentEntry
import com.waillio.mhp3rdcompanion.data.ReferenceEntityType
import com.waillio.mhp3rdcompanion.data.ReferenceHistoryLogic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class OrderedMutationQueueTest {
    private sealed interface Intent {
        data class ToggleFavorite(val id: String, val at: Long) : Intent
        data class Open(val id: String, val at: Long) : Intent
        data class SetTable(val table: Int?) : Intent
    }

    @Test
    fun acceptedMutationsAreAppliedInOrderEvenWhenFirstWriteSuspends() = runTest {
        val applied = mutableListOf<Int>()
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val queue = OrderedMutationQueue<Int>(
            scope = this,
            context = EmptyCoroutineContext,
            apply = { value ->
                if (value == 1) {
                    firstStarted.complete(Unit)
                    releaseFirst.await()
                }
                applied += value
            }
        )

        queue.submit(1)
        queue.submit(2)
        runCurrent()
        assertTrue(firstStarted.isCompleted)
        assertEquals(emptyList<Int>(), applied)

        releaseFirst.complete(Unit)
        runCurrent()
        assertEquals(listOf(1, 2), applied)
        queue.close()
        runCurrent()
    }

    @Test
    fun rapidMixedPreferenceActionsKeepEventOrderAndDoNotRestoreStaleSnapshots() = runTest {
        var favorites = emptyList<FavoriteEntry>()
        var recent = emptyList<RecentEntry>()
        var table: Int? = null
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val queue = OrderedMutationQueue<Intent>(
            scope = this,
            context = EmptyCoroutineContext,
            apply = { intent ->
                if (intent == Intent.ToggleFavorite("A", 1)) {
                    firstStarted.complete(Unit)
                    releaseFirst.await()
                }
                when (intent) {
                    is Intent.ToggleFavorite -> favorites = ReferenceHistoryLogic.toggleFavorite(
                        favorites, ReferenceEntityType.MONSTER, intent.id, intent.at
                    )
                    is Intent.Open -> recent = ReferenceHistoryLogic.recordRecent(
                        recent, ReferenceEntityType.MONSTER, intent.id, intent.at
                    )
                    is Intent.SetTable -> table = intent.table
                }
            }
        )

        queue.submit(Intent.ToggleFavorite("A", 1))
        queue.submit(Intent.ToggleFavorite("B", 2))
        queue.submit(Intent.Open("A", 3))
        queue.submit(Intent.SetTable(2))
        queue.submit(Intent.ToggleFavorite("A", 4)) // remove A after its add
        queue.submit(Intent.ToggleFavorite("B", 5)) // toggle B off, then on
        queue.submit(Intent.ToggleFavorite("B", 6))
        queue.submit(Intent.Open("B", 7))
        queue.submit(Intent.Open("C", 8))
        queue.submit(Intent.Open("B", 9)) // duplicate recent moves to front
        queue.submit(Intent.SetTable(7))
        runCurrent()
        assertTrue(firstStarted.isCompleted)
        assertEquals(emptyList<FavoriteEntry>(), favorites)
        assertEquals(emptyList<RecentEntry>(), recent)
        assertEquals(null, table)

        releaseFirst.complete(Unit)
        runCurrent()
        assertEquals(listOf("B"), favorites.map { it.entityId })
        assertEquals(listOf("B", "C", "A"), recent.map { it.entityId })
        assertEquals(7, table)
        queue.close()
        runCurrent()
    }
}
