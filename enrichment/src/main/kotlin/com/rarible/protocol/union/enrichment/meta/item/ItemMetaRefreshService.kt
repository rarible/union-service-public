package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.core.common.asyncWithTraceId
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.PartialDownloadException
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.MetaRefreshRequest
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.MetaRefreshRequestRepository
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ItemMetaRefreshService(
    private val esItemRepository: EsItemRepository,
    private val itemRepository: ItemRepository,
    private val itemMetaService: ItemMetaService,
    private val metaRefreshRequestRepository: MetaRefreshRequestRepository,
) {

    /**
     * Run refresh for Collection's Items if there is no running refresh.
     * Can be run even if there are postponed refresh scheduled for this collection.
     * For internal usage, you should understand what are you doing!
     */
    suspend fun scheduleRefreshIfNotRunning(
        collections: List<CollectionIdDto>,
        full: Boolean,
        scheduledAt: Instant,
        withSimpleHash: Boolean = false
    ) = collections.map { scheduleRefreshIfNotRunning(it, full, scheduledAt, withSimpleHash) }

    suspend fun scheduleRefreshIfNotRunning(
        collectionId: CollectionIdDto,
        full: Boolean,
        scheduledAt: Instant,
        withSimpleHash: Boolean = false
    ) {
        try {
            if (metaRefreshRequestRepository.countNotScheduledForCollectionId(collectionId.fullId()) == 0L) {
                logger.info("Scheduling refresh for $collectionId, full=$full at $scheduledAt")
                scheduleRefresh(
                    collectionId = collectionId,
                    full = full,
                    scheduledAt = scheduledAt,
                    withSimpleHash = withSimpleHash
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to schedule refresh for $collectionId", e)
        }
    }

    /**
     * Run refresh for Collection's Items if all conditions for that are met.
     * This method can be used by users, so there are several constraints for this procedure.
     */
    suspend fun runRefreshIfAllowed(
        collectionId: CollectionIdDto,
        full: Boolean,
        withSimpleHash: Boolean = false
    ): Boolean {
        if (isRefreshAllowed(collectionId)) {
            scheduleRefresh(
                collectionId = collectionId,
                full = full,
                withSimpleHash = withSimpleHash
            )
            return true
        }
        return false
    }

    /**
     * Run auto-refresh for Collection's Items if all conditions for that are met.
     * Auto refresh is internal procedure to keep Item's meta updated, can be considered as 'auto-healing'.
     * Launch of such refresh can be not allowed, for example, if collection is too big or there are
     * already several refreshes are running
     */
    suspend fun runAutoRefreshIfAllowed(
        collectionId: CollectionIdDto,
        withSimpleHash: Boolean
    ): Boolean {
        if (isAutoRefreshAllowed(collectionId.fullId())) {
            scheduleRefresh(
                collectionId = collectionId,
                full = true,
                withSimpleHash = withSimpleHash
            )
            return true
        }
        return false
    }

    /**
     * Schedule or run refresh for Collection's Items without any condition checks.
     */
    suspend fun scheduleRefresh(
        collectionId: CollectionIdDto,
        full: Boolean,
        scheduledAt: Instant = Instant.now(),
        withSimpleHash: Boolean = false
    ) {
        metaRefreshRequestRepository.save(
            MetaRefreshRequest(
                collectionId = collectionId.fullId(),
                full = full,
                scheduledAt = scheduledAt,
                withSimpleHash = withSimpleHash
            )
        )
    }

    suspend fun deleteAllScheduledRequests() {
        metaRefreshRequestRepository.deleteAll()
    }

    suspend fun countNotScheduled(): Long {
        return metaRefreshRequestRepository.countNotScheduled()
    }

    private suspend fun isRefreshAllowed(collectionId: CollectionIdDto): Boolean {
        val collectionFullId = collectionId.fullId()
        logger.info("Checking collection $collectionFullId for meta changes")
        val collectionSize = esItemRepository.countItemsInCollection(collectionFullId)
        if (collectionSize < COLLECTION_SIZE_THRESHOLD) {
            logger.info("Collection size $collectionSize is less than $COLLECTION_SIZE_THRESHOLD will do refresh")
            return true
        }
        if (collectionSize > BIG_COLLECTION_SIZE_THRESHOLD) {
            logger.info(
                "Collection size $collectionSize is bigger than $BIG_COLLECTION_SIZE_THRESHOLD will not do refresh"
            )
            return false
        }
        val refreshCount = metaRefreshRequestRepository.countForCollectionId(collectionFullId)
        if (refreshCount >= MAX_COLLECTION_REFRESH_COUNT) {
            logger.info("Collection refresh count $refreshCount gte $MAX_COLLECTION_REFRESH_COUNT. Will not refresh")
            return false
        }
        val scheduledCount = metaRefreshRequestRepository.countNotScheduledForCollectionId(collectionFullId)
        if (scheduledCount > 0) {
            logger.info("Collection refresh already pending for $collectionFullId. Will not refresh")
            return false
        }
        return checkMetaChanges(collectionFullId)
    }

    private suspend fun checkMetaChanges(collectionFullId: String): Boolean {
        return coroutineScope {
            esItemRepository.getRandomItemsFromCollection(collectionId = collectionFullId, size = RANDOM_ITEMS_TO_CHECK)
                .map { esItem ->
                    asyncWithTraceId {
                        val idDto = IdParser.parseItemId(esItem.itemId)
                        val oldItem = itemRepository.get(ShortItemId(idDto)) ?: return@asyncWithTraceId false
                        val meta = try {
                            itemMetaService.download(itemId = idDto, pipeline = ItemMetaPipeline.REFRESH, force = true)
                                ?: return@asyncWithTraceId false
                        } catch (e: PartialDownloadException) {
                            e.data as UnionMeta
                        } catch (e: Exception) {
                            return@asyncWithTraceId false
                        }
                        if (oldItem.metaEntry?.data?.toComparable() != meta.toComparable()) {
                            logger.info(
                                "Meta changed for item $idDto from ${oldItem.metaEntry?.data} to $meta " +
                                    "will allow meta refresh for collection"
                            )
                            true
                        } else {
                            false
                        }
                    }
                }.awaitAll()
        }.any { it }
    }

    private suspend fun isAutoRefreshAllowed(collectionId: String): Boolean {
        val collectionSize = esItemRepository.countItemsInCollection(collectionId)
        if (collectionSize > BIG_COLLECTION_SIZE_THRESHOLD) {
            logger.info(
                "Collection $collectionId size $collectionSize is bigger than $BIG_COLLECTION_SIZE_THRESHOLD " +
                    "will not do auto refresh"
            )
            return false
        }
        val scheduledCount = metaRefreshRequestRepository.countNotScheduledForCollectionId(collectionId)
        if (scheduledCount > 0) {
            logger.info("Collection refresh already pending for $collectionId. Will not auto refresh")
            return false
        }
        if (!checkMetaChanges(collectionId)) {
            logger.info("No meta changes found for sample items from $collectionId. Will not auto refresh")
            return false
        }
        return true
    }

    companion object {
        private const val COLLECTION_SIZE_THRESHOLD = 1000
        private const val BIG_COLLECTION_SIZE_THRESHOLD = 40000
        private const val RANDOM_ITEMS_TO_CHECK = 100
        private const val MAX_COLLECTION_REFRESH_COUNT = 3
        private val logger = LoggerFactory.getLogger(ItemMetaRefreshService::class.java)
    }
}
