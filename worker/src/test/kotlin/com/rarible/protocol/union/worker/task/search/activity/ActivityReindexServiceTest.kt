package com.rarible.protocol.union.worker.task.search.activity

import com.rarible.core.common.nowMillis
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import com.rarible.protocol.union.worker.task.search.EsRateLimiter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.action.support.WriteRequest
import org.junit.jupiter.api.Test
import randomUnionAddress
import java.math.BigInteger

internal class ActivityReindexServiceTest {

    private val counter = mockk<RegisteredCounter> {
        every {
            increment(any())
        } returns Unit
    }

    private val searchTaskMetricFactory = mockk<SearchTaskMetricFactory> {
        every {
            createReindexActivityCounter(any(), any())
        } returns counter
    }

    private val esRepo = mockk<EsActivityRepository> {
        coEvery {
            saveAll(any(), any(), WriteRequest.RefreshPolicy.NONE)
        } answers { arg(0) }

        coEvery {
            deleteAll(any())
        } returns Unit
    }

    private val converter = mockk<EsActivityConverter> {
        coEvery { batchConvert(any()) } returns listOf(mockk())
    }

    private val rateLimiter = mockk<EsRateLimiter> {
        coEvery { waitIfNecessary(any()) } just runs
    }

    @Test
    fun `should skip reindexing if there's nothing to reindex`() = runBlocking<Unit> {
        val service = ActivityReindexService(
            mockk {
                coEvery {
                    getAllActivitiesSync(any(), any(), any(), any(), any())
                } returns ActivitiesDto(
                    null, null, emptyList()
                )
            },
            esRepo,
            searchTaskMetricFactory,
            converter,
            rateLimiter,
        )
        coEvery { converter.batchConvert(any()) } returns emptyList()

        assertThat(
            service
                .reindex(BlockchainDto.FLOW, SyncTypeDto.ORDER, "test_index")
                .toList()
        ).containsExactly("")

        coVerify(exactly = 1) {
            esRepo.saveAll(emptyList(), "test_index", WriteRequest.RefreshPolicy.NONE)
            counter.increment(0)
        }
    }

    @Test
    fun `should reindex two rounds`() = runBlocking<Unit> {
        val service = ActivityReindexService(
            mockk {
                coEvery {
                    getAllActivitiesSync(BlockchainDto.ETHEREUM, eq("step_1"), any(), any(), SyncTypeDto.ORDER)
                } returns ActivitiesDto(
                    null, null, listOf(
                        randomActivityDto()
                    )
                )

                coEvery {
                    getAllActivitiesSync(BlockchainDto.ETHEREUM, null, any(), any(), SyncTypeDto.ORDER)
                } returns ActivitiesDto(
                    "step_1", "step_1", listOf(
                        randomActivityDto()
                    )
                )
            },
            esRepo,
            searchTaskMetricFactory,
            converter,
            rateLimiter
        )

        assertThat(
            service
                .reindex(BlockchainDto.ETHEREUM, SyncTypeDto.ORDER, "test_index")
                .toList()
        ).containsExactly("step_1", "") // an empty string is always emitted in the end of loop

        coVerify(exactly = 2) {
            esRepo.saveAll(any(), "test_index", WriteRequest.RefreshPolicy.NONE)
            counter.increment(1)
        }
    }

    @Test
    fun `should remove reverted`() = runBlocking<Unit> {
        val service = ActivityReindexService(
            mockk {
                coEvery {
                    getAllRevertedActivitiesSync(BlockchainDto.ETHEREUM, eq("test_index"), any(), any(), any())
                } returns ActivitiesDto("step_1", "step_1", listOf(randomActivityDto()))

                coEvery {
                    getAllRevertedActivitiesSync(BlockchainDto.ETHEREUM, eq("step_1"), any(), any(), any())
                } returns ActivitiesDto(null, null, listOf(randomActivityDto()))
            },
            esRepo,
            searchTaskMetricFactory,
            converter,
            rateLimiter
        )

        assertThat(
            service
                .removeReverted(BlockchainDto.ETHEREUM, SyncTypeDto.ORDER, "test_index")
                .toList()
        ).containsExactly("step_1", "") // an empty string is always emitted in the end of loop

        coVerify(exactly = 2) {
            esRepo.deleteAll(any())
        }
    }

    private fun randomActivityDto(): MintActivityDto {
        return MintActivityDto(
            id = ActivityIdDto(BlockchainDto.ETHEREUM, randomString()),
            nowMillis(),
            owner = randomUnionAddress(),
            value = BigInteger.ONE,
            transactionHash = randomString()
        )
    }
}
