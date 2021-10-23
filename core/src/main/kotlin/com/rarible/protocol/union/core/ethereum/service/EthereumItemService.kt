package com.rarible.protocol.union.core.ethereum.service

import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull

class EthereumItemService(
    blockchain: BlockchainDto,
    private val itemControllerApi: NftItemControllerApi
) : AbstractBlockchainService(blockchain), ItemService {

    override suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): Page<UnionItem> {
        val items = itemControllerApi.getNftAllItems(
            continuation,
            size,
            showDeleted,
            lastUpdatedFrom,
            lastUpdatedTo
        ).awaitFirst()
        return EthItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemById(itemId: String): UnionItem {
        val item = itemControllerApi.getNftItemById(itemId).awaitFirst()
        return EthItemConverter.convert(item, blockchain)
    }

    override suspend fun getItemMetaById(itemId: String): UnionMeta {
        val meta = itemControllerApi.getNftItemMetaById(itemId).awaitFirst()
        return EthItemConverter.convert(meta)
    }

    override suspend fun resetItemMeta(itemId: String) {
        itemControllerApi.resetNftItemMetaById(itemId).awaitFirstOrNull()
    }

    override suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        val items = itemControllerApi.getNftItemsByCollection(
            collection,
            continuation,
            size
        ).awaitFirst()
        return EthItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        val items = itemControllerApi.getNftItemsByCreator(
            creator,
            continuation,
            size
        ).awaitFirst()
        return EthItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        val items = itemControllerApi.getNftItemsByOwner(
            owner,
            continuation,
            size
        ).awaitFirst()
        return EthItemConverter.convert(items, blockchain)
    }

}
