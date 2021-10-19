package com.rarible.protocol.union.listener.handler.tezos

import com.rarible.core.test.data.randomString
import com.rarible.protocol.tezos.dto.ItemEventDto
import com.rarible.protocol.union.core.tezos.converter.TezosItemConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.listener.service.EnrichmentItemEventService
import com.rarible.protocol.union.test.data.randomTezosItemId
import com.rarible.protocol.union.test.data.randomTezosNftItemDto
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@FlowPreview
class TezosItemEventHandlerTest {

    private val itemEventService: EnrichmentItemEventService = mockk()
    private val handler = TezosItemEventHandler(itemEventService, BlockchainDto.TEZOS)

    @BeforeEach
    fun beforeEach() {
        clearMocks(itemEventService)
        coEvery { itemEventService.onItemUpdated(any()) } returns Unit
        coEvery { itemEventService.onItemDeleted(any()) } returns Unit
    }

    @Test
    fun `tezos item update event`() = runBlocking {
        val tezosItem = randomTezosNftItemDto()
        val dto = ItemEventDto(ItemEventDto.Type.UPDATE, randomString(), tezosItem.id, tezosItem)

        handler.handle(dto)

        val expected = TezosItemConverter.convert(tezosItem, BlockchainDto.TEZOS)
        coVerify(exactly = 1) { itemEventService.onItemUpdated(expected) }
        coVerify(exactly = 0) { itemEventService.onItemDeleted(any()) }
    }

    @Test
    fun `tezos item delete event`() = runBlocking {
        val tezosItemId = randomTezosItemId()
        val tezosItem = randomTezosNftItemDto(tezosItemId)
        val dto = ItemEventDto(ItemEventDto.Type.DELETE, randomString(), tezosItem.id, tezosItem)

        handler.handle(dto)

        coVerify(exactly = 0) { itemEventService.onItemUpdated(any()) }
        coVerify(exactly = 1) { itemEventService.onItemDeleted(tezosItemId) }
    }

    @Test
    fun `tezos item unparseable event`() = runBlocking {
        val dto = ItemEventDto(ItemEventDto.Type.SERIALIZATION_FAILED, null, null, null)

        handler.handle(dto)

        coVerify(exactly = 0) { itemEventService.onItemUpdated(any()) }
        coVerify(exactly = 0) { itemEventService.onItemDeleted(any()) }
    }

}