package com.rarible.protocol.union.core.continuation

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.continuation.ItemContinuation
import com.rarible.protocol.union.test.data.randomEthNftItemDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class ContinuationPagingTest {

    @Test
    fun `get page - trimmed and ordered`() {
        val lastUpdated = nowMillis()
        val items = (1..15L).map { createItem(lastUpdated.minusMillis(it)) }

        val paging = ContinuationPaging(ItemContinuation.ByLastUpdatedAndId, items.shuffled())

        val page = paging.getPage(5)
        val oldestTs = page.entities.map { it.lastUpdatedAt.toEpochMilli() }.min()
        val last = page.entities.last()

        assertThat(page.entities).hasSize(5)
        assertThat(oldestTs).isEqualTo(lastUpdated.toEpochMilli() - 5)
        assertThat(oldestTs).isEqualTo(last.lastUpdatedAt.toEpochMilli())
        assertThat(page.continuation).isEqualTo(ItemContinuation.ByLastUpdatedAndId.getContinuation(last))
    }

    @Test
    fun `get page - last page`() {
        val lastUpdated = nowMillis()
        val items = (1..15L).map { createItem(lastUpdated.minusMillis(it)) }

        val paging = ContinuationPaging(ItemContinuation.ByLastUpdatedAndId, items.shuffled())

        val page = paging.getPage(20)
        assertThat(page.entities).hasSize(15)
        assertThat(page.continuation).isNull()
    }

    @Test
    fun `get page - no items`() {
        val paging = ContinuationPaging(ItemContinuation.ByLastUpdatedAndId, listOf())

        val page = paging.getPage(10)
        assertThat(page.entities).hasSize(0)
        assertThat(page.continuation).isNull()
    }

    private fun createItem(lastUpdated: Instant): ItemDto {
        val item = randomEthNftItemDto().copy(date = lastUpdated)
        return EthItemConverter.convert(item, BlockchainDto.ETHEREUM)
    }

}