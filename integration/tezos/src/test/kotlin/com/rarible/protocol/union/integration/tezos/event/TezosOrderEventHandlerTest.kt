package com.rarible.protocol.union.integration.tezos.event

import com.rarible.core.test.data.randomString
import com.rarible.protocol.tezos.dto.OrderEventDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.converter.TezosOrderConverter
import com.rarible.protocol.union.test.data.randomTezosAssetFa2
import com.rarible.protocol.union.test.data.randomTezosOrderDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TezosOrderEventHandlerTest {

    private val incomingEventHandler: IncomingEventHandler<UnionOrderEvent> = mockk()
    private val converter = TezosOrderConverter(CurrencyMock.currencyServiceMock)
    private val handler = TezosOrderEventHandler(incomingEventHandler, converter)

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `tezos order event`() = runBlocking {
        val order = randomTezosOrderDto().copy(take = randomTezosAssetFa2())
        val event = OrderEventDto(OrderEventDto.Type.UPDATE, randomString(), order.hash, order)

        handler.handle(event)

        val expected = UnionOrderUpdateEvent(converter.convert(order, BlockchainDto.TEZOS))

        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }

}