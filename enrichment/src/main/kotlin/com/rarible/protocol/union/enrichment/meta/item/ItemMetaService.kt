package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadMetrics
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadService
import com.rarible.protocol.union.enrichment.meta.simplehash.HookEventType
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashConverter
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashItem
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import com.rarible.protocol.union.enrichment.repository.RawMetaCacheRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemMetaService(
    private val rawMetaCacheRepository: RawMetaCacheRepository,
    repository: ItemMetaRepository,
    publisher: ItemMetaTaskPublisher,
    downloader: ItemMetaDownloader,
    notifier: ItemMetaNotifier,
    metrics: DownloadMetrics
) : DownloadService<ItemIdDto, UnionMeta>(repository, publisher, downloader, notifier, metrics) {

    override val type = downloader.type
    override fun toId(key: ItemIdDto) = key.fullId()
    override fun getBlockchain(key: ItemIdDto) = key.blockchain

    suspend fun get(
        itemId: ItemIdDto,
        sync: Boolean,
        pipeline: ItemMetaPipeline
    ) = get(itemId, sync, pipeline.pipeline)

    suspend fun download(
        itemId: ItemIdDto,
        pipeline: ItemMetaPipeline,
        force: Boolean
    ) = download(itemId, pipeline.pipeline, force)

    suspend fun schedule(
        itemId: ItemIdDto,
        pipeline: ItemMetaPipeline,
        force: Boolean
    ) = schedule(itemId, pipeline.pipeline, force)

    suspend fun handleSimpleHashWebhook(update: String) {
        val updateDto = SimpleHashConverter.safeConvertToMetaUpdate(update) ?: run {
            logger.error("Unable to parse SimpleHash webhook event: $update")
            return
        }
        when (updateDto.type) {
            is HookEventType.ChainNftMetadataUpdate -> {
                updateDto.nfts.forEach { nft ->
                    scheduleWebHookMetaRefresh(nft)
                }
            }
            is HookEventType.Unknown -> {
                logger.warn("Unknown webhook event type: ${updateDto.type.value}")
            }
        }
    }

    private suspend fun scheduleWebHookMetaRefresh(item: SimpleHashItem) {
        try {
            val itemIdDto = SimpleHashConverter.parseNftId(item.nftId)
            rawMetaCacheRepository.save(
                SimpleHashConverter.convert(itemIdDto, item)
            )
            schedule(itemIdDto, ItemMetaPipeline.REFRESH, true)
        } catch (e: Exception) {
            logger.error("Error processing webhook event for item ${item.nftId}", e)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ItemMetaService::class.java)
    }
}