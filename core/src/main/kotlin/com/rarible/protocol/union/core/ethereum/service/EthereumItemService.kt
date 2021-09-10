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
        size: Int?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?,
        includeMeta: Boolean?
    ): UnionItemsDto {
        val items = itemControllerApi.getNftAllItems(
            continuation,
            size,
            showDeleted,
            lastUpdatedFrom,
            lastUpdatedTo,
            includeMeta
        ).awaitFirst()
        return EthUnionItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemById(
        itemId: String,
        includeMeta: Boolean?
    ): UnionItemDto {
        val item = itemControllerApi.getNftItemById(itemId, includeMeta).awaitFirst()
        return EthUnionItemConverter.convert(item, blockchain)
    }

    override suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?,
        includeMeta: Boolean?
    ): UnionItemsDto {
        val items = itemControllerApi.getNftItemsByCollection(collection, continuation, size, includeMeta).awaitFirst()
        return EthUnionItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int?,
        includeMeta: Boolean?
    ): UnionItemsDto {
        val items = itemControllerApi.getNftItemsByCreator(creator, continuation, size, includeMeta).awaitFirst()
        return EthUnionItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int?,
        includeMeta: Boolean?
    ): UnionItemsDto {
        val items = itemControllerApi.getNftItemsByOwner(owner, continuation, size, includeMeta).awaitFirst()
        return EthUnionItemConverter.convert(items, blockchain)
    }

}