package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.FlowNftDeletedItemDto
import com.rarible.protocol.dto.FlowNftItemDeleteEventDto
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemUpdateEventDto
import com.rarible.protocol.union.core.flow.converter.FlowItemConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.listener.service.EnrichmentItemEventService
import com.rarible.protocol.union.test.data.randomFlowItemId
import com.rarible.protocol.union.test.data.randomFlowNftItemDto
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@FlowPreview
class FlowItemEventHandlerTest {

    private val itemEventService: EnrichmentItemEventService = mockk()
    private val handler = FlowItemEventHandler(itemEventService, BlockchainDto.FLOW)

    @BeforeEach
    fun beforeEach() {
        clearMocks(itemEventService)
        coEvery { itemEventService.onItemUpdated(any()) } returns Unit
        coEvery { itemEventService.onItemDeleted(any()) } returns Unit
    }

    @Test
    fun `ethereum item update event`() = runBlocking {
        val flowItem = randomFlowNftItemDto()
        val dto: FlowNftItemEventDto = FlowNftItemUpdateEventDto(randomString(), flowItem.id, flowItem)

        handler.handle(dto)

        val expected = FlowItemConverter.convert(flowItem, BlockchainDto.FLOW)
        coVerify(exactly = 1) { itemEventService.onItemUpdated(expected) }
        coVerify(exactly = 0) { itemEventService.onItemDeleted(any()) }
    }

    @Test
    fun `ethereum item delete event`() = runBlocking {
        val flowItemId = randomFlowItemId()
        val deletedDto = FlowNftDeletedItemDto(
            flowItemId.value,
            flowItemId.token.value,
            flowItemId.tokenId.toLong()
        )

        val dto: FlowNftItemEventDto = FlowNftItemDeleteEventDto(randomString(), flowItemId.value, deletedDto)

        handler.handle(dto)

        coVerify(exactly = 0) { itemEventService.onItemUpdated(any()) }
        coVerify(exactly = 1) { itemEventService.onItemDeleted(flowItemId) }
    }

}