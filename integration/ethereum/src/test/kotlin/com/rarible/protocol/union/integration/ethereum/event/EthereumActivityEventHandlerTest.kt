package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.dto.ActivityDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderBidActivity
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@FlowPreview
class EthereumActivityEventHandlerTest {

    private val incomingEventHandler: IncomingEventHandler<com.rarible.protocol.union.dto.ActivityDto> = mockk()
    private val converter = EthActivityConverter(CurrencyMock.currencyServiceMock)
    private val handler = EthereumActivityEventHandler(incomingEventHandler, converter)

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `ethereum activity event`() = runBlocking {
        val event: ActivityDto = randomEthOrderBidActivity()

        handler.handle(event)

        val expected = converter.convert(event, BlockchainDto.ETHEREUM)
        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }

}