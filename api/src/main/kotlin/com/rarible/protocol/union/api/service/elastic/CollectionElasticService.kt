package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.elastic.EsCollectionCursor.Companion.fromCollectionLite
import com.rarible.protocol.union.core.model.elastic.EsCollectionGenericFilter
import com.rarible.protocol.union.core.model.elastic.EsCollectionLite
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import com.rarible.protocol.union.enrichment.service.query.collection.CollectionQueryService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.system.measureTimeMillis

@Service
@CaptureSpan(type = SpanType.APP)
class CollectionElasticService(
    private val repository: EsCollectionRepository,
    private val router: BlockchainRouter<CollectionService>,
    private val enrichmentCollectionService: EnrichmentCollectionService,
    private val ff: FeatureFlagsProperties
) : CollectionQueryService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getAllCollections(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
    ): CollectionsDto {
        val enabledBlockchains = router.getEnabledBlockchains(blockchains).toList()
        if (enabledBlockchains.isEmpty()) {
            logger.info("Unable to find enabled blockchains for getAllCollections(), blockchains={}", blockchains)
            return CollectionsDto()
        }
        val filter = EsCollectionGenericFilter(
            blockchains = enabledBlockchains.toSet(),
            cursor = continuation,
        )
        val result = repository.search(filter, size)

        return processResult(result)
    }

    override suspend fun getCollectionsByOwner(
        owner: UnionAddress,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
    ): CollectionsDto {
        val enabledBlockchains = router.getEnabledBlockchains(blockchains).toList()
        if (enabledBlockchains.isEmpty()) {
            logger.info(
                "Unable to find enabled blockchains for getCollectionsByOwner(), owner={}, blockchains={}",
                owner, blockchains
            )
            return CollectionsDto()
        }
        val filter = EsCollectionGenericFilter(
            blockchains = enabledBlockchains.toSet(),
            owners = setOf(owner.fullId()),
            cursor = continuation,
        )
        val result = repository.search(filter, size)

        return processResult(result)
    }

    private suspend fun processResult(result: List<EsCollectionLite>): CollectionsDto {
        return if (ff.enableUnionCollections) {
            processResultModern(result)
        } else {
            processResultLegacy(result)
        }
    }

    private suspend fun processResultModern(result: List<EsCollectionLite>): CollectionsDto {
        val seq = mutableMapOf<String, Int>()
        val enabledBlockchains = router.getEnabledBlockchains(null)

        result.forEachIndexed { index, esCollection ->
            seq[IdParser.parseCollectionId(esCollection.collectionId).fullId()] = index
        }

        val collectionIds = seq.keys.map { EnrichmentCollectionId(IdParser.parseCollectionId(it)) }
            .filter { enabledBlockchains.contains(it.blockchain) }

        val cursor = if (result.isEmpty()) null else result.last().fromCollectionLite()

        val enrichmentCollections = enrichmentCollectionService.getAll(collectionIds)

        return CollectionsDto(
            total = result.size.toLong(),
            continuation = cursor.toString(),
            collections = enrichmentCollectionService.enrich(enrichmentCollections, CollectionMetaPipeline.API)
        )
    }

    @Deprecated("Should be removed after the migration")
    private suspend fun processResultLegacy(result: List<EsCollectionLite>): CollectionsDto {
        val seq = mutableMapOf<String, Int>()
        val enabledBlockchains = router.getEnabledBlockchains(null)

        result.forEachIndexed { index, esCollection ->
            seq[IdParser.parseCollectionId(esCollection.collectionId).fullId()] = index
        }

        val collections = seq.keys.groupBy { IdParser.extractBlockchain(it).first }
            .filter { enabledBlockchains.contains(it.key) }
            .mapAsync { (k, v) ->
                val ids = v.map { IdParser.parseCollectionId(it).value }
                var mapAsyncResult: List<UnionCollection>
                val time = measureTimeMillis {
                    mapAsyncResult = router.getService(k).getCollectionsByIds(ids)
                }
                logger.info("getCollectionsByIds(), time: ${time}ms. blockchain: ${k}, size ${ids.size}")
                return@mapAsync mapAsyncResult
            }
            .flatten().associateBy { seq[it.id.fullId()] }.toSortedMap(compareBy { it })

        val cursor = if (result.isEmpty()) null else result.last().fromCollectionLite()

        val enrichmentCollections: Map<CollectionIdDto, EnrichmentCollection> = enrichmentCollectionService
            .findAll(collections.map { EnrichmentCollectionId(it.value.id) })
            .associateBy { it.id.toDto() }

        return CollectionsDto(
            total = result.size.toLong(),
            continuation = cursor.toString(),
            collections = collections.values.toList().map {
                enrichmentCollectionService.enrichCollection(
                    enrichmentCollections[it.id],
                    it,
                    emptyMap(),
                    CollectionMetaPipeline.API
                )
            }
        )
    }
}
