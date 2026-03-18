package io.github.couchtracker.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * A class to publish events and get a flow to subscribe to them
 */
class EventBus<T : Any>(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) {
    private val pendingEvents = Channel<T>(capacity = Channel.UNLIMITED)
    private val flow = MutableSharedFlow<T>()

    init {
        scope.launch {
            pendingEvents.consumeEach {
                flow.emit(it)
            }
        }
    }

    /**
     * Subscribes to events of this events bus.
     * Note: `null` is immediately emitted once, to allow for the subscriber to do something immediately
     */
    fun subscribe(): Flow<T?> {
        return flow {
            emit(null)
            emitAll(flow)
        }
    }

    /**
     * Publishes an event.
     * Importantly, this does _not_ suspend until the event is sent and received.
     * This allows calling this function from a subscriber.
     */
    fun publish(event: T) {
        check(pendingEvents.trySend(event).isSuccess) {
            "Event $event not published"
        }
    }
}
