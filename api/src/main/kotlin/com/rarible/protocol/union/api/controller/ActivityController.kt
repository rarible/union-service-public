package com.rarible.protocol.union.api.controller

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.api.service.select.ActivitySourceSelectService
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class ActivityController(
    private val activitySourceSelector: ActivitySourceSelectService,
) : ActivityControllerApi {

    companion object {
        private val logger by Logger()
    }

    override suspend fun getAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        logger.debug("Got request to get all activities, parameters: $type, $blockchains, $continuation, $cursor, $size, $sort")
        val result = activitySourceSelector.getAllActivities(type, blockchains, continuation, cursor, size, sort)
        return ResponseEntity.ok(result)
    }

    override suspend fun getActivitiesByCollection(
        type: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val result = activitySourceSelector.getActivitiesByCollection(type, collection, continuation, cursor, size, sort)
        return ResponseEntity.ok(result)
    }

    override suspend fun getActivitiesByItem(
        type: List<ActivityTypeDto>,
        itemId: String,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val result = activitySourceSelector.getActivitiesByItem(type, itemId, continuation, cursor, size, sort)
        return ResponseEntity.ok(result)
    }

    override suspend fun getActivitiesByUser(
        type: List<UserActivityTypeDto>,
        user: List<String>,
        blockchains: List<BlockchainDto>?,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val result = activitySourceSelector.getActivitiesByUser(type, user, blockchains, from, to, continuation, cursor, size, sort)
        return ResponseEntity.ok(result)
    }
}
