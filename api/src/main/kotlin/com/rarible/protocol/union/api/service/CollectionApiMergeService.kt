package com.rarible.protocol.union.api.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.api.service.api.OrderApiService
import com.rarible.protocol.union.api.service.elastic.CollectionElasticService
import com.rarible.protocol.union.api.util.BlockchainFilter
import com.rarible.protocol.union.core.continuation.UnionCollectionContinuation
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgPage
import com.rarible.protocol.union.dto.continuation.page.ArgPaging
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ShortCollection
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import com.rarible.protocol.union.enrichment.util.spent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component

@Component
@ConditionalOnMissingBean(CollectionElasticService::class)
class CollectionApiMergeService(
    private val orderApiService: OrderApiService,
    private val router: BlockchainRouter<CollectionService>,
    private val enrichmentCollectionService: EnrichmentCollectionService
): CollectionQueryService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getAllCollections(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
    ): CollectionsDto {
        val safeSize = PageSize.COLLECTION.limit(size)
        val slices = getAllCollections(blockchains, continuation, safeSize)
        val arg = ArgPaging(UnionCollectionContinuation.ById, slices.map { it.toSlice() }).getSlice(safeSize)
        val total = slices.sumOf { it.page.total }
        logger.info("Response for getAllCollections(blockchains={}, continuation={}, size={}):" +
                " Page(size={}, total={}, continuation={}) from blockchain pages {} ",
            blockchains, continuation, size, arg.entities.size, total,
            arg.continuation, slices.map { it.page.entities.size }
        )
        return enrich(arg, total)
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
    ): CollectionsDto {
        val safeSize = PageSize.COLLECTION.limit(size)
        val ownerAddress = IdParser.parseAddress(owner)
        val filter = BlockchainFilter(blockchains)
        val blockchainPages = router.executeForAll(filter.exclude(ownerAddress.blockchainGroup)) {
            it.getCollectionsByOwner(ownerAddress.value, continuation, safeSize)
        }

        val total = blockchainPages.sumOf { it.total }

        val combinedPage = Paging(
            UnionCollectionContinuation.ById,
            blockchainPages.flatMap { it.entities }
        ).getPage(safeSize, total)

        logger.info(
            "Response for getCollectionsByOwner(owner={}, continuation={}, size={}):" +
                    " Page(size={}, total={}, continuation={})",
            owner, continuation, size, combinedPage.entities.size, combinedPage.total, combinedPage.continuation
        )

        return enrich(combinedPage)
    }

    private suspend fun getAllCollections(
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
