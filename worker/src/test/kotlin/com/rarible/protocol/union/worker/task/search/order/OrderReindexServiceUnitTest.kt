package com.rarible.protocol.union.worker.task.search.order;

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.enrichment.service.query.order.OrderApiMergeService
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import randomOrder

class OrderReindexServiceUnitTest {

    private val repo = mockk<EsOrderRepository> {
        coEvery {
            saveAll(any(), any(), any())
        } answers { arg(0) }
    }

    private val orderDto1 = randomOrder()
    private val orderDto2 = randomOrder()
    private val orderDto3 = randomOrder()
    private val continuation = orderDto2.id.fullId()

    private val orderApiMergeService = mockk<OrderApiMergeService> {

        coEvery { getOrdersAll(any(), null, any(), any(), any()) } returns OrdersDto(
            orders = listOf(orderDto1, orderDto2), continuation = continuation
        )

        coEvery {
            getOrdersAll(
                any(), continuation, any(), any(), any()
            )
        } returns OrdersDto(
            orders = listOf(orderDto3), continuation = null
        )
    }

    private val counter = mockk<RegisteredCounter> {
        every {
            increment(any())
        } returns Unit
    }

    private val searchTaskMetricFactory = mockk<SearchTaskMetricFactory> {
        every {
            createReindexOrderCounter(any())
        } returns counter
    }

    @Test
    fun `should skip reindexing if there's nothing to reindex`() = runBlocking<Unit> {
        val service = OrderReindexService(
            mockk {
                coEvery {
                    getOrdersAll(
                        any(), any(), any(), any(), any()
                    )
                } returns OrdersDto(null, emptyList())
            },
            repo,
            searchTaskMetricFactory
        )
        Assertions.assertThat(
            service
                .reindex(BlockchainDto.FLOW, "test_index")
                .toList()
        ).containsExactly("")

        coVerify(exactly = 0) {
            repo.saveAll(any(), any(), any())
            counter.increment(0)
        }
    }

    @Test
    fun `should reindex two rounds`() = runBlocking<Unit> {
        val service = OrderReindexService(
            orderApiMergeService,
            repo,
            searchTaskMetricFactory,
        )

        Assertions.assertThat(
            service
                .reindex(BlockchainDto.ETHEREUM, "test_index")
                .toList()
        ).containsExactly(orderDto2.id.fullId(), "") // an empty string is always emitted in the end of loop

        coVerify {
            repo.saveAll(any(), any(), any())
            counter.increment(1)
        }
    }
}
