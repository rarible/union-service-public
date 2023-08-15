package com.rarible.protocol.union.listener.handler.internal

import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.UnionInternalActivityEvent
import com.rarible.protocol.union.core.model.UnionInternalAuctionEvent
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import com.rarible.protocol.union.core.model.UnionInternalCollectionEvent
import com.rarible.protocol.union.core.model.UnionInternalItemEvent
import com.rarible.protocol.union.core.model.UnionInternalOrderEvent
import com.rarible.protocol.union.core.model.UnionInternalOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOrderMatchSell
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.dto.OwnershipIdDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UnionInternalEventChunker {

    private val logger = LoggerFactory.getLogger(javaClass)

    // IMPORTANT! We suggest these events have SAME key in Kafka
    fun toChunks(events: List<UnionInternalBlockchainEvent>): List<List<UnionInternalBlockchainEvent>> {
        val squashed = squash(events)
        val state = ChunkerState(squashed)
        squashed.forEach { state.add(it) }
        state.flush()
        return state.result
    }

    private fun squash(events: List<UnionInternalBlockchainEvent>): List<UnionInternalBlockchainEvent> {
        return events
            .reversed()
            .distinctBy { Pair(it.data().javaClass, it.getEntityId()) }
            .reversed()
    }

    private class ChunkerState(events: List<UnionInternalBlockchainEvent>) {
        val result = ArrayList<List<UnionInternalBlockchainEvent>>(events.size)
        var current: Chunk? = null

        fun flush() {
            if (current?.isEmpty() == true) {
                return
            }
            result.add(current!!.chunk)
            current = null
        }

        fun add(event: UnionInternalBlockchainEvent) {
            val chunk = getCurrentChunk(event)
            if (chunk.isAcceptable(event)) {
                chunk.add(event)
            } else {
                flush()
                add(event)
            }
        }

        private fun getCurrentChunk(event: UnionInternalBlockchainEvent): Chunk {
            current?.let { return it }

            current = when {
                OwnershipChunk.EMPTY.isAcceptable(event) -> OwnershipChunk()
                else -> DefaultChunk()
            }

            return current!!
        }
    }

    // We consider there is no events with same entityId in batch after squash
    private abstract class Chunk {
        val chunk = ArrayList<UnionInternalBlockchainEvent>(4)
        private val entityIds = HashSet<Any>(4)

        fun isEmpty() = chunk.isEmpty()
        open fun add(event: UnionInternalBlockchainEvent) {
            chunk.add(event)
            entityIds.add(event.getEntityId())
        }

        open fun isAcceptable(event: UnionInternalBlockchainEvent): Boolean = !entityIds.contains(event.getEntityId())
    }

    // By default, we do NOT join events in chunks
    private class DefaultChunk : Chunk() {
        override fun isAcceptable(event: UnionInternalBlockchainEvent) = isEmpty()
    }

    // All Ownership events can be joined to the chunk
    private class OwnershipChunk : Chunk() {

        private val ownershipIds = HashSet<OwnershipIdDto>(4)

        override fun add(event: UnionInternalBlockchainEvent) {
            super.add(event)
            when (val data = event.data()) {
                is UnionOwnershipEvent -> ownershipIds.add(data.ownershipId)
                is UnionActivity -> data.ownershipId()?.let { ownershipIds.add(it) }
            }
        }

        override fun isAcceptable(event: UnionInternalBlockchainEvent): Boolean {
            val data = event.data()
            return super.isAcceptable(event) &&
                (data is UnionOwnershipEvent || checkActivityCompatibleWithOwnershipEvents(data))
        }

        private fun checkActivityCompatibleWithOwnershipEvents(
            data: Any
        ): Boolean {
            // MatchSell affects Item update, so can't be updated in parallel with other events
            if (data !is UnionActivity || data is UnionOrderMatchSell) {
                return false
            }

            // Such activities can't trigger ownership updates, can be handled in parallel
            val ownershipId = data.ownershipId() ?: return true

            // There is already something with same ownershipId, chunk should be finalised
            return !ownershipIds.contains(ownershipId)
        }

        companion object {
            val EMPTY = OwnershipChunk()
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
