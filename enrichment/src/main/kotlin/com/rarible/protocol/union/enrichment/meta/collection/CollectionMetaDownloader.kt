package com.rarible.protocol.union.enrichment.meta.collection

import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.download.DownloadException
import com.rarible.protocol.union.enrichment.meta.MetaDownloader
import com.rarible.protocol.union.enrichment.meta.collection.provider.CollectionMetaCustomProvider
import com.rarible.protocol.union.enrichment.meta.collection.provider.CollectionMetaProvider
import com.rarible.protocol.union.enrichment.meta.downloader.Downloader
import org.springframework.stereotype.Component

@Component
class CollectionMetaDownloader(
    collectionMetaContentEnrichmentService: CollectionMetaContentEnrichmentService,
    providers: List<CollectionMetaProvider>,
    customProviders: List<CollectionMetaCustomProvider>,
    metrics: CollectionMetaMetrics
) : Downloader<UnionCollectionMeta>, MetaDownloader<CollectionIdDto, UnionCollectionMeta>(
    metaContentEnrichmentService = collectionMetaContentEnrichmentService,
    providers = providers,
    customProviders = customProviders,
    metrics = metrics,
    type = "Collection"
) {

    override suspend fun download(id: String): UnionCollectionMeta {
        val result = try {
            val collectionId = IdParser.parseCollectionId(id)
            LogUtils.addToMdc(collectionId) {
                load(collectionId)
            }
        } catch (e: Exception) {
            throw DownloadException(e.message ?: "Unexpected exception")
        }
        result ?: throw DownloadException("No meta resolved for Collection: $id")
        return result
    }
}
