package com.rarible.protocol.union.worker.task.search.activity

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import com.rarible.protocol.union.worker.task.search.collection.CollectionReindexService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import randomUnionAddress


class CollectionReindexServiceTest {

    private val counter = mockk<RegisteredCounter> {
        every {
            increment(any())
        } returns Unit
    }

    private val searchTaskMetricFactory = mockk<SearchTaskMetricFactory> {
        every {
            createReindexCollectionCounter(any())
        } returns counter
    }

    private val esRepo = mockk<EsCollectionRepository> {
        coEvery {
            saveAll(any(), any(), any())
        } answers { arg(0) }
    }

    @Test
    fun `should skip reindexing if there's nothing to reindex`() = runBlocking<Unit> {
        val service = CollectionReindexService(
            mockk {
                coEvery {
                    getAllCollections(
                        any(), any(), any()
                    )
                } returns CollectionsDto(0, null, emptyList())
            },
            esRepo,
            searchTaskMetricFactory
        )
        Assertions.assertThat(
            service
                .reindex(BlockchainDto.FLOW, "test_index")
                .toList()
        ).containsExactly("")

        coVerify(exactly = 0) {
            esRepo.saveAll(any(), "test_index")
            counter.increment(0)
        }
    }

    @Test
    fun `should reindex two rounds`() = runBlocking<Unit> {
        val service = CollectionReindexService(
            mockk {
                coEvery {
                    getAllCollections(listOf(BlockchainDto.ETHEREUM), eq("step_1"), any())
                } returns CollectionsDto(
                    1, null, listOf(
                        randomCollectionDto()
                    )
                )

                coEvery {
                    getAllCollections(listOf(BlockchainDto.ETHEREUM), null, any())
                } returns CollectionsDto(
                    1, "step_1", listOf(
                        randomCollectionDto()
                    )
                )
            },
            esRepo,
            searchTaskMetricFactory,
        )

        Assertions.assertThat(
            service
                .reindex(BlockchainDto.ETHEREUM, "test_index")
                .toList()
        ).containsExactly("step_1", "") // an empty string is always emitted in the end of loop

        coVerify(exactly = 2) {
            esRepo.saveAll(any(), "test_index", any())
            counter.increment(1)
        }
    }

    private fun randomCollectionDto(): CollectionDto {
        return CollectionDto(
            id = CollectionIdDto(BlockchainDto.ETHEREUM, randomString()),
            parent = null,
            blockchain = BlockchainDto.ETHEREUM,
            type = CollectionDto.Type.CRYPTO_PUNKS,
            name = randomString(),
            symbol = null,
            owner = randomUnionAddress(BlockchainDto.ETHEREUM),
        )
    }

}