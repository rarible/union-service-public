package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadException
import com.rarible.protocol.union.core.model.download.MetaProviderType
import com.rarible.protocol.union.core.model.download.PartialDownloadException
import com.rarible.protocol.union.core.model.download.ProviderDownloadException
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.MetaSource
import com.rarible.protocol.union.enrichment.meta.WrappedMeta
import com.rarible.protocol.union.enrichment.meta.downloader.Downloader
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import org.springframework.stereotype.Component
import java.util.concurrent.ArrayBlockingQueue

@Component
class PartialItemMetaDownloader(
    private val router: BlockchainRouter<ItemService>,
    private val itemMetaContentEnrichmentService: ItemMetaContentEnrichmentService,
    private val providers: List<ItemMetaProvider>,
    private val itemRepository: ItemRepository,
) : Downloader<UnionMeta> {
    override val type = "Item_Partial"

    override suspend fun download(id: String): UnionMeta {
        val result = try {
            val itemId = IdParser.parseItemId(id)
            val item = itemRepository.get(ShortItemId(itemId)) ?: throw IllegalArgumentException("Item $id not found")
            LogUtils.addToMdc(itemId, router) { load(itemId = itemId, item = item) }
        } catch (e: PartialDownloadException) {
            throw e
        } catch (e: Exception) {
            throw DownloadException(e.message ?: "Unexpected exception")
        }
        result ?: throw DownloadException("No meta resolved for Item: $id")
        return result
    }

    protected suspend fun load(itemId: ItemIdDto, item: ShortItem): UnionMeta? {
        val failedProviders = ArrayBlockingQueue<MetaProviderType>(providers.size)

        val wrappedMeta = WrappedMeta(
            source = MetaSource.ORIGINAL,
            data = item.metaEntry?.data ?: return null,
        )
        val providersToUse = item.metaEntry.failedProviders ?: return item.metaEntry.data
        val meta = providers.filter { it.getType() in providersToUse }.fold(wrappedMeta) { current, provider ->
            try {
                provider.fetch(itemId, current) ?: current
            } catch (e: ProviderDownloadException) {
                failedProviders.add(e.provider)
                current
            }
        }

        if (failedProviders == providersToUse) {
            throw DownloadException("Failed to download meta from providers: $failedProviders")
        }

        val result = itemMetaContentEnrichmentService.enrcih(key = itemId, meta = meta)
        return if (failedProviders.isEmpty()) {
            result
        } else {
            throw PartialDownloadException(failedProviders = failedProviders.toList(), data = result)
        }
    }
}
