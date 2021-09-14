package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.dto.UnionActivitiesDto
import com.rarible.protocol.union.dto.UnionActivitySortDto
import com.rarible.protocol.union.dto.UnionActivityTypeDto
import com.rarible.protocol.union.dto.UnionUserActivityTypeDto

interface ActivityService : BlockchainService {

    suspend fun getAllActivities(
        types: List<UnionActivityTypeDto>,
        continuation: String?,
        size: Int,
        sort: UnionActivitySortDto?
    ): UnionActivitiesDto

    suspend fun getActivitiesByCollection(
        types: List<UnionActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int,
        sort: UnionActivitySortDto?
    ): UnionActivitiesDto

    suspend fun getActivitiesByItem(
        types: List<UnionActivityTypeDto>,
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int,
        sort: UnionActivitySortDto?
    ): UnionActivitiesDto

    suspend fun getActivitiesByUser(
        types: List<UnionUserActivityTypeDto>,
        users: List<String>,
        continuation: String?,
        size: Int,
        sort: UnionActivitySortDto?
    ): UnionActivitiesDto

}