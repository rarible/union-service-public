package com.rarible.protocol.union.api.service.api

import com.rarible.protocol.union.api.service.OwnershipQueryService
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.subchains
import org.springframework.stereotype.Component

@Component
class OwnershipApiService(
    private val router: BlockchainRouter<OwnershipService>,
) : OwnershipQueryService {
    override suspend fun getOwnershipByOwner(
        owner: UnionAddress,
        continuation: String?,
        size: Int,
    ): List<UnionOwnership> = router.executeForAll(owner.blockchainGroup.subchains()) {
        it.getOwnershipsByOwner(owner.value, continuation, size).entities
    }.flatten()

    override suspend fun getOwnershipsByItem(
        itemId: ItemIdDto,
        continuation: String?,
        size: Int,
    ): List<UnionOwnership> =
        router.getService(itemId.blockchain).getOwnershipsByItem(itemId.value, continuation, size).entities
}
