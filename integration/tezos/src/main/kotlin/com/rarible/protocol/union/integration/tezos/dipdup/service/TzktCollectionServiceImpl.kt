package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.converter.TzktCollectionConverter
import com.rarible.protocol.union.integration.tezos.dipdup.converter.TzktCollectionConverter.convertType
import com.rarible.protocol.union.integration.tezos.entity.TezosCollectionRepository
import com.rarible.tzkt.client.CollectionClient
import com.rarible.tzkt.client.TokenClient
import com.rarible.tzkt.model.CollectionType
import com.rarible.tzkt.model.Contract
import com.rarible.tzkt.model.TzktNotFound
import java.math.BigInteger

class TzktCollectionServiceImpl(
    val collectionClient: CollectionClient,
    val tokenClient: TokenClient,
    val tezosCollectionRepository: TezosCollectionRepository,
    val tzktProperties: DipDupIntegrationProperties.TzktProperties
) : TzktCollectionService {

    private val blockchain = BlockchainDto.TEZOS

    override suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        val tzktPage = collectionClient.collectionsAll(size, continuation).run {
            copy(items = enrichWithType(items))
        }
        return TzktCollectionConverter.convert(tzktPage, blockchain)
    }

    override suspend fun getCollectionById(collectionId: String, useMeta: Boolean): UnionCollection {
        val requestMeta = useMeta || tzktProperties.requestedCollectionMeta
        val tzktCollection = safeApiCall { collectionClient.collection(collectionId, requestMeta) }.run {
            copy(collectionType = typeByAddress(address!!))
        }
        return TzktCollectionConverter.convert(tzktCollection, blockchain)
    }

    override suspend fun getCollectionByIds(collectionIds: List<String>): List<UnionCollection> {
        val tzktCollections = enrichWithType(safeApiCall { collectionClient.collectionsByIds(collectionIds) })
        return TzktCollectionConverter.convert(tzktCollections, blockchain)
    }

    override suspend fun getCollectionByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        val tzktCollection = safeApiCall { collectionClient.collectionsByOwner(owner, size, continuation) }
        return TzktCollectionConverter.convert(tzktCollection, blockchain)
    }

    override suspend fun tokenCount(collectionId: String): BigInteger {
        return tokenClient.tokenCount(collectionId)
    }

    private suspend fun <T> safeApiCall(clientCall: suspend () -> T): T {
        return try {
            clientCall()
        } catch (e: TzktNotFound) {
            throw UnionNotFoundException(message = e.message ?: "")
        }
    }

    private suspend fun typeByAddress(id: String): CollectionType? = typeMap(listOf(id))[id]

    private suspend fun enrichWithType(items: List<Contract>): List<Contract> {
        val ids = items.mapNotNull { it.address }
        val cache = typeMap(ids)
        return items.map { it.copy(collectionType = cache[it.address]) }
    }

    private suspend fun typeMap(ids: List<String>): Map<String, CollectionType> {
        val collections: Map<String, CollectionType> = tezosCollectionRepository.getCollections(ids)
            .filter { it.type != null }
            .map { it.contract to convertType(it.type) }
            .toMap()
        val missed: Map<String, CollectionType> = (ids - collections.keys).mapAsync {
            val type = collectionClient.collectionType(it)
            tezosCollectionRepository.adjustCollectionType(it, convertType(type))
            Pair(it, type)
        }.toMap()
        return collections + missed
    }

}
