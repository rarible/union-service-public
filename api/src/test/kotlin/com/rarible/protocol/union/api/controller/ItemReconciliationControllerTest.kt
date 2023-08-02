package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.select.ItemSourceSelectService
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.enrichment.model.ShortDateIdItem
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomItemDto
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class ItemReconciliationControllerTest {
    private val itemRepository = mockk<ItemRepository>()
    private val itemSourceSelectService = mockk<ItemSourceSelectService>()
    private val controller = ItemReconciliationController(itemRepository, itemSourceSelectService)

    @Test
    fun `get item - with continuation`() = runBlocking<Unit> {
        val from = Instant.ofEpochSecond(1)
        val to = Instant.ofEpochSecond(10)

        val id1 = ShortDateIdItem(randomShortItem().id, Instant.ofEpochSecond(2))
        val id2 = ShortDateIdItem(randomShortItem().id, Instant.ofEpochSecond(3))

        coEvery { itemRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = from,
            lastUpdatedTo = to,
            fromId = null,
            size = 2
        ) } returns listOf(id1, id2)

        coEvery {
            itemSourceSelectService.getItemsByIds(listOf(id1.id.toDto(), id2.id.toDto()))
        } returns listOf(randomItemDto(randomEthItemId()), randomItemDto(randomEthItemId()))

        val items = controller.getItems(
            lastUpdatedFrom = from,
            lastUpdatedTo = to,
            continuation = null,
            size = 2,
        )
        assertThat(items.items).hasSize(2)
        assertThat(items.continuation).isEqualTo(DateIdContinuation(id2.lastUpdatedAt, id2.id.toDto().fullId()).toString())
    }

    @Test
    fun `get item - no continuation`() = runBlocking<Unit> {
        val from = Instant.ofEpochSecond(1)
        val to = Instant.ofEpochSecond(10)

        val actualFrom = Instant.ofEpochSecond(5)
        val actualFromId = randomShortItem()

        val id1 = ShortDateIdItem(randomShortItem().id, Instant.ofEpochSecond(2))
        val id2 = ShortDateIdItem(randomShortItem().id, Instant.ofEpochSecond(3))

        coEvery { itemRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = actualFrom,
            lastUpdatedTo = to,
            fromId = actualFromId.id,
            size = 10
        ) } returns listOf(id1, id2)

        coEvery {
            itemSourceSelectService.getItemsByIds(listOf(id1.id.toDto(), id2.id.toDto()))
        } returns listOf(randomItemDto(randomEthItemId()), randomItemDto(randomEthItemId()))

        val items = controller.getItems(
            lastUpdatedFrom = from,
            lastUpdatedTo = to,
            continuation = DateIdContinuation(actualFrom, actualFromId.id.toDto().fullId()).toString(),
            size = 10,
        )
        assertThat(items.items).hasSize(2)
        assertThat(items.continuation).isNull()
    }
}
