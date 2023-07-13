package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadException
import com.rarible.protocol.union.core.model.download.PartialDownloadException
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.MetaDownloader
import com.rarible.protocol.union.enrichment.meta.downloader.Downloader
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemMetaDownloader(
    private val router: BlockchainRouter<ItemService>,
    metaContentEnrichmentService: ItemMetaContentEnrichmentService,
    providers: List<ItemMetaProvider>,
    metrics: ItemMetaMetrics
) : Downloader<UnionMeta>, MetaDownloader<ItemIdDto, UnionMeta>(
    metaContentEnrichmentService = metaContentEnrichmentService,
    providers = providers,
    metrics = metrics,
    type = "Item"
) {

    override suspend fun getRawMeta(key: ItemIdDto) = router.getService(key.blockchain).getItemMetaById(key.value)

    override suspend fun download(id: String): UnionMeta {
        val result = try {
            val itemId = IdParser.parseItemId(id)
            LogUtils.addToMdc(itemId, router) { load(itemId) }
        } catch (e: PartialDownloadException) {
            throw e
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw DownloadException(e.message ?: "Unexpected exception")
        }
        result ?: throw DownloadException("No meta resolved for Item: $id")
        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ItemMetaDownloader::class.java)
    }
}
