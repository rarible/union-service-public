package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.EthLooksRareOrderDataV1Dto
import com.rarible.protocol.union.dto.EthLooksRareOrderDataV2Dto
import com.rarible.protocol.union.dto.EthOrderBasicSeaportDataV1Dto
import com.rarible.protocol.union.dto.EthOrderCryptoPunksDataDto
import com.rarible.protocol.union.dto.EthOrderDataLegacyDto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV1Dto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV3BuyDto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV3SellDto
import com.rarible.protocol.union.dto.EthOrderOpenSeaV1DataV1Dto
import com.rarible.protocol.union.dto.EthSudoSwapAmmDataV1Dto
import com.rarible.protocol.union.dto.EthX2Y2OrderDataV1Dto
import com.rarible.protocol.union.dto.FlowOrderDataV1Dto
import com.rarible.protocol.union.dto.ImmutablexOrderDataV1Dto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderDataDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SolanaAuctionHouseDataV1Dto
import com.rarible.protocol.union.dto.TezosOrderDataFxhashV1Dto
import com.rarible.protocol.union.dto.TezosOrderDataFxhashV2Dto
import com.rarible.protocol.union.dto.TezosOrderDataHenDto
import com.rarible.protocol.union.dto.TezosOrderDataLegacyDto
import com.rarible.protocol.union.dto.TezosOrderDataObjktV1Dto
import com.rarible.protocol.union.dto.TezosOrderDataObjktV2Dto
import com.rarible.protocol.union.dto.TezosOrderDataRaribleV2DataV2Dto
import com.rarible.protocol.union.dto.TezosOrderDataTeiaV1Dto
import com.rarible.protocol.union.dto.TezosOrderDataV2Dto
import com.rarible.protocol.union.dto.TezosOrderDataVersumV1Dto
import com.rarible.protocol.union.dto.UnionAddress
import java.math.BigDecimal
import java.time.Instant

data class UnionOrder(
    val id: OrderIdDto,
    val fill: BigDecimal,
    val platform: PlatformDto,
    val status: Status,
    val startedAt: Instant? = null,
    val endedAt: Instant? = null,
    val makeStock: BigDecimal,
    val cancelled: Boolean,
    val optionalRoyalties: Boolean? = false,
    val createdAt: Instant,
    val lastUpdatedAt: Instant,
    val dbUpdatedAt: Instant? = null,
    val makePrice: BigDecimal? = null,
    val takePrice: BigDecimal? = null,
    val makePriceUsd: BigDecimal? = null,
    val takePriceUsd: BigDecimal? = null,
    val maker: UnionAddress,
    val taker: UnionAddress? = null,
    val make: UnionAsset,
    val take: UnionAsset,
    val salt: String,
    val signature: String? = null,
    val pending: List<UnionPendingOrder>? = listOf(),
    val data: OrderDataDto // TODO I hope we will never need to enrich THIS
) {

    enum class Status {
        ACTIVE,
        FILLED,
        HISTORICAL,
        INACTIVE,
        CANCELLED
    }

    fun itemId(): ItemIdDto? = when {
        (make.type.isNft() && !make.type.isCollectionAsset()) -> make.type.itemId()
        (take.type.isNft() && !take.type.isCollectionAsset()) -> take.type.itemId()
        else -> null
    }

    fun assetCollectionId(): CollectionIdDto? = when {
        (make.type.isCollectionAsset()) -> make.type.collectionId()
        (take.type.isCollectionAsset()) -> take.type.collectionId()
        else -> null
    }

    fun sellCurrencyId(): String {
        if (!take.type.isCurrency()) {
            throw IllegalArgumentException("Not a currency AssetType: $this")
        }
        return take.type.currencyId()!!
    }

    fun bidCurrencyId(): String {
        if (!make.type.isCurrency()) {
            throw IllegalArgumentException("Not a currency AssetType: $this")
        }
        return make.type.currencyId()!!
    }

    fun origins(): Set<String> {
        val data = this.data
        return when (data) {
            is EthOrderDataLegacyDto -> emptyList()
            is EthOrderDataRaribleV2DataV1Dto -> data.originFees.map { it.account.value }
            is EthOrderDataRaribleV2DataV3SellDto -> setOfNotNull(
                data.originFeeFirst?.account?.value,
                data.originFeeSecond?.account?.value
            )

            is EthOrderDataRaribleV2DataV3BuyDto -> setOfNotNull(
                data.originFeeFirst?.account?.value,
                data.originFeeSecond?.account?.value
            )

            is EthOrderOpenSeaV1DataV1Dto -> emptyList()
            is EthOrderBasicSeaportDataV1Dto -> emptyList()
            is EthOrderCryptoPunksDataDto -> emptyList()
            is TezosOrderDataLegacyDto -> data.originFees.map { it.account.value }
            is TezosOrderDataRaribleV2DataV2Dto,
            is TezosOrderDataHenDto,
            is TezosOrderDataVersumV1Dto,
            is TezosOrderDataTeiaV1Dto,
            is TezosOrderDataObjktV1Dto, is TezosOrderDataObjktV2Dto,
            is TezosOrderDataFxhashV1Dto, is TezosOrderDataFxhashV2Dto -> {
                val dto = data as TezosOrderDataV2Dto
                dto.originFees.map { it.account.value }
            }

            is FlowOrderDataV1Dto -> data.originFees.map { it.account.value }
            is SolanaAuctionHouseDataV1Dto -> listOfNotNull(data.auctionHouse?.value)
            is ImmutablexOrderDataV1Dto -> data.originFees.map { it.account.value }
            is EthX2Y2OrderDataV1Dto -> emptyList()
            is EthLooksRareOrderDataV1Dto -> emptyList()
            is EthLooksRareOrderDataV2Dto -> emptyList()
            is EthSudoSwapAmmDataV1Dto -> emptyList()
        }.toSet()
    }

    fun isPoolOrder(): Boolean {
        return when (this.data) {
            is EthSudoSwapAmmDataV1Dto -> true
            else -> false
        }
    }

    fun applyStatusByAction(action: PoolItemAction): UnionOrder {
        return when (action) {
            PoolItemAction.INCLUDED, PoolItemAction.UPDATED -> this
            // If item excluded from the pool, we can consider this order as FILLED to recalculate actual best sell
            PoolItemAction.EXCLUDED -> this.copy(status = UnionOrder.Status.FILLED)
        }
    }
}