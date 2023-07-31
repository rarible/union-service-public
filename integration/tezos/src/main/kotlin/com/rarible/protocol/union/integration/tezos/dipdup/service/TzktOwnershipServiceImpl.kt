package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.tezos.dipdup.converter.TzktOwnershipConverter
import com.rarible.tzkt.client.OwnershipClient
import com.rarible.tzkt.model.TzktNotFound

class TzktOwnershipServiceImpl(val ownershipClient: OwnershipClient) : TzktOwnershipService {

    private val blockchain = BlockchainDto.TEZOS

    override suspend fun getOwnershipsAll(continuation: String?, size: Int): Slice<UnionOwnership> {
        val page = safeApiCall { ownershipClient.ownershipsAll(continuation, size) }
        val ownerships = page.items.map { TzktOwnershipConverter.convert(it, blockchain) }
        return return Slice(page.continuation, ownerships)
    }

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        val tzktOwnership = safeApiCall { ownershipClient.ownershipById(ownershipId) }
        return TzktOwnershipConverter.convert(tzktOwnership, blockchain)
    }

    override suspend fun getOwnershipsByIds(ids: List<String>): List<UnionOwnership> {
        val tzktOwnerships = safeApiCall { ownershipClient.ownershipsByIds(ids) }
        return tzktOwnerships.map { TzktOwnershipConverter.convert(it, blockchain) }
    }

    override suspend fun getOwnershipsByItem(itemId: String, continuation: String?, size: Int): Page<UnionOwnership> {
        val tzktPage = ownershipClient.ownershipsByToken(itemId, size, continuation)
        return TzktOwnershipConverter.convert(tzktPage, blockchain)
    }

    private suspend fun <T> safeApiCall(clientCall: suspend () -> T): T {
        return try {
            clientCall()
        } catch (e: TzktNotFound) {
            throw UnionNotFoundException(message = e.message ?: "")
        }
    }
}
