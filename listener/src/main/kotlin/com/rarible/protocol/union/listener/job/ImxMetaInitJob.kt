package com.rarible.protocol.union.listener.job

import com.rarible.core.common.mapAsync
import com.rarible.loader.cache.internal.CacheRepository
import com.rarible.loader.cache.internal.MongoCacheEntry
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloader
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.ItemMetaService
import com.rarible.protocol.union.integration.immutablex.service.ImxItemService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
@Deprecated("Only for launch, remove later")
class ImxMetaInitJob(
    private val cacheRepository: CacheRepository,
    private val imxItemService: ImxItemService,
    private val itemMetaService: ItemMetaService,
    private val enrichmentItemService: EnrichmentItemService,
    private val eventProducer: UnionInternalBlockchainEventProducer,
    @Value("\${listener.immutablex-meta-task.delay:1000}")
    private val delay: Long,
    @Value("\${listener.immutablex-meta-task.enabled:false}")
    private val enabled: Boolean
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(continuation: String?): Flow<String> {
        if (!enabled) return emptyFlow()

        return flow {
            var next = continuation
            do {
                next = scheduleMeta(next)
                if (next != null) {
                    emit(next)
                }
            } while (next != null)
        }
    }

    private suspend fun scheduleMeta(continuation: String?): String? {
        val page = imxItemService.getAllItems(continuation, 200, false, null, null)
        val items = page.entities
        if (items.isEmpty()) return null

        val itemsById = items.associateBy { it.id.fullId() }
        val missing = itemsById.keys.toHashSet()
        val schemaAttributes = imxItemService.getMetaAttributeKeys(items.map { it.id.value })
        val found = cacheRepository.getAll<UnionMeta>(ItemMetaDownloader.TYPE, itemsById.keys.toList())

        val updated = AtomicInteger(0)
        found.chunked(16).map { batch ->
            batch.mapAsync { entry ->
                val itemId = IdParser.parseItemId(entry.key)
                val result = fixMetaAttributes(
                    itemsById[entry.key]!!,
                    entry,
                    schemaAttributes[itemId.value] ?: emptySet()
                )
                if (result) updated.incrementAndGet()
                missing.remove(entry.key)
            }
        }

        missing.chunked(4).forEach { batch ->
            batch.mapAsync {
                itemMetaService.schedule(IdParser.parseItemId(it), "default", true)
            }
        }

        logger.info("Sent {} Immutablex meta refresh tasks, {} cached meta entries updated", items.size, updated.get())
        if (delay > 0) {
            delay(delay)
        }
        return page.continuation
    }

    private suspend fun fixMetaAttributes(
        item: UnionItem,
        entry: MongoCacheEntry<UnionMeta>,
        allowedAttributeKeys: Set<String>
    ): Boolean {
        val meta = entry.data
        val attributes = meta.attributes
        val filteredAttributes = attributes.filter { allowedAttributeKeys.contains(it.key) }

        return if (filteredAttributes.size != attributes.size) {
            val updated = cacheRepository.save(
                type = ItemMetaDownloader.TYPE,
                key = entry.key,
                data = entry.data.copy(attributes = filteredAttributes),
                cachedAt = entry.cachedAt
            )
            notify(item, updated)
            true
        } else {
            false
        }
    }

    private suspend fun notify(item: UnionItem, entry: MongoCacheEntry<UnionMeta>) {
        val itemWithMeta = item.copy(meta = entry.data)
        val message = KafkaEventFactory.internalItemEvent(UnionItemUpdateEvent(itemWithMeta))
        eventProducer.getProducer(item.id.blockchain).send(message)
        logger.info("Immutablex meta attributes fix ${entry.key} finished, notification sent")
    }

}