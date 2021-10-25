package com.rarible.protocol.union.integration.tezos.event

import com.rarible.core.test.data.randomString
import com.rarible.protocol.tezos.dto.ItemEventDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.converter.TezosItemConverter
import com.rarible.protocol.union.test.data.randomTezosNftItemDto
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TezosItemEventHandlerTest {

    private val incomingEventHandler: IncomingEventHandler<UnionItemEvent> = mockk()
    private val handler = TezosItemEventHandler(incomingEventHandler)

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `tezos item update event`() = runBlocking {
        val item = randomTezosNftItemDto()
        val event = ItemEventDto(ItemEventDto.Type.UPDATE, randomString(), item.id, item)

        handler.handle(event)

        val expected = UnionItemUpdateEvent(TezosItemConverter.convert(item, BlockchainDto.TEZOS))
        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }

    @Test
    fun `tezos item delete event`() = runBlocking {
        val item = randomTezosNftItemDto()
        val event = ItemEventDto(ItemEventDto.Type.DELETE, randomString(), item.id, item)

        handler.handle(event)

        val expected = UnionItemDeleteEvent(TezosItemConverter.convert(item, BlockchainDto.TEZOS).id)
        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }

    @Test
    fun `tezos item unparseable event`() = runBlocking {
        val dto = ItemEventDto(ItemEventDto.Type.SERIALIZATION_FAILED, null, null, null)

        handler.handle(dto)

        coVerify(exactly = 0) { incomingEventHandler.onEvent(any()) }
    }

}