package com.rarible.protocol.union.worker.job.collection

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address

class CustomCollectionItemFetcherByCollectionTest {

    private val customCollectionItemProvider: CustomCollectionItemProvider = mockk()

    @Test
    fun `fetch by batches - ok, single collection`() = runBlocking<Unit> {
        val collectionId1 = randomEthCollectionId()
        val ethCollection1 = collectionId1.value

        val item1 = randomUnionItem(ItemIdDto(BlockchainDto.ETHEREUM, ethCollection1, "1".toBigInteger()))
        val item2 = randomUnionItem(ItemIdDto(BlockchainDto.ETHEREUM, ethCollection1, "2".toBigInteger()))
        val state1 = DateIdContinuation(item2.lastUpdatedAt, item2.id.fullId())
        val nativeState1 = DateIdContinuation(item2.lastUpdatedAt, item2.id.value)

        val fetcher = CustomCollectionItemFetcherByCollection(customCollectionItemProvider, listOf(collectionId1))
        coEvery { customCollectionItemProvider.fetch(collectionId1, null, 2) } returns listOf(item1, item2)
        coEvery { customCollectionItemProvider.getItemCollectionId(item2.id) } returns collectionId1
        coEvery { customCollectionItemProvider.fetch(collectionId1, nativeState1.toString(), 2) } returns listOf()

        // First batch
        val batch1 = fetcher.next(null, 2)
        assertThat(batch1.items).isEqualTo(listOf(item1, item2))
        assertThat(batch1.state).isEqualTo(state1.toString())

        // Second batch
        val batch2 = fetcher.next(batch1.state, 2)
        assertThat(batch2.items).isEmpty()
        assertThat(batch2.state).isNull()
    }

    @Test
    fun `fetch by batches - ok, several collections`() = runBlocking<Unit> {
        val collectionId1 = CollectionIdDto(BlockchainDto.ETHEREUM, Address.ONE().prefixed())
        val ethCollection1 = collectionId1.value

        val collectionId2 = CollectionIdDto(BlockchainDto.ETHEREUM, Address.TWO().prefixed())

        val collectionId3 = CollectionIdDto(BlockchainDto.ETHEREUM, Address.THREE().prefixed())
        val ethCollection3 = collectionId3.value

        val item1 = randomUnionItem(ItemIdDto(BlockchainDto.ETHEREUM, ethCollection1, "1".toBigInteger()))
        val nativeState1 = DateIdContinuation(item1.lastUpdatedAt, item1.id.value)
        val state1 = DateIdContinuation(item1.lastUpdatedAt, item1.id.fullId())
        val item3 = randomUnionItem(ItemIdDto(BlockchainDto.ETHEREUM, ethCollection3, "1".toBigInteger()))
        val nativeState3 = DateIdContinuation(item3.lastUpdatedAt, item3.id.value)
        val state3 = DateIdContinuation(item3.lastUpdatedAt, item3.id.fullId())

        val fetcher = CustomCollectionItemFetcherByCollection(
            customCollectionItemProvider,
            listOf(collectionId1, collectionId2, collectionId3)
        )

        coEvery { customCollectionItemProvider.getItemCollectionId(item1.id) } returns collectionId1
        coEvery { customCollectionItemProvider.fetch(collectionId1, null, 2) } returns listOf(item1)
        coEvery { customCollectionItemProvider.fetch(collectionId1, nativeState1.toString(), 2) } returns listOf()

        coEvery { customCollectionItemProvider.fetch(collectionId2, null, 2) } returns listOf()

        coEvery { customCollectionItemProvider.getItemCollectionId(item3.id) } returns collectionId3
        coEvery { customCollectionItemProvider.fetch(collectionId3, null, 2) } returns listOf(item3)
        coEvery { customCollectionItemProvider.fetch(collectionId3, nativeState3.toString(), 2) } returns listOf()

        // First batch
        val batch1 = fetcher.next(null, 2)
        assertThat(batch1.items).isEqualTo(listOf(item1))
        assertThat(batch1.state).isEqualTo(state1.toString())

        // Second batch
        val batch2 = fetcher.next(batch1.state, 2)
        assertThat(batch2.items).isEqualTo(listOf(item3))
        assertThat(batch2.state).isEqualTo(state3.toString())

        // Third batch
        val batch3 = fetcher.next(batch2.state, 2)
        assertThat(batch3.items).isEmpty()
        assertThat(batch3.state).isNull()
    }

}