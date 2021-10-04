package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftDeletedOwnershipDto
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.union.core.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.listener.service.EnrichmentOwnershipEventService
import com.rarible.protocol.union.test.data.randomEthOwnershipDto
import com.rarible.protocol.union.test.data.randomEthOwnershipId
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
class EthereumOwnershipEventHandlerTest {

    private val ownershipEventService: EnrichmentOwnershipEventService = mockk()
    private val handler = EthereumOwnershipEventHandler(ownershipEventService, BlockchainDto.ETHEREUM)

    @BeforeEach
    fun beforeEach() {
        clearMocks(ownershipEventService)
        coEvery { ownershipEventService.onOwnershipUpdated(any()) } returns Unit
        coEvery { ownershipEventService.onOwnershipDeleted(any()) } returns Unit
    }

    @Test
    fun `ethereum ownership update event`() = runBlocking {
        val ethOwnership = randomEthOwnershipDto()
        val dto: NftOwnershipEventDto = NftOwnershipUpdateEventDto(randomString(), ethOwnership.id, ethOwnership)

        handler.handle(dto)

        val expected = EthOwnershipConverter.convert(ethOwnership, BlockchainDto.ETHEREUM)
        coVerify(exactly = 1) { ownershipEventService.onOwnershipUpdated(expected) }
        coVerify(exactly = 0) { ownershipEventService.onOwnershipDeleted(any()) }
    }

    @Test
    fun `ethereum ownership delete event`() = runBlocking {

        val ethOwnershipId = randomEthOwnershipId()

        val deletedDto = NftDeletedOwnershipDto(
            ethOwnershipId.value,
            Address.apply(ethOwnershipId.token.value),
            ethOwnershipId.tokenId,
            Address.apply(ethOwnershipId.owner.value)
        )

        val dto: NftOwnershipEventDto = NftOwnershipDeleteEventDto(randomString(), ethOwnershipId.value, deletedDto)

        handler.handle(dto)

        coVerify(exactly = 0) { ownershipEventService.onOwnershipUpdated(any()) }
        coVerify(exactly = 1) { ownershipEventService.onOwnershipDeleted(ethOwnershipId) }
    }

}
