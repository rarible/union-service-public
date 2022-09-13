package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.core.model.TokenId
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktCollectionService
import com.rarible.protocol.union.integration.tezos.entity.TezosCollectionRepository
import java.math.BigInteger

@CaptureSpan(type = "blockchain")
open class TezosCollectionService(
    private val tzktCollectionService: TzktCollectionService,
    private val tezosCollectionRepository: TezosCollectionRepository
) : AbstractBlockchainService(BlockchainDto.TEZOS), CollectionService {

    override suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        return tzktCollectionService.getAllCollections(continuation, size)
    }

    override suspend fun getCollectionById(collectionId: String): UnionCollection {
        return tzktCollectionService.getCollectionById(collectionId)
    }

    override suspend fun refreshCollectionMeta(collectionId: String) {
        // TODO[TEZOS]: implement.
    }

    override suspend fun getCollectionsByIds(ids: List<String>): List<UnionCollection> {
        return tzktCollectionService.getCollectionByIds(ids)
    }

    override suspend fun generateNftTokenId(collectionId: String, minter: String?): TokenId {
        val tokenId: BigInteger = try { // Adjust to existed count
            val actualCount = tzktCollectionService.tokenCount(collectionId)
            tezosCollectionRepository.adjustTokenCount(collectionId, actualCount)
            tezosCollectionRepository.generateTokenId(collectionId)
        } catch (ex: Exception) {
            throw UnionException("Collection wasn't found")
        }
        return TokenId(tokenId.toString())
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        return tzktCollectionService.getCollectionByOwner(owner, continuation, size)
    }
}
