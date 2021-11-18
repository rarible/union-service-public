package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.tezos.api.client.NftOwnershipControllerApi
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.converter.UnionConverter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.converter.TezosOwnershipConverter
import kotlinx.coroutines.reactive.awaitFirst

@CaptureSpan(type = "blockchain")
open class TezosOwnershipService(
    private val ownershipControllerApi: NftOwnershipControllerApi
) : AbstractBlockchainService(BlockchainDto.TEZOS), OwnershipService {

    override suspend fun getAllOwnerships(
        continuation: String?,
        size: Int
    ): Page<UnionOwnership> {
        val ownerships = ownershipControllerApi.getNftAllOwnerships(
            size,
            continuation
        ).awaitFirst()
        return TezosOwnershipConverter.convert(ownerships, blockchain)
    }

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        val ownership = ownershipControllerApi.getNftOwnershipById(ownershipId).awaitFirst()
        return TezosOwnershipConverter.convert(ownership, blockchain)
    }

    override suspend fun getOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int
    ): Page<UnionOwnership> {
        val ownerships =
            ownershipControllerApi.getNftOwnershipByItem(
                contract,
                UnionConverter.convertToBigInteger(tokenId).toString(),
                size,
                continuation
            ).awaitFirst()
        return TezosOwnershipConverter.convert(ownerships, blockchain)
    }
}