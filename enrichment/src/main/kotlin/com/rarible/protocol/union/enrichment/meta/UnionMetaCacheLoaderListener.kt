package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.CaptureTransaction
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.loader.cache.CacheEntry
import com.rarible.loader.cache.CacheLoaderEvent
import com.rarible.loader.cache.CacheLoaderEventListener
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionWrappedEvent
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UnionMetaCacheLoaderListener(
    private val itemServiceRouter: BlockchainRouter<ItemService>,
    private val wrappedEventProducer: RaribleKafkaProducer<UnionWrappedEvent>,
    private val unionMetaProperties: UnionMetaProperties
) : CacheLoaderEventListener<UnionMeta> {

    private val logger = LoggerFactory.getLogger(UnionMetaCacheLoaderListener::class.java)

    override val type
        get() = UnionMetaCacheLoader.TYPE

    @CaptureTransaction("UnionMetaCacheLoaderListener")
    override suspend fun onEvent(cacheLoaderEvent: CacheLoaderEvent<UnionMeta>) {
        val itemId = IdParser.parseItemId(cacheLoaderEvent.key)
        sendItemUpdateEvent(itemId, cacheLoaderEvent)
    }

    private suspend fun sendItemUpdateEvent(
        itemId: ItemIdDto,
        cacheLoaderEvent: CacheLoaderEvent<UnionMeta>
    ) {
        val meta = when (val cacheEntry = cacheLoaderEvent.cacheEntry) {
            is CacheEntry.Loaded -> {
                logger.info("Loaded meta for $itemId")
                cacheEntry.data
            }
            is CacheEntry.LoadedAndUpdateFailed -> {
                logger.info("Failed to update meta for $itemId: ${cacheEntry.failedUpdateStatus.errorMessage}")
                return
            }
            is CacheEntry.InitialFailed -> {
                logger.info("Failed to load meta for $itemId: ${cacheEntry.failedStatus.errorMessage}")
                return
            }
            is CacheEntry.LoadedAndUpdateScheduled -> return
            is CacheEntry.InitialLoadScheduled -> return
            is CacheEntry.NotAvailable -> return
        }
        if (unionMetaProperties.skipAttachingMetaInEvents) {
            logger.info("Skip meta item update event for $itemId")
            return
        }
        logger.info("Sending meta item update event for $itemId")
        val item = getItem(itemId)
        val itemWithMeta = item.copy(meta = meta)
        wrappedEventProducer.send(KafkaEventFactory.wrappedItemEvent(UnionItemUpdateEvent(itemWithMeta)))
    }

    @CaptureSpan("getItemById")
    private suspend fun getItem(itemId: ItemIdDto): UnionItem =
        itemServiceRouter.getService(itemId.blockchain).getItemById(itemId.value)
}
