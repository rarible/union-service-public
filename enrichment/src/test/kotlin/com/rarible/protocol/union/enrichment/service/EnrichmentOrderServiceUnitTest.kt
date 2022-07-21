package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
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
    fun `should get best sell order by item`() = runBlocking<Unit> {
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
                } returns Slice(null, listOf(
                    randomUnionSellOrderDto().copy(platform = PlatformDto.OPEN_SEA, takePrice = BigDecimal(1337)), // first is best
                    randomUnionSellOrderDto().copy(platform = PlatformDto.RARIBLE),
                    randomUnionSellOrderDto().copy(platform = PlatformDto.CRYPTO_PUNKS),
                ))
            }
        )

        assertThat(
            service.getBestSell(ShortItemId(randomEthItemId()), "USD", null)?.takePrice
        ).isEqualTo(BigDecimal(1337))
    }


    private fun mockService(vararg specificServices: OrderService): EnrichmentOrderService {
        val router = mockk<BlockchainRouter<OrderService>> {
            specificServices.forEach { orderService ->
                when(orderService) {
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