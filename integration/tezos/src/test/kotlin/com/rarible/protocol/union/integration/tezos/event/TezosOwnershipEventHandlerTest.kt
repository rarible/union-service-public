package com.rarible.protocol.union.integration.tezos.event

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.rarible.core.test.data.randomString
import com.rarible.dipdup.listener.model.DipDupDeleteOwnershipEvent
import com.rarible.dipdup.listener.model.DipDupUpdateOwnershipEvent
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.integration.tezos.data.randomTezosDipDupOwnershipDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosOwnershipId
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOwnershipConverter
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupOwnershipEventHandler
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TezosOwnershipEventHandlerTest {

    private val mapper = JsonMapper.builder().addModule(KotlinModule()).addModule(JavaTimeModule()).build()
    private val incomingEventHandler: IncomingEventHandler<UnionOwnershipEvent> = mockk()
    private val handler = DipDupOwnershipEventHandler(incomingEventHandler, mapper)

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `tezos ownership update event`() = runBlocking {
        val ownership = randomTezosDipDupOwnershipDto()
        val event = DipDupUpdateOwnershipEvent(
            eventId = randomString(),
            ownershipId = ownership.id,
            ownership = ownership
        )

        handler.handle(event)

        val expected = DipDupOwnershipConverter.convert(ownership)

        coVerify(exactly = 1) {
            incomingEventHandler.onEvent(match {
                it is UnionOwnershipUpdateEvent && it.ownership == expected
            })
        }
    }

    @Test
    fun `tezos ownership delete event`() = runBlocking {
        val ownershipId = randomTezosOwnershipId()
        val event = DipDupDeleteOwnershipEvent(
            eventId = randomString(),
            ownershipId = ownershipId.value
        )

        handler.handle(event)

        coVerify(exactly = 1) {
            incomingEventHandler.onEvent(match {
                it is UnionOwnershipDeleteEvent && it.ownershipId == ownershipId
            })
        }
    }

}
