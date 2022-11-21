package com.rarible.protocol.union.enrichment.util

import com.rarible.protocol.union.core.model.PoolItemAction
import com.rarible.protocol.union.dto.EthLooksRareOrderDataV1Dto
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
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderStatusDto
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
import com.rarible.protocol.union.dto.ext

// TODO move to openapi
val OrderDto.sellCurrencyId: String
    get() = take.type.ext.currencyAddress()

val OrderDto.bidCurrencyId: String
    get() = make.type.ext.currencyAddress()

val OrderDto.origins: Set<String>
    get() {
        val data = this.data
        return when (data) {
            is EthOrderDataLegacyDto -> emptyList()
            is EthOrderDataRaribleV2DataV1Dto -> data.originFees.map { it.account.value }
            is EthOrderDataRaribleV2DataV3SellDto -> setOfNotNull(data.originFeeFirst?.account?.value, data.originFeeSecond?.account?.value)
            is EthOrderDataRaribleV2DataV3BuyDto -> setOfNotNull(data.originFeeFirst?.account?.value, data.originFeeSecond?.account?.value)
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
            is EthSudoSwapAmmDataV1Dto -> emptyList()
        }.toSet()
    }

val OrderDto.isPoolOrder: Boolean
    get() {
        return when (this.data) {
            is EthSudoSwapAmmDataV1Dto -> true
            else -> false
        }
    }

fun OrderDto.setStatusByAction(action: PoolItemAction): OrderDto {
    return when (action) {
        PoolItemAction.INCLUDED, PoolItemAction.UPDATED -> this
        // If item excluded from the pool, we can consider this order as FILLED to recalculate actual best sell
        PoolItemAction.EXCLUDED -> this.copy(status = OrderStatusDto.FILLED)
    }
}
