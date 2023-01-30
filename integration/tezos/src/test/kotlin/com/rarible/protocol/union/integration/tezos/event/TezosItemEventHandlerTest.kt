package com.rarible.protocol.union.integration.tezos.event

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.rarible.core.test.data.randomString
import com.rarible.dipdup.listener.model.DipDupDeleteItemEvent
import com.rarible.dipdup.listener.model.DipDupUpdateItemEvent
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.integration.tezos.data.randomTezosDipDupItemDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosItemId
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupItemConverter
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupItemEventHandler
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TezosItemEventHandlerTest {

    private val mapper = JsonMapper.builder().addModule(KotlinModule()).addModule(JavaTimeModule()).build()
    private val incomingEventHandler: IncomingEventHandler<UnionItemEvent> = mockk()
    private val handler = DipDupItemEventHandler(incomingEventHandler, mapper)

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `tezos item update event`() = runBlocking {
        val item = randomTezosDipDupItemDto()
        val event = DipDupUpdateItemEvent(
            eventId = randomString(),
            itemId = item.id,
            item = item
        )

        handler.handle(event)

        val expected = DipDupItemConverter.convert(item)

        coVerify(exactly = 1) {
            incomingEventHandler.onEvent(match {
                it is UnionItemUpdateEvent && it.item == expected
            })
        }
    }

    @Test
    fun `tezos item delete event`() = runBlocking {
        val itemId = randomTezosItemId()
        val event = DipDupDeleteItemEvent(
            eventId = randomString(),
            itemId = itemId.value
        )

        handler.handle(event)

        coVerify(exactly = 1) {
            incomingEventHandler.onEvent(match {
                it is UnionItemDeleteEvent && it.itemId == itemId
            })
        }
    }

}
