package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftDeletedItemDto
import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.integration.ethereum.data.randomEventTimeMarks
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import scalether.domain.Address

class EthereumItemEventHandlerTest {

    private val incomingEventHandler: IncomingEventHandler<UnionItemEvent> = mockk()
    private val handler = EthereumItemEventHandler(incomingEventHandler)

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `ethereum item update event`() = runBlocking {
        val item = randomEthNftItemDto()
        val dto = NftItemUpdateEventDto(
            eventId = randomString(),
            itemId = item.id,
            item = item,
            eventTimeMarks = randomEventTimeMarks()
        )

        handler.handle(dto)

        val expected = EthItemConverter.convert(dto, BlockchainDto.ETHEREUM)
        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }

    @Test
    fun `ethereum item delete event`() = runBlocking {
        val itemId = randomEthItemId()
        val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
        val deletedDto = NftDeletedItemDto(
            id = itemId.value,
            token = Address.apply(contract),
            tokenId = tokenId
        )

        val dto: NftItemEventDto = NftItemDeleteEventDto(
            eventId = randomString(),
            itemId = itemId.value,
            item = deletedDto,
            eventTimeMarks = randomEventTimeMarks()
        )

        handler.handle(dto)

        val expected = EthItemConverter.convert(dto, BlockchainDto.ETHEREUM)
        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }

}