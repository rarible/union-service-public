package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionEthErc20AssetType
import com.rarible.protocol.union.core.model.UnionEthErc721AssetType
import com.rarible.protocol.union.core.model.UnionEthEthereumAssetType
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.ImmutablexOrderDataV1Dto
import com.rarible.protocol.union.dto.OrderDataDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.PayoutDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.integration.immutablex.client.Fees
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrderFee
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrderSide
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.BigInteger


open class ImxOrderConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    // private val originFees = setOf("ecosystem", "protocol")
    // private val royalties = setOf("royalty")

    fun convert(order: ImmutablexOrder, blockchain: BlockchainDto): UnionOrder {
        return try {
            convertInternal(order, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Order: {} \n{}", blockchain, e.message, order)
            throw e
        }
    }

    open fun convertInternal(order: ImmutablexOrder, blockchain: BlockchainDto): UnionOrder {
        val make = toAsset(order, order.sell, blockchain)
        val take = toAsset(order, order.buy, blockchain)

        val (quantity, makePrice, takePrice) = if (make.type.isNft()) {
            Triple(getQuantityWithFees(order.buy, order.takerFees), take.value, null)
        } else {
            Triple(getQuantityWithFees(order.sell, order.makerFees), null, make.value)
        }

        val status = convertStatus(order)

        val fill = if (status == UnionOrder.Status.FILLED) BigDecimal.ONE else BigDecimal.ZERO
        val makeStock = if (status == UnionOrder.Status.FILLED) BigDecimal.ZERO else make.value
        return UnionOrder(
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
            cancelled = status == UnionOrder.Status.CANCELLED,
            makeStock = makeStock,
            data = makeData(quantity, order, blockchain)
        )
    }

    open fun makeData(
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

    fun toPayout(quantity: BigInteger, fee: ImmutablexOrderFee, blockchain: BlockchainDto): PayoutDto {
        // TODO not really sure this is the right way to round it
        val amountBp = fee.amount.multiply(BigDecimal.valueOf(10000L)).toBigInteger()
        return PayoutDto(
            account = UnionAddressConverter.convert(blockchain, fee.address),
            value = amountBp.divide(quantity).toInt()
        )
    }

    private fun convertStatus(order: ImmutablexOrder): UnionOrder.Status = when (order.status) {
        "active" -> UnionOrder.Status.ACTIVE
        "inactive" -> UnionOrder.Status.INACTIVE
        "filled" -> UnionOrder.Status.FILLED
        "cancelled" -> UnionOrder.Status.CANCELLED
        else -> UnionOrder.Status.HISTORICAL
    }

    open fun toAsset(order: ImmutablexOrder, side: ImmutablexOrderSide, blockchain: BlockchainDto): UnionAsset {
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

    open fun normalizeQuantity(side: ImmutablexOrderSide, totalFees: BigInteger): BigDecimal {
        val quantity = side.data.quantityWithFees
        val decimals = side.data.decimals
        if (quantity.isNullOrBlank()) {
            throw ImxDataException("Quantity is not specified in Order")
        } else {
            return quantity.toBigInteger().minus(totalFees).toBigDecimal(decimals)
        }
    }

    open fun getQuantityWithFees(side: ImmutablexOrderSide, fees: Fees?): BigInteger {
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

    companion object {
        const val ERC721 = "ERC721"
        const val ETH = "ETH"
        const val ERC20 = "ERC20"

        val originFees = setOf("ecosystem", "protocol", "royalty", "taker", "maker")
        val royalties = emptySet<String>() // TODO in IMX royalties works as originFees
    }
}
