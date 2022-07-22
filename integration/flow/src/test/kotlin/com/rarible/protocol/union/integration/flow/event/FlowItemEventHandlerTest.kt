package com.rarible.protocol.union.integration.flow.event

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.FlowNftDeletedItemDto
import com.rarible.protocol.dto.FlowNftItemDeleteEventDto
import com.rarible.protocol.dto.FlowNftItemUpdateEventDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.flow.converter.FlowItemConverter
import com.rarible.protocol.union.integration.flow.data.randomFlowItemId
import com.rarible.protocol.union.integration.flow.data.randomFlowNftItemDto
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlowItemEventHandlerTest {

    private val incomingEventHandler: IncomingEventHandler<UnionItemEvent> = mockk()
    private val handler = FlowItemEventHandler(incomingEventHandler)

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `flow item update event`() = runBlocking {
        val item = randomFlowNftItemDto()
        val dto = FlowNftItemUpdateEventDto(randomString(), item.id, item)

        handler.handle(dto)

        val expected = UnionItemUpdateEvent(FlowItemConverter.convert(item, BlockchainDto.FLOW))
        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }

    @Test
    fun `flow item delete event`() = runBlocking {
        val itemId = randomFlowItemId()
        val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
        val deletedDto = FlowNftDeletedItemDto(
            itemId.value,
            contract,
            tokenId.toLong()
        )

        val dto = FlowNftItemDeleteEventDto(randomString(), itemId.value, deletedDto)

        handler.handle(dto)

        coVerify(exactly = 1) { incomingEventHandler.onEvent(UnionItemDeleteEvent(itemId)) }
    }

}
