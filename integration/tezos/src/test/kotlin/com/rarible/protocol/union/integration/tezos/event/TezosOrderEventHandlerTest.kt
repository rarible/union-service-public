package com.rarible.protocol.union.integration.tezos.event

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosAssetNFT
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderDto
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOrderConverter
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupOrderEventHandler
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TezosOrderEventHandlerTest {

    private val mapper = JsonMapper.builder().addModule(KotlinModule()).addModule(JavaTimeModule()).build()
    private val incomingEventHandler: IncomingEventHandler<UnionOrderEvent> = mockk()
    private val converter = DipDupOrderConverter(CurrencyMock.currencyServiceMock)
    private val handler = DipDupOrderEventHandler(
        incomingEventHandler,
        converter,
        mapper,
        DipDupIntegrationProperties.Marketplaces()
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `tezos order event`() = runBlocking {
        val event = randomTezosOrderDto().copy(take = randomTezosAssetNFT())

        handler.handle(event)

        val expected = UnionOrderUpdateEvent(converter.convert(event, BlockchainDto.TEZOS))

        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }

}
