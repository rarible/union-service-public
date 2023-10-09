package com.rarible.protocol.union.enrichment.converter

import com.rarible.marketplace.generated.whitelabelinternal.dto.MarketplaceDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.model.Marketplace
import com.rarible.protocol.union.enrichment.model.MetaDownloadPriority

object MarketplaceConverter {
    fun convert(marketplace: MarketplaceDto): Marketplace = Marketplace(
        id = marketplace.id,
        collectionIds = marketplace.collectionIds.map { EnrichmentCollectionId(IdParser.parseCollectionId(it)) }
            .toSet(),
        metaRefreshPriority = if ((marketplace.metaRefreshPriority ?: 0) > 0) {
            MetaDownloadPriority.RIGHT_NOW + (marketplace.metaRefreshPriority ?: 0)
        } else {
            null
        }
    )
}
