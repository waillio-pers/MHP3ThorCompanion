package com.waillio.mhp3rdcompanion.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/** A small FIFO for persisted user intents; every handler still uses DataStore's atomic edit. */
internal class OrderedMutationQueue<T>(
    scope: CoroutineScope,
    context: CoroutineContext = Dispatchers.IO,
    private val apply: suspend (T) -> Unit,
    private val onFailure: (Throwable) -> Unit = {}
) {
    private val channel = Channel<T>(Channel.UNLIMITED)

    init {
        scope.launch(context) {
            for (mutation in channel) {
                try {
                    apply(mutation)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Throwable) {
                    onFailure(failure)
                }
            }
        }
    }

    fun submit(mutation: T) {
        check(channel.trySend(mutation).isSuccess) { "Persistence queue is closed" }
    }

    fun close() {
        channel.close()
    }
}
