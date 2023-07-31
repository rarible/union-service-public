package com.rarible.protocol.union.worker.task.meta

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaService
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class RefreshMetaTaskTest {
    @InjectMockKs
    private lateinit var refreshMetaTask: RefreshMetaTask

    @MockK
    private lateinit var router: BlockchainRouter<ItemService>

    @MockK
    private lateinit var itemService: ItemService

    @MockK
    private lateinit var itemRepository: ItemRepository

    @SpyK
    private var objectMapper: ObjectMapper = jacksonObjectMapper()

    @MockK
    private lateinit var itemMetaService: ItemMetaService

    @BeforeEach
    fun before() {
        every { router.getService(BlockchainDto.ETHEREUM) } returns itemService
    }

    @Test
    fun `run full refresh`() = runBlocking<Unit> {
        val collectionId = "ETHEREUM:0xb887cd1ab6d2d26befce83029a80c7964b8dc28d"
        val itemId1 = ItemIdDto(BlockchainDto.ETHEREUM, "0xb887cd1ab6d2d26befce83029a80c7964b8dc28d:1")
        val itemId2 = ItemIdDto(BlockchainDto.ETHEREUM, "0xb887cd1ab6d2d26befce83029a80c7964b8dc28d:2")

        coEvery {
            itemService.getItemsByCollection(
                collection = "0xb887cd1ab6d2d26befce83029a80c7964b8dc28d",
                owner = null,
                continuation = "from",
                size = 1000
            )
        } returns Page(
            total = 0,
            continuation = "continuation2",
            entities = listOf(randomUnionItem(id = itemId1))
        )
        coEvery {
            itemService.getItemsByCollection(
                collection = "0xb887cd1ab6d2d26befce83029a80c7964b8dc28d",
                owner = null,
                continuation = "continuation2",
                size = 1000
            )
        } returns Page(
            total = 0,
            continuation = null,
            entities = listOf(randomUnionItem(id = itemId2))
        )
        coEvery { itemRepository.getAll(listOf(ShortItemId(itemId1))) } returns listOf(createShortItem(id = itemId1))
        coEvery { itemRepository.getAll(listOf(ShortItemId(itemId2))) } returns listOf(
            createShortItem(
                id = itemId2,
                meta = randomItemMetaDownloadEntry()
            )
        )
        coEvery {
            itemMetaService.schedule(
                itemId = itemId1,
                pipeline = ItemMetaPipeline.REFRESH,
                force = true
            )
        } returns Unit

        coEvery {
            itemMetaService.schedule(
                itemId = itemId2,
                pipeline = ItemMetaPipeline.REFRESH,
                force = true
            )
        } returns Unit

        val result = refreshMetaTask.runLongTask(
            "from",
            """{"collectionId":"$collectionId","full":true}"""
        ).toList()

        assertThat(result).containsExactly("continuation2")

        coVerify {
            itemMetaService.schedule(
                itemId = itemId1,
                pipeline = ItemMetaPipeline.REFRESH,
                force = true
            )
            itemMetaService.schedule(
                itemId = itemId2,
                pipeline = ItemMetaPipeline.REFRESH,
                force = true
            )
        }
    }

    @Test
    fun `partial refresh`() = runBlocking<Unit> {
        val collectionId = "ETHEREUM:0xb887cd1ab6d2d26befce83029a80c7964b8dc28d"
        val itemId1 = ItemIdDto(BlockchainDto.ETHEREUM, "0xb887cd1ab6d2d26befce83029a80c7964b8dc28d:1")
        val itemId2 = ItemIdDto(BlockchainDto.ETHEREUM, "0xb887cd1ab6d2d26befce83029a80c7964b8dc28d:2")
        coEvery {
            itemService.getItemsByCollection(
                collection = "0xb887cd1ab6d2d26befce83029a80c7964b8dc28d",
                owner = null,
                continuation = "from",
                size = 1000
            )
        } returns Page(
            total = 0,
            continuation = "continuation2",
            entities = listOf(randomUnionItem(id = itemId1))
        )
        coEvery {
            itemService.getItemsByCollection(
                collection = "0xb887cd1ab6d2d26befce83029a80c7964b8dc28d",
                owner = null,
                continuation = "continuation2",
                size = 1000
            )
        } returns Page(
            total = 0,
            continuation = null,
            entities = listOf(randomUnionItem(id = itemId2))
        )
        coEvery { itemRepository.getAll(listOf(ShortItemId(itemId1))) } returns listOf(
            createShortItem(
                id = itemId1,
                meta = randomItemMetaDownloadEntry()
            )
        )
        coEvery { itemRepository.getAll(listOf(ShortItemId(itemId2))) } returns listOf(createShortItem(id = itemId2))

        coEvery {
            itemMetaService.schedule(
                itemId = itemId2,
                pipeline = ItemMetaPipeline.REFRESH,
                force = true
            )
        } returns Unit

        val result = refreshMetaTask.runLongTask(
            "from",
            """{"collectionId":"$collectionId","full":false}"""
        ).toList()

        assertThat(result).containsExactly("continuation2")

        coVerify(exactly = 0) {
            itemMetaService.schedule(
                itemId = itemId1,
                pipeline = ItemMetaPipeline.REFRESH,
                force = true
            )
        }

        coVerify {
            itemMetaService.schedule(
                itemId = itemId2,
                pipeline = ItemMetaPipeline.REFRESH,
                force = true
            )
        }
    }

    private fun createShortItem(id: ItemIdDto, meta: DownloadEntry<UnionMeta>? = null) =
        randomShortItem(id).copy(metaEntry = meta)
}
