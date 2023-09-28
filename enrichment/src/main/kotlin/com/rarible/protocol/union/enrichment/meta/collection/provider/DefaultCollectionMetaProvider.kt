package com.rarible.protocol.union.enrichment.meta.collection.provider

import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaMetrics
import com.rarible.protocol.union.enrichment.meta.provider.DefaultMetaProvider
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class DefaultCollectionMetaProvider(
    private val router: BlockchainRouter<CollectionService>,
    metrics: ItemMetaMetrics
) : CollectionMetaProvider, DefaultMetaProvider<UnionCollectionMeta>(
    metrics,
    type = "collection"
) {
    override suspend fun fetch(blockchain: BlockchainDto, key: String): UnionCollectionMeta {
        return router.getService(blockchain).getCollectionMetaById(key)
    }
}
