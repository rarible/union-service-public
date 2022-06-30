package com.rarible.protocol.union.api.service

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.page.Slice

interface OwnershipQueryService {
    suspend fun getOwnershipById(fullOwnershipId: OwnershipIdDto): OwnershipDto
    suspend fun getOwnershipsByIds(ids: List<OwnershipIdDto>): List<OwnershipDto>
    suspend fun getOwnershipByOwner(owner: UnionAddress, continuation: String?, size: Int): Slice<UnionOwnership>
    suspend fun getOwnershipsByItem(itemId: ItemIdDto, continuation: String?, size: Int): OwnershipsDto
}
