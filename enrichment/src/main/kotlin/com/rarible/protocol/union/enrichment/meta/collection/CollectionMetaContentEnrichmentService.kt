package com.rarible.protocol.union.enrichment.meta.collection

import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.meta.content.ContentMetaDownloader
import com.rarible.protocol.union.enrichment.meta.content.MetaContentEnrichmentService
import org.springframework.stereotype.Service

@Service
class CollectionMetaContentEnrichmentService(
    contentMetaLoader: ContentMetaDownloader,
    customizers: List<CollectionMetaCustomizer>,
) : MetaContentEnrichmentService<CollectionIdDto, UnionCollectionMeta>(
    contentMetaLoader = contentMetaLoader,
    customizers = customizers
) {
    override fun extractBlockchain(key: CollectionIdDto): Pair<String, BlockchainDto> =
        Pair(key.fullId(), key.blockchain)
}
