package com.rarible.protocol.union.worker.task.search.order

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionEthCollectionAssetType
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.converter.OrderDtoConverter
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService
import com.rarible.protocol.union.enrichment.service.query.order.OrderApiMergeService
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
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import randomUnionOrder
import java.math.BigDecimal

class OrderReindexServiceUnitTest {

    private val repo = mockk<EsOrderRepository> {
        coEvery {
            saveAll(any(), any(), any())
        } answers { arg(0) }
    }

    private val enrichmentOrderService: EnrichmentOrderService = mockk() {
        coEvery { enrich(any<UnionOrder>()) } coAnswers { OrderDtoConverter.convert(it.invocation.args[0] as UnionOrder) }
    }

    private val orderDto1 = randomUnionOrder().copy(
        take = UnionAsset(
            type = UnionEthCollectionAssetType(
                contract = ContractAddress(
                    blockchain = BlockchainDto.ETHEREUM,
                    value = "0x8584d66b25318f31baf5b0fe13e7d2486d2b2f63"
                )
            ), value = BigDecimal.ONE
        )
    )
    private val orderDto2 = randomUnionOrder()
    private val orderDto3 = randomUnionOrder()
    private val continuation = orderDto2.id.fullId()

    private val orderApiMergeService = mockk<OrderApiMergeService> {

        coEvery { getOrdersAll(any(), null, any(), any(), any()) } returns Slice(
            entities = listOf(orderDto1, orderDto2), continuation = continuation
        )

        coEvery { getOrdersAll(any(), continuation, any(), any(), any()) } returns Slice(
            entities = listOf(orderDto3), continuation = null
        )
    }

    private val counter = mockk<RegisteredCounter> {
        every { increment(any()) } returns Unit
    }

    private val searchTaskMetricFactory = mockk<SearchTaskMetricFactory> {
        every {
            createReindexOrderCounter(any())
        } returns counter
    }

    private val rateLimiter = mockk<EsRateLimiter> {
        coEvery { waitIfNecessary(any()) } just runs
    }

    @Test
    fun `should skip reindexing if there's nothing to reindex`() = runBlocking<Unit> {
        val service = OrderReindexService(
            mockk(),
            mockk {
                coEvery { getOrdersAll(any(), any(), any(), any(), any()) } returns Slice(null, emptyList())
            },
            repo,
            searchTaskMetricFactory,
            rateLimiter,
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
            enrichmentOrderService,
            orderApiMergeService,
            repo,
            searchTaskMetricFactory,
            rateLimiter,
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
