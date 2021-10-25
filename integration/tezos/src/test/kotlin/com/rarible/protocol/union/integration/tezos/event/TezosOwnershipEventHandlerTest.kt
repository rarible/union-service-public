package com.rarible.protocol.union.integration.tezos.event

import com.rarible.core.test.data.randomString
import com.rarible.protocol.tezos.dto.OwnershipEventDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.converter.TezosOwnershipConverter
import com.rarible.protocol.union.test.data.randomTezosOwnershipDto
import com.rarible.protocol.union.test.data.randomTezosOwnershipId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TezosOwnershipEventHandlerTest {

    private val incomingEventHandler: IncomingEventHandler<UnionOwnershipEvent> = mockk()
    private val handler = TezosOwnershipEventHandler(incomingEventHandler)

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `tezos ownership update event`() = runBlocking {
        val ownership = randomTezosOwnershipDto()
        val dto = OwnershipEventDto(OwnershipEventDto.Type.UPDATE, randomString(), ownership.id, ownership)

        handler.handle(dto)

        val expected = TezosOwnershipConverter.convert(ownership, BlockchainDto.TEZOS)
        coVerify(exactly = 1) { incomingEventHandler.onEvent(UnionOwnershipUpdateEvent(expected)) }
    }

    @Test
    fun `tezos ownership delete event`() = runBlocking {

        val ownershipId = randomTezosOwnershipId()
        val ownership = randomTezosOwnershipDto(ownershipId)

        val dto = OwnershipEventDto(OwnershipEventDto.Type.DELETE, randomString(), ownership.id, ownership)

        handler.handle(dto)

        coVerify(exactly = 1) { incomingEventHandler.onEvent(UnionOwnershipDeleteEvent(ownershipId)) }
    }
    @Test
    fun `tezos ownership unparseable event`() = runBlocking {
        val dto = OwnershipEventDto(OwnershipEventDto.Type.SERIALIZATION_FAILED, null, null, null)

        handler.handle(dto)

        coVerify(exactly = 0) { incomingEventHandler.onEvent(any()) }
    }

}
