package com.rarible.protocol.union.integration.flow.event

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.FlowOrderUpdateEventDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.flow.converter.FlowOrderConverter
import com.rarible.protocol.union.integration.flow.data.randomFlowV1OrderDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlowOrderEventHandlerTest {

    private val incomingEventHandler: IncomingEventHandler<UnionOrderEvent> = mockk()
    private val converter = FlowOrderConverter(CurrencyMock.currencyServiceMock)
    private val handler = FlowOrderEventHandler(incomingEventHandler, converter)

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `flow order event`() = runBlocking {
        val order = randomFlowV1OrderDto()

        handler.handle(
            FlowOrderUpdateEventDto(
                eventId = randomString(),
                orderId = order.id.toString(),
                order = order
            )
        )

        val expected = converter.convert(order, BlockchainDto.FLOW)

        coVerify(exactly = 1) {
            incomingEventHandler.onEvent(match {
                (it as UnionOrderUpdateEvent).order == expected
            })
        }
    }

}
