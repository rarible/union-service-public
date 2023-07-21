package com.rarible.protocol.union.enrichment.meta.collection

import com.rarible.core.logging.asyncWithTraceId
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.PartialDownloadException
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaService
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.MetaRefreshRequestRepository
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CollectionMetaRefreshService(
    private val esItemRepository: EsItemRepository,
    private val itemRepository: ItemRepository,
    private val itemMetaService: ItemMetaService,
    private val metaRefreshRequestRepository: MetaRefreshRequestRepository,
) {

    suspend fun shouldRefresh(collectionId: CollectionIdDto): Boolean {
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

    suspend fun shouldAutoRefresh(collectionId: String): Boolean {
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
        private val logger = LoggerFactory.getLogger(CollectionMetaRefreshService::class.java)
    }
}