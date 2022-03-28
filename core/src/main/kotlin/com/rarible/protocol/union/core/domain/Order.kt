package com.rarible.protocol.union.core.domain

import com.rarible.domain.AssetType.ETH_COLLECTION
import com.rarible.domain.constant.OrderStatus
import com.rarible.domain.constant.PlatformType
import com.rarible.marketplace.core.common.util.divide128
import com.rarible.marketplace.core.model.BlockchainAddress
import java.math.BigDecimal
import java.util.*

data class Order(
    val id: String,

    val salt: String,

    val maker: BlockchainAddress,
    val taker: BlockchainAddress?,

    val make: Asset,
    val makeCurrency: Currency?,

    val take: Asset,
    val takeCurrency: Currency?,

    val sellPrice: BigDecimal = take.value.divide128(make.value),
    val makePriceUsd: BigDecimal? = null,
    val sellPriceEth: BigDecimal? = takeCurrency?.toEth(sellPrice),

    val buyPrice: BigDecimal = make.value.divide128(take.value),
    val takePriceUsd: BigDecimal? = null,
    val buyPriceEth: BigDecimal? = makeCurrency?.toEth(buyPrice),

    val makeStock: BigDecimal,
    val fill: BigDecimal,
    val sold: BigDecimal = BigDecimal.ZERO,

    val platform: PlatformType? = null,
    val type: OrderType,
    val data: OrderDataDTO,

    val signature: String?,

    val createdAt: Date,
    val lastUpdateAt: Date,

    /**
     * Статус.
     *
     * Информация берётся напрямую из протокола. **Будьте аккуратны при использовании**, т.к. информация может
     * расходиться с флагами ордера [cancelled], [isActive], [isCompleted].
     */
    val status: OrderStatus? = null,
    val cancelled: Boolean
) {
    val makeToken: BlockchainAddress
        get() = make.token

    val makeTokenId: String
        get() = make.tokenId

    val takeToken: BlockchainAddress
        get() = take.token

    val takeTokenId: String
        get() = take.tokenId

    val itemId: String
        get() {
            return if (isOffer) {
                Item.getId(takeToken, takeTokenId)
            } else {
                Item.getId(makeToken, makeTokenId)
            }
        }

    val isOffer: Boolean
        get() {
            return take.assetType.nft
        }

    val isSellOrder: Boolean
        get() {
            return make.assetType.nft
        }

    val isCompleted: Boolean
        get() = make.value == sold

    val isActive: Boolean
        get() = makeStock > BigDecimal.ZERO && !cancelled && !isCompleted

    val isFloorBid: Boolean
        get() = take.isFloorBid()
}

fun Asset.isFloorBid(): Boolean = this.assetType == ETH_COLLECTION
