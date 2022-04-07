package com.rarible.protocol.union.search.core.converter

import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.EthCollectionAssetTypeDto
import com.rarible.protocol.union.dto.EthCryptoPunksAssetTypeDto
import com.rarible.protocol.union.dto.EthErc1155AssetTypeDto
import com.rarible.protocol.union.dto.EthErc1155LazyAssetTypeDto
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721LazyAssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.EthGenerativeArtAssetTypeDto
import com.rarible.protocol.union.dto.EthOrderCryptoPunksDataDto
import com.rarible.protocol.union.dto.EthOrderDataLegacyDto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV1Dto
import com.rarible.protocol.union.dto.EthOrderOpenSeaV1DataV1Dto
import com.rarible.protocol.union.dto.FlowAssetTypeFtDto
import com.rarible.protocol.union.dto.FlowAssetTypeNftDto
import com.rarible.protocol.union.dto.FlowOrderDataV1Dto
import com.rarible.protocol.union.dto.OrderDataDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.dto.SolanaAuctionHouseDataV1Dto
import com.rarible.protocol.union.dto.SolanaFtAssetTypeDto
import com.rarible.protocol.union.dto.SolanaNftAssetTypeDto
import com.rarible.protocol.union.dto.SolanaSolAssetTypeDto
import com.rarible.protocol.union.dto.TezosFTAssetTypeDto
import com.rarible.protocol.union.dto.TezosMTAssetTypeDto
import com.rarible.protocol.union.dto.TezosNFTAssetTypeDto
import com.rarible.protocol.union.dto.TezosOrderDataRaribleV2DataV1Dto
import com.rarible.protocol.union.dto.TezosXTZAssetTypeDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.search.core.ElasticOrder
import org.springframework.stereotype.Service

@Service
class ElasticOrderConverter {

    fun convert(source: OrderEventDto): ElasticOrder {
        return when (source) {
            is OrderUpdateEventDto -> ElasticOrder(
                orderId = source.orderId.fullId(),
                lastUpdatedAt = source.order.lastUpdatedAt,
                type = orderType(source.order.make),
                blockchain = source.orderId.blockchain,
                platform = source.order.platform,
                maker = source.order.maker,
                make = asset(source.order.make),
                take = asset(source.order.take),
                taker = source.order.taker,
                start = source.order.startedAt,
                end = source.order.endedAt,
                origins = origins(source.order.data),
                status = source.order.status
            )
        }
    }

    fun origins(data: OrderDataDto): List<UnionAddress> {
        return when(data) {
            is EthOrderDataRaribleV2DataV1Dto -> data.payouts.map { it.account }
            is EthOrderOpenSeaV1DataV1Dto -> listOf(data.feeRecipient)
            is TezosOrderDataRaribleV2DataV1Dto -> data.originFees.map { it.account }
            is FlowOrderDataV1Dto -> data.originFees.map { it.account }
            is SolanaAuctionHouseDataV1Dto,
            is EthOrderDataLegacyDto,
            is EthOrderCryptoPunksDataDto -> emptyList()
            else -> emptyList()
        }
    }

    fun asset(assetDto: AssetDto): ElasticOrder.Asset {
        return ElasticOrder.Asset(
            type = assetDto.type
        )
    }

    fun orderType(assetDto: AssetDto): ElasticOrder.Type {
        val type = listOf(::ethType, ::flowType, ::tezosType, ::solanaType)
            .firstNotNullOfOrNull { it(assetDto) }
        return type ?: throw IllegalArgumentException("Unknown parameter $assetDto for order assetType conversion")
    }

    fun flowType(assetDto: AssetDto) = when (assetDto.type) {
        is FlowAssetTypeNftDto -> ElasticOrder.Type.SELL
        is FlowAssetTypeFtDto -> ElasticOrder.Type.BID
        else -> null
    }

    fun ethType(assetDto: AssetDto) = when (assetDto.type) {
        is EthErc721AssetTypeDto,
        is EthErc1155AssetTypeDto,
        is EthErc721LazyAssetTypeDto,
        is EthErc1155LazyAssetTypeDto,
        is EthCryptoPunksAssetTypeDto,
        is EthGenerativeArtAssetTypeDto,
        is EthCollectionAssetTypeDto -> ElasticOrder.Type.SELL
        is EthEthereumAssetTypeDto, is EthErc20AssetTypeDto -> ElasticOrder.Type.BID
        else -> null
    }

    fun tezosType(assetDto: AssetDto) = when (assetDto.type) {
        is TezosNFTAssetTypeDto,
        is TezosMTAssetTypeDto -> ElasticOrder.Type.SELL
        is TezosXTZAssetTypeDto, is TezosFTAssetTypeDto -> ElasticOrder.Type.BID
        else -> null
    }

    fun solanaType(assetDto: AssetDto) = when (assetDto.type) {
        is SolanaNftAssetTypeDto,
        is SolanaFtAssetTypeDto -> ElasticOrder.Type.SELL
        is SolanaSolAssetTypeDto -> ElasticOrder.Type.BID
        else -> null
    }
}
