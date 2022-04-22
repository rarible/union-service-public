package com.rarible.protocol.union.api.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.api.service.api.OrderApiService
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgPage
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.model.ShortCollection
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import com.rarible.protocol.union.enrichment.util.spent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CollectionApiService(
    private val orderApiService: OrderApiService,
    private val router: BlockchainRouter<CollectionService>,
    private val enrichmentCollectionService: EnrichmentCollectionService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getAllCollections(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        safeSize: Int
    ): List<ArgPage<UnionCollection>> {
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains).map(BlockchainDto::name)
        val slices = getCollectionsByBlockchains(continuation, evaluatedBlockchains) { blockchain, continuation ->
            val blockDto = BlockchainDto.valueOf(blockchain)
            router.getService(blockDto).getAllCollections(continuation, safeSize)
        }
        return slices
    }

    private suspend fun getCollectionsByBlockchains(
        continuation: String?,
        blockchains: Collection<String>,
        clientCall: suspend (blockchain: String, continuation: String?) -> Page<UnionCollection>
    ): List<ArgPage<UnionCollection>> {
        val currentContinuation = CombinedContinuation.parse(continuation)
        return coroutineScope {
            blockchains.map { blockchain ->
                async {
                    val blockchainContinuation = currentContinuation.continuations[blockchain]
                    // For completed blockchain we do not request collections
                    if (blockchainContinuation == ArgSlice.COMPLETED) {
                        ArgPage(blockchain, blockchainContinuation, Page(0, null, emptyList()))
                    } else {
                        ArgPage(
                            blockchain,
                            blockchainContinuation,
                            clientCall.invoke(blockchain, blockchainContinuation)
                        )
                    }
                }
            }
        }.awaitAll()
    }

    suspend fun enrich(unionCollectionsPage: Page<UnionCollection>): CollectionsDto {
        return CollectionsDto(
            total = unionCollectionsPage.total,
            continuation = unionCollectionsPage.continuation,
            collections = enrich(unionCollectionsPage.entities)
        )
    }

    suspend fun enrich(unionCollectionsSlice: Slice<UnionCollection>, total: Long): CollectionsDto {
        return CollectionsDto(
            total = total,
            continuation = unionCollectionsSlice.continuation,
            collections = enrich(unionCollectionsSlice.entities)
        )
    }


    private suspend fun enrich(unionCollections: List<UnionCollection>): List<CollectionDto> {
        if (unionCollections.isEmpty()) {
            return emptyList()
        }
        val now = nowMillis()
        val shortCollections: Map<CollectionIdDto, ShortCollection> = enrichmentCollectionService
            .findAll(unionCollections.map { ShortCollectionId(it.id) })
            .associateBy { it.id.toDto() }

        val shortOrderIds = shortCollections.values
            .map { listOfNotNull(it.bestBidOrder?.dtoId, it.bestSellOrder?.dtoId) }
            .flatten()

        val orders = orderApiService.getByIds(shortOrderIds)
            .associateBy { it.id }

        val enrichedCollections = unionCollections.map {
            val shortCollection = shortCollections[it.id]
            enrichmentCollectionService.enrichCollection(
                shortCollection = shortCollection,
                collection = it,
                orders = orders
            )
        }

        logger.info(
            "Enriched {} of {} Collections, {} Orders fetched ({}ms)",
            shortCollections.size, enrichedCollections.size, orders.size, spent(now)
        )

        return enrichedCollections
    }

    suspend fun enrich(unionCollection: UnionCollection): CollectionDto {
        val shortId = ShortCollectionId(unionCollection.id)
        val shortCollection = enrichmentCollectionService.get(shortId)
        return enrichmentCollectionService.enrichCollection(shortCollection, unionCollection)
    }
}
