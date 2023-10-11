package com.rarible.protocol.union.integration.tezos.service

import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionCollectionTokenId
import com.rarible.protocol.union.core.model.UnionDefaultCollectionTokenId
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipDupCollectionService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktCollectionService
import com.rarible.protocol.union.integration.tezos.entity.TezosCollectionRepository
import org.slf4j.LoggerFactory
import java.math.BigInteger

open class TezosCollectionService(
    private val tzktCollectionService: TzktCollectionService,
    private val dipdupCollectionService: DipDupCollectionService,
    private val tezosCollectionRepository: TezosCollectionRepository,
    private val properties: DipDupIntegrationProperties
) : AbstractBlockchainService(BlockchainDto.TEZOS), CollectionService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        return if (properties.useDipDupTokens) {
            dipdupCollectionService.getCollectionsAll(continuation, size)
        } else {
            tzktCollectionService.getAllCollections(continuation, size)
        }
    }

    override suspend fun getCollectionById(collectionId: String): UnionCollection {
        return if (properties.useDipDupTokens) {
            dipdupCollectionService.getCollectionById(collectionId)
        } else {
            tzktCollectionService.getCollectionById(collectionId)
        }
    }

    override suspend fun getCollectionMetaById(collectionId: String): UnionCollectionMeta {
        // TODO[TEZOS]: implement in right way
        return getCollectionById(collectionId).meta
            ?: throw UnionNotFoundException("Meta not found for Collection $blockchain:$collectionId")
    }

    override suspend fun refreshCollectionMeta(collectionId: String) {
        // TODO[TEZOS]: implement.
    }

    override suspend fun getCollectionsByIds(ids: List<String>): List<UnionCollection> {
        return if (properties.useDipDupTokens) {
            dipdupCollectionService.getCollectionByIds(ids)
        } else {
            tzktCollectionService.getCollectionByIds(ids)
        }
    }

    override suspend fun generateTokenId(collectionId: String, minter: String?): UnionCollectionTokenId {
        val tokenId: BigInteger = try { // Adjust to existed count
            val actualCount = dipdupCollectionService.getTokenLastId(collectionId)
            tezosCollectionRepository.adjustTokenCount(collectionId, actualCount)
            tezosCollectionRepository.generateTokenId(collectionId)
        } catch (ex: Exception) {
            logger.error("Error generating new tokenId: ${ex.message}", ex)
            throw UnionException("Collection wasn't found")
        }
        return UnionDefaultCollectionTokenId(tokenId)
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        return tzktCollectionService.getCollectionByOwner(owner, continuation, size)
    }
}
