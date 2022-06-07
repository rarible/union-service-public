package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.core.model.EsCollectionCursor.Companion.fromCollectionLite
import com.rarible.protocol.union.enrichment.service.query.collection.CollectionQueryService
import com.rarible.protocol.union.core.model.EsCollectionGenericFilter
import com.rarible.protocol.union.core.model.EsCollectionLite
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import org.springframework.stereotype.Service

@Service
@CaptureSpan(type = SpanType.APP)
class CollectionElasticService(
    private val repository: EsCollectionRepository,
    private val router: BlockchainRouter<CollectionService>,
    private val enrichmentCollectionService: EnrichmentCollectionService
): CollectionQueryService {
    override suspend fun getAllCollections(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
    ): CollectionsDto {
        val filter = EsCollectionGenericFilter(
            blockchains = blockchains?.toSet().orEmpty(),
            cursor = continuation,
        )
        val result = repository.search(filter, size)

        return processResult(result)
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
    ): CollectionsDto {
        val filter = EsCollectionGenericFilter(
            blockchains = blockchains?.toSet().orEmpty(),
            owners = setOf(owner),
            cursor = continuation,
        )
        val result = repository.search(filter, size)

        return processResult(result)
    }

    private suspend fun processResult(result: List<EsCollectionLite>): CollectionsDto {
        val seq = mutableMapOf<String, Int>()
        val enabledBlockchains = router.getEnabledBlockchains(null)

        result.forEachIndexed { index, esCollection ->
            seq[IdParser.parseCollectionId(esCollection.collectionId).fullId()] = index
        }

        val collections = seq.keys.groupBy { IdParser.extractBlockchain(it).first }
            .filter { enabledBlockchains.contains(it.key) }
            .mapAsync { (k, v) ->
                val ids = v.map { IdParser.parseCollectionId(it).value }
                router.getService(k).getCollectionsByIds(ids)
            }
        .flatten().associateBy { seq[it.id.fullId()] }.toSortedMap(compareBy { it })

        val cursor = if (result.isEmpty()) null else result.last().fromCollectionLite()

        return CollectionsDto(
            total = result.size.toLong(),
            continuation = cursor.toString(),
            collections = collections.values.toList().map {
                val shortCollection = enrichmentCollectionService.get(ShortCollectionId(it.id))
                enrichmentCollectionService.enrichCollection(shortCollection, it)
            }
        )
    }
}
