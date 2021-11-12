package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
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

    private val incomingEventHandler: IncomingEventHandler<CollectionEventDto> = mockk()
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

        handler.handle(NftCollectionUpdateEventDto(eventId, collection.id, collection))

        val unionCollection = EthCollectionConverter.convert(collection, BlockchainDto.ETHEREUM)
        val expected = CollectionUpdateEventDto(unionCollection.id, eventId, unionCollection)

        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }
}