package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import java.math.BigInteger

interface DipdupOrderActivityService {

    suspend fun getAll(
        types: List<ActivityTypeDto>,
        continuation: String?,
        limit: Int,
        sort: ActivitySortDto?
    ): Slice<UnionActivity> {
        TODO("Not implemented")
    }

    suspend fun getSync(
        continuation: String?,
        limit: Int,
        sort: SyncSortDto?
    ): Slice<UnionActivity> {
        TODO("Not implemented")
    }

    suspend fun getByItem(
        types: List<ActivityTypeDto>,
        contract: String,
        tokenId: BigInteger,
        continuation: String?,
        limit: Int,
        sort: ActivitySortDto?
    ): Slice<UnionActivity> {
        TODO("Not implemented")
    }

    suspend fun getByIds(ids: List<String>): List<UnionActivity> {
        TODO("Not implemented")
    }
}
