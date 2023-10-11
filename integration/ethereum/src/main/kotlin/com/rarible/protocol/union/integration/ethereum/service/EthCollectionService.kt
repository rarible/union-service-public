package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.protocol.dto.CollectionsByIdRequestDto
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.union.core.exception.UnionMetaException
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionCollectionTokenId
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.ethereum.converter.EthCollectionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthMetaConverter
import com.rarible.protocol.union.integration.ethereum.converter.MetaStatusChecker
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.HttpStatus

class EthCollectionService(
    blockchain: BlockchainDto,
    private val collectionControllerApi: NftCollectionControllerApi
) : AbstractBlockchainService(blockchain), CollectionService {

    override suspend fun generateTokenId(collectionId: String, minter: String?): UnionCollectionTokenId {
        if (minter == null) {
            throw UnionValidationException("Minter must be specified")
        }
        val tokenId = collectionControllerApi.generateNftTokenId(collectionId, minter).awaitSingle()
        return EthCollectionConverter.convert(tokenId)
    }

    override suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        val collections = collectionControllerApi.searchNftAllCollections(
            continuation,
            size
        ).awaitFirst()
        return EthCollectionConverter.convert(collections, blockchain)
    }

    override suspend fun getCollectionById(collectionId: String): UnionCollection {
        val collection = collectionControllerApi.getNftCollectionById(collectionId).awaitFirst()
        return EthCollectionConverter.convert(collection, blockchain)
    }

    override suspend fun getCollectionMetaById(collectionId: String): UnionCollectionMeta {
        val entityId = "Collection: $blockchain:$collectionId"
        try {
            val response = collectionControllerApi.getCollectionMeta(collectionId).awaitFirst()
            MetaStatusChecker.checkStatus(response.status, entityId)

            return response.meta?.let { EthMetaConverter.convert(it, blockchain) }
                ?: throw UnionNotFoundException("Meta not found for $entityId")
        } catch (e: NftCollectionControllerApi.ErrorGetCollectionMeta) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                throw UnionNotFoundException("Meta not found for $entityId")
            }
            throw UnionMetaException(UnionMetaException.ErrorCode.ERROR, e.message)
        }
    }

    override suspend fun refreshCollectionMeta(collectionId: String) {
        // collectionControllerApi.resetNftCollectionMetaById(collectionId).awaitFirstOrNull()
    }

    override suspend fun getCollectionsByIds(ids: List<String>): List<UnionCollection> {
        val collections = collectionControllerApi.getNftCollectionsByIds(CollectionsByIdRequestDto(ids)).awaitSingle()
        return EthCollectionConverter.convert(collections, blockchain).entities
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        val items = collectionControllerApi.searchNftCollectionsByOwner(
            owner,
            continuation,
            size
        ).awaitFirst()
        return EthCollectionConverter.convert(items, blockchain)
    }
}
