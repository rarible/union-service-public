package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.service.EthOrderService
import com.rarible.protocol.union.integration.tezos.service.TezosOrderService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class EnrichmentOrderServiceUnitTest {

    @Test
    fun `should get best sell order by item with preferred platform`() = runBlocking<Unit> {
        val service = mockService(
            mockk<EthOrderService> {
                coEvery {
                    getSellOrdersByItem(
                        isNull(),
                        any(),
                        any(),
                        any(),
                        listOf(OrderStatusDto.ACTIVE),
                        any(),
                        any(),
                        any()
                    )
                } returns Slice(
                    null, listOf(
                        randomUnionSellOrderDto().copy(
                            platform = PlatformDto.OPEN_SEA,
                            takePriceUsd = BigDecimal("1337.0")
                        ), // first is best
                    )
                )
                coEvery {
                    getSellOrdersByItem(
                        PlatformDto.RARIBLE,
                        any(),
                        any(),
                        any(),
                        listOf(OrderStatusDto.ACTIVE),
                        any(),
                        any(),
                        any()
                    )
                } returns Slice(
                    null, listOf(
                        randomUnionSellOrderDto().copy(
                            platform = PlatformDto.RARIBLE,
                            takePriceUsd = BigDecimal("1337.00")
                        ),
                    )
                )
            }
        )

        assertThat(service.getBestSell(ShortItemId(randomEthItemId()), "USD", null))
            .hasFieldOrPropertyWithValue(OrderDto::takePriceUsd.name, BigDecimal("1337.00"))
            .hasFieldOrPropertyWithValue(OrderDto::platform.name, PlatformDto.RARIBLE)
    }

    @Test
    fun `should get best sell order by item with preferred platform not best price`() = runBlocking<Unit> {
        val service = mockService(
            mockk<EthOrderService> {
                coEvery {
                    getSellOrdersByItem(
                        isNull(),
                        any(),
                        any(),
                        any(),
                        listOf(OrderStatusDto.ACTIVE),
                        any(),
                        any(),
                        any()
                    )
                } returns Slice(
                    null, listOf(
                        randomUnionSellOrderDto().copy(
                            platform = PlatformDto.OPEN_SEA,
                            takePriceUsd = BigDecimal(1337)
                        ), // first is best
                    )
                )
                coEvery {
                    getSellOrdersByItem(
                        PlatformDto.RARIBLE,
                        any(),
                        any(),
                        any(),
                        listOf(OrderStatusDto.ACTIVE),
                        any(),
                        any(),
                        any()
                    )
                } returns Slice(
                    null, listOf(
                        randomUnionSellOrderDto().copy(platform = PlatformDto.RARIBLE, takePriceUsd = BigDecimal(1338)),
                    )
                )
            }
        )

        assertThat(service.getBestSell(ShortItemId(randomEthItemId()), "USD", null))
            .hasFieldOrPropertyWithValue(OrderDto::takePriceUsd.name, BigDecimal(1337))
            .hasFieldOrPropertyWithValue(OrderDto::platform.name, PlatformDto.OPEN_SEA)
    }

    private fun mockService(vararg specificServices: OrderService): EnrichmentOrderService {
        val router = mockk<BlockchainRouter<OrderService>> {
            specificServices.forEach { orderService ->
                when (orderService) {
                    is EthOrderService -> every {
                        getService(BlockchainDto.ETHEREUM)
                    } returns orderService

                    is TezosOrderService -> every {
                        getService(BlockchainDto.TEZOS)
                    } returns orderService
                }
            }
        }

        return EnrichmentOrderService(router)
    }
}