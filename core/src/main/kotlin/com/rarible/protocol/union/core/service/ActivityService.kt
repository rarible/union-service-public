package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.model.ItemAndOwnerActivityType
import com.rarible.protocol.union.core.model.TypedActivityId
import com.rarible.protocol.union.core.model.UnionActivityDto
import com.rarible.protocol.union.core.service.router.BlockchainService
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import java.time.Instant

interface ActivityService : BlockchainService {

    suspend fun getAllActivities(
        types: List<ActivityTypeDto>,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<UnionActivityDto>

    suspend fun getAllActivitiesSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): Slice<UnionActivityDto>

    suspend fun getAllRevertedActivitiesSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): Slice<UnionActivityDto>

    suspend fun getActivitiesByCollection(
        types: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<UnionActivityDto>

    suspend fun getActivitiesByItem(
        types: List<ActivityTypeDto>,
        itemId: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<UnionActivityDto>

    suspend fun getActivitiesByItemAndOwner(
        types: List<ItemAndOwnerActivityType>,
        itemId: String,
        owner: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<UnionActivityDto>

    suspend fun getActivitiesByUser(
        types: List<UserActivityTypeDto>,
        users: List<String>,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<UnionActivityDto>

    suspend fun getActivitiesByIds(
        ids: List<TypedActivityId>
    ): List<UnionActivityDto>
}
