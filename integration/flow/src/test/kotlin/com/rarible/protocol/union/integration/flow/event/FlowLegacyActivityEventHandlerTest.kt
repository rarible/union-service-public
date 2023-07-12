package com.rarible.protocol.union.integration.flow.event

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.integration.flow.converter.FlowActivityConverter
import com.rarible.protocol.union.integration.flow.data.randomFlowCancelListActivityDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlowLegacyActivityEventHandlerTest() {

    private val incomingEventHandler: IncomingEventHandler<UnionActivity> = mockk()
    private val converter = FlowActivityConverter(CurrencyMock.currencyServiceMock)
    private val handler = FlowLegacyActivityEventHandler(incomingEventHandler, converter)

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `flow activity event`() = runBlocking {
        val event = randomFlowCancelListActivityDto()

        handler.handle(event)

        val expected = converter.convert(event)
        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }

}
