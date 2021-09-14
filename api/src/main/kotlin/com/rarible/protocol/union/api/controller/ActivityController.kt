package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.configuration.PageSize
import com.rarible.protocol.union.core.continuation.ContinuationPaging
import com.rarible.protocol.union.core.service.ActivityServiceRouter
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.dto.continuation.UnionActivityContinuation
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ActivityController(
    private val router: ActivityServiceRouter
) : ActivityControllerApi {

    override suspend fun getAllActivities(
        type: List<UnionActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        sort: UnionActivitySortDto?
    ): ResponseEntity<UnionActivitiesDto> {
        val safeSize = PageSize.ACTIVITY.limit(size)
        val blockchainPages = router.executeForAll(blockchains) {
            it.getAllActivities(type, continuation, safeSize, sort)
        }

        val result = merge(blockchainPages, safeSize, sort)
        return ResponseEntity.ok(result)
    }

    override suspend fun getActivitiesByCollection(
        type: List<UnionActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int?,
        sort: UnionActivitySortDto?
    ): ResponseEntity<UnionActivitiesDto> {
        val safeSize = PageSize.ACTIVITY.limit(size)
        val (blockchain, shortCollection) = IdParser.parse(collection)
        val result = router.getService(blockchain)
            .getActivitiesByCollection(type, shortCollection, continuation, safeSize, sort)
        return ResponseEntity.ok(result)
    }

    override suspend fun getActivitiesByItem(
        type: List<UnionActivityTypeDto>,
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int?,
        sort: UnionActivitySortDto?
    ): ResponseEntity<UnionActivitiesDto> {
        val safeSize = PageSize.ACTIVITY.limit(size)
        val (blockchain, shortContact) = IdParser.parse(contract)
        val result = router.getService(blockchain)
            .getActivitiesByItem(type, shortContact, tokenId, continuation, safeSize, sort)
        return ResponseEntity.ok(result)
    }

    override suspend fun getActivitiesByUser(
        type: List<UnionUserActivityTypeDto>,
        user: List<String>,
        continuation: String?,
        size: Int?,
        sort: UnionActivitySortDto?
    ): ResponseEntity<UnionActivitiesDto> {
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
        blockchainPages: List<UnionActivitiesDto>,
        size: Int,
        sort: UnionActivitySortDto?
    ): UnionActivitiesDto {
        val continuationFactory = when (sort) {
            UnionActivitySortDto.EARLIEST_FIRST -> UnionActivityContinuation.ByLastUpdatedAndIdAsc
            UnionActivitySortDto.LATEST_FIRST, null -> UnionActivityContinuation.ByLastUpdatedAndIdDesc
        }

        val combinedPage = ContinuationPaging(
            continuationFactory,
            blockchainPages.flatMap { it.activities }
        ).getPage(size)

        return UnionActivitiesDto(combinedPage.printContinuation(), combinedPage.entities)
    }
}