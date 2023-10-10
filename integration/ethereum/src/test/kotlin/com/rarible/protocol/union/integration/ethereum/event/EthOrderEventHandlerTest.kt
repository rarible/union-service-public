package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.dto.AmmOrderNftUpdateEventDto
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthEventTimeMarks
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthSellOrderDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class EthOrderEventHandlerTest {

    private val blockchain = BlockchainDto.ETHEREUM

    @MockK
    private lateinit var incomingEventHandler: IncomingEventHandler<UnionOrderEvent>

    private val converter = EthOrderConverter(CurrencyMock.currencyServiceMock)

    @InjectMockKs
    private lateinit var handler: EthOrderEventHandler

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `ethereum order event`() = runBlocking {
        val order = randomEthSellOrderDto()
        val event = OrderUpdateEventDto(
            eventId = randomString(),
            orderId = order.hash.prefixed(),
            order = order,
            eventTimeMarks = randomEthEventTimeMarks()
        )

        handler.handle(event)

        val expected = converter.convert(event, BlockchainDto.ETHEREUM)
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

        val expected = converter.convert(event, BlockchainDto.ETHEREUM)
        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }
}
