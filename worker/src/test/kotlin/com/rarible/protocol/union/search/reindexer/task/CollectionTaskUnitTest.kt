package com.rarible.protocol.union.search.reindexer.task

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.dto.continuation.CollectionContinuation
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.service.query.collection.CollectionApiMergeService
import com.rarible.protocol.union.worker.config.BlockchainReindexProperties
import com.rarible.protocol.union.worker.config.CollectionReindexProperties
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import com.rarible.protocol.union.worker.task.search.ParamFactory
import com.rarible.protocol.union.worker.task.search.collection.CollectionTask
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@Suppress("ReactiveStreamsUnusedPublisher")
class CollectionTaskUnitTest {

    private val paramFactory = ParamFactory(jacksonObjectMapper().registerKotlinModule())

    private val collection = CollectionDto(
        id = CollectionIdDto(BlockchainDto.ETHEREUM, "${randomAddress()}"),
        blockchain = BlockchainDto.ETHEREUM,
        type = CollectionDto.Type.ERC721,
        name = randomString(),
    )

    private val repo = mockk<EsCollectionRepository> {
        coEvery {
            saveAll(any())
        } answers { arg(0) }
    }


    private val collectionApiMergeService = mockk<CollectionApiMergeService> {
        coEvery { getAllCollections(any<List<BlockchainDto>>(), null, any()) } returns CollectionsDto(
            total = 1L,
            collections = listOf(
                collection
            ),
            continuation = CollectionContinuation.ById.getContinuation(collection).toString()
        )

        coEvery {
            getAllCollections(any<List<BlockchainDto>>(),
                CollectionContinuation.ById.getContinuation(collection).toString(),
                any())
        } returns CollectionsDto(
            total = 0L,
            collections = emptyList(),
            continuation = null
        )
    }

    private val mockCounter = mockk<RegisteredCounter> {
        every { increment(any()) } returns Unit
    }

    private val metricFactory = mockk<SearchTaskMetricFactory> {
        every {
            createReindexCollectionCounter(any())
        } returns mockCounter
    }

    @Test
    internal fun `should start first task`() {
        runBlocking {
            val task = CollectionTask(
                CollectionReindexProperties(
                    enabled = true,
                    blockchains = listOf(BlockchainReindexProperties(enabled = true, BlockchainDto.ETHEREUM))
                ),
                paramFactory,
                mockk()
            )
            val result = task.runLongTask(null, "ETHEREUM").toList()

            assertThat(result).containsExactly(collection.id.value, "")

            coVerifyAll {
                collectionApiMergeService.getAllCollections(
                    listOf(BlockchainDto.ETHEREUM),
                    null,
                    1000
                )
                repo.saveAll(any())
                mockCounter.increment(1)
                collectionApiMergeService.getAllCollections(
                    listOf(BlockchainDto.ETHEREUM),
                    collection.id.value,
                    1000
                )
            }
        }
    }

    @Test
    internal fun `should return empty continuation`() {
        runBlocking {
            val task = CollectionTask(
                CollectionReindexProperties(
                    enabled = true,
                    blockchains = listOf(BlockchainReindexProperties(enabled = true, BlockchainDto.ETHEREUM))
                ),
                paramFactory,
                mockk()
            )

            val from = CollectionContinuation.ById.getContinuation(collection).toString()
            val list = task.runLongTask(from, "ETHEREUM").toList()

            assertThat(list).containsExactly("")

            coVerifyAll {
                collectionApiMergeService.getAllCollections(
                    listOf(BlockchainDto.ETHEREUM),
                    from,
                    1000
                )
            }
            assertThat(list).isEqualTo(listOf(""))
        }
    }
}
