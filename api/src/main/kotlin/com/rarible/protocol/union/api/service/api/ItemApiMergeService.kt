package com.rarible.protocol.union.api.service.api

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.continuation.UnionItemContinuation
import com.rarible.protocol.union.core.converter.ItemOwnershipConverter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemWithOwnershipDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.ItemsWithOwnershipDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgPage
import com.rarible.protocol.union.dto.continuation.page.ArgPaging
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.subchains
import com.rarible.protocol.union.enrichment.service.query.ownership.OwnershipApiService
import com.rarible.protocol.union.enrichment.util.BlockchainFilter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Component

@Component
class ItemApiMergeService(
    private val itemEnrichService: ItemEnrichService,
    private val router: BlockchainRouter<ItemService>,
    private val ownershipApiService: OwnershipApiService,
) : ItemQueryService {

    private val logger by Logger()

    override suspend fun getAllItems(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): ItemsDto {
        val safeSize = PageSize.ITEM.limit(size)
        val slices = getAllItemsInner(blockchains, continuation, safeSize, showDeleted, lastUpdatedFrom, lastUpdatedTo)
        val total = slices.sumOf { it.page.total }
        val arg = ArgPaging(UnionItemContinuation.ByLastUpdatedAndId, slices.map { it.toSlice() }).getSlice(safeSize)

        logger.info("Response for getAllItems(blockchains={}, continuation={}, size={}):" +
            " Page(size={}, total={}, continuation={}) from blockchain pages {} ",
            blockchains, continuation, safeSize, arg.entities.size, total,
            arg.continuation, slices.map { it.page.entities.size }
        )
        return itemEnrichService.enrich(arg, total)
    }

    private suspend fun getAllItemsInner(
        blockchains: List<BlockchainDto>?,
        cursor: String?,
        safeSize: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): List<ArgPage<UnionItem>> {
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains).map(BlockchainDto::name)
        val slices = getItemsByBlockchains(cursor, evaluatedBlockchains) { blockchain, continuation ->
            val blockDto = BlockchainDto.valueOf(blockchain)
            router.getService(blockDto).getAllItems(continuation, safeSize, showDeleted, lastUpdatedFrom, lastUpdatedTo)
        }
        return slices
    }

    override suspend fun getItemsByCollection(
        collection: CollectionIdDto,
        continuation: String?,
        size: Int?
    ): ItemsDto {
        val safeSize = PageSize.ITEM.limit(size)
        val result = router.getService(collection.blockchain)
            .getItemsByCollection(collection.value, null, continuation, safeSize)

        logger.info(
            "Response for getItemsByCollection(collection={}, continuation={}, size={}):" +
                " Page(size={}, total={}, continuation={})",
            collection, continuation, size, result.entities.size, result.total, result.continuation
        )

        return itemEnrichService.enrich(result)
    }

    override suspend fun getItemsByCreator(
        creator: UnionAddress,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ItemsDto {
        val safeSize = PageSize.ITEM.limit(size)
        val filter = BlockchainFilter(blockchains)

        val blockchainPages = router.executeForAll(filter.exclude(creator.blockchainGroup)) {
            it.getItemsByCreator(creator.value, continuation, safeSize)
        }

        val total = blockchainPages.sumOf { it.total }

        val combinedPage = Paging(
            UnionItemContinuation.ByLastUpdatedAndId,
            blockchainPages.flatMap { it.entities }
        ).getPage(safeSize, total)

        logger.info(
            "Response for getItemsByCreator(creator={}, continuation={}, size={}):" +
                " Page(size={}, total={}, continuation={}) from blockchain pages {} ",
            creator, continuation, size, combinedPage.entities.size, combinedPage.total, combinedPage.continuation,
            blockchainPages.map { it.entities.size }
        )

        return itemEnrichService.enrich(combinedPage)
    }

    override suspend fun getItemsByOwner(
        owner: UnionAddress,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ItemsDto {
        val safeSize = PageSize.ITEM.limit(size)
        val filter = BlockchainFilter(blockchains)
        val blockchainPages = router.executeForAll(filter.exclude(owner.blockchainGroup)) {
            it.getItemsByOwner(owner.value, continuation, safeSize)
        }

        val total = blockchainPages.sumOf { it.total }

        val combinedPage = Paging(
            UnionItemContinuation.ByLastUpdatedAndId,
            blockchainPages.flatMap { it.entities }
        ).getPage(safeSize, total)

        logger.info(
            "Response for getItemsByOwner(owner={}, continuation={}, size={}):" +
                " Page(size={}, total={}, continuation={}) from blockchain pages {} ",
            owner, continuation, size, combinedPage.entities.size, combinedPage.total, combinedPage.continuation,
            blockchainPages.map { it.entities.size }
        )

        return itemEnrichService.enrich(combinedPage)
    }

    override suspend fun getItemsByIds(ids: List<ItemIdDto>): List<ItemDto> = coroutineScope {
        logger.info(
            "Getting {} items by IDs: first 100 [{}]",
            ids.size, ids.take(100).map { "${it.blockchain}:${it.value}" }
        )

        val groupedIds = ids.groupBy({ it.blockchain }, { it.value })
        val items = groupedIds.map {
            async { router.getService(it.key).getItemsByIds(it.value) }
        }.awaitAll().flatten()

        itemEnrichService.enrich(items)
    }

    override suspend fun getAllItemIdsByCollection(collectionId: CollectionIdDto): Flow<ItemIdDto> {
        val pageSize = PageSize.ITEM.max
        var continuation: String? = null
        var returned = 0L
        return flow {
            while (true) {
                val page = router.getService(collectionId.blockchain)
                    .getItemsByCollection(collectionId.value, null, continuation, pageSize)
                page.entities.forEach { emit(it.id) }
                continuation = page.continuation
                if (continuation == null || page.total == 0L) break
                returned += page.total
                check(returned < 1_000_000) { "Cyclic continuation $continuation for collection $collectionId" }
            }
        }
    }

    private suspend fun getItemsByBlockchains(
        continuation: String?,
        blockchains: Collection<String>,
        clientCall: suspend (blockchain: String, continuation: String?) -> Page<UnionItem>
    ): List<ArgPage<UnionItem>> {
        val currentContinuation = CombinedContinuation.parse(continuation)
        return coroutineScope {
            blockchains.map { blockchain ->
                async {
                    val blockchainContinuation = currentContinuation.continuations[blockchain]
                    // For completed blockchain we do not request orders
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

    override suspend fun getItemsByOwnerWithOwnership(
        owner: UnionAddress,
        continuation: String?,
        size: Int?
    ): ItemsWithOwnershipDto {
        val safeSize = PageSize.ITEM.limit(size)
        val page = ownershipApiService.getOwnershipByOwner(owner, continuation, safeSize)

        val ids = page.entities.map { it.id.getItemId().value }
        val items = router.executeForAll(owner.blockchainGroup.subchains()) {
            it.getItemsByIds(ids)
        }.flatten()

        val enrichedItems = itemEnrichService.enrich(items).associateBy { it.id }

        val wrapped = page.entities.mapNotNull {
            val item = enrichedItems[it.id.getItemId()]
            if (null != item) {
                ItemWithOwnershipDto(item, ItemOwnershipConverter.convert(it))
            } else {
                logger.warn("Item for ${it.id} ownership wasn't found")
                null
            }
        }

        return ItemsWithOwnershipDto(wrapped.size.toLong(), page.continuation, wrapped)
    }
}
