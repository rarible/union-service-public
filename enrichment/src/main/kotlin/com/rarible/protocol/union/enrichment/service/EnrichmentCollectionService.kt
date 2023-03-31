package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.enrichment.converter.EnrichedCollectionConverter
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaMetrics
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaService
import com.rarible.protocol.union.enrichment.meta.content.ContentMetaService
import com.rarible.protocol.union.enrichment.model.ShortCollection
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.service.query.order.OrderApiMergeService
import com.rarible.protocol.union.enrichment.util.spent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EnrichmentCollectionService(
    private val collectionServiceRouter: BlockchainRouter<CollectionService>,
    private val collectionRepository: CollectionRepository,
    private val enrichmentOrderService: EnrichmentOrderService,
    private val contentMetaService: ContentMetaService,
    private val orderApiService: OrderApiMergeService,
    private val collectionMetaService: CollectionMetaService,
    private val metrics: CollectionMetaMetrics
) {

    private val logger = LoggerFactory.getLogger(EnrichmentCollectionService::class.java)

    suspend fun get(collectionId: ShortCollectionId): ShortCollection? {
        return collectionRepository.get(collectionId)
    }

    suspend fun getOrCreateWithLastUpdatedAtUpdate(collectionId: ShortCollectionId): ShortCollection {
        val collection = collectionRepository.get(collectionId) ?: ShortCollection.empty(collectionId)
        return collectionRepository.save(collection.withCalculatedFields())
    }

    suspend fun save(collection: ShortCollection): ShortCollection? {
        return collectionRepository.save(collection.withCalculatedFields())
    }

    suspend fun getOrEmpty(collectionId: ShortCollectionId): ShortCollection {
        return collectionRepository.get(collectionId) ?: ShortCollection.empty(collectionId)
    }

    suspend fun findAll(ids: List<ShortCollectionId>): List<ShortCollection> {
        return collectionRepository.getAll(ids)
    }

    suspend fun fetch(collectionId: ShortCollectionId): UnionCollection {
        val now = nowMillis()
        val collectionDto = collectionServiceRouter.getService(collectionId.blockchain)
            .getCollectionById(collectionId.collectionId)
        logger.info("Fetched collection [{}] ({} ms)", collectionId.toDto().fullId(), spent(now))
        return collectionDto
    }

    suspend fun enrichCollection(
        shortCollection: ShortCollection?,
        collection: UnionCollection?,
        orders: Map<OrderIdDto, OrderDto> = emptyMap(),
        metaPipeline: CollectionMetaPipeline
    ) = coroutineScope {
        require(shortCollection != null || collection != null)
        val collectionId = shortCollection?.id?.toDto() ?: collection!!.id
        val fetchedCollection = async {
            collection ?: fetch(ShortCollectionId(collectionId))
        }

        val bestOrders = enrichmentOrderService.fetchMissingOrders(
            existing = shortCollection?.getAllBestOrders() ?: emptyList(),
            orders = orders
        )

        val unionCollection = fetchedCollection.await()
        val metaEntry = shortCollection?.metaEntry
        val meta = metaEntry?.data ?: unionCollection.meta

        onMetaEntry(unionCollection.id, metaPipeline, metaEntry)

        EnrichedCollectionConverter.convert(
            collection = unionCollection,
            // replacing inner IPFS urls with public urls
            meta = contentMetaService.exposePublicUrls(meta),
            shortCollection = shortCollection,
            orders = bestOrders
        )
    }

    suspend fun enrich(
        shortCollections: List<ShortCollection>,
        metaPipeline: CollectionMetaPipeline
    ): List<CollectionDto> {
        if (shortCollections.isEmpty()) {
            return emptyList()
        }
        val shortCollectionsById: Map<CollectionIdDto, ShortCollection> = shortCollections.associateBy { it.id.toDto() }

        val groupedIds = shortCollections.groupBy({ it.blockchain }, { it.id.collectionId })

        val unionCollections = groupedIds.flatMap {
            collectionServiceRouter.getService(it.key).getCollectionsByIds(it.value)
        }

        return enrichCollections(shortCollectionsById, unionCollections, metaPipeline)
    }

    suspend fun enrichUnionCollections(
        unionCollections: List<UnionCollection>,
        metaPipeline: CollectionMetaPipeline
    ): List<CollectionDto> {
        if (unionCollections.isEmpty()) {
            return emptyList()
        }
        val shortCollections: Map<CollectionIdDto, ShortCollection> =
            collectionRepository.getAll(unionCollections.map { ShortCollectionId(it.id) })
                .associateBy { it.id.toDto() }

        return enrichCollections(shortCollections, unionCollections, metaPipeline)
    }

    private suspend fun enrichCollections(
        shortCollections: Map<CollectionIdDto, ShortCollection>,
        unionCollections: List<UnionCollection>,
        metaPipeline: CollectionMetaPipeline
    ): List<CollectionDto> {
        val shortOrderIds = shortCollections.values
            .map { it.getAllBestOrders() }
            .flatten()
            .map { it.dtoId }

        val orders = orderApiService.getByIds(shortOrderIds)
            .associateBy { it.id }

        return unionCollections.map {
            enrichCollection(shortCollections[it.id], it, orders, metaPipeline)
        }
    }

    private suspend fun onMetaEntry(
        collectionId: CollectionIdDto,
        metaPipeline: CollectionMetaPipeline,
        entry: DownloadEntry<UnionCollectionMeta>?
    ) {
        when {
            // No entry - it means we see this item/meta first time, not cached at all
            entry == null -> {
                collectionMetaService.schedule(collectionId, metaPipeline, false)
                metrics.onMetaCacheMiss(collectionId.blockchain)
            }
            // Downloaded - cool, we hit cache!
            entry.status == DownloadStatus.SUCCESS -> {
                metrics.onMetaCacheHit(collectionId.blockchain)
            }
            // Otherwise, downloading in progress or completely failed - mark as "empty" cache
            else -> {
                metrics.onMetaCacheEmpty(collectionId.blockchain)
            }
        }
    }
}
