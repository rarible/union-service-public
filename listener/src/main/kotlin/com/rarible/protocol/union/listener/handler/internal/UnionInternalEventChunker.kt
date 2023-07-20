package com.rarible.protocol.union.listener.handler.internal

import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.core.model.UnionInternalActivityEvent
import com.rarible.protocol.union.core.model.UnionInternalAuctionEvent
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import com.rarible.protocol.union.core.model.UnionInternalCollectionEvent
import com.rarible.protocol.union.core.model.UnionInternalItemEvent
import com.rarible.protocol.union.core.model.UnionInternalOrderEvent
import com.rarible.protocol.union.core.model.UnionInternalOwnershipEvent
import com.rarible.protocol.union.core.model.UnionItemChangeEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.core.model.UnionOwnershipChangeEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import org.springframework.stereotype.Component

@Component
class UnionInternalEventChunker {

    private val squashableEventTypes = setOf(
        // Since we send actual data in Updated events, we can use only last one
        UnionItemUpdateEvent::class.java,
        UnionOwnershipUpdateEvent::class.java,
        UnionOrderUpdateEvent::class.java,
        UnionCollectionUpdateEvent::class.java,

        // The same for change events - if there are several in the batch, we can handle only last one
        UnionItemChangeEvent::class.java,
        UnionOwnershipChangeEvent::class.java
    )

    // IMPORTANT! We suggest these events have SAME key in Kafka
    fun toChunks(events: List<UnionInternalBlockchainEvent>): List<List<UnionInternalBlockchainEvent>> {
        val squashed = squash(events)
        val state = ChunkerState(squashed)
        squashed.forEach { event ->
            val entityEvent = event.data()
            val current = state.current

            when {
                // In an empty chunk we add any event without conditions
                current.isEmpty() -> state.add(event)

                // For some events of same type we can do parallel handling,
                // ATM implemented only for ownerships
                (current.containsOnlyTypeOf(event)) -> when {
                    // Ownership updates can be handled in parallel - but not for the same ownership
                    entityEvent is UnionOwnershipUpdateEvent && !current.containsId(event) -> state.add(event)
                    else -> state.flushAndAdd(event)
                }

                else -> state.flushAndAdd(event)
            }
        }
        state.flush()
        return state.result
    }

    private fun squash(events: List<UnionInternalBlockchainEvent>): List<UnionInternalBlockchainEvent> {
        val ids = HashSet<Pair<Class<*>, Any>>(events.size)
        return events.reversed().filter {
            val type = it.data().javaClass
            val key = Pair(type, it.getEntityId())
            if (type in squashableEventTypes) {
                ids.add(key)
            } else {
                true
            }
        }.reversed()
    }

    private class ChunkerState(events: List<UnionInternalBlockchainEvent>) {
        val result = ArrayList<List<UnionInternalBlockchainEvent>>(events.size)
        var current = UnionIndependentEventChunk()

        fun flush() {
            if (current.isEmpty()) {
                return
            }
            result.add(current.chunk)
            current = UnionIndependentEventChunk()
        }

        fun add(event: UnionInternalBlockchainEvent) {
            current.add(event)
        }

        fun flushAndAdd(event: UnionInternalBlockchainEvent) {
            flush()
            current.add(event)
        }
    }

    private class UnionIndependentEventChunk {

        // In most cases there will be only 1 event
        val chunk = ArrayList<UnionInternalBlockchainEvent>(4)
        private val typesInChunk = HashSet<Class<*>>(4)
        private val entityIds = HashSet<Any>(4)

        fun add(event: UnionInternalBlockchainEvent) {
            chunk.add(event)
            typesInChunk.add(event.data().javaClass)
            entityIds.add(event.getEntityId())
        }

        fun containsOnlyTypeOf(event: UnionInternalBlockchainEvent): Boolean {
            return typesInChunk.size == 1 && containsTypeOf(event)
        }

        fun containsTypeOf(event: UnionInternalBlockchainEvent): Boolean {
            return typesInChunk.contains(event.data().javaClass)
        }

        fun containsId(event: UnionInternalBlockchainEvent): Boolean {
            return entityIds.contains(event.getEntityId())
        }

        fun isEmpty(): Boolean {
            return chunk.isEmpty()
        }
    }
}

private fun UnionInternalBlockchainEvent.data(): Any = when (this) {
    is UnionInternalItemEvent -> event
    is UnionInternalOwnershipEvent -> event
    is UnionInternalActivityEvent -> event
    is UnionInternalAuctionEvent -> event
    is UnionInternalCollectionEvent -> event
    is UnionInternalOrderEvent -> event
    else -> throw IllegalArgumentException("Unexpected event type: $this")
}