package com.rarible.protocol.union.integration.immutablex.handlers

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.core.model.UnionItemMetaRefreshEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.data.randomImxAsset
import com.rarible.protocol.union.integration.immutablex.converter.ImxItemConverter
import com.rarible.protocol.union.integration.immutablex.converter.ImxItemMetaConverter
import com.rarible.protocol.union.integration.immutablex.model.ImxItemMeta
import com.rarible.protocol.union.integration.immutablex.repository.ImxItemMetaRepository
import com.rarible.protocol.union.integration.immutablex.service.ImxItemService
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ImxItemEventHandlerTest {

    private val itemHandler: IncomingEventHandler<UnionItemEvent> = mockk {
        coEvery { onEvent(any()) } returns Unit
    }
    private val itemMetaHandler: IncomingEventHandler<UnionItemMetaEvent> = mockk {
        coEvery { onEvent(any()) } returns Unit
    }
    private val itemService: ImxItemService = mockk() {
        coEvery { getItemCreators(emptyList()) } returns emptyMap()
    }

    private val itemMetaRepository: ImxItemMetaRepository = mockk()

    private val itemEventHandler = ImxItemEventHandler(
        itemMetaHandler,
        itemHandler,
        itemService,
        itemMetaRepository
    )

    private val blockchain = BlockchainDto.IMMUTABLEX

    @BeforeEach
    fun beforeEach() {
        clearMocks(itemMetaRepository)
        coEvery { itemMetaRepository.save(any()) } returns Unit
    }

    @Test
    fun `on empty list`() = runBlocking<Unit> {
        itemEventHandler.handle(emptyList())

        coVerify(exactly = 0) { itemHandler.onEvent(any()) }
        coVerify(exactly = 0) { itemMetaHandler.onEvent(any()) }
        coVerify(exactly = 0) { itemMetaRepository.save(any()) }
    }

    @Test
    fun `on item updated - meta not changed`() = runBlocking<Unit> {
        val asset = randomImxAsset()
        val item = ImxItemConverter.convert(asset, null, blockchain)
        val meta = ImxItemMetaConverter.convert(asset, blockchain)

        mockMeta(asset.itemId, meta)
        mockCreators(asset.itemId)

        itemEventHandler.handle(listOf(asset))

        // Meta is the same, nothing to send
        coVerify(exactly = 1) { itemHandler.onEvent(UnionItemUpdateEvent(item)) }
        coVerify(exactly = 0) { itemMetaHandler.onEvent(any()) }
        coVerify(exactly = 0) { itemMetaRepository.save(any()) }
    }

    @Test
    fun `on item updated - current meta is missing`() = runBlocking<Unit> {
        val asset = randomImxAsset()
        val item = ImxItemConverter.convert(asset, null, blockchain)
        val meta = ImxItemMetaConverter.convert(asset, blockchain)

        mockMeta(asset.itemId, null)
        mockCreators(asset.itemId)

        itemEventHandler.handle(listOf(asset))

        // Meta saved, but there is no refresh event
        coVerify(exactly = 1) { itemHandler.onEvent(UnionItemUpdateEvent(item)) }
        coVerify(exactly = 1) { itemMetaRepository.save(ImxItemMeta(asset.itemId, meta)) }
        coVerify(exactly = 0) { itemMetaHandler.onEvent(any()) }
    }

    @Test
    fun `on item updated - meta changed`() = runBlocking<Unit> {
        val asset = randomImxAsset()
        val item = ImxItemConverter.convert(asset, null, blockchain)
        val newMeta = ImxItemMetaConverter.convert(asset, blockchain)
        val currentMeta = newMeta.copy(name = randomString())

        mockMeta(asset.itemId, currentMeta)
        mockCreators(asset.itemId)

        itemEventHandler.handle(listOf(asset))

        coVerify(exactly = 1) { itemHandler.onEvent(UnionItemUpdateEvent(item)) }
        coVerify(exactly = 1) { itemMetaHandler.onEvent(UnionItemMetaRefreshEvent(item.id)) }
        coVerify(exactly = 1) { itemMetaRepository.save(ImxItemMeta(asset.itemId, newMeta)) }
    }

    @Test
    fun `on item deleted`() = runBlocking<Unit> {
        val asset = randomImxAsset().copy(status = "eth")
        val item = ImxItemConverter.convert(asset, null, blockchain)

        itemEventHandler.handle(listOf(asset))

        // Only delete event, nothing else
        coVerify(exactly = 1) { itemHandler.onEvent(UnionItemDeleteEvent(item.id)) }
        coVerify(exactly = 0) { itemMetaHandler.onEvent(any()) }
        coVerify(exactly = 0) { itemMetaRepository.save(any()) }
    }

    private fun mockMeta(itemId: String, meta: UnionMeta? = null) {
        coEvery {
            itemMetaRepository.getAll(listOf(itemId))
        } returns listOfNotNull(meta?.let { ImxItemMeta(itemId, it) })
    }

    private fun mockCreators(itemId: String, creator: String? = null) {
        coEvery {
            itemService.getItemCreators(listOf(itemId))
        } answers {
            creator?.let { mapOf(itemId to it) } ?: emptyMap()
        }
    }

}