package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.model.elastic.EsCollectionCursor.Companion.fromCollectionLite
import com.rarible.protocol.union.core.model.elastic.EsCollectionGenericFilter
import com.rarible.protocol.union.core.model.elastic.EsCollectionLite
import com.rarible.protocol.union.core.model.elastic.EsCollectionTextFilter
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.dto.CollectionsSearchRequestDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import com.rarible.protocol.union.enrichment.service.query.collection.CollectionQueryService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@CaptureSpan(type = SpanType.APP)
class CollectionElasticService(
    private val repository: EsCollectionRepository,
    private val router: BlockchainRouter<CollectionService>,
    private val enrichmentCollectionService: EnrichmentCollectionService,
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

    suspend fun searchCollections(request: CollectionsSearchRequestDto): CollectionsDto {
        val blockchains = router.getEnabledBlockchains(request.filter.blockchains).toList()
        val filter = EsCollectionTextFilter(
            blockchains = blockchains.toSet(),
            text = request.filter.text,
            cursor = request.continuation,
        )
        val result = repository.search(filter, request.size)
        return processResult(result)
    }

    private suspend fun processResult(result: List<EsCollectionLite>): CollectionsDto {
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
}
