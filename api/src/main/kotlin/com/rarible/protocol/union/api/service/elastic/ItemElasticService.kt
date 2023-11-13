package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.common.asyncWithTraceId
import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.api.service.api.ItemEnrichService
import com.rarible.protocol.union.api.service.api.ItemQueryService
import com.rarible.protocol.union.core.converter.ItemOwnershipConverter
import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.elastic.EsItemFilter
import com.rarible.protocol.union.core.model.elastic.EsItemLite
import com.rarible.protocol.union.core.model.elastic.EsItemSort
import com.rarible.protocol.union.core.model.elastic.EsItemSortType
import com.rarible.protocol.union.core.model.elastic.EsOwnership
import com.rarible.protocol.union.core.model.elastic.EsOwnershipByOwnerFilter
import com.rarible.protocol.union.core.model.elastic.EsOwnershipSort
import com.rarible.protocol.union.core.model.elastic.SortType
import com.rarible.protocol.union.core.model.elastic.TraitSort
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
import com.rarible.protocol.union.dto.SortOrderDto
import com.rarible.protocol.union.dto.SortTypeDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.dto.subchains
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.elasticsearch.search.sort.SortOrder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ItemElasticService(
    private val itemFilterConverter: ItemFilterConverter,
    private val esItemOptimizedSearchService: EsItemOptimizedSearchService,
    private val esOwnershipRepository: EsOwnershipRepository,
    private val ownershipElasticHelper: OwnershipElasticHelper,
    private val router: BlockchainRouter<ItemService>,
    private val itemEnrichService: ItemEnrichService,
) : ItemQueryService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getAllItems(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): ItemsDto {
        logger.info("Called getAllItems($blockchains, $continuation, $size)")

        val safeSize = PageSize.ITEM.limit(size)
        val slice = getAllItemsInner(
            blockchains, showDeleted, lastUpdatedFrom, lastUpdatedTo, continuation, safeSize
        )

        logger.info(
            "Response for getAllItemsInner(): " +
                "slice size=${slice.entities.size}, continuation=${slice.continuation}"
        )

        val before = nowMillis().toEpochMilli()
        val enriched = itemEnrichService.enrich(slice.entities)
        logger.info("Enrichment took ${nowMillis().minusMillis(before).toEpochMilli()} ms")

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
        if (!router.isBlockchainEnabled(collection.blockchain)) {
            logger.info("Unable to find enabled blockchains for getItemsByCollection() where collection={}", collection)
            return ItemsDto()
        }

        val start = System.currentTimeMillis()

        val filter = itemFilterConverter.getItemsByCollection(collection.fullId(), continuation)
        logger.info("Built filter: $filter")
        val queryResult = search(filter, EsItemSort.DEFAULT, safeSize)
        val items = getItems(queryResult.entities)
        val cursor = queryResult.continuation

        val searchTime = System.currentTimeMillis()

        val enriched = itemEnrichService.enrich(items)

        val end = System.currentTimeMillis()
        logger.info(
            "Response for ES getItemsByCollection(collection={}, continuation={}, size={}):" +
                " Page(size={}, continuation={}) Performance(searchTime={}, enrichTime={})",
            collection, continuation, size, items.size, cursor, searchTime - start, end - searchTime
        )

        return ItemsDto(
            items = enriched,
            continuation = cursor
        )
    }

    override suspend fun getAllItemIdsByCollection(collectionId: CollectionIdDto): Flow<ItemIdDto> {
        if (!router.isBlockchainEnabled(collectionId.blockchain)) {
            return emptyFlow()
        }

        val pageSize = PageSize.ITEM.max
        var cursor: String? = null
        var returned = 0L
        return flow {
            while (true) {
                val filter = itemFilterConverter.getAllItemIdsByCollection(collectionId.fullId(), cursor.toString())
                logger.info("Built filter: $filter")
                val queryResult = search(filter, EsItemSort.DEFAULT, pageSize)
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
        val enabledBlockchains = router.getEnabledBlockchains(blockchains).map { it.name }.toSet()
        if (enabledBlockchains.isEmpty()) {
            logger.info(
                "Unable to find enabled blockchains for getItemsByCreator() where creator={} and blockchains={}",
                creator, blockchains
            )
            return ItemsDto()
        }

        val filter = itemFilterConverter.getItemsByCreator(creator.fullId(), enabledBlockchains, continuation)
        logger.info("Built filter: $filter")
        val queryResult = search(filter, EsItemSort.DEFAULT, safeSize)
        val cursor = queryResult.continuation

        if (queryResult.entities.isEmpty()) return ItemsDto()
        val items = getItems(queryResult.entities)
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
        if (evaluatedBlockchains.isEmpty()) {
            logger.info(
                "Unable to find enabled blockchains for getItemsByOwner() where owner={} and blockchains={}",
                owner, blockchains
            )
            return ItemsDto()
        }

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
        val enabledBlockchains = router.getEnabledBlockchains(owner.blockchainGroup.subchains()).toSet()
        if (enabledBlockchains.isEmpty()) {
            logger.info("Unable to find enabled blockchains for getItemsByOwnerWithOwnership() where owner={}", owner)
            return ItemsWithOwnershipDto()
        }
        val safeSize = PageSize.OWNERSHIP.limit(size)
        val (cursor, ownerships) = ownershipElasticHelper.getRawOwnershipsByOwner(
            blockchains = enabledBlockchains,
            owner = owner,
            continuation = continuation,
            size = safeSize,
        )
        val resultOwnerships = ownerships.map { ItemOwnershipConverter.convert(it) }

        val items: List<UnionItem> = getItemsByIdsInner(resultOwnerships.map { it.id.getItemId().fullId() })
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
        val sort = convertSort(request)
        val result = search(filter, sort, request.size)
        if (result.entities.isEmpty()) return ItemsDto()
        val items = getItems(result.entities)
        val enriched = itemEnrichService.enrich(items)
        return ItemsDto(
            continuation = result.continuation,
            items = enriched
        )
    }

    private suspend fun getItems(esItems: List<EsItemLite>): List<UnionItem> {
        val mapping = esItems.groupBy(
            { it.blockchain },
            { IdParser.parseItemId(it.itemId).value }
        )

        val items = getItemsFromBlockchains(mapping)
        val itemsIdMapping = items.associateBy { it.id.fullId() }

        return esItems.mapNotNull { esItem ->
            itemsIdMapping[esItem.itemId]
        }
    }

    private suspend fun getItemsByOwnerships(ownerships: List<EsOwnership>): List<UnionItem> {
        return getItemsByIdsInner(ownerships.mapNotNull { it.itemId })
    }

    private suspend fun getItemsByIdsInner(itemIds: List<String>): List<UnionItem> {
        val mapping = itemIds.map(IdParser::parseItemId).groupBy(
            { it.blockchain },
            { it.value }
        )

        val items = getItemsFromBlockchains(mapping)
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
    ): Slice<UnionItem> {
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains).map { it.name }.toSet()
        if (evaluatedBlockchains.isEmpty()) {
            return Slice.empty()
        }

        val filter = itemFilterConverter.convertGetAllItems(
            evaluatedBlockchains, showDeleted, lastUpdatedFrom, lastUpdatedTo, continuation
        )
        logger.info("Built filter: $filter")
        val queryResult = search(filter, EsItemSort.DEFAULT, size)
        logger.info("Got ${queryResult.entities.size} ES entities")
        val items = getItems(queryResult.entities)
        logger.info("Got ${items.size} items")
        return Slice(
            entities = items,
            continuation = queryResult.continuation
        )
    }

    private suspend fun getItemsFromBlockchains(
        itemsPerBlockchain: Map<BlockchainDto, List<String>>,
    ): List<UnionItem> {
        logger.debug("Getting ${itemsPerBlockchain.size} items from blockchains")

        val items = coroutineScope {
            itemsPerBlockchain.map { element ->
                asyncWithTraceId(context = NonCancellable) {
                    val blockchain = element.key
                    val ids = element.value
                    val isBlockchainEnabled = router.isBlockchainEnabled(blockchain)
                    if (isBlockchainEnabled) {
                        logger.info("Requested ${ids.size} items from $blockchain")
                        router.getService(blockchain).getItemsByIds(ids)
                            .also { logger.info("Returned ${it.size} of ${ids.size} items from $blockchain") }
                    } else emptyList()
                }
            }.awaitAll()
        }.flatten()

        return items
    }

    suspend fun search(filter: EsItemFilter, sort: EsItemSort, limit: Int?): Slice<EsItemLite> {
        return esItemOptimizedSearchService.search(filter, sort, limit)
    }

    private fun convertSort(request: ItemsSearchRequestDto): EsItemSort {
        val sort = request.sort ?: return EsItemSort.DEFAULT
        return when (sort) {
            ItemsSearchSortDto.RELEVANCE -> EsItemSort(type = EsItemSortType.RELEVANCE)
            ItemsSearchSortDto.LATEST -> EsItemSort(type = EsItemSortType.LATEST_FIRST)
            ItemsSearchSortDto.EARLIEST -> EsItemSort(type = EsItemSortType.EARLIEST_FIRST)
            ItemsSearchSortDto.HIGHEST_SELL -> EsItemSort(type = EsItemSortType.HIGHEST_SELL_PRICE_FIRST)
            ItemsSearchSortDto.LOWEST_SELL -> EsItemSort(type = EsItemSortType.LOWEST_SELL_PRICE_FIRST)
            ItemsSearchSortDto.HIGHEST_BID -> EsItemSort(type = EsItemSortType.HIGHEST_BID_PRICE_FIRST)
            ItemsSearchSortDto.LOWEST_BID -> EsItemSort(type = EsItemSortType.LOWEST_BID_PRICE_FIRST)
            ItemsSearchSortDto.TRAIT -> {
                val traitSort =
                    request.traitSort ?: throw UnionValidationException("traitSort is required when sort  = TRAIT")
                if (request.filter.collections.isNullOrEmpty()) {
                    throw UnionValidationException("collections is required when sort = TRAIT")
                }
                EsItemSort(
                    type = EsItemSortType.TRAIT,
                    traitSort = TraitSort(
                        key = traitSort.key,
                        sortType = traitSort.type?.toSortType() ?: SortType.TEXT,
                        sortOrder = traitSort.order?.toSortOrder() ?: SortOrder.ASC,
                    )
                )
            }
        }
    }

    private fun SortTypeDto.toSortType(): SortType = when (this) {
        SortTypeDto.NUMERIC -> SortType.NUMERIC
        SortTypeDto.TEXT -> SortType.TEXT
    }

    private fun SortOrderDto.toSortOrder(): SortOrder = when (this) {
        SortOrderDto.ASC -> SortOrder.ASC
        SortOrderDto.DESC -> SortOrder.DESC
    }
}
