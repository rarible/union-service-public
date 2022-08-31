package com.rarible.protocol.union.worker.task.search.item

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.ItemContinuation
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.enrichment.converter.EnrichedItemConverter
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.worker.config.BlockchainReindexProperties
import com.rarible.protocol.union.worker.config.ItemReindexProperties
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import com.rarible.protocol.union.worker.task.search.ItemTaskParam
import com.rarible.protocol.union.worker.task.search.ParamFactory
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class ItemTaskTest {

    private val repo = mockk<EsItemRepository> {
        coEvery {
            saveAll(any(), any(), any())
        } answers { arg(0) }
    }

    private val taskRepository = mockk<TaskRepository> {
        coEvery {
            findByTypeAndParam(any(), any())
        } returns mono { Task(type = "", param = "", running = true) }
    }

    private val item = randomUnionItem(randomEthItemId())
    private val enrichedItem = EnrichedItemConverter.convert(item)
    private val firstContinuation = ItemContinuation.ByLastUpdatedAndId.getContinuation(enrichedItem).toString()

    private val itemService = mockk<ItemService> {
        coEvery { getAllItems(null, any(), any(), any(), any()) } returns Page(
            total = 1L, entities = listOf(item), continuation = firstContinuation
        )

        coEvery {
            getAllItems(firstContinuation, any(), any(), any(), any())
        } returns Page.empty()
    }

    private val itemServiceRouter = mockk<BlockchainRouter<ItemService>>() {
        coEvery { getService(any()) } returns itemService
    }

    private val searchTaskMetricFactory = SearchTaskMetricFactory(SimpleMeterRegistry(), mockk {
        every { metrics } returns mockk { every { rootPath } returns "protocol.union.worker" }
    })

    private val enrichmentItemService: EnrichmentItemService = mockk()

    private val paramFactory = ParamFactory(jacksonObjectMapper().registerKotlinModule())

    private val itemReindexProperties = ItemReindexProperties(
        enabled = true,
        blockchains = listOf(BlockchainReindexProperties(enabled = true, BlockchainDto.ETHEREUM))
    )

    @Test
    fun `should start first task`() = runBlocking<Unit> {

        val task = ItemTask(
            itemReindexProperties,
            itemServiceRouter,
            enrichmentItemService,
            paramFactory,
            repo,
            searchTaskMetricFactory,
            taskRepository
        )

        val param = paramFactory.toString(
            ItemTaskParam(
                versionData = 1,
                settingsHash = "",
                blockchain = BlockchainDto.ETHEREUM,
                index = "test_index"
            )
        )
        coEvery { enrichmentItemService.enrichItems(listOf(item), ItemMetaPipeline.SYNC) } returns listOf(enrichedItem)
        coEvery { enrichmentItemService.enrichItems(listOf(), ItemMetaPipeline.SYNC) } returns emptyList()

        task.runLongTask(null, param).toList()

        coVerifyAll {
            itemService.getAllItems(null, 1000, true, null, null
                )
            repo.saveAll(any(), any(), any())
            itemService.getAllItems(firstContinuation, 1000, true, null, null)
        }
    }
}
