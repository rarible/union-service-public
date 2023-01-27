package com.rarible.protocol.union.integration.flow.event

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.FlowNftOwnershipDeleteEventDto
import com.rarible.protocol.dto.FlowNftOwnershipUpdateEventDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.flow.converter.FlowOwnershipConverter
import com.rarible.protocol.union.integration.flow.data.randomFlowNftOwnershipDto
import com.rarible.protocol.union.integration.flow.data.randomFlowOwnershipId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlowOwnershipEventHandlerTest {

    private val incomingEventHandler: IncomingEventHandler<UnionOwnershipEvent> = mockk()
    private val handler = FlowOwnershipEventHandler(incomingEventHandler)

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `flow ownership update event`() = runBlocking {
        val ownership = randomFlowNftOwnershipDto()
        val dto = FlowNftOwnershipUpdateEventDto(randomString(), ownership.id!!, ownership)

        handler.handle(dto)

        val expected = FlowOwnershipConverter.convert(ownership, BlockchainDto.FLOW)
        coVerify(exactly = 1) {
            incomingEventHandler.onEvent(match {
                (it as UnionOwnershipUpdateEvent).ownership == expected
            })
        }
    }

    @Test
    fun `flow ownership delete event`() = runBlocking {

        val ownershipId = randomFlowOwnershipId()
        val ownership = randomFlowNftOwnershipDto(ownershipId)

        val dto = FlowNftOwnershipDeleteEventDto(randomString(), ownershipId.value, ownership)

        handler.handle(dto)

        coVerify(exactly = 1) {
            incomingEventHandler.onEvent(match {
                it is UnionOwnershipDeleteEvent && it.ownershipId == ownershipId
            })
        }
    }

}
