package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.union.core.ethereum.converter.EthCollectionConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.listener.service.EnrichmentCollectionEventService
import com.rarible.protocol.union.test.data.randomEthCollectionDto
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@FlowPreview
class EthereumCollectionEventHandlerTest {

    private val collectionEventService: EnrichmentCollectionEventService = mockk()
    private val handler = EthereumCollectionEventHandler(collectionEventService, BlockchainDto.ETHEREUM)

    @BeforeEach
    fun beforeEach() {
        clearMocks(collectionEventService)
        coEvery { collectionEventService.onCollectionUpdated(any()) } returns Unit
    }

    @Test
    fun `ethereum collection update event`() = runBlocking {
        val ethCollection = randomEthCollectionDto()
        val dto = NftCollectionUpdateEventDto(randomString(), ethCollection.id, ethCollection)

        handler.handle(dto)

        val expected = EthCollectionConverter.convert(ethCollection, BlockchainDto.ETHEREUM)
        coVerify(exactly = 1) { collectionEventService.onCollectionUpdated(expected) }
    }
}