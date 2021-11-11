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
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class ActivityController(
    private val router: BlockchainRouter<ActivityService>
) : ActivityControllerApi {

    private val logger = LoggerFactory.getLogger(javaClass)

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

        logger.info("Response for getAllActivities(type={}, blockchains={}, continuation={}, size={}, sort={}):" +
                " Slice(size={}, continuation={}) from blockchain slices {} ",
            type, blockchains, continuation, size, sort,
            result.activities.size, result.continuation, blockchainPages.map { it.entities.size }
        )
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

        logger.info(
            "Response for getActivitiesByCollection(type={}, collection={}, continuation={}, size={}, sort={}): " +
                    "Slice(size={}, continuation={}) ",
            type, collection, continuation, size, sort, result.entities.size, result.continuation
        )
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

        logger.info(
            "Response for getActivitiesByItem(type={}, contract={}, tokenId={} continuation={}, size={}, sort={}): " +
                    "Slice(size={}, continuation={}) ",
            type, contract, tokenId, continuation, size, sort, result.entities.size, result.continuation
        )
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

        logger.info("Response for getActivitiesByUser(type={}, users={}, continuation={}, size={}, sort={}):" +
                " Slice(size={}, continuation={}) from user slices {} ",
            type, user, continuation, size, sort,
            result.activities.size, result.continuation, blockchainPages.map { it.entities.size }
        )

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
