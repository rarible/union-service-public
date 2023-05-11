package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadException
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.MetaDownloader
import com.rarible.protocol.union.enrichment.meta.content.ContentMetaDownloader
import com.rarible.protocol.union.enrichment.meta.downloader.Downloader
import org.springframework.stereotype.Component

@Component
class ItemMetaDownloader(
    private val router: BlockchainRouter<ItemService>,
    contentMetaLoader: ContentMetaDownloader,
    customizers: List<ItemMetaCustomizer>,
    metrics: ItemMetaMetrics
) : Downloader<UnionMeta>, MetaDownloader<ItemIdDto, UnionMeta>(
    contentMetaLoader,
    customizers,
    metrics,
    "Item"
) {

    override fun generaliseKey(key: ItemIdDto) = Pair(key.fullId(), key.blockchain)

    override suspend fun getRawMeta(key: ItemIdDto) = router.getService(key.blockchain).getItemMetaById(key.value)

    override suspend fun download(id: String): UnionMeta {
        val result = try {
            val itemId = IdParser.parseItemId(id)
            LogUtils.addToMdc(itemId, router) { load(itemId) }
        } catch (e: Exception) {
            throw DownloadException(e.message ?: "Unexpected exception")
        }
        result ?: throw DownloadException("No meta resolved for Item: $id")
        return result
    }
}
