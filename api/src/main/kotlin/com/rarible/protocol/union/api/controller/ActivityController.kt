package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.ActivityApiService
import com.rarible.protocol.union.api.service.extractItemId
import com.rarible.protocol.union.core.continuation.ActivityContinuation
import com.rarible.protocol.union.core.continuation.CombinedContinuation
import com.rarible.protocol.union.core.continuation.page.ArgPaging
import com.rarible.protocol.union.core.continuation.page.ArgSlice
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
import com.rarible.protocol.union.dto.subchains
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class ActivityController(
    private val router: BlockchainRouter<ActivityService>,
    private val activityApiService: ActivityApiService
) : ActivityControllerApi {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val safeSize = PageSize.ACTIVITY.limit(size)
        val (result, slicesCounter) = if (null == cursor) {
            val blockchainPages = router.executeForAll(blockchains) {
                it.getAllActivities(type, continuation, safeSize, sort)
            }
            val dto = merge(blockchainPages, safeSize, sort)
            Pair(dto, blockchainPages.map { it.entities.size })
        } else {
            val slices = activityApiService.getAllActivities(type, blockchains, cursor, safeSize, sort)
            val dto = toDtoWithCursor(ArgPaging(continuationFactory(sort), slices).getSlice(safeSize))
            Pair(dto, slices.map { it.slice.entities.size })
        }
        logger.info("Response for getAllActivities(type={}, blockchains={}, continuation={}, size={}, sort={}):" +
                " Slice(size={}, continuation={}, cursor={}) from blockchain slices {} ",
            type, blockchains, continuation, size, sort, result.activities.size, result.continuation, result.cursor, slicesCounter
        )
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
        val safeSize = PageSize.ACTIVITY.limit(size)
        val collectionAddress = IdParser.parseContract(collection)

        if (null == cursor) {
            val result = router.getService(collectionAddress.blockchain)
                .getActivitiesByCollection(type, collectionAddress.value, continuation, safeSize, sort)

            logger.info(
                "Response for getActivitiesByCollection(type={}, collection={}, continuation={}, size={}, sort={}): " +
                        "Slice(size={}, continuation={}) ",
                type, collection, continuation, size, sort, result.entities.size, result.continuation
            )
            return ResponseEntity.ok(toDto(result))
        } else {
            //TODO
            return ResponseEntity.ok(null)
        }
    }

    override suspend fun getActivitiesByItem(
        type: List<ActivityTypeDto>,
        itemId: String?,
        contract: String?,
        tokenId: String?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val safeSize = PageSize.ACTIVITY.limit(size)

        val fullItemId = extractItemId(contract, tokenId, itemId)


        if (null == cursor) {
            val result = router.getService(fullItemId.blockchain)
                .getActivitiesByItem(
                    type,
                    fullItemId.contract,
                    fullItemId.tokenId.toString(),
                    continuation,
                    safeSize,
                    sort
                )

            logger.info(
                "Response for getActivitiesByItem(type={}, itemId={} continuation={}, size={}, sort={}): " +
                        "Slice(size={}, continuation={}) ",
                type, fullItemId.fullId(), continuation, size, sort, result.entities.size, result.continuation
            )
            return ResponseEntity.ok(toDto(result))
        } else {
            //TODO
            return ResponseEntity.ok(null)
        }
    }

    override suspend fun getActivitiesByUser(
        type: List<UserActivityTypeDto>,
        user: List<String>,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val safeSize = PageSize.ACTIVITY.limit(size)
        if (null == cursor) {
            val groupedByBlockchain = user.map { IdParser.parseAddress(it) }
                // Since user specified here with blockchain group, we need to route request to all subchains
                .flatMap { address -> address.blockchainGroup.subchains().map { it to address.value } }
                .groupBy({ it.first }, { it.second })

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
        } else {
            //TODO
            return ResponseEntity.ok(null)
        }
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

        return toDto(combinedSlice, cursor(blockchainPages))
    }


    fun cursor(blockchainPages: List<Slice<ActivityDto>>): String {
        val m = blockchainPages.associateBy({ it.entities.first().id.blockchain.name }, { it.continuation ?: ArgSlice.COMPLETED })
        return CombinedContinuation(m).toString()
    }

    private fun toDto(slice: Slice<ActivityDto>, cursor: String? = null): ActivitiesDto {
        return ActivitiesDto(
            continuation = slice.continuation,
            cursor = cursor,
            activities = slice.entities
        )
    }

    private fun toDtoWithCursor(slice: Slice<ActivityDto>): ActivitiesDto {
        return ActivitiesDto(
            continuation = null,
            cursor = slice.continuation,
            activities = slice.entities
        )
    }

    private fun continuationFactory(sort: ActivitySortDto?) = when (sort) {
        ActivitySortDto.EARLIEST_FIRST -> ActivityContinuation.ByLastUpdatedAndIdAsc
        ActivitySortDto.LATEST_FIRST, null -> ActivityContinuation.ByLastUpdatedAndIdDesc
    }
}
