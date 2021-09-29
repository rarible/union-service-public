package com.rarible.protocol.union.core.ethereum.service

import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.union.core.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemsDto
import kotlinx.coroutines.reactive.awaitFirst

class EthereumItemService(
    blockchain: BlockchainDto,
    private val itemControllerApi: NftItemControllerApi
) : AbstractEthereumService(blockchain), ItemService {

    override suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): ItemsDto {
        val items = itemControllerApi.getNftAllItems(
            continuation,
            size,
            showDeleted,
            lastUpdatedFrom,
            lastUpdatedTo
        ).awaitFirst()
        return EthItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemById(
        itemId: String
    ): ItemDto {
        val item = itemControllerApi.getNftItemById(itemId).awaitFirst()
        return EthItemConverter.convert(item, blockchain)
    }

    override suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int
    ): ItemsDto {
        val items = itemControllerApi.getNftItemsByCollection(collection, continuation, size).awaitFirst()
        return EthItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int
    ): ItemsDto {
        val items = itemControllerApi.getNftItemsByCreator(creator, continuation, size).awaitFirst()
        return EthItemConverter.convert(items, blockchain)
    }

    override suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): ItemsDto {
        val items = itemControllerApi.getNftItemsByOwner(owner, continuation, size).awaitFirst()
        return EthItemConverter.convert(items, blockchain)
    }

}