package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.protocol.dto.NftOwnershipIdsDto
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import kotlinx.coroutines.reactive.awaitFirst

class EthOwnershipService(
    blockchain: BlockchainDto,
    private val ownershipControllerApi: NftOwnershipControllerApi
) : AbstractBlockchainService(blockchain), OwnershipService {

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        val ownership = ownershipControllerApi.getNftOwnershipById(ownershipId, false).awaitFirst()
        return EthOwnershipConverter.convert(ownership, blockchain)
    }

    override suspend fun getOwnershipsByIds(ownershipIds: List<String>): List<UnionOwnership> {
        val ownerships = ownershipControllerApi.getNftOwnershipsByIds(NftOwnershipIdsDto(ownershipIds)).awaitFirst()
        return ownerships.ownerships.map { EthOwnershipConverter.convert(it, blockchain) }
    }

    override suspend fun getOwnershipsAll(continuation: String?, size: Int): Slice<UnionOwnership> {
        val ownerships = ownershipControllerApi.getNftAllOwnerships(continuation, size, false).awaitFirst()
        val converted = ownerships.ownerships.map { EthOwnershipConverter.convert(it, blockchain) }
        return Slice(ownerships.continuation, converted)
    }

    override suspend fun getOwnershipsByItem(
        itemId: String,
        continuation: String?,
        size: Int
    ): Page<UnionOwnership> {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        val ownerships = ownershipControllerApi.getNftOwnershipsByItem(
            contract, tokenId.toString(), continuation, size
        ).awaitFirst()
        return EthOwnershipConverter.convert(ownerships, blockchain)
    }

    override suspend fun getOwnershipsByOwner(
        address: String,
        continuation: String?,
        size: Int
    ): Page<UnionOwnership> {
        val ownerships = ownershipControllerApi.getNftOwnershipsByOwner(
            address, null, continuation, size
        ).awaitFirst()
        return EthOwnershipConverter.convert(ownerships, blockchain)
    }
}
