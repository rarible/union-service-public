package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.AuctionDeleteEventDto
import com.rarible.protocol.dto.AuctionUpdateEventDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionAuctionDeleteEvent
import com.rarible.protocol.union.core.model.UnionAuctionEvent
import com.rarible.protocol.union.core.model.UnionAuctionUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionDto
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
class EthActionEventHandlerTest {

    private val blockchain = BlockchainDto.ETHEREUM

    @MockK
    private lateinit var incomingEventHandler: IncomingEventHandler<UnionAuctionEvent>

    private val converter = EthAuctionConverter(CurrencyMock.currencyServiceMock)

    @InjectMockKs
    private lateinit var handler: EthAuctionEventHandler

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `ethereum auction update event`() = runBlocking {
        val auction = randomEthAuctionDto()

        handler.handle(AuctionUpdateEventDto(randomString(), auction.hash.prefixed(), auction))

        val expected = UnionAuctionUpdateEvent(converter.convert(auction, BlockchainDto.ETHEREUM))

        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }

    @Test
    fun `ethereum auction delete event`() = runBlocking {
        val auction = randomEthAuctionDto()

        handler.handle(AuctionDeleteEventDto(randomString(), auction.hash.prefixed(), auction))

        val expected = UnionAuctionDeleteEvent(converter.convert(auction, BlockchainDto.ETHEREUM))

        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }
}
