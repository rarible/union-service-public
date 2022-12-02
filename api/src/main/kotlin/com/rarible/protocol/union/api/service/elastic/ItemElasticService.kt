package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.mapAsync
import com.rarible.core.common.nowMillis
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.api.service.api.ItemEnrichService
import com.rarible.protocol.union.api.service.api.ItemQueryService
import com.rarible.protocol.union.core.converter.ItemOwnershipConverter
import com.rarible.protocol.union.core.model.EsItemLite
import com.rarible.protocol.union.core.model.EsItemSort
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.EsOwnershipByOwnerFilter
import com.rarible.protocol.union.core.model.EsOwnershipSort
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
import com.rarible.protocol.union.dto.ItemsSearchRequestDto
import com.rarible.protocol.union.dto.ItemsSearchSortDto
import com.rarible.protocol.union.dto.ItemsWithOwnershipDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
@CaptureSpan(type = SpanType.APP)
class ItemElasticService(
    private val itemFilterConverter: ItemFilterConverter,
    private val esItemRepository: EsItemRepository,
    private val esOwnershipRepository: EsOwnershipRepository,
    private val ownershipElasticHelper: OwnershipElasticHelper,
    private val router: BlockchainRouter<ItemService>,
    private val itemEnrichService: ItemEnrichService,
) : ItemQueryService {

    companion object {

        private val logger by Logger()
    }

    override suspend fun getAllItems(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): ItemsDto {
        val requestId = Random.nextLong()
        log("Called getAllItems($blockchains, $continuation, $size)", requestId)

        val safeSize = PageSize.ITEM.limit(size)
        val slice = getAllItemsInner(blockchains, showDeleted, lastUpdatedFrom, lastUpdatedTo, continuation, safeSize, requestId)

        log("Response for getAllItemsInner(): " +
                "slice size=${slice.entities.size}, continuation=${slice.continuation}", requestId)

        val before = nowMillis().toEpochMilli()
        val enriched = itemEnrichService.enrich(slice.entities)
        log("Enrichment took ${nowMillis().minusMillis(before).toEpochMilli()} ms", requestId)

        return ItemsDto(
            items = enriched,
            continuation = slice.continuation
        )
    }

    override suspend fun getItemsByCollection(
        collection: CollectionIdDto,
        continuation: String?,
        size: Int?
    ): ItemsDto {
        val safeSize = PageSize.ITEM.limit(size)

        val filter = itemFilterConverter.getItemsByCollection(collection.fullId(), continuation)
        logger.info("Built filter: $filter")
        val queryResult = esItemRepository.search(filter, EsItemSort.DEFAULT, safeSize)
        val items = getItems(queryResult.entities, null)
        val cursor = queryResult.continuation

        logger.info(
            "Response for ES getItemsByCollection(collection={}, continuation={}, size={}):" +
                " Page(size={}, continuation={})",
            collection, continuation, size, items.size, cursor
        )
        val enriched = itemEnrichService.enrich(items)

        return ItemsDto(
            items = enriched,
            continuation = cursor
        )
    }

    override suspend fun getAllItemIdsByCollection(collectionId: CollectionIdDto): Flow<ItemIdDto> {
        val pageSize = PageSize.ITEM.max
        var cursor: String? = null
        var returned = 0L
        return flow {
            while (true) {
                val filter = itemFilterConverter.getAllItemIdsByCollection(collectionId.fullId(), cursor.toString())
                logger.info("Built filter: $filter")
                val queryResult = esItemRepository.search(filter, EsItemSort.DEFAULT, pageSize)
                logger.info(
                    "getAllItemIdsByCollection ES Query result:" +
                        " size=${queryResult.entities.size}, continuation=${queryResult.continuation}"
                )
                queryResult.entities.forEach { emit(IdParser.parseItemId(it.itemId)) }
                cursor = queryResult.continuation
                if (cursor == null) break
                returned += queryResult.entities.size
                check(returned < 1_000_000) { "Cyclic continuation $cursor for collection $collectionId" }
            }
        }
    }

    override suspend fun getItemsByCreator(
        creator: UnionAddress,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ItemsDto {
        val safeSize = PageSize.ITEM.limit(size)

        val filter = itemFilterConverter.getItemsByCreator(creator.fullId(), continuation)
        logger.info("Built filter: $filter")
        val queryResult = esItemRepository.search(filter, EsItemSort.DEFAULT, safeSize)
        val cursor = queryResult.continuation

        if (queryResult.entities.isEmpty()) return ItemsDto()
        val items = getItems(queryResult.entities, null)
        val enriched = itemEnrichService.enrich(items)

        logger.info(
            "Response for ES getItemsByCreator(creator={}, continuation={}, size={}): Page(size={}, continuation={})",
            creator, continuation, size, items.size, cursor
        )

        return ItemsDto(
            items = enriched,
            continuation = cursor
        )
    }

    override suspend fun getItemsByOwner(
        owner: UnionAddress,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ItemsDto {
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains)
        val ownerships = esOwnershipRepository.search(
            EsOwnershipByOwnerFilter(
                owner = owner,
                blockchains = evaluatedBlockchains.toSet(),
                cursor = continuation,
            ),
            EsOwnershipSort.DEFAULT,
            size,
        )

        val cursor = ownerships.continuation
        val items: List<UnionItem> = getItemsByOwnerships(ownerships.entities)

        val enriched = itemEnrichService.enrich(items)

        logger.info(
            "Response for ES getItemsByOwner(creator={}, continuation={}, size={}): Page(size={}, continuation={})",
            owner.fullId(), continuation, size, items.size, cursor
        )

        return ItemsDto(
            items = enriched,
            continuation = cursor
        )
    }

    override suspend fun getItemsByOwnerWithOwnership(
        owner: UnionAddress,
        continuation: String?,
        size: Int?
    ): ItemsWithOwnershipDto {
        val safeSize = PageSize.OWNERSHIP.limit(size)
        val (cursor, ownerships) = ownershipElasticHelper.getRawOwnershipsByOwner(
            owner = owner,
            continuation = continuation,
            size = safeSize,
        )
        val resultOwnerships = ownerships.map { ItemOwnershipConverter.convert(it) }

        val items: List<UnionItem> = getItemsByIdsInner(resultOwnerships.map { it.id.getItemId().fullId() }, null)
        val enriched = itemEnrichService.enrich(items)

        val result = resultOwnerships.mapNotNull { ownership ->
            val item = enriched.firstOrNull { it.id.fullId() == ownership.id.getItemId().fullId() }
                ?: return@mapNotNull null

            ItemWithOwnershipDto(
                item = item,
                ownership = ownership
            )
        }

        logger.info(
            "Response for ES getItemsByOwnerWithOwnership(owner={}, continuation={}, size={}):" +
                " Page(size={}, continuation={})",
            owner, continuation, size, items.size, cursor
        )

        return ItemsWithOwnershipDto(
            items = result,
            continuation = cursor
        )
    }

    override suspend fun getItemsByIds(ids: List<ItemIdDto>): List<ItemDto> {
        throw NotImplementedError()
    }

    suspend fun searchItems(request: ItemsSearchRequestDto): ItemsDto {
        val filter = itemFilterConverter.searchItems(request.filter, request.continuation)
        val sort = convertSort(request.sort)
        val result = esItemRepository.search(filter, sort, request.size)
        if (result.entities.isEmpty()) return ItemsDto()
        val items = getItems(result.entities, null)
        val enriched = itemEnrichService.enrich(items)
        return ItemsDto(
            continuation = result.continuation,
            items = enriched
        )
    }

    private suspend fun getItems(esItems: List<EsItemLite>, requestId: Long?): List<UnionItem> {
        val mapping = hashMapOf<BlockchainDto, MutableList<String>>()

        esItems.forEach { item ->
            mapping
                .computeIfAbsent(item.blockchain) { ArrayList(esItems.size) }
                .add(IdParser.parseItemId(item.itemId).value)
        }

        val items = getItemsFromBlockchains(mapping, requestId)
        val itemsIdMapping = items.associateBy { it.id.fullId() }

        return esItems.mapNotNull { esItem ->
            itemsIdMapping[esItem.itemId]
        }
    }

    private suspend fun getItemsByOwnerships(ownerships: List<EsOwnership>): List<UnionItem> {
        return getItemsByIdsInner(ownerships.mapNotNull { it.itemId }, null)
    }

    private suspend fun getItemsByIdsInner(itemIds: List<String>, requestId: Long?): List<UnionItem> {
        val mapping = hashMapOf<BlockchainDto, MutableList<String>>()

        itemIds
            .forEach { itemId ->
                val itemIdDto = IdParser.parseItemId(itemId)
                mapping
                    .computeIfAbsent(itemIdDto.blockchain) { ArrayList(itemIds.size) }
                    .add(itemIdDto.value)
            }

        val items = getItemsFromBlockchains(mapping, requestId)
        val itemsIdMapping = items.associateBy { it.id.fullId() }

        return itemIds.mapNotNull {
            itemsIdMapping[it]
        }
    }

    private suspend fun getAllItemsInner(
        blockchains: List<BlockchainDto>?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?,
        continuation: String?,
        size: Int?,
        requestId: Long,
    ): Slice<UnionItem> {
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains).map { it.name }.toSet()

        val filter = itemFilterConverter.convertGetAllItems(
            evaluatedBlockchains, showDeleted, lastUpdatedFrom, lastUpdatedTo, continuation
        )
        log("Built filter: $filter", requestId)
        val queryResult = esItemRepository.search(filter, EsItemSort.DEFAULT, size)
        log("Got ${queryResult.entities.size} ES entities", requestId)
        val items = getItems(queryResult.entities, requestId)
        log("Got ${items.size} items", requestId)
        return Slice(
            entities = items,
            continuation = queryResult.continuation
        )
    }

    private suspend fun getItemsFromBlockchains(
        itemsPerBlockchain: Map<BlockchainDto, MutableList<String>>,
        requestId: Long?
    ): List<UnionItem> {
        logger.debug("Getting ${itemsPerBlockchain.size} items from blockchains")
        val items = itemsPerBlockchain.mapAsync { element ->
            val blockchain = element.key
            val ids = element.value
            val isBlockchainEnabled = router.isBlockchainEnabled(blockchain)
            if (isBlockchainEnabled) {
                    log("Requested ${ids.size} items from $blockchain", requestId)
                router.getService(blockchain).getItemsByIds(ids)
                    .also { log("Returned ${it.size} of ${ids.size} items from $blockchain", requestId) }
            } else emptyList()
        }.flatten()

        return items
    }

    private fun convertSort(sort: ItemsSearchSortDto?): EsItemSort {
        if (sort == null) return EsItemSort.DEFAULT
        return when (sort) {
            ItemsSearchSortDto.LATEST -> EsItemSort.LATEST_FIRST
            ItemsSearchSortDto.EARLIEST -> EsItemSort.EARLIEST_FIRST
            ItemsSearchSortDto.HIGHEST_SELL -> EsItemSort.HIGHEST_SELL_PRICE_FIRST
            ItemsSearchSortDto.LOWEST_SELL -> EsItemSort.LOWEST_SELL_PRICE_FIRST
            ItemsSearchSortDto.HIGHEST_BID -> EsItemSort.HIGHEST_BID_PRICE_FIRST
            ItemsSearchSortDto.LOWEST_BID -> EsItemSort.LOWEST_BID_PRICE_FIRST
        }
    }

    private fun log(message: String, requestId: Long?) {
        if (requestId != null) {
            logger.info("[requestId=$requestId] $message")
        } else {
            logger.info(message)
        }
    }
}
