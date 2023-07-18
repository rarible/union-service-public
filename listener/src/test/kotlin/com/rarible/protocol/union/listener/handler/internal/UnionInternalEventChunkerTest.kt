package com.rarible.protocol.union.listener.handler.internal

import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.core.model.UnionInternalActivityEvent
import com.rarible.protocol.union.core.model.UnionInternalCollectionEvent
import com.rarible.protocol.union.core.model.UnionInternalItemEvent
import com.rarible.protocol.union.core.model.UnionInternalOrderEvent
import com.rarible.protocol.union.core.model.UnionInternalOwnershipEvent
import com.rarible.protocol.union.core.model.UnionItemChangeEvent
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.core.model.UnionOwnershipChangeEvent
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.core.model.stubEventMark
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityBurn
import com.rarible.protocol.union.enrichment.test.data.randomUnionBidOrder
import com.rarible.protocol.union.enrichment.test.data.randomUnionCollection
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionOwnership
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UnionInternalEventChunkerTest {

    private val unionInternalEventChunker = UnionInternalEventChunker()

    @Test
    fun `to chunks - ok, individual events`() {
        val marks = stubEventMark()
        val activity = randomUnionActivityBurn()

        val events = listOf(
            UnionInternalActivityEvent(activity),
            UnionInternalOrderEvent(UnionOrderUpdateEvent(randomUnionBidOrder(), marks)),
            UnionInternalItemEvent(UnionItemUpdateEvent(randomUnionItem(), marks)),
            UnionInternalItemEvent(UnionItemDeleteEvent(randomEthItemId(), marks)),
            UnionInternalItemEvent(UnionItemChangeEvent(randomEthItemId(), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipDeleteEvent(randomEthOwnershipId(), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipChangeEvent(randomEthOwnershipId(), marks)),
            UnionInternalCollectionEvent(UnionCollectionUpdateEvent(randomUnionCollection(), marks)),
        ).shuffled()

        // Single chunks expected, no parallelism allowed
        val expected = events.map { listOf(it) }
        val chunks = unionInternalEventChunker.toChunks(events)

        assertThat(chunks).isEqualTo(expected)
    }

    @Test
    fun `to chunks - ok, single chunk`() {
        val marks = stubEventMark()

        val events = listOf(
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(), marks)),
        )

        val expected = listOf(events)
        val chunks = unionInternalEventChunker.toChunks(events)

        assertThat(chunks).isEqualTo(expected)
    }

    @Test
    fun `to chunks - ok, with ownership chunk`() {
        val marks = stubEventMark()

        val events = listOf(
            UnionInternalOrderEvent(UnionOrderUpdateEvent(randomUnionBidOrder(), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(), marks)),
            UnionInternalCollectionEvent(UnionCollectionUpdateEvent(randomUnionCollection(), marks)),
        )

        val expected = listOf(
            listOf(events[0]),
            listOf(events[1], events[2]),
            listOf(events[3])
        )
        val chunks = unionInternalEventChunker.toChunks(events)

        assertThat(chunks).isEqualTo(expected)
    }

    @Test
    fun `to chunks - ok, with same ownership id, ends with chunk`() {
        val marks = stubEventMark()

        val ownershipId = randomEthOwnershipId()

        val events = listOf(
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(ownershipId), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(ownershipId), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(), marks)),
        )

        // Since 0 and 1 have same ID, they should be in different chunks
        val expected = listOf(
            listOf(events[0]),
            listOf(events[1], events[2])
        )
        val chunks = unionInternalEventChunker.toChunks(events)

        assertThat(chunks).isEqualTo(expected)
    }

    @Test
    fun `to chunks - ok, starts with chunk`() {
        val marks = stubEventMark()

        val events = listOf(
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(), marks)),
            UnionInternalItemEvent(UnionItemChangeEvent(randomEthItemId(), marks)),
        )

        val expected = listOf(
            listOf(events[0], events[1], events[2]),
            listOf(events[3])
        )
        val chunks = unionInternalEventChunker.toChunks(events)

        assertThat(chunks).isEqualTo(expected)
    }
}