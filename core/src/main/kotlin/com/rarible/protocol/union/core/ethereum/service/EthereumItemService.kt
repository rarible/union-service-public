package com.rarible.protocol.union.core.ethereum.service

import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.union.core.ethereum.converter.EthUnionItemConverter
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.UnionItemDto
import com.rarible.protocol.union.dto.UnionItemsDto
import kotlinx.coroutines.reactive.awaitFirst

class EthereumItemService(
    blockchain: EthBlockchainDto,
    private val itemControllerApi: NftItemControllerApi
) : AbstractEthereumService(blockchain), ItemService {

    override suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): UnionItemsDto {
        val items = itemControllerApi.getNftAllItems(
            continuation,
            size,
            showDeleted,
            lastUpdatedFrom,
            lastUpdatedTo
        ).awaitFirst()
        return EthUnionItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemById(
        itemId: String
    ): UnionItemDto {
        val item = itemControllerApi.getNftItemById(itemId).awaitFirst()
        return EthUnionItemConverter.convert(item, blockchain)
    }

    override suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int
    ): UnionItemsDto {
        val items = itemControllerApi.getNftItemsByCollection(collection, continuation, size).awaitFirst()
        return EthUnionItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int
    ): UnionItemsDto {
        val items = itemControllerApi.getNftItemsByCreator(creator, continuation, size).awaitFirst()
        return EthUnionItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): UnionItemsDto {
        val items = itemControllerApi.getNftItemsByOwner(owner, continuation, size).awaitFirst()
        return EthUnionItemConverter.convert(items, blockchain)
    }

}