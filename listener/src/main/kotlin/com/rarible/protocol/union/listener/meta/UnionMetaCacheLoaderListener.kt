package com.rarible.protocol.union.listener.meta

import com.rarible.loader.cache.CacheEntry
import com.rarible.loader.cache.CacheLoaderEvent
import com.rarible.loader.cache.CacheLoaderEventListener
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.UnionMetaCacheLoader
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UnionMetaCacheLoaderListener(
    private val itemServiceRouter: BlockchainRouter<ItemService>,
    private val itemEventHandler: IncomingEventHandler<UnionItemEvent>
) : CacheLoaderEventListener<UnionMeta> {

    private val logger = LoggerFactory.getLogger(UnionMetaCacheLoaderListener::class.java)

    override val type
        get() = UnionMetaCacheLoader.TYPE

    override suspend fun onEvent(cacheLoaderEvent: CacheLoaderEvent<UnionMeta>) {
        val itemId = IdParser.parseItemId(cacheLoaderEvent.key)
        val meta = when (val cacheEntry = cacheLoaderEvent.cacheEntry) {
            is CacheEntry.Loaded -> {
                logger.info("Loaded meta for $itemId, sending item update event")
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
        val item = itemServiceRouter.getService(itemId.blockchain).getItemById(itemId.value)
        val itemWithMeta = item.copy(meta = meta)
        itemEventHandler.onEvent(UnionItemUpdateEvent(itemWithMeta))
    }
}
