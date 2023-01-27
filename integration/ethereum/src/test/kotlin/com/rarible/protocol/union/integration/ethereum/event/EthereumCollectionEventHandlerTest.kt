package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthCollectionConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionDto
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EthereumCollectionEventHandlerTest {

    private val incomingEventHandler: IncomingEventHandler<UnionCollectionEvent> = mockk()
    private val handler = EthereumCollectionEventHandler(incomingEventHandler)

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `ethereum collection event`() = runBlocking {
        val collection = randomEthCollectionDto()
        val eventId = randomString()
        val event = NftCollectionUpdateEventDto(
            eventId = eventId,
            id = collection.id,
            collection = collection
        )

        handler.handle(event)

        val expected = EthCollectionConverter.convert(event, BlockchainDto.ETHEREUM)

        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }
}