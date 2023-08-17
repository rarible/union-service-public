package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionEthErc20AssetType
import com.rarible.protocol.union.core.model.UnionEthErc721AssetType
import com.rarible.protocol.union.core.model.UnionEthEthereumAssetType
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ImmutablexOrderDataV1Dto
import com.rarible.protocol.union.dto.OrderDataDto
import com.rarible.protocol.union.dto.PayoutDto
import com.rarible.protocol.union.integration.immutablex.client.Fees
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrderSide
import java.math.BigDecimal
import java.math.BigInteger

class ImxOrderV3Converter : ImxOrderConverter() {

    override fun toAsset(order: ImmutablexOrder, side: ImmutablexOrderSide, blockchain: BlockchainDto): UnionAsset {
        return when (side.type) {
            ERC721 -> {
                val tokenId = side.data.encodedTokenId() ?: throw ImxDataException("Token ID not specified in asset")
                val contract = ContractAddressConverter.convert(blockchain, side.data.tokenAddress!!)
                UnionAsset(UnionEthErc721AssetType(contract, tokenId), BigDecimal.ONE)
            }

            ETH -> {
                val type = UnionEthEthereumAssetType(blockchain)
                UnionAsset(type, normalizeQuantity(side, BigInteger.ZERO))
            }

            ERC20 -> {
                val contract = ContractAddressConverter.convert(blockchain, side.data.tokenAddress!!)
                val type = UnionEthErc20AssetType(contract)
                UnionAsset(type, normalizeQuantity(side, BigInteger.ZERO))
            }

            else -> throw IllegalStateException("Unsupported asset type: ${side.type}")
        }
    }

    override fun makeData(
        quantityWithFee: BigInteger,
        order: ImmutablexOrder,
        blockchain: BlockchainDto
    ): OrderDataDto {
        val make = toAsset(order, order.sell, blockchain)
        val (orderFees, quantity, payout) = when (make.type.isNft()) {
            true -> Triple(order.takerFees, order.buy.data.quantity, order.makerFees)
            else -> Triple(order.makerFees, order.sell.data.quantity, order.takerFees)
        }

        val fees = orderFees?.fees?.filter { originFees.contains(it.type) }
            ?.map { toPayout(quantity!!.toBigInteger(), it, blockchain) } ?: emptyList()
        val payouts = when {
            payout?.fees?.isNotEmpty() == true -> payouts(payout, quantity, blockchain, order)
            else -> emptyList()
        }

        return ImmutablexOrderDataV1Dto(
            originFees = fees,
            payouts = payouts
        )
    }

    fun payouts(payout: Fees?, quantity: String?, blockchain: BlockchainDto, order: ImmutablexOrder): List<PayoutDto> {
        val fees = payout!!.fees.map { toPayout(quantity!!.toBigInteger(), it, blockchain) }
        return listOf(
            PayoutDto(
                account = UnionAddressConverter.convert(blockchain, order.creator),
                value = 10000 - fees?.sumOf { it.value }
            )
        ) + fees
    }

    override fun getQuantityWithFees(side: ImmutablexOrderSide, fees: Fees?): BigInteger {
        return when {
            fees != null -> {
                if (side.type == ERC721 && side.data.quantity.isNullOrBlank()) {
                    BigInteger.ONE
                } else {
                    val fees = fees?.fees?.sumOf { it.amount }
                    val quantity = side.data.quantity?.toBigInteger() ?: throw ImxDataException("Quantity is not specified in Order")
                    quantity.toBigDecimal().add(fees).toBigInteger()
                }
            }
            else -> BigInteger(side.data.quantity)
        }
    }


    override fun normalizeQuantity(side: ImmutablexOrderSide, totalFees: BigInteger): BigDecimal {
        val quantity = side.data.quantity
        val decimals = side.data.decimals
        if (quantity.isNullOrBlank()) {
            throw ImxDataException("Quantity is not specified in Order")
        } else {
            return quantity.toBigInteger().toBigDecimal(decimals)
        }
    }
}
