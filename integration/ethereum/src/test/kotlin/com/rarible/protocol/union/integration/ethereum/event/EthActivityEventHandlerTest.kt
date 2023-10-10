package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.dto.EventTimeMarksDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionOpenActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderBidActivity
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
class EthActivityEventHandlerTest {

    private val blockchain = BlockchainDto.ETHEREUM

    @MockK
    private lateinit var incomingEventHandler: IncomingEventHandler<UnionActivity>

    private val ethAuctionConverter = EthAuctionConverter(CurrencyMock.currencyServiceMock)

    private val ethActivityConverter = EthActivityConverter(ethAuctionConverter)

    @InjectMockKs
    private lateinit var handler: EthActivityEventHandler

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `ethereum activity order event`() = runBlocking {
        val event = EthActivityEventDto(randomEthOrderBidActivity(), EventTimeMarksDto("test"))

        handler.handle(event)

        val expected = ethActivityConverter.convert(event, BlockchainDto.ETHEREUM)
        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }

    @Test
    fun `ethereum activity auction event`() = runBlocking {
        val event = EthActivityEventDto(randomEthAuctionOpenActivity(), EventTimeMarksDto("test"))

        handler.handle(event)

        val expected = ethActivityConverter.convert(event, BlockchainDto.ETHEREUM)
        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }
}
