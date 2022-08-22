package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.ImmutablexOrderDataV1Dto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.integration.data.randomImxOrder
import com.rarible.protocol.union.integration.data.randomImxOrderBuySide
import com.rarible.protocol.union.integration.data.randomImxOrderFee
import com.rarible.protocol.union.integration.data.randomImxOrderSellSide
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

class ImxOrderConverterTest {

    @Test
    fun `convert - sell`() {
        val imxOrder = randomImxOrder()
        val payment = imxOrder.buy.data.quantity!!.toBigInteger().toBigDecimal()

        val order = ImxOrderConverter.convert(imxOrder, BlockchainDto.IMMUTABLEX)

        assertThat(order.id.value).isEqualTo(imxOrder.orderId.toString())

        assertThat(order.make.type).isInstanceOf(EthErc721AssetTypeDto::class.java)
        assertThat(order.make.value).isEqualTo(BigDecimal.ONE)

        // By default we don't have decimals in DTO
        assertThat(order.take.type).isInstanceOf(EthEthereumAssetTypeDto::class.java)
        assertThat(order.take.value).isEqualTo(payment)

        assertThat(order.maker.value).isEqualTo(imxOrder.creator)
        assertThat(order.taker).isNull()
        assertThat(order.makePrice).isEqualTo(payment)
        assertThat(order.takePrice).isNull()

        assertThat(order.platform).isEqualTo(PlatformDto.IMMUTABLEX)
        assertThat(order.status).isEqualTo(OrderStatusDto.ACTIVE)
        assertThat(order.salt).isEqualTo(imxOrder.orderId.toString())
        assertThat(order.lastUpdatedAt).isEqualTo(imxOrder.updatedAt)
        assertThat(order.createdAt).isEqualTo(imxOrder.createdAt)

        assertThat(order.makeStock).isEqualTo(BigDecimal.ONE)

        assertThat(order.data).isInstanceOf(ImmutablexOrderDataV1Dto::class.java)
    }

    @Test
    fun `convert - buy`() {
        val imxOrder = randomImxOrder(
            sell = randomImxOrderBuySide(),
            buy = randomImxOrderSellSide()
        )
        val payment = imxOrder.sell.data.quantity!!.toBigInteger().toBigDecimal()

        val order = ImxOrderConverter.convert(imxOrder, BlockchainDto.IMMUTABLEX)

        assertThat(order.id.value).isEqualTo(imxOrder.orderId.toString())

        // By default we don't have decimals in DTO
        assertThat(order.make.type).isInstanceOf(EthEthereumAssetTypeDto::class.java)
        assertThat(order.make.value).isEqualTo(payment)

        assertThat(order.take.type).isInstanceOf(EthErc721AssetTypeDto::class.java)
        assertThat(order.take.value).isEqualTo(BigDecimal.ONE)

        assertThat(order.maker.value).isEqualTo(imxOrder.creator)
        assertThat(order.taker).isNull()
        assertThat(order.takePrice).isEqualTo(payment)
        assertThat(order.makePrice).isNull()

        assertThat(order.makeStock).isEqualTo(payment)
    }

    @Test
    fun `convert data - sell`() {
        val imxOrder = randomImxOrder().copy(
            buy = randomImxOrderBuySide(quantity = BigInteger("100000"), quantityWithFees = BigInteger("105000")),
            fees = listOf(randomImxOrderFee(type = "royalty", amount = BigDecimal("5000")))
        )

        val converted = ImxOrderConverter.convert(imxOrder, BlockchainDto.IMMUTABLEX)
        val data = converted.data as ImmutablexOrderDataV1Dto

        assertThat(data.payouts[0].value).isEqualTo(500)
    }

    @Test
    fun `convert data - buy`() {
        val imxOrder = randomImxOrder().copy(
            sell = randomImxOrderBuySide(quantity = BigInteger("100000"), quantityWithFees = BigInteger("105000")),
            buy = randomImxOrderSellSide(),
            fees = listOf(randomImxOrderFee(type = "royalty", amount = BigDecimal("5000")))
        )

        val converted = ImxOrderConverter.convert(imxOrder, BlockchainDto.IMMUTABLEX)
        val data = converted.data as ImmutablexOrderDataV1Dto

        assertThat(data.payouts[0].value).isEqualTo(500)
    }

    @Test
    fun `convert asset - eth`() {
        val order = randomImxOrder()
        val side = randomImxOrderBuySide(
            quantityWithFees = BigInteger("1000"),
            decimals = 1
        )

        val asset = ImxOrderConverter.toAsset(order, side, BlockchainDto.IMMUTABLEX)

        assertThat(asset.type).isInstanceOf(EthEthereumAssetTypeDto::class.java)
        assertThat(asset.value).isEqualTo(BigDecimal("100.0"))
    }

    @Test
    fun `convert asset - erc20`() {
        val order = randomImxOrder()
        val side = randomImxOrderBuySide(
            quantityWithFees = BigInteger("1000"),
            decimals = 0,
            type = "ERC20"
        )

        val asset = ImxOrderConverter.toAsset(order, side, BlockchainDto.IMMUTABLEX)
        val type = asset.type as EthErc20AssetTypeDto

        assertThat(type.contract.value).isEqualTo(side.data.tokenAddress)
        assertThat(asset.value).isEqualTo(BigDecimal("1000"))
    }

    @Test
    fun `convert asset - erc721`() {
        val order = randomImxOrder()
        val side = randomImxOrderSellSide()

        val asset = ImxOrderConverter.toAsset(order, side, BlockchainDto.IMMUTABLEX)
        val type = asset.type as EthErc721AssetTypeDto

        assertThat(type.contract.value).isEqualTo(side.data.tokenAddress)
        assertThat(type.tokenId).isEqualTo(side.data.tokenId)
        assertThat(asset.value).isEqualTo(BigDecimal.ONE)
    }

    @Test
    fun `convert - fill and make stock - sell`() {
        val activeOrder = ImxOrderConverter.convert(randomImxOrder(), BlockchainDto.IMMUTABLEX)
        val filledOrder = ImxOrderConverter.convert(randomImxOrder(status = "filled"), BlockchainDto.IMMUTABLEX)

        assertThat(activeOrder.fill).isEqualTo(BigDecimal.ZERO)
        assertThat(activeOrder.makeStock).isEqualTo(BigDecimal.ONE)
        assertThat(filledOrder.fill).isEqualTo(BigDecimal.ONE)
        assertThat(filledOrder.makeStock).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `convert - fill and make stock - buy`() {
        val imxOrder = randomImxOrder(
            sell = randomImxOrderBuySide(),
            buy = randomImxOrderSellSide()
        )

        // Buy order can be filled only
        val filledOrder = ImxOrderConverter.convert(imxOrder.copy(status = "filled"), BlockchainDto.IMMUTABLEX)

        assertThat(filledOrder.fill).isEqualTo(BigDecimal.ONE)
        assertThat(filledOrder.makeStock).isEqualTo(BigDecimal.ZERO)
    }
}