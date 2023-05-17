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
import com.rarible.protocol.union.enrichment.service.SimpleHashService
import org.springframework.stereotype.Component

@Component
class ItemMetaDownloader(
    private val router: BlockchainRouter<ItemService>,
    private val simpleHashService: SimpleHashService,
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

    override fun isSimpleHashSupported(key: ItemIdDto) = simpleHashService.isSupported(key.blockchain)
    override suspend fun getSimpleHashMeta(key: ItemIdDto) = simpleHashService.fetch(key)

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

    override fun mergeMeta(raw: UnionMeta?, simpleHash: UnionMeta?) = when {
        raw == null -> simpleHash
        // TODO: add merging attributes
        raw != null && simpleHash != null -> mergeContent(raw, simpleHash)
        else -> raw
    }

    fun mergeContent(raw: UnionMeta, simpleHash: UnionMeta): UnionMeta {
        val existed = raw.content.map { it.representation }.toSet()
        val adding = simpleHash.content.filterNot { it.representation in existed }
        return raw.copy(content = raw.content + adding)
    }
}
