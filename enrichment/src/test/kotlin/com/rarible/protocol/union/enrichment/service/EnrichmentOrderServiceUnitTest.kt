package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionEthCollectionAssetType
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.enrichment.converter.OrderDtoConverter
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.test.data.randomUnionBidOrder
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrder
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.service.EthOrderService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class EnrichmentOrderServiceUnitTest {

    private val customCollectionResolver: CustomCollectionResolver = mockk {
        coEvery { resolveCustomCollection(any<ItemIdDto>()) } returns null
        coEvery { resolveCustomCollection(any<CollectionIdDto>()) } returns null
    }
    private val ethOrderService: EthOrderService = mockk()
    private val router: BlockchainRouter<OrderService> = mockk {
        coEvery { getService(BlockchainDto.ETHEREUM) } returns ethOrderService
    }

    private val service = EnrichmentOrderService(router, customCollectionResolver)

    @Test
    fun `enrich - ok`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val order = randomUnionSellOrder(itemId)

        coEvery { customCollectionResolver.resolveCustomCollection(itemId) } returns null

        val result = service.enrich(order)
        val expected = OrderDtoConverter.convert(order)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `enrich - ok, custom collection by item`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val customCollection = randomEthCollectionId()
        val order = randomUnionSellOrder(itemId)

        coEvery { customCollectionResolver.resolveCustomCollection(itemId) } returns customCollection

        val result = service.enrich(order)

        assertThat(result.make.type.ext.collectionId).isEqualTo(customCollection)
    }

    @Test
    fun `enrich - ok, custom collection by collection`() = runBlocking<Unit> {
        val contract = ContractAddress(BlockchainDto.ETHEREUM, randomEthAddress())
        val collectionId = CollectionIdDto(contract.blockchain, contract.value)
        val collectionAsset = UnionAsset(UnionEthCollectionAssetType(contract), BigDecimal.ONE)
        val customCollection = randomEthCollectionId()

        val order = randomUnionBidOrder().copy(take = collectionAsset)

        coEvery { customCollectionResolver.resolveCustomCollection(collectionId) } returns customCollection

        val result = service.enrich(order)

        assertThat(result.take.type.ext.collectionId).isEqualTo(customCollection)
    }

    @Test
    fun `should get best sell order by item with preferred platform`() = runBlocking<Unit> {

        coEvery {
            ethOrderService.getSellOrdersByItem(
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
                randomUnionSellOrder().copy(
                    platform = PlatformDto.OPEN_SEA,
                    takePriceUsd = BigDecimal("1337.0")
                ), // first is best
            )
        )

        coEvery {
            ethOrderService.getSellOrdersByItem(
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
                randomUnionSellOrder().copy(
                    platform = PlatformDto.RARIBLE,
                    takePriceUsd = BigDecimal("1337.00")
                ),
            )
        )

        assertThat(service.getBestSell(ShortItemId(randomEthItemId()), "USD", null))
            .hasFieldOrPropertyWithValue(OrderDto::takePriceUsd.name, BigDecimal("1337.00"))
            .hasFieldOrPropertyWithValue(OrderDto::platform.name, PlatformDto.RARIBLE)
    }

    @Test
    fun `should get best sell order by item with preferred platform not best price`() = runBlocking<Unit> {

        coEvery {
            ethOrderService.getSellOrdersByItem(
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
                randomUnionSellOrder().copy(
                    platform = PlatformDto.OPEN_SEA,
                    takePriceUsd = BigDecimal(1337)
                ), // first is best
            )
        )
        coEvery {
            ethOrderService.getSellOrdersByItem(
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
                randomUnionSellOrder().copy(platform = PlatformDto.RARIBLE, takePriceUsd = BigDecimal(1338)),
            )
        )

        assertThat(service.getBestSell(ShortItemId(randomEthItemId()), "USD", null))
            .hasFieldOrPropertyWithValue(OrderDto::takePriceUsd.name, BigDecimal(1337))
            .hasFieldOrPropertyWithValue(OrderDto::platform.name, PlatformDto.OPEN_SEA)
    }
}