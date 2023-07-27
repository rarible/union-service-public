package com.rarible.protocol.union.worker.job.sync

import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.core.converter.EsCollectionConverter
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.producer.UnionInternalCollectionEventProducer
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.toSlice
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaService
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import com.rarible.protocol.union.worker.task.search.EsRateLimiter
import org.elasticsearch.action.support.WriteRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SyncCollectionJob(
    private val collectionServiceRouter: BlockchainRouter<CollectionService>,
    private val enrichmentCollectionService: EnrichmentCollectionService,
    private val collectionMetaService: CollectionMetaService,
    private val esCollectionRepository: EsCollectionRepository,
    private val producer: UnionInternalCollectionEventProducer,
    esRateLimiter: EsRateLimiter
) : AbstractSyncJob<UnionCollection, EnrichmentCollection, SyncCollectionJobParam>(
    "Collection",
    SyncCollectionJobParam::class.java,
    esRateLimiter
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getNext(param: SyncCollectionJobParam, state: String?): Slice<UnionCollection> {
        return collectionServiceRouter.getService(param.blockchain).getAllCollections(
            state,
            param.batchSize
        ).toSlice()
    }

    override suspend fun updateDb(
        param: SyncCollectionJobParam,
        unionEntities: List<UnionCollection>
    ): List<EnrichmentCollection> {
        // We should NOT catch exceptions here, our SYNC job should not skip entities
        return unionEntities.chunked(param.chunkSize).map { chunk ->
            chunk.mapAsync { collection ->
                val enrichmentCollection = enrichmentCollectionService.update(collection, false)
                if (enrichmentCollection.metaEntry == null) {
                    collectionMetaService.schedule(collection.id, CollectionMetaPipeline.SYNC, false)
                }
                enrichmentCollection
            }
        }.flatten()
    }

    override suspend fun updateEs(
        param: SyncCollectionJobParam,
        enrichmentEntities: List<EnrichmentCollection>,
        unionEntities: List<UnionCollection>
    ) {
        val deleted = ArrayList<String>()
        val exist = enrichmentEntities.filter {
            if (it.status == UnionCollection.Status.ERROR) {
                deleted.add(it.id.toString())
                false
            } else {
                true
            }
        }
        val esCollections = enrichmentCollectionService.enrich(exist, CollectionMetaPipeline.SYNC)
            .map { EsCollectionConverter.convert(it) }

        esCollectionRepository.bulk(esCollections, deleted, param.esIndex, WriteRequest.RefreshPolicy.NONE)
    }

    override suspend fun notify(
        param: SyncCollectionJobParam,
        enrichmentEntities: List<EnrichmentCollection>,
        unionEntities: List<UnionCollection>
    ) {
        producer.sendChangeEvents(enrichmentEntities.map { it.id.toDto() })
    }
}

data class SyncCollectionJobParam(
    override val blockchain: BlockchainDto,
    override val scope: SyncScope,
    override val esIndex: String? = null,
    override val batchSize: Int = DEFAULT_BATCH,
    override val chunkSize: Int = DEFAULT_CHUNK,
) : AbstractSyncJobParam()
