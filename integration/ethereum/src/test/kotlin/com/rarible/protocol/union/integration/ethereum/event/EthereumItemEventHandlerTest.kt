package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftDeletedItemDto
import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
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
        val dto = NftItemUpdateEventDto(randomString(), item.id, item)

        handler.handle(dto)

        val expected = UnionItemUpdateEvent(EthItemConverter.convert(item, BlockchainDto.ETHEREUM))
        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }

    @Test
    fun `ethereum item delete event`() = runBlocking {
        val itemId = randomEthItemId()
        val deletedDto = NftDeletedItemDto(
            itemId.value,
            Address.apply(itemId.token.value),
            itemId.tokenId
        )

        val dto: NftItemEventDto = NftItemDeleteEventDto(randomString(), itemId.value, deletedDto)

        handler.handle(dto)

        coVerify(exactly = 1) { incomingEventHandler.onEvent(UnionItemDeleteEvent(itemId)) }
    }

}