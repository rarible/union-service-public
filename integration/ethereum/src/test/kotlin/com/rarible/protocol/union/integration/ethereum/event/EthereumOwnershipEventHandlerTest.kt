package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftDeletedOwnershipDto
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthEventTimeMarks
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
        val dto = NftOwnershipUpdateEventDto(
            eventId = randomString(),
            ownershipId = ownership.id,
            ownership = ownership,
            eventTimeMarks = randomEthEventTimeMarks()
        )

        handler.handle(dto)

        val expected = EthOwnershipConverter.convert(dto, BlockchainDto.ETHEREUM)
        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }

    @Test
    fun `ethereum ownership delete event`() = runBlocking {
        val ownershipId = randomEthOwnershipId()
        val (contract, tokenId) = CompositeItemIdParser.split(ownershipId.itemIdValue)

        val deletedDto = NftDeletedOwnershipDto(
            id = ownershipId.value,
            token = Address.apply(contract),
            tokenId = tokenId,
            owner = Address.apply(ownershipId.owner.value)
        )

        val dto = NftOwnershipDeleteEventDto(
            eventId = randomString(),
            ownershipId = ownershipId.value,
            ownership = deletedDto
        )

        handler.handle(dto)

        val expected = EthOwnershipConverter.convert(dto, BlockchainDto.ETHEREUM)
        coVerify(exactly = 1) { incomingEventHandler.onEvent(expected) }
    }

}
