package com.rarible.protocol.union.enrichment.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.enrichment.model.CollectionStatistics
import com.rarible.protocol.union.enrichment.model.ShortCollection

object EnrichedCollectionConverter {

    fun convert(
        collection: UnionCollection,
        shortCollection: ShortCollection? = null,
        meta: UnionCollectionMeta? = null,
        orders: Map<OrderIdDto, OrderDto> = emptyMap()
    ): CollectionDto {
        return CollectionDto(
            id = collection.id,
            blockchain = collection.id.blockchain,
            features = collection.features,
            owner = collection.owner,
            minters = collection.minters,
            name = collection.name,
            status = collection.status,
            symbol = collection.symbol,
            parent = collection.parent,
            meta = (meta ?: collection.meta)?.let { EnrichedMetaConverter.convert(it) },
            type = collection.type,
            statistics = shortCollection?.statistics?.let { CollectionStatisticsConverter.convert(it) },
            bestSellOrder = shortCollection?.bestSellOrder?.let { orders[it.dtoId] },
            bestBidOrder = shortCollection?.bestBidOrder?.let { orders[it.dtoId] },
            originOrders = shortCollection?.originOrders?.let { OriginOrdersConverter.convert(it, orders) }
                ?: emptyList()
        )
    }

    // TODO Refactor me. This method must be in test package
    fun convertToShortCollection(collection: UnionCollection, statistics: CollectionStatistics?): ShortCollection {
        return ShortCollection(
            blockchain = collection.id.blockchain,
            collectionId = collection.id.value,
            statistics = statistics,
            bestSellOrders = emptyMap(),
            bestBidOrders = emptyMap(),
            bestSellOrder = null,
            bestBidOrder = null,
            lastUpdatedAt = nowMillis()
        )
    }
}
