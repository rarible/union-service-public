package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftDeletedItemDto
import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.union.core.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.listener.service.EnrichmentItemEventService
import com.rarible.protocol.union.test.data.randomEthItemId
import com.rarible.protocol.union.test.data.randomEthNftItemDto
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import scalether.domain.Address

@FlowPreview
class EthereumItemEventHandlerTest {

    private val itemEventService: EnrichmentItemEventService = mockk()
    private val handler = EthereumItemEventHandler(itemEventService, BlockchainDto.ETHEREUM)

    @BeforeEach
    fun beforeEach() {
        clearMocks(itemEventService)
        coEvery { itemEventService.onItemUpdated(any()) } returns Unit
        coEvery { itemEventService.onItemDeleted(any()) } returns Unit
    }

    @Test
    fun `ethereum item update event`() = runBlocking {
        val ethItem = randomEthNftItemDto()
        val dto: NftItemEventDto = NftItemUpdateEventDto(randomString(), ethItem.id, ethItem)

        handler.handle(dto)

        val expected = EthItemConverter.convert(ethItem, BlockchainDto.ETHEREUM)
        coVerify(exactly = 1) { itemEventService.onItemUpdated(expected) }
        coVerify(exactly = 0) { itemEventService.onItemDeleted(any()) }
    }

    @Test
    fun `ethereum item delete event`() = runBlocking {
        val ethItemId = randomEthItemId()
        val deletedDto = NftDeletedItemDto(
            ethItemId.value,
            Address.apply(ethItemId.token.value),
            ethItemId.tokenId
        )

        val dto: NftItemEventDto = NftItemDeleteEventDto(randomString(), ethItemId.value, deletedDto)

        handler.handle(dto)

        coVerify(exactly = 0) { itemEventService.onItemUpdated(any()) }
        coVerify(exactly = 1) { itemEventService.onItemDeleted(ethItemId) }
    }

}