package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.FeatureFlagsProperties
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
import com.rarible.protocol.union.enrichment.converter.CollectionDtoConverter
import com.rarible.protocol.union.enrichment.converter.EnrichmentCollectionConverter
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaMetrics
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaService
import com.rarible.protocol.union.enrichment.meta.content.ContentMetaService
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
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
    private val metrics: CollectionMetaMetrics,
    private val ff: FeatureFlagsProperties
) {

    private val logger = LoggerFactory.getLogger(EnrichmentCollectionService::class.java)

    suspend fun get(collectionId: EnrichmentCollectionId): EnrichmentCollection? {
        return collectionRepository.get(collectionId)
    }

    suspend fun getAll(ids: List<EnrichmentCollectionId>): List<EnrichmentCollection> {
        return collectionRepository.getAll(ids)
    }

    suspend fun fetch(collectionId: EnrichmentCollectionId): UnionCollection {
        val now = nowMillis()
        val collectionDto = collectionServiceRouter.getService(collectionId.blockchain)
            .getCollectionById(collectionId.collectionId)
        logger.info("Fetched collection [{}] ({} ms)", collectionId.toDto().fullId(), spent(now))
        return collectionDto
    }

    suspend fun getOrFetch(collectionId: EnrichmentCollectionId): EnrichmentCollection {
        get(collectionId)?.let { return it }
        // Ideally we shouldn't have such situations (possible only if we got order event earlier than collection)
        logger.warn("Collection not found in Union DB, fetching: {}", collectionId.toDto().fullId())
        val fetched = fetch(collectionId)
        return EnrichmentCollectionConverter.convert(fetched)
    }

    suspend fun update(collection: UnionCollection, updateDate: Boolean = true): EnrichmentCollection {
        val id = EnrichmentCollectionId(collection.id)
        val updated = collectionRepository.get(id)?.withData(collection) // Update existing
            ?: EnrichmentCollectionConverter.convert(collection) // Or create new one

        // TODO update after the migration
        val withCalculatedFields = if (updateDate) {
            updated.withCalculatedFieldsAndUpdatedAt()
        } else {
            updated.withCalculatedFields()
        }
        return collectionRepository.save(withCalculatedFields)
    }

    suspend fun save(collection: EnrichmentCollection): EnrichmentCollection? {
        return collectionRepository.save(collection.withCalculatedFieldsAndUpdatedAt())
    }

    suspend fun findAll(ids: List<EnrichmentCollectionId>): List<EnrichmentCollection> {
        return collectionRepository.getAll(ids)
    }

    suspend fun enrichCollection(
        enrichmentCollection: EnrichmentCollection?,
        collection: UnionCollection?,
        orders: Map<OrderIdDto, OrderDto> = emptyMap(),
        metaPipeline: CollectionMetaPipeline
    ): CollectionDto {
        require(enrichmentCollection != null || collection != null)
        if (ff.enableUnionCollections) {
            if (enrichmentCollection != null) {
                return enrichCollection(enrichmentCollection, orders, metaPipeline)
            } else {
                logger.warn("Enrichment Collection {} not synced!", collection!!.id.fullId())
            }
        }
        return enrichCollectionLegacy(enrichmentCollection, collection, orders, metaPipeline)
    }

    suspend fun enrichCollection(
        enrichmentCollection: EnrichmentCollection,
        orders: Map<OrderIdDto, OrderDto>,
        metaPipeline: CollectionMetaPipeline
    ): CollectionDto {

        val collectionId = enrichmentCollection.id.toDto()
        val metaEntry = enrichmentCollection.metaEntry

        val bestOrders = enrichmentOrderService.fetchMissingOrders(
            existing = enrichmentCollection.getAllBestOrders(),
            orders = orders
        )

        onMetaEntry(collectionId, metaPipeline, metaEntry)

        return CollectionDtoConverter.convert(
            collection = enrichmentCollection,
            // replacing inner IPFS urls with public urls
            meta = contentMetaService.exposePublicUrls(metaEntry?.data),
            orders = bestOrders
        )
    }

    @Deprecated("Remove after the migration to Union data")
    private suspend fun enrichCollectionLegacy(
        enrichmentCollection: EnrichmentCollection?,
        collection: UnionCollection?,
        orders: Map<OrderIdDto, OrderDto> = emptyMap(),
        metaPipeline: CollectionMetaPipeline
    ) = coroutineScope {
        require(enrichmentCollection != null || collection != null)
        val collectionId = enrichmentCollection?.id?.toDto() ?: collection!!.id
        val fetchedCollection = async {
            collection ?: fetch(EnrichmentCollectionId(collectionId))
        }

        val bestOrders = enrichmentOrderService.fetchMissingOrders(
            existing = enrichmentCollection?.getAllBestOrders() ?: emptyList(),
            orders = orders
        )

        val unionCollection = fetchedCollection.await()
        val metaEntry = enrichmentCollection?.metaEntry
        val meta = metaEntry?.data ?: unionCollection.meta

        onMetaEntry(unionCollection.id, metaPipeline, metaEntry)

        CollectionDtoConverter.convertLegacy(
            collection = unionCollection,
            // replacing inner IPFS urls with public urls
            meta = contentMetaService.exposePublicUrls(meta),
            enrichmentCollection = enrichmentCollection,
            orders = bestOrders
        )
    }

    suspend fun enrich(
        enrichmentCollections: List<EnrichmentCollection>,
        metaPipeline: CollectionMetaPipeline
    ): List<CollectionDto> {
        if (enrichmentCollections.isEmpty()) {
            return emptyList()
        }
        val enrichmentCollectionsById: Map<CollectionIdDto, EnrichmentCollection> =
            enrichmentCollections.associateBy { it.id.toDto() }

        val groupedIds = enrichmentCollections.groupBy({ it.blockchain }, { it.id.collectionId })

        val unionCollections = groupedIds.flatMap {
            collectionServiceRouter.getService(it.key).getCollectionsByIds(it.value)
        }

        return enrichCollections(enrichmentCollectionsById, unionCollections, metaPipeline)
    }

    suspend fun enrichUnionCollections(
        unionCollections: List<UnionCollection>,
        metaPipeline: CollectionMetaPipeline
    ): List<CollectionDto> {
        if (unionCollections.isEmpty()) {
            return emptyList()
        }
        val enrichmentCollections: Map<CollectionIdDto, EnrichmentCollection> =
            collectionRepository.getAll(unionCollections.map { EnrichmentCollectionId(it.id) })
                .associateBy { it.id.toDto() }

        return enrichCollections(enrichmentCollections, unionCollections, metaPipeline)
    }

    private suspend fun enrichCollections(
        enrichmentCollections: Map<CollectionIdDto, EnrichmentCollection>,
        unionCollections: List<UnionCollection>,
        metaPipeline: CollectionMetaPipeline
    ): List<CollectionDto> {
        val shortOrderIds = enrichmentCollections.values
            .map { it.getAllBestOrders() }
            .flatten()
            .map { it.dtoId }

        val orders = orderApiService.getByIds(shortOrderIds)
            .associateBy { it.id }

        return unionCollections.map {
            enrichCollection(enrichmentCollections[it.id], it, orders, metaPipeline)
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
