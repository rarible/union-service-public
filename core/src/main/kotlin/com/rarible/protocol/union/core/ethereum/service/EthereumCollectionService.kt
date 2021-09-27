package com.rarible.protocol.union.core.ethereum.service

import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.union.core.ethereum.converter.EthUnionCollectionConverter
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionCollectionDto
import com.rarible.protocol.union.dto.UnionCollectionsDto
import kotlinx.coroutines.reactive.awaitFirst

class EthereumCollectionService(
    blockchain: BlockchainDto,
    private val collectionControllerApi: NftCollectionControllerApi
) : AbstractEthereumService(blockchain), CollectionService {

    override suspend fun getAllCollections(continuation: String?, size: Int): UnionCollectionsDto {
        val collections = collectionControllerApi.searchNftAllCollections(continuation, size).awaitFirst()
        return EthUnionCollectionConverter.convert(collections, blockchain)
    }

    override suspend fun getCollectionById(collectionId: String): UnionCollectionDto {
        val collection = collectionControllerApi.getNftCollectionById(collectionId).awaitFirst()
        return EthUnionCollectionConverter.convert(collection, blockchain)
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): UnionCollectionsDto {
        val items = collectionControllerApi.searchNftCollectionsByOwner(owner, continuation, size).awaitFirst()
        return EthUnionCollectionConverter.convert(items, blockchain)
    }
}