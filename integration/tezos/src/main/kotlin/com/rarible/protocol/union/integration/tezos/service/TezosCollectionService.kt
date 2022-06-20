package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.tezos.api.client.NftCollectionControllerApi
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.core.model.TokenId
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.tezos.converter.TezosCollectionConverter
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktCollectionService
import com.rarible.protocol.union.integration.tezos.entity.TezosTokenIdRepository
import kotlinx.coroutines.reactive.awaitFirst

@CaptureSpan(type = "blockchain")
open class TezosCollectionService(
    private val collectionControllerApi: NftCollectionControllerApi,
    private val pgService: TezosPgCollectionService,
    private val tzktCollectionService: TzktCollectionService,
    private val tezosTokenIdRepository: TezosTokenIdRepository
) : AbstractBlockchainService(BlockchainDto.TEZOS), CollectionService {

    override suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        if (tzktCollectionService.enabled()) {
            return tzktCollectionService.getAllCollections(continuation, size)
        }
        val collections = collectionControllerApi.searchNftAllCollections(
            size,
            continuation
        ).awaitFirst()
        return TezosCollectionConverter.convert(collections, blockchain)
    }

    override suspend fun getCollectionById(collectionId: String): UnionCollection {
        if (tzktCollectionService.enabled()) {
            return tzktCollectionService.getCollectionById(collectionId)
        }
        val collection = collectionControllerApi.getNftCollectionById(collectionId).awaitFirst()
        return TezosCollectionConverter.convert(collection, blockchain)
    }

    override suspend fun refreshCollectionMeta(collectionId: String) {
        // TODO[TEZOS]: implement.
    }

    override suspend fun getCollectionsByIds(ids: List<String>): List<UnionCollection> {
        if (tzktCollectionService.enabled()) {
            return tzktCollectionService.getCollectionByIds(ids)
        }
        return pgService.getCollectionsByIds(ids)
    }

    override suspend fun generateNftTokenId(collectionId: String, minter: String?): TokenId {
        try { // Adjust to existed count
            val actualCount = tzktCollectionService.tokenCount(collectionId)
            tezosTokenIdRepository.adjustTokenCount(collectionId, actualCount)
        } catch (ex: Exception) {
            throw UnionException("Collection wasn't found")
        }
        val tezosTokenId = tezosTokenIdRepository.generateNftTokenId(collectionId)
        return TokenId(tezosTokenId.toString())
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        if (tzktCollectionService.enabled()) {
            return tzktCollectionService.getCollectionByOwner(owner, continuation, size)
        }
        val items = collectionControllerApi.searchNftCollectionsByOwner(
            owner,
            size,
            continuation
        ).awaitFirst()
        return TezosCollectionConverter.convert(items, blockchain)
    }
}
