package com.rarible.protocol.union.api.controller.internal

import com.rarible.core.kafka.KafkaMessage
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.test.data.randomEthItemId
import com.rarible.protocol.union.test.data.randomEthLegacyOrderDto
import com.rarible.protocol.union.test.data.randomEthNftItemDto
import com.rarible.protocol.union.test.data.randomEthOwnershipDto
import com.rarible.protocol.union.test.data.randomEthOwnershipId
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@IntegrationTest
class RefreshControllerFt : AbstractIntegrationTest() {

    @Test
    fun `refresh item only`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val nftItemDto = randomEthNftItemDto(itemId)
        val bestSell = randomEthLegacyOrderDto(itemId)
        val bestBid = randomEthLegacyOrderDto(itemId)

        val uri = "$baseUri/v0.1/refresh/item/${itemId.fullId()}"

        ethereumItemControllerApiMock.mockGetNftItemById(itemId, nftItemDto)
        ethereumOrderControllerApiMock.mockGetSellOrdersByItem(itemId, bestSell)
        ethereumOrderControllerApiMock.mockGetBidOrdersByItem(itemId, bestBid)

        val result = testRestTemplate.postForEntity(uri, null, ItemDto::class.java).body!!

        assertThat(result.bestSellOrder).isEqualTo(EthOrderConverter.convert(bestSell, itemId.blockchain))
        assertThat(result.bestBidOrder).isEqualTo(EthOrderConverter.convert(bestBid, itemId.blockchain))

        coVerify {
            testItemEventProducer.send(match { message: KafkaMessage<ItemEventDto> ->
                message.value.itemId == itemId
            })
        }
    }

    @Test
    fun `refresh item with ownerships`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val nftItemDto = randomEthNftItemDto(itemId)
        val bestSell = randomEthLegacyOrderDto(itemId)
        val bestBid = randomEthLegacyOrderDto(itemId)

        val ownershipId = randomEthOwnershipId(itemId)
        val nftOwnershipDto = randomEthOwnershipDto(ownershipId)

        val uri = "$baseUri/v0.1/refresh/item/${itemId.fullId()}?full=true"

        ethereumOwnershipControllerApiMock.mockGetNftOwnershipsByItem(itemId, null, 1000, nftOwnershipDto)
        ethereumItemControllerApiMock.mockGetNftItemById(itemId, nftItemDto)
        ethereumOrderControllerApiMock.mockGetSellOrdersByItem(itemId)
        ethereumOrderControllerApiMock.mockGetSellOrdersByOwnership(ownershipId, bestSell)
        ethereumOrderControllerApiMock.mockGetBidOrdersByItem(itemId, bestBid)

        val result = testRestTemplate.postForEntity(uri, null, ItemDto::class.java).body!!

        assertThat(result.bestBidOrder).isEqualTo(EthOrderConverter.convert(bestBid, itemId.blockchain))
        assertThat(result.bestSellOrder).isNull()
        assertThat(result.sellers).isEqualTo(1)
        assertThat(result.totalStock).isEqualTo(bestSell.makeStock)

        coVerify {
            testItemEventProducer.send(match { message: KafkaMessage<ItemEventDto> ->
                message.value.itemId == itemId
            })
        }
    }

}