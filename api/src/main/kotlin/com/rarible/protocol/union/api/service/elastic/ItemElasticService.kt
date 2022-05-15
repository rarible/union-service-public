package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.mapAsync
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.api.service.ItemQueryService
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemSort
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgPage
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
@CaptureSpan(type = SpanType.APP)
class ItemElasticService(
    private val itemFilterConverter: ItemFilterConverter,
    private val esItemRepository: EsItemRepository,
    private val router: BlockchainRouter<ItemService>
) : ItemQueryService {

    companion object {
        private val logger by Logger()
    }

    override suspend fun getAllItems(
        blockchains: List<BlockchainDto>?,
        cursor: String?,
        safeSize: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): List<ArgPage<UnionItem>> {
        logger.info("getAllActivities() from ElasticSearch")
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains).map { it.name }.toSet()

        val сontinuation = if (cursor != null) {
            val currentContinuation = CombinedContinuation.parse(cursor)
            val entry = currentContinuation.continuations.entries.first()
            val dateIdContinuation = DateIdContinuation.parse(entry.value)
            ItemIdDto(BlockchainDto.valueOf(entry.key), dateIdContinuation!!.id).toString()
        } else null

        val filter = itemFilterConverter.convertGetAllItems(
            evaluatedBlockchains, showDeleted, lastUpdatedFrom, lastUpdatedTo, сontinuation
        )
        logger.info("Built filter: $filter")
        val queryResult = esItemRepository.search(filter, EsItemSort.DEFAULT, safeSize)
        logger.info("Query result: $queryResult")
        return getItems(queryResult.items, queryResult.continuation)
    }

    private suspend fun getItems(esItems: List<EsItem>, continuation: String?): List<ArgPage<UnionItem>> {
        if (esItems.isEmpty()) return emptyList()
        val mapping = hashMapOf<BlockchainDto, MutableList<String>>()

        esItems.forEach { item ->
            mapping
                .computeIfAbsent(item.blockchain) { ArrayList(esItems.size) }
                .add(item.itemId)
        }
        val items = mapping.mapAsync { element ->
            val blockchain = element.key
            val ids = element.value
            val isBlockchainEnabled = router.isBlockchainEnabled(blockchain)
            if (isBlockchainEnabled) {
                val page = router.getService(blockchain).getItemsByIds(ids)
                ArgPage(
                    blockchain.name,
                    "",
                    Page(0, continuation, page)
                )
            } else ArgPage(blockchain.name, null, Page(0, null, emptyList()))
        }

        return items
    }

    override suspend fun getAllItemIdsByCollection(collectionId: CollectionIdDto): Flow<ItemIdDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getItemsByIds(ids: List<ItemIdDto>): List<ItemDto> {
        throw NotImplementedError()
    }

    override suspend fun enrich(unionItemsPage: Page<UnionItem>): ItemsDto {
        throw NotImplementedError()
    }

    override suspend fun enrich(unionItemsSlice: Slice<UnionItem>, total: Long): ItemsDto {
        throw NotImplementedError()
    }

    override suspend fun enrich(unionItem: UnionItem): ItemDto {
        throw NotImplementedError()
    }
}