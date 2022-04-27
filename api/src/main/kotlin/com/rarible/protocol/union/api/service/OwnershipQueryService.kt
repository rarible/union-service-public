package com.rarible.protocol.union.api.service

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.UnionAddress

interface OwnershipQueryService {

    suspend fun getOwnershipByOwner(owner: UnionAddress, continuation: String?, size: Int): List<UnionOwnership>

    suspend fun getOwnershipsByItem(itemId: ItemIdDto, continuation: String?, size: Int): List<UnionOwnership>
}
