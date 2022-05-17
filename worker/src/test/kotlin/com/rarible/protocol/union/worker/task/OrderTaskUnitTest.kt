package com.rarible.protocol.union.worker.task

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.union.api.client.OrderControllerApi
import com.rarible.protocol.union.core.converter.EsOrderConverter
import com.rarible.protocol.union.core.model.EsOrder
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.core.elasticsearch.repository.EsOrderRepository
import com.rarible.protocol.union.worker.config.*
import com.rarible.protocol.union.worker.task.search.order.OrderTask
import io.mockk.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.math.BigInteger
import java.time.Instant

@Disabled
class OrderTaskUnitTest {
    private val orderRepository = mockk<EsOrderRepository> {
        coEvery {
            saveAll(any<List<EsOrder>>())
        } answers { arg(0) }
    }

    val converter = mockk<EsOrderConverter> {
        every {
            convert(any<OrderDto>())
        } returns EsOrder(
            orderId = randomOrderId().fullId(),
            lastUpdatedAt = Instant.now(),
            type = EsOrder.Type.SELL,
            blockchain = BlockchainDto.ETHEREUM,
            platform = PlatformDto.RARIBLE,
            maker = randomUnionAddress(),
            make = EsOrder.Asset(EthErc721AssetTypeDto(contract = randomContract(), tokenId = BigInteger.ZERO)),
            taker = randomUnionAddress(),
            take = EsOrder.Asset(EthEthereumAssetTypeDto()),
            start = null,
            end = null,
            origins = emptyList(),
            status =  OrderStatusDto.ACTIVE
        )
    }

    val orderClient = mockk<OrderControllerApi> {
        every {
            getOrdersAll(
                listOf(BlockchainDto.ETHEREUM),
                null,
                PageSize.ORDER.max,
                OrderSortDto.LAST_UPDATE_ASC,
                emptyList()
            )
        } returns Mono.just(OrdersDto("ETHEREUM:cursor_1", listOf(mockk())))

        every {
            getOrdersAll(
                listOf(BlockchainDto.ETHEREUM),
                "ETHEREUM:cursor_1",
                PageSize.ORDER.max,
                OrderSortDto.LAST_UPDATE_ASC,
                emptyList()
            )
        } returns Mono.just(OrdersDto(null, listOf(mockk())))
    }

    @Test
    fun `should launch first run of the task`(): Unit = runBlocking {
        val task = OrderTask(
            OrderReindexProperties(
                enabled = true,
                blockchains = listOf(BlockchainReindexProperties(enabled = true, BlockchainDto.ETHEREUM))
            ),
            orderClient,
            orderRepository,
        )

        task.runLongTask(
            null,
            "ETHEREUM"
        ).toList()

        coVerify {
            orderClient.getOrdersAll(
                listOf(BlockchainDto.ETHEREUM),
                null,
                PageSize.ORDER.max,
                OrderSortDto.LAST_UPDATE_ASC,
                emptyList()
            )

            converter.convert(any<OrderDto>())
            orderRepository.saveAll(any())
        }
    }

    @Test
    fun `should launch next run of the task`(): Unit = runBlocking {
        val task = OrderTask(
            OrderReindexProperties(
                enabled = true,
                blockchains = listOf(BlockchainReindexProperties(enabled = true, BlockchainDto.ETHEREUM))
            ),
            orderClient,
            orderRepository
        )

        task.runLongTask(
            "ETHEREUM:cursor_1",
            "ETHEREUM"
        ).toList()

        coVerify {
            orderClient.getOrdersAll(
                listOf(BlockchainDto.ETHEREUM),
                "ETHEREUM:cursor_1",
                PageSize.ORDER.max,
                OrderSortDto.LAST_UPDATE_ASC,
                emptyList()
            )
            converter.convert(any<OrderDto>())
            orderRepository.saveAll(any())
        }
    }

    companion object {
        private fun randomOrderId(): OrderIdDto {
            return OrderIdDto(
                blockchain = BlockchainDto.ETHEREUM,
                value = randomAddress().toString()
            )
        }
        private fun randomUnionAddress(): UnionAddress {
            return UnionAddress(
                blockchainGroup = BlockchainGroupDto.ETHEREUM,
                value = randomAddress().toString()
            )
        }
        private fun randomContract(): ContractAddress {
            return ContractAddress(
                blockchain = BlockchainDto.ETHEREUM,
                value = randomAddress().toString()
            )
        }
    }
}
