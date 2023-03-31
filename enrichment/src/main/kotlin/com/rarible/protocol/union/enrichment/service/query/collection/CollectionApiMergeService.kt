package com.rarible.protocol.union.enrichment.service.query.collection

import com.rarible.protocol.union.core.continuation.UnionCollectionContinuation
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgPage
import com.rarible.protocol.union.dto.continuation.page.ArgPaging
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import com.rarible.protocol.union.enrichment.util.BlockchainFilter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CollectionApiMergeService(
    private val router: BlockchainRouter<CollectionService>,
    private val enrichmentCollectionService: EnrichmentCollectionService
) : CollectionQueryService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getAllCollections(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
    ): CollectionsDto {
        val safeSize = PageSize.COLLECTION.limit(size)
        val slices = getAllCollectionsSafe(blockchains, continuation, safeSize)
        val arg = ArgPaging(UnionCollectionContinuation.ById, slices.map { it.toSlice() }).getSlice(safeSize)
        val total = slices.sumOf { it.page.total }
        logger.info("Response for getAllCollections(blockchains={}, continuation={}, size={}):" +
            " Slice(size={}, continuation={}) from blockchain pages {} ",
            blockchains, continuation, size, arg.entities.size, arg.continuation, slices.map { it.page.entities.size }
        )
        return enrich(arg, total)
    }

    override suspend fun getCollectionsByOwner(
        owner: UnionAddress,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
    ): CollectionsDto {
        val safeSize = PageSize.COLLECTION.limit(size)
        val filter = BlockchainFilter(blockchains)
        val blockchainPages = router.executeForAll(filter.exclude(owner.blockchainGroup)) {
            it.getCollectionsByOwner(owner.value, continuation, safeSize)
        }

        val total = blockchainPages.sumOf { it.total }

        val combinedPage = Paging(
            UnionCollectionContinuation.ById,
            blockchainPages.flatMap { it.entities }
        ).getPage(safeSize, total)

        logger.info(
            "Response for getCollectionsByOwner(owner={}, continuation={}, size={}): Slice(size={}, continuation={})",
            owner.fullId(), continuation, size, combinedPage.entities.size, combinedPage.continuation
        )

        return enrich(combinedPage)
    }

    private suspend fun getAllCollectionsSafe(
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

    suspend fun enrich(page: Page<UnionCollection>): CollectionsDto {
        return CollectionsDto(
            total = page.total,
            continuation = page.continuation,
            collections = enrichmentCollectionService.enrichUnionCollections(page.entities, CollectionMetaPipeline.API)
        )
    }

    suspend fun enrich(slice: Slice<UnionCollection>, total: Long): CollectionsDto {
        return CollectionsDto(
            total = total,
            continuation = slice.continuation,
            collections = enrichmentCollectionService.enrichUnionCollections(slice.entities, CollectionMetaPipeline.API)
        )
    }
}
