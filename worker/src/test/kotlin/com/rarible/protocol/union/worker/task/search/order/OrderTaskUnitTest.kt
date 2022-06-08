package com.rarible.protocol.union.worker.task.search.order

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.enrichment.service.query.order.OrderApiMergeService
import com.rarible.protocol.union.worker.config.BlockchainReindexProperties
import com.rarible.protocol.union.worker.config.OrderReindexProperties
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import com.rarible.protocol.union.worker.task.search.ParamFactory
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyAll
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import randomOrder

class OrderTaskUnitTest {

    private val repo = mockk<EsOrderRepository> {
        coEvery {
            saveAll(any())
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

    private val searchTaskMetricFactory = SearchTaskMetricFactory(SimpleMeterRegistry(), mockk {
        every { metrics } returns mockk { every { rootPath } returns "protocol.union.worker" }
    })

    private val paramFactory = ParamFactory(jacksonObjectMapper().registerKotlinModule())

    @Test
    internal fun `should start first task`() = runBlocking {

        val task = OrderTask(
            OrderReindexProperties(
                enabled = true,
                blockchains = listOf(BlockchainReindexProperties(enabled = true, BlockchainDto.ETHEREUM))
            ),
            orderApiMergeService,
            repo,
            paramFactory,
            searchTaskMetricFactory
        )
        task.runLongTask(
            null, paramFactory.toString(
                OrderTaskParam(
                    blockchain = BlockchainDto.ETHEREUM, index = "test_index"
                )
            )
        ).toList()

        coVerifyAll {
            orderApiMergeService.getOrdersAll(
                listOf(BlockchainDto.ETHEREUM),
                continuation,
                1000,
                OrderSortDto.LAST_UPDATE_DESC,
                OrderStatusDto.values().asList()
            )

            repo.saveAll(any())

            orderApiMergeService.getOrdersAll(
                listOf(BlockchainDto.ETHEREUM),
                null,
                1000,
                OrderSortDto.LAST_UPDATE_DESC,
                OrderStatusDto.values().asList()
            )
        }
    }

    @Test
    fun `should launch next run of the task`(): Unit = runBlocking {
        val task = OrderTask(
            OrderReindexProperties(
                enabled = true,
                blockchains = listOf(BlockchainReindexProperties(enabled = true, BlockchainDto.ETHEREUM))
            ),
            orderApiMergeService,
            repo,
            paramFactory,
            searchTaskMetricFactory
        )

        task.runLongTask(
            orderDto2.id.fullId(),
            paramFactory.toString(
                OrderTaskParam(
                    blockchain = BlockchainDto.ETHEREUM, index = "test_index"
                )
            )
        ).toList()

        coVerify {
            orderApiMergeService.getOrdersAll(
                listOf(BlockchainDto.ETHEREUM),
                continuation,
                1000,
                OrderSortDto.LAST_UPDATE_DESC,
                OrderStatusDto.values().asList()
            )

            repo.saveAll(any())

        }
    }
}
