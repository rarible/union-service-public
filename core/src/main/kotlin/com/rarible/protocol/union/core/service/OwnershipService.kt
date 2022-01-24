package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.router.BlockchainService
import com.rarible.protocol.union.dto.continuation.page.Page
import java.math.BigInteger

interface OwnershipService : BlockchainService {

    suspend fun getOwnershipById(
        ownershipId: String
    ): UnionOwnership

    suspend fun getOwnershipsByItem(
        contract: String,
        tokenId: BigInteger,
        continuation: String?,
        size: Int
    ): Page<UnionOwnership>

}