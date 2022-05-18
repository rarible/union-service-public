package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.api.service.CollectionQueryService
import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.core.model.EsCollectionFilterAll
import com.rarible.protocol.union.core.model.EsCollectionFilterByOwner
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
@ConditionalOnProperty(
    value = ["enableCollectionQueriesToElastic"],
    prefix = "common.feature-flags",
)
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
        val result = repository.findByFilter(EsCollectionFilterAll(
            blockchains = blockchains?.toSet().orEmpty(),
            cursor = continuation,
            size = PageSize.COLLECTION.limit(size)
        ))
        return processResult(result)
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
    ): CollectionsDto {
        val result = repository.findByFilter(EsCollectionFilterByOwner(
            blockchains = blockchains?.toSet().orEmpty(),
            owner = owner,
            cursor = continuation,
            size = PageSize.COLLECTION.limit(size)
        ))
        return processResult(result)
    }

    private suspend fun processResult(result: List<EsCollection>): CollectionsDto {
        val seq = mutableMapOf<String, Int>()

        result.forEachIndexed { index, esCollection ->
            seq[IdParser.parseCollectionId(esCollection.collectionId).fullId()] = index
        }

        val collections = seq.keys.groupBy { IdParser.parseBlockchain(it) }.map { (k, v) ->
            withContext(Dispatchers.IO) {
                async {
                    router.getService(k).getCollectionsByIds(v)
                }
            }
        }.awaitAll().map { it.entities }.flatten().associateBy { seq[it.id.fullId()] }.toSortedMap(compareBy { it })

        return CollectionsDto(
            total = result.size.toLong(),
            continuation = if(result.isEmpty()) null else result.last().collectionId,
            collections = collections.values.toList().map {
                val shortCollection = enrichmentCollectionService.get(ShortCollectionId(it.id))
                enrichmentCollectionService.enrichCollection(shortCollection, it)
            }
        )
    }
}
