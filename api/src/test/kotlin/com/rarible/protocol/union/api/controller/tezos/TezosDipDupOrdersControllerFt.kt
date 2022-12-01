package com.rarible.protocol.union.api.controller.tezos

import com.rarible.core.common.nowMillis
import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.client.core.model.OrderStatus
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.dipdup.client.exception.DipDupNotFound
import com.rarible.dipdup.client.model.DipDupOrdersPage
import com.rarible.protocol.union.api.client.OrderControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.UnionApiErrorEntityNotFoundDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.tezos.data.randomTezosItemId
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.BigInteger
import java.time.ZoneOffset
import java.util.*

@FlowPreview
@IntegrationTest
class TezosDipDupOrdersControllerFt : AbstractIntegrationTest() {

    private val continuation: String? = null
    private val size = PageSize.ORDER.default
    private val platform: PlatformDto? = null

    @Autowired
    lateinit var orderControllerClient: OrderControllerApi

    @Test
    fun `get sell orders by item - tezos`() = runBlocking<Unit> {
        val tezosItemId = randomTezosItemId()

        val (contract, tokenId) = CompositeItemIdParser.split(tezosItemId.value)
        val maker = UnionAddressConverter.convert(BlockchainDto.TEZOS, randomEthAddress())
        val dipdupOrder = dipDupOrder(UUID.randomUUID().toString())

        coEvery {
            testDipDupOrderClient.getOrdersByItem(contract, tokenId.toString(), any(), "XTZ", any(), any(), any(), any(), any())
        } returns DipDupOrdersPage(listOf(dipdupOrder))

        coEvery {
            testDipDupOrderClient.getSellOrdersCurrenciesByItem(contract, tokenId.toString())
        } returns listOf(Asset.XTZ())

        val orders = orderControllerClient.getSellOrdersByItem(
            tezosItemId.fullId(),
            platform,
            maker.fullId(),
            null,
            null,
            continuation,
            size,
            null
        ).awaitFirst()

        assertThat(orders.orders).hasSize(1)
        assertThat(orders.orders[0]).isInstanceOf(OrderDto::class.java)
        assertThat(orders.continuation).isNull()
    }

    @Test
    fun `should send 404`() = runBlocking<Unit> {
        val orderId = UnionAddressConverter.convert(BlockchainDto.TEZOS, UUID.randomUUID().toString())

        coEvery {
            testDipDupOrderClient.getOrderById(orderId.value)
        } throws DipDupNotFound("")

        try {
            orderControllerClient.getOrderById(orderId.fullId()).awaitFirstOrNull()
        } catch (e: OrderControllerApi.ErrorGetOrderById) {
            assertThat(e.on404).isInstanceOf(UnionApiErrorEntityNotFoundDto::class.java)
        }
    }

    private fun dipDupOrder(orderId: String): DipDupOrder {
        return DipDupOrder(
            id = orderId,
            internalOrderId = "0",
            fill = BigDecimal.ZERO,
            platform = TezosPlatform.HEN,
            payouts = emptyList(),
            originFees = emptyList(),
            status = OrderStatus.ACTIVE,
            startAt = null,
            endAt = null,
            endedAt = null,
            lastUpdatedAt = nowMillis().atOffset(ZoneOffset.UTC),
            createdAt = nowMillis().atOffset(ZoneOffset.UTC),
            maker = UUID.randomUUID().toString(),
            makePrice = null,
            make = Asset(
                assetType = Asset.NFT(
                    contract = UUID.randomUUID().toString(),
                    tokenId = BigInteger.ONE
                ),
                assetValue = BigDecimal.ONE
            ),
            taker = null,
            take = Asset(
                assetType = Asset.XTZ(),
                assetValue = BigDecimal.ONE
            ),
            takePrice = null,
            cancelled = false,
            salt = BigInteger.ONE,
            legacyData = null
        )
    }
}
