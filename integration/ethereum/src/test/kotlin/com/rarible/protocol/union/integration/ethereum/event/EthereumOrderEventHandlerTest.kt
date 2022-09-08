package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.dto.AmmOrderNftUpdateEventDto
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.core.model.UnionPoolNftUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthSellOrderDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EthereumOrderEventHandlerTest {

    private val incomingEventHandler: IncomingEventHandler<UnionOrderEvent> = mockk()
    private val converter = EthOrderConverter(CurrencyMock.currencyServiceMock)
    private val handler = EthereumOrderEventHandler(incomingEventHandler, converter)

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `ethereum order event`() = runBlocking {
        val order = randomEthSellOrderDto()

        handler.handle(OrderUpdateEventDto(randomString(), order.hash.prefixed(), order))

        val expected = UnionOrderUpdateEvent(converter.convert(order, BlockchainDto.ETHEREUM))

        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }

    @Test
    fun `ethereum amm order nft event`() = runBlocking {
        val orderId = randomWord()

        val itemIdIncluded = randomEthItemId()
        val itemIdExcluded = randomEthItemId()

        val event = AmmOrderNftUpdateEventDto(
            eventId = randomString(),
            orderId = orderId,
            inNft = listOf(itemIdIncluded.value),
            outNft = listOf(itemIdExcluded.value)
        )

        handler.handle(event)

        val expected = UnionPoolNftUpdateEvent(
            orderId = OrderIdDto(BlockchainDto.ETHEREUM, orderId),
            inNft = setOf(itemIdIncluded),
            outNft = setOf(itemIdExcluded)
        )

        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }

}