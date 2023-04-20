package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.common.nowMillis
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionEthErc20AssetType
import com.rarible.protocol.union.core.model.UnionEthErc721AssetType
import com.rarible.protocol.union.core.model.UnionEthEthereumAssetType
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
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
import java.math.BigInteger

private const val ERC721 = "ERC721"

private const val ETH = "ETH"

private const val ERC20 = "ERC20"

object ImxOrderConverter {

    private val logger by Logger()

    //private val originFees = setOf("ecosystem", "protocol")
    //private val royalties = setOf("royalty")

    private val originFees = setOf("ecosystem", "protocol", "royalty")
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
        val make: AssetDto = toAssetLegacy(order, order.sell, blockchain)
        val take: AssetDto = toAssetLegacy(order, order.buy, blockchain)

        val (quantity, makePrice, takePrice) = if (make.type.ext.isNft) {
            Triple(getQuantityWithFees(order.buy), take.value, null)
        } else {
            Triple(getQuantityWithFees(order.sell), null, make.value)
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

    fun toAsset(order: ImmutablexOrder, side: ImmutablexOrderSide, blockchain: BlockchainDto): UnionAsset {
        // In the Asset we should specify price WITHOUT fees
        val totalFees = order.fees?.sumOf { it.amount }?.toBigInteger() ?: BigInteger.ZERO
        return when (side.type) {
            ERC721 -> {
                val tokenId = side.data.encodedTokenId() ?: throw ImxDataException("Token ID not specified in asset")
                val contract = ContractAddressConverter.convert(blockchain, side.data.tokenAddress!!)
                UnionAsset(UnionEthErc721AssetType(contract, tokenId), BigDecimal.ONE)
            }

            ETH -> {
                val type = UnionEthEthereumAssetType(blockchain)
                UnionAsset(type, normalizeQuantity(side, totalFees))
            }

            ERC20 -> {
                val contract = ContractAddressConverter.convert(blockchain, side.data.tokenAddress!!)
                val type = UnionEthErc20AssetType(contract)
                UnionAsset(type, normalizeQuantity(side, totalFees))
            }

            else -> throw IllegalStateException("Unsupported asset type: ${side.type}")
        }
    }

    private fun toAssetLegacy(order: ImmutablexOrder, side: ImmutablexOrderSide, blockchain: BlockchainDto): AssetDto {
        // In the Asset we should specify price WITHOUT fees
        val totalFees = order.fees?.sumOf { it.amount }?.toBigInteger() ?: BigInteger.ZERO
        return when (side.type) {
            ERC721 -> {
                val tokenId = side.data.encodedTokenId() ?: throw ImxDataException("Token ID not specified in asset")
                val contract = ContractAddressConverter.convert(blockchain, side.data.tokenAddress!!)
                val collectionId = CollectionIdDto(contract.blockchain, contract.value)
                AssetDto(EthErc721AssetTypeDto(contract, collectionId, tokenId), BigDecimal.ONE)
            }

            ETH -> {
                val type = EthEthereumAssetTypeDto(blockchain)
                AssetDto(type, normalizeQuantity(side, totalFees))
            }

            ERC20 -> {
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

    private fun getQuantityWithFees(side: ImmutablexOrderSide): BigInteger {
        return if (side.data.quantityWithFees.isNullOrBlank()) {
            if (side.type == ERC721 && side.data.quantity.isNullOrBlank()) {
                BigInteger.ONE
            } else {
                side.data.quantity?.toBigInteger() ?: throw ImxDataException("Quantity is not specified in Order")
            }
        } else {
            BigInteger(side.data.quantityWithFees)
        }
    }
}
