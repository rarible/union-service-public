package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.protocol.union.core.model.UnionEthErc20AssetType
import com.rarible.protocol.union.core.model.UnionEthErc721AssetType
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.ImmutablexOrderDataV1Dto
import com.rarible.protocol.union.dto.PayoutDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.integration.data.randomImxOrder
import com.rarible.protocol.union.integration.data.randomImxOrderBuySide
import com.rarible.protocol.union.integration.data.randomImxOrderSellSide
import com.rarible.protocol.union.integration.immutablex.client.FeeToken
import com.rarible.protocol.union.integration.immutablex.client.FeeTokenData
import com.rarible.protocol.union.integration.immutablex.client.Fees
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrderFee
import com.rarible.protocol.union.integration.immutablex.client.MakerTakerType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

class ImxOrderConverterV3Test {

    private val imxOrderConverter = ImxOrderV3Converter()

    @Test
    fun `convert - sell`() {
        val imxOrder = randomImxOrder()
        val payment = imxOrder.buy.data.quantity!!.toBigInteger().toBigDecimal()

        val order = imxOrderConverter.convert(imxOrder, BlockchainDto.IMMUTABLEX)

        assertThat(order.id.value).isEqualTo(imxOrder.orderId.toString())

        assertThat(order.make.type).isInstanceOf(UnionEthErc721AssetType::class.java)
        assertThat(order.make.value).isEqualTo(BigDecimal.ONE)

        assertThat(order.take.type).isInstanceOf(UnionEthErc20AssetType::class.java)
        assertThat(order.take.value).isEqualTo(payment)

        assertThat(order.maker.value).isEqualTo(imxOrder.creator)
        assertThat(order.taker).isNull()
        assertThat(order.makePrice).isEqualTo(payment)
        assertThat(order.takePrice).isNull()

        assertThat(order.platform).isEqualTo(PlatformDto.IMMUTABLEX)
        assertThat(order.status).isEqualTo(UnionOrder.Status.ACTIVE)
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

        val order = imxOrderConverter.convert(imxOrder, BlockchainDto.IMMUTABLEX)

        assertThat(order.id.value).isEqualTo(imxOrder.orderId.toString())

        // By default we don't have decimals in DTO
        assertThat(order.make.type).isInstanceOf(UnionEthErc20AssetType::class.java)
        assertThat(order.make.value).isEqualTo(payment)

        assertThat(order.take.type).isInstanceOf(UnionEthErc721AssetType::class.java)
        assertThat(order.take.value).isEqualTo(BigDecimal.ONE)

        assertThat(order.maker.value).isEqualTo(imxOrder.creator)
        assertThat(order.taker).isNull()
        assertThat(order.takePrice).isEqualTo(payment)
        assertThat(order.makePrice).isNull()

        assertThat(order.makeStock).isEqualTo(payment)
    }

    // Implemented test for listing from this example:
    // https://docs.immutable.com/docs/x/maker-taker-fees/#new-maker-taker-fields-and-the-deprecation-of-quantity_with_fees-in-buy-and-sell
    @Test
    fun `convert data - sell`() {
        val imxOrder = randomImxOrder().copy(
            buy = randomImxOrderBuySide(quantity = BigInteger("500000000000000000")),
            makerFees = Fees(
                quantityWithFees = "495000000000000000",
                tokenType = "ETH",
                decimals = 18,
                symbol = "ETH",
                fees = listOf(
                    fee(
                        type = "maker",
                        address = "0x1111",
                        amount = "5000000000000000"
                    )
                )
            ),
            takerFees = Fees(
                quantityWithFees = "520000000000000000",
                tokenType = "ETH",
                decimals = 18,
                symbol = "ETH",
                fees = listOf(
                    fee(
                        type = "taker",
                        address = "0x22221",
                        amount = "5000000000000000"
                    ),
                    fee(
                        type = "royalty",
                        address = "0x22222",
                        amount = "5000000000000000"
                    ),
                    fee(
                        type = "protocol",
                        address = "0x22223",
                        amount = "10000000000000000"
                    )
                )
            ),
            makerTakerType = MakerTakerType.MAKER
        )

        val converted = imxOrderConverter.convert(imxOrder, BlockchainDto.IMMUTABLEX)
        val data = converted.data as ImmutablexOrderDataV1Dto

        assertThat(data.originFees).isEqualTo(
            listOf(
                PayoutDto(UnionAddress(BlockchainGroupDto.ETHEREUM, "0x22221"), 100),
                PayoutDto(UnionAddress(BlockchainGroupDto.ETHEREUM, "0x22222"), 100),
                PayoutDto(UnionAddress(BlockchainGroupDto.ETHEREUM, "0x22223"), 200)
            )
        )

        assertThat(data.payouts).isEqualTo(
            listOf(
                PayoutDto(UnionAddress(BlockchainGroupDto.ETHEREUM, imxOrder.creator), 9900),
                PayoutDto(UnionAddress(BlockchainGroupDto.ETHEREUM, "0x1111"), 100),
            )
        )
    }

    @Test
    fun `convert data with empty maker fee - sell`() {
        val imxOrder = randomImxOrder().copy(
            buy = randomImxOrderBuySide(quantity = BigInteger("500000000000000000")),
            makerFees = Fees(
                quantityWithFees = "500000000000000000",
                tokenType = "ETH",
                decimals = 18,
                symbol = "ETH",
                fees = listOf()
            ),
            takerFees = Fees(
                quantityWithFees = "520000000000000000",
                tokenType = "ETH",
                decimals = 18,
                symbol = "ETH",
                fees = listOf(
                    fee(
                        type = "taker",
                        address = "0x22221",
                        amount = "5000000000000000"
                    ),
                    fee(
                        type = "royalty",
                        address = "0x22222",
                        amount = "5000000000000000"
                    ),
                    fee(
                        type = "protocol",
                        address = "0x22223",
                        amount = "10000000000000000"
                    )
                )
            ),
            makerTakerType = MakerTakerType.MAKER
        )

        val converted = imxOrderConverter.convert(imxOrder, BlockchainDto.IMMUTABLEX)
        val data = converted.data as ImmutablexOrderDataV1Dto

        assertThat(data.originFees).isEqualTo(
            listOf(
                PayoutDto(UnionAddress(BlockchainGroupDto.ETHEREUM, "0x22221"), 100),
                PayoutDto(UnionAddress(BlockchainGroupDto.ETHEREUM, "0x22222"), 100),
                PayoutDto(UnionAddress(BlockchainGroupDto.ETHEREUM, "0x22223"), 200)
            )
        )

        assertThat(data.payouts).isEmpty()
    }

    // Implemented test for bid from this example:
    // https://docs.immutable.com/docs/x/maker-taker-fees/#new-maker-taker-fields-and-the-deprecation-of-quantity_with_fees-in-buy-and-sell
    @Test
    fun `convert data - buy`() {
        val imxOrder = randomImxOrder().copy(
            sell = randomImxOrderBuySide(quantity = BigInteger("500000000000000000")),
            buy = randomImxOrderSellSide(),
            takerFees = Fees(
                quantityWithFees = "495000000000000000",
                tokenType = "ETH",
                decimals = 18,
                symbol = "ETH",
                fees = listOf(
                    fee(
                        type = "maker",
                        address = "0x1111",
                        amount = "5000000000000000"
                    )
                )
            ),
            makerFees = Fees(
                quantityWithFees = "520000000000000000",
                tokenType = "ETH",
                decimals = 18,
                symbol = "ETH",
                fees = listOf(
                    fee(
                        type = "taker",
                        address = "0x22221",
                        amount = "5000000000000000"
                    ),
                    fee(
                        type = "royalty",
                        address = "0x22222",
                        amount = "5000000000000000"
                    ),
                    fee(
                        type = "protocol",
                        address = "0x22223",
                        amount = "10000000000000000"
                    )
                )
            ),
            makerTakerType = MakerTakerType.MAKER
        )

        val converted = imxOrderConverter.convert(imxOrder, BlockchainDto.IMMUTABLEX)
        val data = converted.data as ImmutablexOrderDataV1Dto

        assertThat(data.originFees).isEqualTo(
            listOf(
                PayoutDto(UnionAddress(BlockchainGroupDto.ETHEREUM, "0x22221"), 100),
                PayoutDto(UnionAddress(BlockchainGroupDto.ETHEREUM, "0x22222"), 100),
                PayoutDto(UnionAddress(BlockchainGroupDto.ETHEREUM, "0x22223"), 200)
            )
        )

        assertThat(data.payouts).isEqualTo(
            listOf(
                PayoutDto(UnionAddress(BlockchainGroupDto.ETHEREUM, imxOrder.creator), 9900),
                PayoutDto(UnionAddress(BlockchainGroupDto.ETHEREUM, "0x1111"), 100),
            )
        )
    }

    private fun fee(type: String, address: String, amount: String) = ImmutablexOrderFee(
        type = type,
        address = address,
        amount = BigDecimal(amount),
        token = FeeToken(
            type = "ETH",
            data = FeeTokenData(
                decimals = 18
            )
        )
    )
}
