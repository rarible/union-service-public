package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.configuration.PageSize
import com.rarible.protocol.union.core.continuation.Paging
import com.rarible.protocol.union.core.continuation.Slice
import com.rarible.protocol.union.core.service.ActivityServiceRouter
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.continuation.ActivityContinuation
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ActivityController(
    private val router: ActivityServiceRouter
) : ActivityControllerApi {

    override suspend fun getAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val safeSize = PageSize.ACTIVITY.limit(size)
        val blockchainPages = router.executeForAll(blockchains) {
            it.getAllActivities(type, continuation, safeSize, sort)
        }

        val result = merge(blockchainPages, safeSize, sort)
        return ResponseEntity.ok(result)
    }

    override suspend fun getActivitiesByCollection(
        type: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val safeSize = PageSize.ACTIVITY.limit(size)
        val (blockchain, shortCollection) = IdParser.parse(collection)
        val result = router.getService(blockchain)
            .getActivitiesByCollection(type, shortCollection, continuation, safeSize, sort)
        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun getActivitiesByItem(
        type: List<ActivityTypeDto>,
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val safeSize = PageSize.ACTIVITY.limit(size)
        val (blockchain, shortContact) = IdParser.parse(contract)
        val result = router.getService(blockchain)
            .getActivitiesByItem(type, shortContact, tokenId, continuation, safeSize, sort)
        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun getActivitiesByUser(
        type: List<UserActivityTypeDto>,
        user: List<String>,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val safeSize = PageSize.ACTIVITY.limit(size)
        val groupedByBlockchain = user.map { IdParser.parse(it) }
            .groupBy({ it.first }, { it.second })

        val blockchainPages = coroutineScope {
            groupedByBlockchain.map {
                val blockchain = it.key
                val blockchainUsers = it.value
                async {
                    router.getService(blockchain)
                        .getActivitiesByUser(type, blockchainUsers, continuation, safeSize, sort)
                }
            }
        }.map { it.await() }

        val result = merge(blockchainPages, safeSize, sort)
        return ResponseEntity.ok(result)
    }

    private fun merge(
        blockchainPages: List<Slice<ActivityDto>>,
        size: Int,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        val continuationFactory = when (sort) {
            ActivitySortDto.EARLIEST_FIRST -> ActivityContinuation.ByLastUpdatedAndIdAsc
            ActivitySortDto.LATEST_FIRST, null -> ActivityContinuation.ByLastUpdatedAndIdDesc
        }

        val combinedSlice = Paging(
            continuationFactory,
            blockchainPages.flatMap { it.entities }
        ).getSlice(size)

        return toDto(combinedSlice)
    }

    private fun toDto(slice: Slice<ActivityDto>): ActivitiesDto {
        return ActivitiesDto(
            continuation = slice.continuation,
            activities = slice.entities
        )
    }
}