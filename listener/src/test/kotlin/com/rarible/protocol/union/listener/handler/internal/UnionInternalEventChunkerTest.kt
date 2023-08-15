package com.rarible.protocol.union.listener.handler.internal

import com.rarible.protocol.union.core.model.UnionCollectionChangeEvent
import com.rarible.protocol.union.core.model.UnionCollectionSetBaseUriEvent
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
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityBurn
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityMint
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityOrderList
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivitySale
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityTransfer
import com.rarible.protocol.union.enrichment.test.data.randomUnionBidOrder
import com.rarible.protocol.union.enrichment.test.data.randomUnionCollection
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionOwnership
import com.rarible.protocol.union.integration.ethereum.data.randomAddressString
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UnionInternalEventChunkerTest {

    private val unionInternalEventChunker = UnionInternalEventChunker()

    @Test
    fun `to chunks - individual events`() {
        val marks = stubEventMark()
        val activity = randomUnionActivityBurn()

        val events = listOf(
            UnionInternalActivityEvent(activity),
            UnionInternalOrderEvent(UnionOrderUpdateEvent(randomUnionBidOrder(), marks)),
            UnionInternalItemEvent(UnionItemUpdateEvent(randomUnionItem(), marks)),
            UnionInternalItemEvent(UnionItemDeleteEvent(randomEthItemId(), marks)),
            UnionInternalItemEvent(UnionItemChangeEvent(randomEthItemId(), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(), marks)),
            UnionInternalCollectionEvent(UnionCollectionUpdateEvent(randomUnionCollection(), marks)),
        ).shuffled()

        // Single chunks expected, no parallelism allowed
        val expected = events.map { listOf(it) }
        val chunks = unionInternalEventChunker.toChunks(events)

        assertThat(chunks).isEqualTo(expected)
    }

    @Test
    fun `to chunks - single chunk`() {
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
    fun `to chunks - with ownership chunk`() {
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
    fun `to chunks - chunk with squashed events`() {
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
    fun `to chunks - starts with chunk`() {
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
    fun `to chunks - ownership chunk with activities`() {
        val marks = stubEventMark()
        val itemId = randomEthItemId()
        val owner = randomAddressString()
        val ownershipId = randomEthOwnershipId(itemId, owner)

        val transfer = randomUnionActivityTransfer(itemId)
            .copy(owner = UnionAddress(BlockchainGroupDto.ETHEREUM, owner))

        val events = listOf(
            // First chunk - events related to different ownerships
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(ownershipId), marks)),
            UnionInternalActivityEvent(randomUnionActivityMint()),
            UnionInternalActivityEvent(randomUnionActivityBurn()),

            // Second chunk - sale activity, can't be handled in parallel
            UnionInternalActivityEvent(randomUnionActivitySale()),

            // Third chunk - ownership+activity which is not related to ownership updates at all
            UnionInternalOwnershipEvent(UnionOwnershipDeleteEvent(ownershipId, marks)),
            UnionInternalActivityEvent(randomUnionActivityOrderList()),

            // Fourth chunk - just ownership event
            UnionInternalOwnershipEvent(UnionOwnershipChangeEvent(ownershipId, marks)),

            // Fifth chunk - transfer related to prev ownership, should be extracted to separate chunk
            UnionInternalActivityEvent(transfer),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(), marks)),
        )

        val expected = listOf(
            listOf(events[0], events[1], events[2]),
            listOf(events[3]),
            listOf(events[4], events[5]),
            listOf(events[6]),
            listOf(events[7], events[8]),
        )
        val chunks = unionInternalEventChunker.toChunks(events)

        assertThat(chunks).isEqualTo(expected)
    }

    @Test
    fun `to chunks - ownership events squashed`() {
        val marks = stubEventMark()
        val ownershipId = randomEthOwnershipId()

        // Chunk at the end, squashed events distributed across entire batch
        val events = listOf(
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(ownershipId), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipChangeEvent(ownershipId, marks)),
            UnionInternalOwnershipEvent(UnionOwnershipChangeEvent(randomEthOwnershipId(), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipDeleteEvent(ownershipId, marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(ownershipId), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(ownershipId), marks)),
            UnionInternalOwnershipEvent(UnionOwnershipDeleteEvent(ownershipId, marks)),
            UnionInternalOwnershipEvent(UnionOwnershipChangeEvent(ownershipId, marks)),
        )

        val expected = listOf(
            listOf(events[2], events[4], events[6]),
            listOf(events[7]),
            listOf(events[8])
        )
        val chunks = unionInternalEventChunker.toChunks(events)

        assertThat(chunks).isEqualTo(expected)
    }

    @Test
    fun `to chunks - item events squashed`() {
        val marks = stubEventMark()
        val itemId = randomEthItemId()

        // Change and Update events for the same item should be squashed - but only in scope of their type
        val events = listOf(
            UnionInternalItemEvent(UnionItemUpdateEvent(randomUnionItem(itemId), marks)),
            UnionInternalItemEvent(UnionItemChangeEvent(itemId, marks)),
            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(), marks)),
            UnionInternalItemEvent(UnionItemUpdateEvent(randomUnionItem(itemId), marks)),
            UnionInternalItemEvent(UnionItemUpdateEvent(randomUnionItem(), marks)),
            UnionInternalItemEvent(UnionItemDeleteEvent(itemId, marks)),
            UnionInternalItemEvent(UnionItemChangeEvent(itemId, marks)),
            UnionInternalItemEvent(UnionItemDeleteEvent(itemId, marks)),
        )

        val expected = listOf(
            listOf(events[2]),
            listOf(events[3]),
            listOf(events[4]),
            listOf(events[6]),
            listOf(events[7])
        )
        val chunks = unionInternalEventChunker.toChunks(events)

        assertThat(chunks).isEqualTo(expected)
    }

    @Test
    fun `to chunks - order events squashed`() {
        val marks = stubEventMark()
        val order = randomUnionBidOrder()

        // Change and Update events for the same item should be squashed - but only in scope of their type
        val events = listOf(
            UnionInternalOrderEvent(UnionOrderUpdateEvent(randomUnionBidOrder(), marks)),
            UnionInternalOrderEvent(UnionOrderUpdateEvent(order, marks)),
            UnionInternalOrderEvent(UnionOrderUpdateEvent(randomUnionBidOrder(), marks)),
            UnionInternalOrderEvent(UnionOrderUpdateEvent(order, marks)),
            UnionInternalOrderEvent(UnionOrderUpdateEvent(order, marks)),
            UnionInternalOrderEvent(UnionOrderUpdateEvent(randomUnionBidOrder(), marks)),
        )

        val expected = listOf(
            listOf(events[0]),
            listOf(events[2]),
            listOf(events[4]),
            listOf(events[5])
        )
        val chunks = unionInternalEventChunker.toChunks(events)

        assertThat(chunks).isEqualTo(expected)
    }

    @Test
    fun `to chunks - collection events squashed`() {
        val marks = stubEventMark()
        val collectionId = randomEthCollectionId()

        // Change and Update events for the same item should be squashed - but only in scope of their type
        val events = listOf(
            UnionInternalCollectionEvent(UnionCollectionUpdateEvent(randomUnionCollection(collectionId), marks)),
            UnionInternalCollectionEvent(UnionCollectionChangeEvent(collectionId, marks)),
            UnionInternalCollectionEvent(UnionCollectionUpdateEvent(randomUnionCollection(collectionId), marks)),
            UnionInternalCollectionEvent(UnionCollectionSetBaseUriEvent(collectionId, marks)),
            UnionInternalCollectionEvent(UnionCollectionUpdateEvent(randomUnionCollection(), marks)),

            UnionInternalOwnershipEvent(UnionOwnershipUpdateEvent(randomUnionOwnership(), marks)),
            UnionInternalCollectionEvent(UnionCollectionChangeEvent(collectionId, marks)),
            UnionInternalCollectionEvent(UnionCollectionUpdateEvent(randomUnionCollection(collectionId), marks)),
            UnionInternalCollectionEvent(UnionCollectionSetBaseUriEvent(randomEthCollectionId(), marks)),
            UnionInternalCollectionEvent(UnionCollectionSetBaseUriEvent(collectionId, marks)),
        )

        val expected = listOf(
            listOf(events[4]),
            listOf(events[5]),
            listOf(events[6]),
            listOf(events[7]),
            listOf(events[8]),
            listOf(events[9]),
        )
        val chunks = unionInternalEventChunker.toChunks(events)

        assertThat(chunks).isEqualTo(expected)
    }
}
