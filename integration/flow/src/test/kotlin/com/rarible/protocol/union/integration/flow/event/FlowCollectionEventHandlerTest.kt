package com.rarible.protocol.union.integration.flow.event

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.FlowCollectionUpdateEventDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.flow.converter.FlowCollectionConverter
import com.rarible.protocol.union.integration.flow.data.randomFlowCollectionDto
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlowCollectionEventHandlerTest {

    private val incomingEventHandler: IncomingEventHandler<UnionCollectionEvent> = mockk()
    private val handler = FlowCollectionEventHandler(incomingEventHandler)

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `flow collection update event`() = runBlocking {
        val collection = randomFlowCollectionDto()
        val dto = FlowCollectionUpdateEventDto(
            eventId = randomString(),
            collectionId = collection.id!!,
            collection = collection
        )

        handler.handle(dto)

        val expected = FlowCollectionConverter.convert(collection, BlockchainDto.FLOW)
        coVerify(exactly = 1) {
            incomingEventHandler.onEvent(
                match {
                    (it as UnionCollectionUpdateEvent).collection == expected
                }
            )
        }
    }
}
