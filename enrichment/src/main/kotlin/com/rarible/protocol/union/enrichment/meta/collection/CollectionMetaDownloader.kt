package com.rarible.protocol.union.enrichment.meta.collection

import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.download.DownloadException
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.MetaDownloader
import com.rarible.protocol.union.enrichment.meta.content.ContentMetaDownloader
import com.rarible.protocol.union.enrichment.meta.downloader.Downloader
import org.springframework.stereotype.Component

@Component
class CollectionMetaDownloader(
    private val router: BlockchainRouter<CollectionService>,
    unionContentMetaLoader: ContentMetaDownloader,
    metrics: CollectionMetaMetrics
) : Downloader<UnionCollectionMeta>, MetaDownloader<CollectionIdDto, UnionCollectionMeta>(
    unionContentMetaLoader,
    metrics,
    "Collection"
) {

    override fun generaliseKey(key: CollectionIdDto) = Pair(key.fullId(), key.blockchain)

    override suspend fun getRawMeta(key: CollectionIdDto) =
        router.getService(key.blockchain).getCollectionMetaById(key.value)

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
