package com.rarible.protocol.union.enrichment.util

import com.rarible.protocol.union.dto.EthOrderBasicSeaportDataV1Dto
import com.rarible.protocol.union.dto.EthOrderCryptoPunksDataDto
import com.rarible.protocol.union.dto.EthOrderDataLegacyDto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV1Dto
import com.rarible.protocol.union.dto.EthOrderOpenSeaV1DataV1Dto
import com.rarible.protocol.union.dto.FlowOrderDataV1Dto
import com.rarible.protocol.union.dto.ImmutablexOrderDataV1Dto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.SolanaAuctionHouseDataV1Dto
import com.rarible.protocol.union.dto.TezosOrderDataRaribleV2DataV1Dto
import com.rarible.protocol.union.dto.TezosOrderDataRaribleV2DataV2Dto
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
            is EthOrderOpenSeaV1DataV1Dto -> emptyList()
            is EthOrderBasicSeaportDataV1Dto -> emptyList()
            is EthOrderCryptoPunksDataDto -> emptyList()
            is TezosOrderDataRaribleV2DataV1Dto -> data.originFees.map { it.account.value }
            is TezosOrderDataRaribleV2DataV2Dto -> data.originFees.map { it.account.value }
            is FlowOrderDataV1Dto -> data.originFees.map { it.account.value }
            is SolanaAuctionHouseDataV1Dto -> listOfNotNull(data.auctionHouse?.value)
            is ImmutablexOrderDataV1Dto -> data.originFees.map { it.account.value }
        }.toSet()
    }
