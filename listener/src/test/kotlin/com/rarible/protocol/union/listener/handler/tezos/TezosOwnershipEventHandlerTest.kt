package com.rarible.protocol.union.listener.handler.tezos

import com.rarible.core.test.data.randomString
import com.rarible.protocol.tezos.dto.OwnershipEventDto
import com.rarible.protocol.union.core.tezos.converter.TezosOwnershipConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.listener.service.EnrichmentOwnershipEventService
import com.rarible.protocol.union.test.data.randomTezosOwnershipDto
import com.rarible.protocol.union.test.data.randomTezosOwnershipId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@FlowPreview
class TezosOwnershipEventHandlerTest {

    private val ownershipEventService: EnrichmentOwnershipEventService = mockk()
    private val handler = TezosOwnershipEventHandler(ownershipEventService, BlockchainDto.TEZOS)

    @BeforeEach
    fun beforeEach() {
        clearMocks(ownershipEventService)
        coEvery { ownershipEventService.onOwnershipUpdated(any()) } returns Unit
        coEvery { ownershipEventService.onOwnershipDeleted(any()) } returns Unit
    }

    @Test
    fun `tezos ownership update event`() = runBlocking {
        val tezosOwnership = randomTezosOwnershipDto()
        val dto = OwnershipEventDto(OwnershipEventDto.Type.UPDATE, randomString(), tezosOwnership.id, tezosOwnership)

        handler.handle(dto)

        val expected = TezosOwnershipConverter.convert(tezosOwnership, BlockchainDto.TEZOS)
        coVerify(exactly = 1) { ownershipEventService.onOwnershipUpdated(expected) }
        coVerify(exactly = 0) { ownershipEventService.onOwnershipDeleted(any()) }
    }

    @Test
    fun `tezos ownership delete event`() = runBlocking {
        val tezosOwnershipId = randomTezosOwnershipId()
        val tezosOwnership = randomTezosOwnershipDto(tezosOwnershipId)
        val dto = OwnershipEventDto(OwnershipEventDto.Type.DELETE, randomString(), tezosOwnership.id, tezosOwnership)

        handler.handle(dto)

        coVerify(exactly = 0) { ownershipEventService.onOwnershipUpdated(any()) }
        coVerify(exactly = 1) { ownershipEventService.onOwnershipDeleted(tezosOwnershipId) }
    }

    @Test
    fun `tezos ownership unparseable event`() = runBlocking {
        val dto = OwnershipEventDto(OwnershipEventDto.Type.SERIALIZATION_FAILED, null, null, null)

        handler.handle(dto)

        coVerify(exactly = 0) { ownershipEventService.onOwnershipUpdated(any()) }
        coVerify(exactly = 0) { ownershipEventService.onOwnershipDeleted(any()) }
    }

}
