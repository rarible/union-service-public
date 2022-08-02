package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.common.nowMillis
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.util.evalMakePrice
import com.rarible.protocol.union.core.util.evalTakePrice
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.ImmutablexOrderDataV1Dto
import com.rarible.protocol.union.dto.OrderDataDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PayoutDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexOrderSide
import org.slf4j.LoggerFactory
import java.math.BigDecimal

object ImmutablexOrderConverter {

    private val logger by Logger()

    fun convert(order: ImmutablexOrder, blockchain: BlockchainDto): OrderDto {
        return try {
            convertInternal(order, blockchain)
        } catch (e: Exception) {
            logger.error("Unable to convert immutablex order: ${e.message}", e)
            throw e
        }
    }

    private fun convertInternal(order: ImmutablexOrder, blockchain: BlockchainDto): OrderDto {
        val make: AssetDto = makeAsset(order.sell, blockchain)
        val take: AssetDto = makeAsset(order.buy, blockchain)

        val makePrice = evalMakePrice(make, take)
        val takePrice = evalTakePrice(make, take)

        val status = convertStatus(order)
        return OrderDto(
            id = OrderIdDto(blockchain, "${order.orderId}"),
            make = make,
            take = take,
            maker = UnionAddressConverter.convert(blockchain, order.creator),
            taker = null,
            makePrice = makePrice,
            takePrice = takePrice,
            fill = order.amountSold?.toBigDecimal() ?: BigDecimal.ZERO,
            platform = PlatformDto.IMMUTABLEX,
            status = status,
            salt = "${order.orderId}",
            lastUpdatedAt = order.updatedAt ?: nowMillis(),
            createdAt = order.createdAt,
            cancelled = status == OrderStatusDto.CANCELLED,
            makeStock = order.sell.data.quantity.toBigDecimal(),
            data = makeData(order, blockchain)
        )
    }

    private fun makeData(order: ImmutablexOrder, blockchain: BlockchainDto): OrderDataDto {
        if (order.fees.isNullOrEmpty()) {
            return ImmutablexOrderDataV1Dto(
                payouts = emptyList(),
                originFees = emptyList()
            )
        }
        val royalty = order.fees.filter {
            "royalty" == it.type
        }.map {
            PayoutDto(
                account = UnionAddressConverter.convert(blockchain, it.address),
                value = it.amount.divide(BigDecimal.TEN.pow(it.token.data.decimals)).toBps()
            )
        }

        val fees = order.fees.filter {
            "ecosystem" == it.type
        }.map {
            PayoutDto(
                account = UnionAddressConverter.convert(blockchain, it.address),
                value = it.amount.divide(BigDecimal.TEN.pow(it.token.data.decimals)).toBps()
            )
        }
        return ImmutablexOrderDataV1Dto(
            originFees = fees,
            payouts = royalty
        )
    }

    private fun convertStatus(order: ImmutablexOrder): OrderStatusDto = when (order.status) {
        "active" -> OrderStatusDto.ACTIVE
        "inactive" -> OrderStatusDto.INACTIVE
        "filled" -> OrderStatusDto.FILLED
        "cancelled" -> OrderStatusDto.CANCELLED
        else -> OrderStatusDto.HISTORICAL
    }

    private fun makeAsset(side: ImmutablexOrderSide, blockchain: BlockchainDto): AssetDto {
        val assetType = when (side.type) {
            "ERC721" -> EthErc721AssetTypeDto(
                // TODO could it be UUID instead of BigInteger?
                tokenId = side.data.tokenId!!.toBigInteger(),
                contract = ContractAddressConverter.convert(blockchain, side.data.tokenAddress!!)
            )
            "ETH" -> EthEthereumAssetTypeDto(
                blockchain = blockchain
            )
            "ERC20" -> EthErc20AssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, side.data.tokenAddress!!)
            )
            else -> throw IllegalStateException("Unsupported asset type: ${side.type}")
        }
        return AssetDto(type = assetType, value = side.data.quantity.toBigDecimal())
    }
}

private fun BigDecimal.toBps(): Int = try {
    this.multiply(BigDecimal.valueOf(10_000)).intValueExact()
} catch (e: Exception) {
    val logger = LoggerFactory.getLogger(ImmutablexOrderConverter::class.java)
    logger.warn("Unable convert BigDecimal to base-points $this")
    10_000
}
