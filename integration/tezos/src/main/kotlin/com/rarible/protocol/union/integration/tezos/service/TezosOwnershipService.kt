package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipDupOwnershipService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktOwnershipService

@CaptureSpan(type = "blockchain")
open class TezosOwnershipService(
    private val tzktOwnershipService: TzktOwnershipService,
    private val dipdupOwnershipService: DipDupOwnershipService,
    private val properties: DipDupIntegrationProperties
) : AbstractBlockchainService(BlockchainDto.TEZOS), OwnershipService {

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        return if (properties.useDipDupTokens) {
            dipdupOwnershipService.getOwnershipById(ownershipId)
        } else {
            tzktOwnershipService.getOwnershipById(ownershipId)
        }
    }

    override suspend fun getOwnershipsByIds(ownershipIds: List<String>): List<UnionOwnership> {
        return if (properties.useDipDupTokens) {
            dipdupOwnershipService.getOwnershipsByIds(ownershipIds)
        } else {
            tzktOwnershipService.getOwnershipsByIds(ownershipIds)
        }
    }

    override suspend fun getOwnershipsAll(continuation: String?, size: Int): Slice<UnionOwnership> {
        return if (properties.useDipDupTokens) {
            dipdupOwnershipService.getOwnershipsAll(continuation, size)
        } else {
            tzktOwnershipService.getOwnershipsAll(continuation, size)
        }
    }

    override suspend fun getOwnershipsByItem(
        itemId: String,
        continuation: String?,
        size: Int
    ): Page<UnionOwnership> {
        return if (properties.useDipDupTokens) {
            dipdupOwnershipService.getOwnershipsByItem(itemId, continuation, size)
        } else {
            tzktOwnershipService.getOwnershipsByItem(itemId, continuation, size)
        }
    }

    override suspend fun getOwnershipsByOwner(address: String, continuation: String?, size: Int): Page<UnionOwnership> {
        // Will be implemented via es
        return Page.empty()
    }
}
