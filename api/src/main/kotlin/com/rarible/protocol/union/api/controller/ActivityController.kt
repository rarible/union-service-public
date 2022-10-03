package com.rarible.protocol.union.api.controller

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.api.service.select.ActivitySourceSelectService
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SearchEngineDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class ActivityController(
    private val activitySourceSelector: ActivitySourceSelectService
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
        sort: ActivitySortDto?,
        searchEngine: SearchEngineDto?
    ): ResponseEntity<ActivitiesDto> {
        logger.info("Got request to get all activities, parameters: $type, $blockchains, $continuation, $cursor, $size, $sort")
        val result = activitySourceSelector.getAllActivities(
            type, blockchains, continuation, cursor, size, sort, searchEngine
        )
        return ResponseEntity.ok(result)
    }

    override suspend fun getAllActivitiesSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): ResponseEntity<ActivitiesDto> {
        logger.info("Got request to get all activities sync, parameters: $blockchain, $continuation, $size, $sort")
        val result = activitySourceSelector.getAllActivitiesSync(blockchain, continuation, size, sort, type)
        return ResponseEntity.ok(result)
    }

    override suspend fun getActivitiesByCollection(
        type: List<ActivityTypeDto>,
        collection: List<String>,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        searchEngine: SearchEngineDto?
    ): ResponseEntity<ActivitiesDto> {
        if (collection.isEmpty()) throw UnionException("No any collection param in query")
        val result = activitySourceSelector.getActivitiesByCollection(
            type, collection, continuation, cursor, size, sort, searchEngine
        )
        return ResponseEntity.ok(result)
    }


    override suspend fun getActivitiesByItem(
        type: List<ActivityTypeDto>,
        itemId: String,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        searchEngine: SearchEngineDto?
    ): ResponseEntity<ActivitiesDto> {
        val result = activitySourceSelector.getActivitiesByItem(
            type, itemId, continuation, cursor, size, sort, searchEngine
        )
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
        sort: ActivitySortDto?,
        searchEngine: SearchEngineDto?
    ): ResponseEntity<ActivitiesDto> {
        val result = activitySourceSelector.getActivitiesByUser(
            type, user, blockchains, from, to, continuation, cursor, size, sort, searchEngine
        )
        return ResponseEntity.ok(result)
    }

    override suspend fun getActivitiesByUsers(
        type: List<UserActivityTypeDto>,
        requestBody: Flow<String>,
        blockchains: List<BlockchainDto>?,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        searchEngine: SearchEngineDto?
    ): ResponseEntity<ActivitiesDto> {
        val result = activitySourceSelector.getActivitiesByUser(
            type, requestBody.toList(), blockchains, from, to, continuation, cursor, size, sort, searchEngine
        )
        return ResponseEntity.ok(result)
    }
}
