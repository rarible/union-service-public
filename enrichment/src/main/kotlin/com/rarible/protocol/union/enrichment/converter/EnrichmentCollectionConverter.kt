package com.rarible.protocol.union.enrichment.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.enrichment.model.ShortCollection


object EnrichmentCollectionConverter {
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
            parent = collection.parent,
            meta = (meta ?: collection.meta)?.let { EnrichedMetaConverter.convert(it) },
            type = collection.type,
            bestSellOrder = shortCollection?.bestSellOrder?.let { orders[it.dtoId] },
            bestBidOrder = shortCollection?.bestBidOrder?.let { orders[it.dtoId] }
        )
    }

    fun convertToShortCollection(collection: UnionCollection): ShortCollection {
        return ShortCollection(
            blockchain = collection.id.blockchain,
            collectionId = collection.id.value,
            bestSellOrders = emptyMap(),
            bestBidOrders = emptyMap(),
            bestSellOrder = null,
            bestBidOrder = null,
            lastUpdatedAt = nowMillis()
        )
    }

}