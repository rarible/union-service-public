package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.api.service.OwnershipQueryService
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.page.Slice
import org.springframework.stereotype.Component

@Component
class OwnershipElasticService : OwnershipQueryService {
    override suspend fun getOwnershipById(fullOwnershipId: OwnershipIdDto): OwnershipDto {
        TODO("Not yet implemented")
    }

    override suspend fun getOwnershipByOwner(
        owner: UnionAddress,
        continuation: String?,
        size: Int,
    ): Slice<UnionOwnership> {
        TODO("Not yet implemented")
    }

    override suspend fun getOwnershipsByItem(itemId: ItemIdDto, continuation: String?, size: Int): OwnershipsDto {
        TODO("Not yet implemented")
    }
}
