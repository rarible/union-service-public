package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomShortOrder
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import randomOrderDto

class ItemDtoConverterTest {

    @Test
    fun `convert - ok, custom collection`() {
        val unionItem = randomUnionItem(randomEthItemId())
        val customCollection = randomEthCollectionId()

        val result = ItemDtoConverter.convert(item = unionItem, customCollection = customCollection)

        assertThat(result.collection).isEqualTo(customCollection)
    }

    @Test
    fun `convert - ok, custom collection not specified`() {
        val unionItem = randomUnionItem(randomEthItemId())

        val result = ItemDtoConverter.convert(item = unionItem)

        assertThat(result.collection).isEqualTo(unionItem.collection)
    }

    @Test
    fun `convert - ok, imx bids removed`() {
        val unionItem = randomUnionItem(randomEthItemId().copy(blockchain = BlockchainDto.IMMUTABLEX))
        val shortItem = randomShortItem(unionItem.id).copy(bestBidOrder = randomShortOrder())
        val orders = mapOf(shortItem.bestBidOrder!!.dtoId to randomOrderDto())

        val result = ItemDtoConverter.convert(item = unionItem, shortItem = shortItem, orders = orders)

        assertThat(result.bestBidOrder).isNull()
    }

    @Test
    fun `convert - ok, eth bids kept`() {
        val unionItem = randomUnionItem(randomEthItemId().copy())
        val shortItem = randomShortItem(unionItem.id).copy(bestBidOrder = randomShortOrder())
        val orders = mapOf(shortItem.bestBidOrder!!.dtoId to randomOrderDto())

        val result = ItemDtoConverter.convert(item = unionItem, shortItem = shortItem, orders = orders)

        assertThat(result.bestBidOrder).isNotNull
    }
}
