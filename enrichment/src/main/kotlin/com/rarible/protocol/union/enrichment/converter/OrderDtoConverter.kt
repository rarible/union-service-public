package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.core.model.UnionOnChainAmmOrder
import com.rarible.protocol.union.core.model.UnionOnChainOrder
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.model.UnionPendingOrder
import com.rarible.protocol.union.core.model.UnionPendingOrderCancel
import com.rarible.protocol.union.core.model.UnionPendingOrderMatch
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.OnChainAmmOrderDto
import com.rarible.protocol.union.dto.OnChainOrderDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PendingOrderCancelDto
import com.rarible.protocol.union.dto.PendingOrderDto
import com.rarible.protocol.union.dto.PendingOrderMatchDto
import com.rarible.protocol.union.enrichment.converter.data.EnrichmentAssetData
import com.rarible.protocol.union.enrichment.converter.data.EnrichmentOrderData

object OrderDtoConverter {

    fun convert(source: UnionOrder, data: EnrichmentOrderData = EnrichmentOrderData.empty()): OrderDto {
        return OrderDto(
            id = source.id,
            fill = source.fill,
            platform = source.platform,
            status = convert(source.status),
            startedAt = source.startedAt,
            endedAt = source.endedAt,
            makeStock = source.makeStock,
            cancelled = source.cancelled,
            optionalRoyalties = source.optionalRoyalties,
            createdAt = source.createdAt,
            lastUpdatedAt = source.lastUpdatedAt,
            dbUpdatedAt = source.dbUpdatedAt,
            makePrice = source.makePrice,
            takePrice = source.takePrice,
            makePriceUsd = source.makePriceUsd,
            takePriceUsd = source.takePriceUsd,
            maker = source.maker,
            taker = source.taker,
            make = convert(source.id, source.make, data),
            take = convert(source.id, source.take, data),
            salt = source.salt,
            signature = source.signature,
            pending = source.pending?.map { convert(it, data) } ?: emptyList(),
            data = source.data,
        )
    }

    private fun convert(source: UnionOrder.Status): OrderStatusDto {
        return when (source) {
            UnionOrder.Status.ACTIVE -> OrderStatusDto.ACTIVE
            UnionOrder.Status.CANCELLED -> OrderStatusDto.CANCELLED
            UnionOrder.Status.FILLED -> OrderStatusDto.FILLED
            UnionOrder.Status.HISTORICAL -> OrderStatusDto.HISTORICAL
            UnionOrder.Status.INACTIVE -> OrderStatusDto.INACTIVE
        }
    }

    private fun convert(source: UnionPendingOrder, data: EnrichmentOrderData): PendingOrderDto {
        return when (source) {
            is UnionPendingOrderCancel -> convert(source, data)
            is UnionPendingOrderMatch -> convert(source, data)
            is UnionOnChainOrder -> convert(source, data)
            is UnionOnChainAmmOrder -> convert(source, data)
        }
    }

    private fun convert(source: UnionPendingOrderCancel, data: EnrichmentOrderData): PendingOrderCancelDto {
        return PendingOrderCancelDto(
            id = source.id,
            make = source.make?.let { convert(source.id, it, data) },
            take = source.take?.let { convert(source.id, it, data) },
            date = source.date,
            maker = source.maker,
            owner = source.owner,
        )
    }

    private fun convert(source: UnionPendingOrderMatch, data: EnrichmentOrderData): PendingOrderMatchDto {
        return PendingOrderMatchDto(
            id = source.id,
            make = source.make?.let { convert(source.id, it, data) },
            take = source.take?.let { convert(source.id, it, data) },
            date = source.date,
            maker = source.maker,
            side = source.side?.let { convert(it) },
            fill = source.fill,
            taker = source.taker,
            counterHash = source.counterHash,
            makeUsd = source.makeUsd,
            takeUsd = source.takeUsd,
            makePriceUsd = source.makePriceUsd,
            takePriceUsd = source.takePriceUsd,
        )
    }

    private fun convert(source: UnionOnChainOrder, data: EnrichmentOrderData): OnChainOrderDto {
        return OnChainOrderDto(
            id = source.id,
            make = source.make?.let { convert(source.id, it, data) },
            take = source.take?.let { convert(source.id, it, data) },
            date = source.date,
            maker = source.maker
        )
    }

    private fun convert(source: UnionOnChainAmmOrder, data: EnrichmentOrderData): OnChainAmmOrderDto {
        return OnChainAmmOrderDto(
            id = source.id,
            make = source.make?.let { convert(source.id, it, data) },
            take = source.take?.let { convert(source.id, it, data) },
            date = source.date,
            maker = source.maker
        )
    }

    private fun convert(source: UnionPendingOrderMatch.Side): PendingOrderMatchDto.Side {
        return when (source) {
            UnionPendingOrderMatch.Side.LEFT -> PendingOrderMatchDto.Side.LEFT
            UnionPendingOrderMatch.Side.RIGHT -> PendingOrderMatchDto.Side.RIGHT
        }
    }

    private fun convert(
        id: OrderIdDto,
        source: UnionAsset,
        data: EnrichmentOrderData
    ): AssetDto {
        return AssetDto(
            type = convert(id, source.type, data),
            value = source.value
        )
    }

    private fun convert(
        id: OrderIdDto,
        source: UnionAssetType,
        data: EnrichmentOrderData
    ): AssetTypeDto {
        return AssetDtoConverter.convert(source, EnrichmentAssetData(data.customCollections[id]))
    }

}