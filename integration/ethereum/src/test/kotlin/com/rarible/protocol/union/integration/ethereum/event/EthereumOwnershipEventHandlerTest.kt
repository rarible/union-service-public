package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftDeletedOwnershipDto
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import scalether.domain.Address

class EthereumOwnershipEventHandlerTest {

    private val incomingEventHandler: IncomingEventHandler<UnionOwnershipEvent> = mockk()
    private val handler = EthereumOwnershipEventHandler(incomingEventHandler)

    @BeforeEach
    fun beforeEach() {
        clearMocks(incomingEventHandler)
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `ethereum ownership update event`() = runBlocking {
        val ownership = randomEthOwnershipDto()
        val dto = NftOwnershipUpdateEventDto(randomString(), ownership.id, ownership)

        handler.handle(dto)

        val expected = EthOwnershipConverter.convert(ownership, BlockchainDto.ETHEREUM)
        coVerify(exactly = 1) { incomingEventHandler.onEvent(UnionOwnershipUpdateEvent(expected)) }
    }

    @Test
    fun `ethereum ownership delete event`() = runBlocking {

        val ownershipId = randomEthOwnershipId()

        val deletedDto = NftDeletedOwnershipDto(
            ownershipId.value,
            Address.apply(ownershipId.token.value),
            ownershipId.tokenId,
            Address.apply(ownershipId.owner.value)
        )

        val dto = NftOwnershipDeleteEventDto(randomString(), ownershipId.value, deletedDto)

        handler.handle(dto)

        coVerify(exactly = 1) { incomingEventHandler.onEvent(UnionOwnershipDeleteEvent(ownershipId)) }
    }

}
