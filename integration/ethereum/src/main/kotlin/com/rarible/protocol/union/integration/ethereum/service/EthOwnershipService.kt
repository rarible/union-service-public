package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.converter.UnionConverter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import kotlinx.coroutines.reactive.awaitFirst

open class EthOwnershipService(
    blockchain: BlockchainDto,
    private val ownershipControllerApi: NftOwnershipControllerApi
) : AbstractBlockchainService(blockchain), OwnershipService {

    override suspend fun getAllOwnerships(
        continuation: String?,
        size: Int
    ): Page<UnionOwnership> {
        val ownerships = ownershipControllerApi.getNftAllOwnerships(
            continuation,
            size
        ).awaitFirst()
        return EthOwnershipConverter.convert(ownerships, blockchain)
    }

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        val ownership = ownershipControllerApi.getNftOwnershipById(ownershipId).awaitFirst()
        return EthOwnershipConverter.convert(ownership, blockchain)
    }

    override suspend fun getOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int
    ): Page<UnionOwnership> {
        val ownerships = ownershipControllerApi.getNftOwnershipsByItem(
            contract,
            UnionConverter.convertToBigInteger(tokenId).toString(),
            continuation,
            size
        ).awaitFirst()
        return EthOwnershipConverter.convert(ownerships, blockchain)
    }
}

@CaptureSpan(type = "blockchain")
open class EthereumOwnershipService(
    ownershipControllerApi: NftOwnershipControllerApi
) : EthOwnershipService(
    BlockchainDto.ETHEREUM,
    ownershipControllerApi
)

@CaptureSpan(type = "blockchain")
open class PolygonOwnershipService(
    ownershipControllerApi: NftOwnershipControllerApi
) : EthOwnershipService(
    BlockchainDto.POLYGON,
    ownershipControllerApi
)