package com.rarible.protocol.union.core.flow.service

import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.flow.converter.FlowItemConverter
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.MetaDto
import com.rarible.protocol.union.dto.UnionItemDto
import kotlinx.coroutines.reactive.awaitFirst

class FlowItemService(
    blockchain: BlockchainDto,
    private val flowNftItemControllerApi: FlowNftItemControllerApi
) : AbstractBlockchainService(blockchain), ItemService {

    override suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): Page<UnionItemDto> {
        val items = flowNftItemControllerApi
            .getNftAllItems(continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo)
            .awaitFirst()
        return FlowItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemById(
        itemId: String
    ): UnionItemDto {
        val item = flowNftItemControllerApi.getNftItemById(itemId).awaitFirst()
        return FlowItemConverter.convert(item, blockchain)
    }

    override suspend fun getItemMetaById(itemId: String): MetaDto {
        // TODO: Flow will return a richer object (MetaDto) soon, and we should put 'attributes' and 'content' here.
        val meta = flowNftItemControllerApi.getNftItemMetaById(itemId).awaitFirst()
        return MetaDto(
            name = meta.title ?: "Flow #$itemId",
            description = meta.description,
            attributes = emptyList(),
            content = emptyList()
        )
    }

    override suspend fun resetItemMeta(itemId: String) {
        // TODO implement
    }

    override suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int
    ): Page<UnionItemDto> {
        val items = flowNftItemControllerApi.getNftItemsByCollection(collection, continuation, size).awaitFirst()
        return FlowItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int
    ): Page<UnionItemDto> {
        val items = flowNftItemControllerApi.getNftItemsByCreator(creator, continuation, size).awaitFirst()
        return FlowItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionItemDto> {
        val items = flowNftItemControllerApi.getNftItemsByOwner(owner, continuation, size).awaitFirst()
        return FlowItemConverter.convert(items, blockchain)
    }

}
