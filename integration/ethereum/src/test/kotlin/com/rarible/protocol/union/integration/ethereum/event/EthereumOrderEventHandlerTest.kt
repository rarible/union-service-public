package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
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

}