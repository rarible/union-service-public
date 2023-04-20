package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.core.model.UnionActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import java.math.BigInteger

interface TzktItemActivityService {

    suspend fun getAll(
        types: List<ActivityTypeDto>,
        continuation: String?,
        limit: Int,
        sort: ActivitySortDto?
    ): Slice<UnionActivityDto> {
        TODO("Not implemented")
    }

    suspend fun getByItem(
        types: List<ActivityTypeDto>,
        contract: String,
        tokenId: BigInteger, continuation: String?, limit: Int, sort: ActivitySortDto?
    ): Slice<UnionActivityDto> {
        TODO("Not implemented")
    }

    suspend fun getByIds(ids: List<String>, wrapHash: Boolean): List<UnionActivityDto> {
        TODO("Not implemented")
    }

}
