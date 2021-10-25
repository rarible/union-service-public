package com.rarible.protocol.union.integration.flow.service

import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.flow.converter.FlowItemConverter
import kotlinx.coroutines.reactive.awaitFirst

class FlowItemService(
    private val flowNftItemControllerApi: FlowNftItemControllerApi
) : AbstractBlockchainService(BlockchainDto.FLOW), ItemService {

    override suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): Page<UnionItem> {
        val items = flowNftItemControllerApi
            .getNftAllItems(continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo)
            .awaitFirst()
        return FlowItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemById(itemId: String): UnionItem {
        val item = flowNftItemControllerApi.getNftItemById(itemId).awaitFirst()
        return FlowItemConverter.convert(item, blockchain)
    }

    override suspend fun getItemMetaById(itemId: String): UnionMeta {
        // TODO FLOW will return a richer object (MetaDto) soon, and we should put 'attributes' and 'content' here.
        val meta = flowNftItemControllerApi.getNftItemMetaById(itemId).awaitFirst()
        return FlowItemConverter.convert(meta)
    }

    override suspend fun resetItemMeta(itemId: String) {
        // TODO FLOW implement
    }

    override suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        val items = flowNftItemControllerApi.getNftItemsByCollection(
            collection,
            continuation,
            size
        ).awaitFirst()
        return FlowItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        val items = flowNftItemControllerApi.getNftItemsByCreator(
            creator,
            continuation,
            size
        ).awaitFirst()
        return FlowItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        val items = flowNftItemControllerApi.getNftItemsByOwner(
            owner,
            continuation,
            size
        ).awaitFirst()
        return FlowItemConverter.convert(items, blockchain)
    }

}
