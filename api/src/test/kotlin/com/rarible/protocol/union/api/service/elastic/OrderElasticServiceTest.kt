package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.core.converter.EsOrderConverter
import com.rarible.protocol.union.core.model.elastic.EsAllOrderFilter
import com.rarible.protocol.union.core.model.elastic.EsOrderSort
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.converter.OrderDtoConverter
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.integration.ethereum.service.EthOrderService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import randomOrderId
import randomUnionOrder

internal class OrderElasticServiceTest {

    val ethOrders = listOf(
        randomUnionOrder().copy(id = randomOrderId(BlockchainDto.ETHEREUM)),
        randomUnionOrder().copy(id = randomOrderId(BlockchainDto.ETHEREUM)),
        randomUnionOrder().copy(id = randomOrderId(BlockchainDto.ETHEREUM))
    )

    val ethereumOrderService = mockk<EthOrderService> {
        coEvery {
            getOrdersByIds(any())
        } returns ethOrders
    }

    val router = mockk<BlockchainRouter<OrderService>> {
        every {
            getEnabledBlockchains(any())
        } answers { arg(0) }

        every {
            isBlockchainEnabled(any())
        } returns true

        every {
            getService(BlockchainDto.ETHEREUM)
        } returns ethereumOrderService
    }

    val esOrderRepository = mockk<EsOrderRepository> {
        coEvery {
            findByFilter(any())
        } returns ethOrders.map {
            EsOrderConverter.convert(OrderDtoConverter.convert(it))
        }.sortedBy { it.lastUpdatedAt }
    }

    val orderElasticService = OrderElasticService(router, esOrderRepository)

    @Test
    fun fetchOrders() = runBlocking<Unit> {
        val result = orderElasticService.fetchOrders(
            EsAllOrderFilter(
                blockchains = listOf(BlockchainDto.ETHEREUM),
                cursor = null,
                size = 100,
                status = null,
                sort = EsOrderSort.LAST_UPDATE_DESC
            )
        )

        assertThat(result.entities).containsExactlyElementsOf(
            ethOrders.sortedBy { it.lastUpdatedAt }
        )
    }
}