package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.EntityType
import com.waillio.mhp3rdcompanion.data.FixtureData
import com.waillio.mhp3rdcompanion.data.Monster
import com.waillio.mhp3rdcompanion.data.SearchCorpus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SearchCorpusLatestQueryTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cachedCorpusSupportsFastQueryChangesAndMapLatestDropsAnOlderSlowSearch() = runTest {
        val corpus = SearchCorpus(
            FixtureData(
                monsters = listOf(
                    Monster("narga", "Nargacuga", "Flying Wyvern"),
                    Monster("narga-tail", "Nargacuga Tail", "Material")
                ),
                materials = emptyList(),
                quests = emptyList()
            )
        )
        val queries = MutableSharedFlow<String>(extraBufferCapacity = 8)
        val slowSearchStarted = CompletableDeferred<Unit>()
        var slowSearchCompleted = false
        val delivered = mutableListOf<Pair<String, List<String>>>()
        val collector = backgroundScope.launch {
            queries.mapLatest { query ->
                if (query == "N") {
                    slowSearchStarted.complete(Unit)
                    delay(1_000)
                    slowSearchCompleted = true
                }
                query to corpus.searchCancellable(query, EntityType.MONSTER).map { it.id }
            }.collect(delivered::add)
        }

        runCurrent() // start the shared-flow collector before emitting its first value
        queries.emit("N")
        runCurrent()
        slowSearchStarted.await()
        listOf("Na", "Nar", "Narg", "Narga", "Nargacuga").forEach { query ->
            queries.emit(query)
            runCurrent()
        }
        runCurrent()

        assertFalse(delivered.isEmpty())
        assertFalse("the older suspended query must be cancelled", slowSearchCompleted)
        assertFalse("the cancelled old result must not be published", delivered.any { it.first == "N" })
        assertEquals("Nargacuga" to listOf("narga", "narga-tail"), delivered.last())
        assertEquals(2, corpus.searchCancellable("Nargacuga", EntityType.MONSTER).size)
        collector.cancel()
    }
}
