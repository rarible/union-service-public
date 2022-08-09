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
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrderFee
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrderSide
import java.math.BigDecimal

object ImmutablexOrderConverter {

    private val logger by Logger()

    private val originFees = setOf("ecosystem", "protocol")
    private val royalties = setOf("royalty")

    fun convert(order: ImmutablexOrder, blockchain: BlockchainDto): OrderDto {
        return try {
            convertInternal(order, blockchain)
        } catch (e: Exception) {
            logger.error("Unable to convert immutablex order: ${e.message}", e)
            throw e
        }
    }

    private fun convertInternal(order: ImmutablexOrder, blockchain: BlockchainDto): OrderDto {
        val make: AssetDto = toAsset(order.sell, blockchain)
        val take: AssetDto = toAsset(order.buy, blockchain)

        val makePrice = evalMakePrice(make, take)
        val takePrice = evalTakePrice(make, take)

        val quantity = if (make.type.ext.isNft) take.value else make.value

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
            data = makeData(quantity, order, blockchain)
        )
    }

    private fun makeData(quantity: BigDecimal, order: ImmutablexOrder, blockchain: BlockchainDto): OrderDataDto {
        val orderFees = order.fees ?: emptyList()

        val royalty = orderFees.filter { royalties.contains(it.type) }
            .map { toPayout(quantity, it, blockchain) }

        val fees = orderFees.filter { originFees.contains(it.type) }
            .map { toPayout(quantity, it, blockchain) }

        return ImmutablexOrderDataV1Dto(
            originFees = fees,
            payouts = royalty
        )
    }

    private fun toPayout(quantity: BigDecimal, fee: ImmutablexOrderFee, blockchain: BlockchainDto): PayoutDto {
        // TODO not really sure this is the right way to round it
        val amountBp = fee.amount.multiply(BigDecimal.valueOf(10000L)).toBigInteger()
        val quantityInt = quantity.toBigInteger()
        return PayoutDto(
            account = UnionAddressConverter.convert(blockchain, fee.address),
            value = amountBp.divide(quantityInt).toInt()
        )
    }

    private fun convertStatus(order: ImmutablexOrder): OrderStatusDto = when (order.status) {
        "active" -> OrderStatusDto.ACTIVE
        "inactive" -> OrderStatusDto.INACTIVE
        "filled" -> OrderStatusDto.FILLED
        "cancelled" -> OrderStatusDto.CANCELLED
        else -> OrderStatusDto.HISTORICAL
    }

    private fun toAsset(side: ImmutablexOrderSide, blockchain: BlockchainDto): AssetDto {
        val assetType = when (side.type) {
            "ERC721" -> EthErc721AssetTypeDto(
                tokenId = side.data.encodedTokenId(),
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
