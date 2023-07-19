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
    fun `to chunks - ok, chunk with squashed events`() {
        val marks = stubEventMark()

        val ownershipId = randomEthOwnershipId()

        val events = listOf(
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(ownershipId), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(ownershipId), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(), marks)),
        )

        val expected = listOf(
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

    @Test
    fun `to chunks - ok, item events squashed`() {
        val marks = stubEventMark()
        val itemId = randomEthItemId()

        // Only last item update should stay
        val events = listOf(
            UnionInternalItemEvent(UnionItemUpdateEvent(randomUnionItem(itemId), marks)),
            UnionInternalItemEvent(UnionItemUpdateEvent(randomUnionItem(), marks)),
            UnionInternalItemEvent(UnionItemUpdateEvent(randomUnionItem(itemId), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(), marks)),
            UnionInternalItemEvent(UnionItemUpdateEvent(randomUnionItem(itemId), marks)),
            UnionInternalItemEvent(UnionItemUpdateEvent(randomUnionItem(), marks)),
        )

        val expected = listOf(
            listOf(events[1]),
            listOf(events[3]),
            listOf(events[4]),
            listOf(events[5])
        )
        val chunks = unionInternalEventChunker.toChunks(events)

        assertThat(chunks).isEqualTo(expected)
    }

    @Test
    fun `to chunks - ok, ownership events squashed`() {
        val marks = stubEventMark()
        val ownershipId = randomEthOwnershipId()

        // Chunk at the end, squashed events distributed across entire batch
        val events = listOf(
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(ownershipId), marks)),
            UnionInternalItemEvent(UnionItemUpdateEvent(randomUnionItem(), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(ownershipId), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(ownershipId), marks))
        )

        val expected = listOf(
            listOf(events[1]),
            listOf(events[2], events[4])
        )
        val chunks = unionInternalEventChunker.toChunks(events)

        assertThat(chunks).isEqualTo(expected)
    }

    @Test
    fun `to chunks - ok, item different events squashed`() {
        val marks = stubEventMark()
        val itemId = randomEthItemId()

        // Change and Update events for the same item should be squashed - but only in scope of their type
        val events = listOf(
            UnionInternalItemEvent(UnionItemUpdateEvent(randomUnionItem(itemId), marks)),
            UnionInternalItemEvent(UnionItemChangeEvent(itemId, marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(), marks)),
            UnionInternalItemEvent(UnionItemUpdateEvent(randomUnionItem(itemId), marks)),
            UnionInternalItemEvent(UnionItemUpdateEvent(randomUnionItem(), marks)),
            UnionInternalItemEvent(UnionItemChangeEvent(itemId, marks)),
        )

        val expected = listOf(
            listOf(events[2]),
            listOf(events[3]),
            listOf(events[4]),
            listOf(events[5])
        )
        val chunks = unionInternalEventChunker.toChunks(events)

        assertThat(chunks).isEqualTo(expected)
    }
}