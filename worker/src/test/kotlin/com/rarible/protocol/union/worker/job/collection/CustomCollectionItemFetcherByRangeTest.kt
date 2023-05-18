package com.rarible.protocol.union.worker.job.collection

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.enrichment.util.TokenRange
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CustomCollectionItemFetcherByRangeTest {

    private val customCollectionItemProvider: CustomCollectionItemProvider = mockk()

    @Test
    fun `fetch by batches - ok`() = runBlocking<Unit> {
        val collection1 = EnrichmentCollectionId(BlockchainDto.ETHEREUM, randomEthAddress())
        val collection2 = EnrichmentCollectionId(BlockchainDto.ETHEREUM, randomEthAddress())

        val range1 = TokenRange(collection1, 1.toBigInteger().rangeTo(3.toBigInteger())) // less than batch
        val range2 = TokenRange(collection1, 10.toBigInteger().rangeTo(17.toBigInteger())) // grater than batch
        val range3 = TokenRange(collection2, 1.toBigInteger().rangeTo(2.toBigInteger())) // nothing returned
        val range4 = TokenRange(collection2, 5.toBigInteger().rangeTo(9.toBigInteger())) // same as batch
        val ranges = listOf(range1, range2, range3, range4)

        val item1 = randomUnionItem(ItemIdDto(BlockchainDto.ETHEREUM, "${collection1.collectionId}:2"))
        val item2 = randomUnionItem(ItemIdDto(BlockchainDto.ETHEREUM, "${collection1.collectionId}:10"))
        val item3 = randomUnionItem(ItemIdDto(BlockchainDto.ETHEREUM, "${collection2.collectionId}:9"))

        val fetcher = CustomCollectionItemFetcherByRange(customCollectionItemProvider, ranges)
        // first range
        coEvery { customCollectionItemProvider.fetch(itemIds(collection1, 1, 2, 3)) } returns listOf(item1)
        // second range
        coEvery { customCollectionItemProvider.fetch(itemIds(collection1, 10, 11, 12, 13, 14)) } returns listOf(item2)
        coEvery { customCollectionItemProvider.fetch(itemIds(collection1, 15, 16, 17)) } returns emptyList()
        // third range
        coEvery { customCollectionItemProvider.fetch(itemIds(collection2, 1, 2)) } returns emptyList()
        // fourth range
        coEvery { customCollectionItemProvider.fetch(itemIds(collection2, 5, 6, 7, 8, 9)) } returns listOf(item3)

        val batch1 = fetcher.next(null, 5)
        assertThat(batch1.items).isEqualTo(listOf(item1))
        assertThat(batch1.state).isEqualTo("0-3")

        val batch2 = fetcher.next(batch1.state, 5)
        assertThat(batch2.items).isEqualTo(listOf(item2))
        assertThat(batch2.state).isEqualTo("1-14")

        val batch3 = fetcher.next(batch2.state, 5)
        assertThat(batch3.items).isEmpty()
        assertThat(batch3.state).isEqualTo("1-17")

        val batch4 = fetcher.next(batch3.state, 5)
        assertThat(batch4.items).isEmpty()
        assertThat(batch4.state).isEqualTo("2-2")

        val batch5 = fetcher.next(batch4.state, 5)
        assertThat(batch5.items).isEqualTo(listOf(item3))
        assertThat(batch5.state).isEqualTo("3-9")

        val batch6 = fetcher.next(batch5.state, 5)
        assertThat(batch6.items).isEmpty()
        assertThat(batch6.state).isNull()
    }

    private fun itemIds(collectionId: EnrichmentCollectionId, vararg tokenIds: Int) = tokenIds.map {
        ItemIdDto(collectionId.blockchain, "${collectionId.collectionId}:$it")
    }

}