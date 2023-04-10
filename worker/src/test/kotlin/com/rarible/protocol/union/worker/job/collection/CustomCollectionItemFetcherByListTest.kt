package com.rarible.protocol.union.worker.job.collection

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CustomCollectionItemFetcherByListTest {

    private val customCollectionItemProvider: CustomCollectionItemProvider = mockk()

    @Test
    fun `fetch by batches - ok`() = runBlocking<Unit> {

        val ethCollection = randomEthAddress()
        val polyCollection = randomEthAddress()

        val item1 = randomUnionItem(ItemIdDto(BlockchainDto.FLOW, "f:1")) //3
        val item2 = randomUnionItem(ItemIdDto(BlockchainDto.POLYGON, "$polyCollection:2")) //4
        val item3 = randomUnionItem(ItemIdDto(BlockchainDto.ETHEREUM, "$ethCollection:2")) //2
        val item4 = randomUnionItem(ItemIdDto(BlockchainDto.ETHEREUM, "$ethCollection:1")) //1

        val items = listOf(item1.id, item2.id, item3.id, item4.id)

        val fetcher = CustomCollectionItemFetcherByList(customCollectionItemProvider, items)
        coEvery { customCollectionItemProvider.fetch(listOf(item4.id, item3.id)) } returns listOf(item4, item3)
        coEvery { customCollectionItemProvider.fetch(listOf(item1.id, item2.id)) } returns listOf(item1, item2)
        coEvery { customCollectionItemProvider.fetch(emptyList()) } returns emptyList()

        // First batch
        val batch1 = fetcher.next(null, 2)
        assertThat(batch1.items).isEqualTo(listOf(item4, item3))
        assertThat(batch1.state).isEqualTo(item3.id.toString())

        // Second batch
        val batch2 = fetcher.next(batch1.state, 2)
        assertThat(batch2.items).isEqualTo(listOf(item1, item2))
        assertThat(batch2.state).isEqualTo(item2.id.toString())

        // Empty batch
        val batch3 = fetcher.next(batch2.state, 2)
        assertThat(batch3.items).isEmpty()
        assertThat(batch3.state).isNull()
    }

}