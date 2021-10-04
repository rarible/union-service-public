package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.FlowNftOwnershipDeleteEventDto
import com.rarible.protocol.dto.FlowNftOwnershipUpdateEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.union.core.flow.converter.FlowOwnershipConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.listener.service.EnrichmentOwnershipEventService
import com.rarible.protocol.union.test.data.randomFlowNftOwnershipDto
import com.rarible.protocol.union.test.data.randomFlowOwnershipId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@FlowPreview
class FlowOwnershipEventHandlerFt {

    private val ownershipEventService: EnrichmentOwnershipEventService = mockk()
    private val handler = FlowOwnershipEventHandler(ownershipEventService, BlockchainDto.FLOW)

    @BeforeEach
    fun beforeEach() {
        clearMocks(ownershipEventService)
        coEvery { ownershipEventService.onOwnershipUpdated(any()) } returns Unit
        coEvery { ownershipEventService.onOwnershipDeleted(any()) } returns Unit
    }

    @Test
    fun `ethereum ownership update event`() = runBlocking {
        val flowOwnership = randomFlowNftOwnershipDto()
        val dto: FlowOwnershipEventDto =
            FlowNftOwnershipUpdateEventDto(randomString(), flowOwnership.id!!, flowOwnership)

        handler.handle(dto)

        val expected = FlowOwnershipConverter.convert(flowOwnership, BlockchainDto.FLOW)
        coVerify(exactly = 1) { ownershipEventService.onOwnershipUpdated(expected) }
        coVerify(exactly = 0) { ownershipEventService.onOwnershipDeleted(any()) }
    }

    @Test
    fun `ethereum ownership delete event`() = runBlocking {

        val ethOwnershipId = randomFlowOwnershipId()
        val flowOwnership = randomFlowNftOwnershipDto(ethOwnershipId)

        val dto = FlowNftOwnershipDeleteEventDto(
            randomString(),
            flowOwnership.id!!,
            flowOwnership
        )

        handler.handle(dto)

        coVerify(exactly = 0) { ownershipEventService.onOwnershipUpdated(any()) }
        coVerify(exactly = 1) { ownershipEventService.onOwnershipDeleted(ethOwnershipId) }
    }

}
