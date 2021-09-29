package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.UserActivityTypeDto

interface ActivityService : BlockchainService {

    suspend fun getAllActivities(
        types: List<ActivityTypeDto>,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): ActivitiesDto

    suspend fun getActivitiesByCollection(
        types: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): ActivitiesDto

    suspend fun getActivitiesByItem(
        types: List<ActivityTypeDto>,
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): ActivitiesDto

    suspend fun getActivitiesByUser(
        types: List<UserActivityTypeDto>,
        users: List<String>,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): ActivitiesDto

}