package com.rarible.protocol.union.enrichment.meta.item.provider

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaMetrics
import com.rarible.protocol.union.enrichment.meta.provider.DefaultMetaProvider
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class DefaultItemMetaProvider(
    private val router: BlockchainRouter<ItemService>,
    metrics: ItemMetaMetrics
) : ItemMetaProvider, DefaultMetaProvider<UnionMeta>(
    metrics,
    type = "item"
) {
    override suspend fun fetch(blockchain: BlockchainDto, key: String): UnionMeta {
        return router.getService(blockchain).getItemMetaById(key)
    }
}
