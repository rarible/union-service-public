package com.rarible.protocol.union.listener.job

import com.rarible.loader.cache.internal.CacheRepository
import com.rarible.loader.cache.internal.MongoCacheEntry
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloader
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
@Deprecated("Should be removed after migration")
class OpenSeaUrlMigrationJob(
    private val cacheRepository: CacheRepository,
    private val eventProducer: UnionInternalBlockchainEventProducer,
    private val enrichmentItemService: EnrichmentItemService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val batchSize = 1000
    private val asyncChuckSize = 16

    private val legacyOpenSeaPath = "https://storage.opensea.io/files/"
    private val actualOpenSeaPath = "https://openseauserdata.com/files/"

    private val cUpdated = AtomicInteger()
    private val cFailed = AtomicInteger()
    private val cSkipped = AtomicInteger()

    private val counters = mapOf(
        "updated" to cUpdated,
        "skipped" to cSkipped,
        "failed" to cFailed
    )

    fun migrate(continuation: String?): Flow<String> {
        return flow {
            var next = continuation
            do {
                next = migrateBatch(next)
                if (next != null) {
                    emit(next)
                }
            } while (next != null)
        }
    }

    private suspend fun migrateBatch(fromId: String?): String? {
        logger.info("Starting to migrate OpenSea URLs, state: $fromId")
        counters.values.forEach { it.set(0) }
        val entries = cacheRepository.findAll<UnionMeta>(ItemMetaDownloader.TYPE, fromId, batchSize)
        coroutineScope {
            entries.chunked(asyncChuckSize).map { chunk ->
                chunk.map {
                    async {
                        tryMigrateMeta(it)
                    }
                }.awaitAll()
            }
        }
        val continuation = entries.lastOrNull()?.key
        logger.info("Checked ${entries.size}, state: $continuation, counters: $counters")
        return continuation
    }

    private suspend fun tryMigrateMeta(entry: MongoCacheEntry<UnionMeta>) {
        try {
            try {
                migrateMeta(entry)
                // And one more try in case of concurrency, originally, should not happen
            } catch (ex: OptimisticLockingFailureException) {
                migrateMeta(cacheRepository.get(ItemMetaDownloader.TYPE, entry.key)!!)
            } catch (ex: DuplicateKeyException) {
                migrateMeta(cacheRepository.get(ItemMetaDownloader.TYPE, entry.key)!!)
            }
        } catch (e: Exception) {
            // Some kind of unexpected exception, should not happen
            logger.warn("Failed to migrate OpenSea URLs for Item ${entry.key}", e)
            cFailed.incrementAndGet()
        }
    }

    private suspend fun migrateMeta(entry: MongoCacheEntry<UnionMeta>) {
        var contentUpdated = false

        val updatedContent = entry.data.content.map {
            if (it.url.startsWith(legacyOpenSeaPath)) {
                contentUpdated = true
                it.copy(url = it.url.replace(legacyOpenSeaPath, actualOpenSeaPath))
            } else {
                it
            }
        }

        // Nothing changed, abort
        if (!contentUpdated) {
            cSkipped.incrementAndGet()
            return
        }

        val updatedMeta = cacheRepository.save(
            type = ItemMetaDownloader.TYPE,
            key = entry.key,
            data = entry.data.copy(content = updatedContent),
            cachedAt = entry.cachedAt
        )

        notify(updatedMeta)

        cUpdated.incrementAndGet()
    }

    private suspend fun notify(entry: MongoCacheEntry<UnionMeta>) {
        val itemId = IdParser.parseItemId(entry.key)
        val item = enrichmentItemService.fetchOrNull(ShortItemId(itemId))
        if (item == null) {
            logger.info("OpenSea content migration for item ${entry.key} finished, but Item NOT_FOUND")
            return
        }
        val itemWithMeta = item.copy(meta = entry.data)
        val message = KafkaEventFactory.internalItemEvent(UnionItemUpdateEvent(itemWithMeta))
        eventProducer.getProducer(itemId.blockchain).send(message)
        logger.info("OpenSea content meta migration for item ${entry.key} finished, notification sent")
    }

}