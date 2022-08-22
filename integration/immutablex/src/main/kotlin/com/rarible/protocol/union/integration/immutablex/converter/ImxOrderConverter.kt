package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.common.nowMillis
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
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
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrderData
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrderFee
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrderSide
import java.math.BigDecimal
import java.math.BigInteger

object ImxOrderConverter {

    private val logger by Logger()

    //private val originFees = setOf("ecosystem", "protocol")
    //private val royalties = setOf("royalty")

    private val originFees = setOf("ecosystem", "royalty")
    private val royalties = emptySet<String>() // TODO in IMX royalties works as originFees

    fun convert(order: ImmutablexOrder, blockchain: BlockchainDto): OrderDto {
        return try {
            convertInternal(order, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Order: {} \n{}", blockchain, e.message, order)
            throw e
        }
    }

    private fun convertInternal(order: ImmutablexOrder, blockchain: BlockchainDto): OrderDto {
        val make: AssetDto = toAsset(order, order.sell, blockchain)
        val take: AssetDto = toAsset(order, order.buy, blockchain)

        val (quantity, makePrice, takePrice) = if (make.type.ext.isNft) {
            Triple(getQuantityWithFees(order.buy.data), take.value, null)
        } else {
            Triple(getQuantityWithFees(order.sell.data), null, make.value)
        }

        val status = convertStatus(order)

        val fill = if (status == OrderStatusDto.FILLED) BigDecimal.ONE else BigDecimal.ZERO
        val makeStock = if (status == OrderStatusDto.FILLED) BigDecimal.ZERO else make.value
        return OrderDto(
            id = OrderIdDto(blockchain, "${order.orderId}"),
            make = make,
            take = take,
            maker = UnionAddressConverter.convert(blockchain, order.creator),
            taker = null,
            fill = fill,
            makePrice = makePrice,
            takePrice = takePrice,
            platform = PlatformDto.IMMUTABLEX,
            status = status,
            salt = "${order.orderId}",
            lastUpdatedAt = order.updatedAt ?: nowMillis(),
            createdAt = order.createdAt,
            cancelled = status == OrderStatusDto.CANCELLED,
            makeStock = makeStock,
            data = makeData(quantity, order, blockchain)
        )
    }

    private fun makeData(
        quantityWithFees: BigInteger,
        order: ImmutablexOrder,
        blockchain: BlockchainDto
    ): OrderDataDto {
        // In order there should be basic price, without considering fees
        val orderFees = order.fees ?: emptyList()

        val totalFees = orderFees.sumOf { it.amount }.toBigInteger()
        val quantity = quantityWithFees.minus(totalFees)

        val royalty = orderFees.filter { royalties.contains(it.type) }
            .map { toPayout(quantity, it, blockchain) }

        val fees = orderFees.filter { originFees.contains(it.type) }
            .map { toPayout(quantity, it, blockchain) }

        return ImmutablexOrderDataV1Dto(
            originFees = fees,
            payouts = royalty
        )
    }

    private fun toPayout(quantity: BigInteger, fee: ImmutablexOrderFee, blockchain: BlockchainDto): PayoutDto {
        // TODO not really sure this is the right way to round it
        val amountBp = fee.amount.multiply(BigDecimal.valueOf(10000L)).toBigInteger()
        return PayoutDto(
            account = UnionAddressConverter.convert(blockchain, fee.address),
            value = amountBp.divide(quantity).toInt()
        )
    }

    private fun convertStatus(order: ImmutablexOrder): OrderStatusDto = when (order.status) {
        "active" -> OrderStatusDto.ACTIVE
        "inactive" -> OrderStatusDto.INACTIVE
        "filled" -> OrderStatusDto.FILLED
        "cancelled" -> OrderStatusDto.CANCELLED
        else -> OrderStatusDto.HISTORICAL
    }

    fun toAsset(order: ImmutablexOrder, side: ImmutablexOrderSide, blockchain: BlockchainDto): AssetDto {
        // In the Asset we should specify price WITHOUT fees
        val totalFees = order.fees?.sumOf { it.amount }?.toBigInteger() ?: BigInteger.ZERO
        return when (side.type) {
            "ERC721" -> {
                val tokenId = side.data.encodedTokenId() ?: throw ImxDataException("Token ID not specified in asset")
                val contract = ContractAddressConverter.convert(blockchain, side.data.tokenAddress!!)
                AssetDto(EthErc721AssetTypeDto(contract, tokenId), BigDecimal.ONE)
            }
            "ETH" -> {
                val type = EthEthereumAssetTypeDto(blockchain)
                AssetDto(type, normalizeQuantity(side, totalFees))
            }
            "ERC20" -> {
                val contract = ContractAddressConverter.convert(blockchain, side.data.tokenAddress!!)
                val type = EthErc20AssetTypeDto(contract)
                AssetDto(type, normalizeQuantity(side, totalFees))
            }
            else -> throw IllegalStateException("Unsupported asset type: ${side.type}")
        }
    }

    private fun normalizeQuantity(side: ImmutablexOrderSide, totalFees: BigInteger): BigDecimal {
        val quantity = side.data.quantityWithFees
        val decimals = side.data.decimals
        if (quantity.isNullOrBlank()) {
            throw ImxDataException("Quantity is not specified in Order")
        } else {
            return quantity.toBigInteger().minus(totalFees).toBigDecimal(decimals)
        }
    }

    private fun getQuantityWithFees(data: ImmutablexOrderData): BigInteger {
        return if (data.quantityWithFees.isNullOrBlank()) {
            data.quantity?.toBigInteger() ?: throw ImxDataException("Quantity is not specified in Order")
        } else {
            BigInteger(data.quantityWithFees)
        }
    }
}
