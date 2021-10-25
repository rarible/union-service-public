package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.core.continuation.ActivityContinuation
import com.rarible.protocol.union.core.continuation.page.PageSize
import com.rarible.protocol.union.core.continuation.page.Paging
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.parser.IdParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class ActivityController(
    private val router: BlockchainRouter<ActivityService>
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
        val collectionAddress = IdParser.parseAddress(collection)
        val result = router.getService(collectionAddress.blockchain)
            .getActivitiesByCollection(type, collectionAddress.value, continuation, safeSize, sort)
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
        val contractAddress = IdParser.parseAddress(contract)
        val result = router.getService(contractAddress.blockchain)
            .getActivitiesByItem(type, contractAddress.value, tokenId, continuation, safeSize, sort)
        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun getActivitiesByUser(
        type: List<UserActivityTypeDto>,
        user: List<String>,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val safeSize = PageSize.ACTIVITY.limit(size)
        val groupedByBlockchain = user.map { IdParser.parseAddress(it) }
            .groupBy({ it.blockchain }, { it.value })

        val blockchainPages = coroutineScope {
            groupedByBlockchain.map {
                val blockchain = it.key
                val blockchainUsers = it.value
                async {
                    router.getService(blockchain)
                        .getActivitiesByUser(type, blockchainUsers, from, to, continuation, safeSize, sort)
                }
            }
        }.awaitAll()

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
