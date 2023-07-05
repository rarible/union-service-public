package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.content.ContentMetaDownloader
import com.rarible.protocol.union.enrichment.meta.content.MetaContentEnrichmentService
import org.springframework.stereotype.Component

@Component
class ItemMetaContentEnrichmentService(
    contentMetaLoader: ContentMetaDownloader,
    customizers: List<ItemMetaCustomizer>,
) : MetaContentEnrichmentService<ItemIdDto, UnionMeta>(
    contentMetaLoader = contentMetaLoader,
    customizers = customizers
) {
    override fun generaliseKey(key: ItemIdDto): Pair<String, BlockchainDto> = Pair(key.fullId(), key.blockchain)
}