package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.download.DownloadTaskSource
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadMetrics
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadService
import com.rarible.protocol.union.enrichment.meta.simplehash.HookEventType
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashConverter
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashConverterService
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashItem
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashItemMetrics
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashNftMetadataUpdate
import com.rarible.protocol.union.enrichment.model.MetaDownloadPriority
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
    metrics: DownloadMetrics,
    private val simpleHashConverterService: SimpleHashConverterService,
    private val simpleHashMetrics: SimpleHashItemMetrics
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
        force: Boolean,
        source: DownloadTaskSource = DownloadTaskSource.INTERNAL,
    ) = download(itemId, pipeline.pipeline, force, source)

    suspend fun schedule(
        itemId: ItemIdDto,
        pipeline: ItemMetaPipeline,
        force: Boolean,
        source: DownloadTaskSource = DownloadTaskSource.INTERNAL,
        priority: Int
    ) = schedule(itemId, pipeline.pipeline, force, source, priority)

    suspend fun handleSimpleHashWebhook(update: String) {
        val updateDto = SimpleHashConverter.safeConvertToMetaUpdate(update) ?: run {
            logger.error("Unable to parse SimpleHash webhook event: $update")
            return
        }
        when (updateDto.type) {
            is HookEventType.ChainNftMetadataUpdate,
            is HookEventType.ContractNftMetadataUpdate -> {
                logHook(updateDto)
                updateDto.nfts.forEach { nft ->
                    scheduleAndSaveSimpleHashItemUpdate(nft, true)
                }
            }
            is HookEventType.Unknown -> {
                logger.warn("Unknown webhook event type: ${updateDto.type.value}")
            }
        }
    }

    suspend fun scheduleAndSaveSimpleHashItemUpdate(item: SimpleHashItem, force: Boolean) {
        try {
            val itemIdDto = SimpleHashConverter.parseNftId(item.nftId)
            val cacheEntity = SimpleHashConverter.convert(itemIdDto, item)
            val existedCached = rawMetaCacheRepository.get(cacheEntity.id)
            val existedEntity = existedCached?.let {
                simpleHashConverterService.convertRawToSimpleHashItem(it.data)
            }
            // we should process items with original urls only (with meta)
            if (item != existedEntity && item.hasOriginalsUrls()) {
                logger.info("Meta for item ${item.nftId} was changed or new, existed meta: $existedCached, new: ${cacheEntity.data}")
                simpleHashMetrics.onMetaItemChanged(itemIdDto.blockchain)
                rawMetaCacheRepository.save(cacheEntity)
                scheduleSimpleHashItemRefresh(item, existedEntity, force)
            } else {
                logger.info("Meta for item ${item.nftId} wasn't change or item's meta is empty.")
                simpleHashMetrics.onMetaItemIgnored(itemIdDto.blockchain)
            }
        } catch (e: Exception) {
            logger.error("Error processing scheduling for item ${item.nftId}", e)
        }
    }

    suspend fun scheduleSimpleHashItemRefresh(item: SimpleHashItem, existedEntity: SimpleHashItem?, force: Boolean) {
        val itemIdDto = SimpleHashConverter.parseNftId(item.nftId)
        // We should refresh only if we already have cache for existed item and original urls were changed
        if (force || (existedEntity != null && item.differentOriginalUrls(existedEntity))) {
            schedule(
                itemId = itemIdDto,
                pipeline = ItemMetaPipeline.REFRESH,
                force = true,
                priority = MetaDownloadPriority.MEDIUM
            )
            simpleHashMetrics.onMetaItemRefresh(itemIdDto.blockchain)
        }
    }

    private fun logHook(updateDto: SimpleHashNftMetadataUpdate) {
        try {
            val itemOIds = updateDto.nfts.joinToString {
                SimpleHashConverter.parseNftId(it.nftId).fullId()
            }
            logger.info("Received webhook event: ${updateDto.type.value}, items=$itemOIds")
        } catch (ex: Throwable) {
            logger.error("Error logging webhook event: ${updateDto.type.value}", ex)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ItemMetaService::class.java)
    }
}
