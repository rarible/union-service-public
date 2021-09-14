package com.rarible.protocol.union.core.ethereum.service

import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.union.core.ethereum.converter.EthUnionOwnershipConverter
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.UnionOwnershipDto
import com.rarible.protocol.union.dto.UnionOwnershipsDto
import kotlinx.coroutines.reactive.awaitFirst

class EthereumOwnershipService(
    blockchain: EthBlockchainDto,
    private val ownershipControllerApi: NftOwnershipControllerApi
) : AbstractEthereumService(blockchain), OwnershipService {

    private val commonBlockchain = BlockchainDto.valueOf(blockchain.name)
    override fun getBlockchain() = commonBlockchain

    override suspend fun getAllOwnerships(continuation: String?, size: Int): UnionOwnershipsDto {
        val ownerships = ownershipControllerApi.getNftAllOwnerships(continuation, size).awaitFirst()
        return EthUnionOwnershipConverter.convert(ownerships, blockchain)
    }

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnershipDto {
        val ownership = ownershipControllerApi.getNftOwnershipById(ownershipId).awaitFirst()
        return EthUnionOwnershipConverter.convert(ownership, blockchain)
    }

    override suspend fun getOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int
    ): UnionOwnershipsDto {
        val items = ownershipControllerApi.getNftOwnershipsByItem(contract, tokenId, continuation, size).awaitFirst()
        return EthUnionOwnershipConverter.convert(items, blockchain)
    }
}