package com.rarible.protocol.union.integration.tezos.event

import com.rarible.protocol.tezos.dto.TezosActivitySafeDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.converter.TezosActivityConverter
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderListActivity
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TezosActivityEventHandlerTest {

    private val incomingEventHandler: IncomingEventHandler<com.rarible.protocol.union.dto.ActivityDto> = mockk()
    private val converter = TezosActivityConverter(CurrencyMock.currencyServiceMock)
    private val handler = TezosActivityEventHandler(incomingEventHandler, converter, false)

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `tezos activity event`() = runBlocking {
        val dto = randomTezosOrderListActivity()
        val event = TezosActivitySafeDto(orderType = dto, nftType = null)


        handler.handle(event)

        val expected = converter.convert(dto, BlockchainDto.TEZOS)
        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }

}
