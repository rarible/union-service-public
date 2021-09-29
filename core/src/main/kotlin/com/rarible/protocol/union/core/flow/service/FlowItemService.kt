package com.rarible.protocol.union.core.flow.service

import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.union.core.continuation.Page
import com.rarible.protocol.union.core.flow.converter.FlowItemConverter
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionItemDto
import kotlinx.coroutines.reactive.awaitFirst

class FlowItemService(
    blockchain: BlockchainDto,
    private val flowNftItemControllerApi: FlowNftItemControllerApi
) : AbstractFlowService(blockchain), ItemService {

    override suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?, //TODO not supported by Flow
        lastUpdatedTo: Long? //TODO not supported by Flow
    ): Page<UnionItemDto> {
        val items = flowNftItemControllerApi.getNftAllItems(continuation, size, showDeleted).awaitFirst()
        return FlowItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemById(
        itemId: String
    ): UnionItemDto {
        val item = flowNftItemControllerApi.getNftItemById(itemId).awaitFirst()
        return FlowItemConverter.convert(item, blockchain)
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