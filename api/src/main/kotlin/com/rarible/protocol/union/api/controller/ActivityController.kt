package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.select.ActivitySourceSelectService
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.dto.ActivitiesByUsersRequestDto
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SearchEngineDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.parser.CurrencyIdParser
import com.rarible.protocol.union.dto.parser.IdParser
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class ActivityController(
    private val activitySourceSelector: ActivitySourceSelectService
) : ActivityControllerApi {

    companion object {
        private val logger = LoggerFactory.getLogger(ActivityController::class.java)
        private const val MAX_USERS_COUNT = 2000
    }

    override suspend fun getAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        bidCurrencies: List<String>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        searchEngine: SearchEngineDto?
    ): ResponseEntity<ActivitiesDto> {
        logger.info("Got request to get all activities, parameters: $type, $blockchains, $continuation, $cursor, $size, $sort, $searchEngine")
        val result = activitySourceSelector.getAllActivities(
            type,
            blockchains,
            bidCurrencies?.map { CurrencyIdParser.parse(it) },
            continuation,
            cursor,
            size,
            sort,
            searchEngine
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
        bidCurrencies: List<String>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        searchEngine: SearchEngineDto?
    ): ResponseEntity<ActivitiesDto> {
        if (collection.isEmpty()) throw UnionException("No any collection param in query")
        val collectionIds = collection.map(IdParser::parseCollectionId)
        val result = activitySourceSelector.getActivitiesByCollection(
            type,
            collectionIds,
            bidCurrencies?.map { CurrencyIdParser.parse(it) },
            continuation,
            cursor,
            size,
            sort,
            searchEngine
        )
        return ResponseEntity.ok(result)
    }

    override suspend fun getActivitiesByItem(
        type: List<ActivityTypeDto>,
        itemId: String,
        bidCurrencies: List<String>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        searchEngine: SearchEngineDto?
    ): ResponseEntity<ActivitiesDto> {
        val result = activitySourceSelector.getActivitiesByItem(
            type,
            IdParser.parseItemId(itemId),
            bidCurrencies?.map { CurrencyIdParser.parse(it) },
            continuation,
            cursor,
            size,
            sort,
            searchEngine
        )
        return ResponseEntity.ok(result)
    }

    override suspend fun getActivitiesByUser(
        type: List<UserActivityTypeDto>,
        user: List<String>,
        blockchains: List<BlockchainDto>?,
        bidCurrencies: List<String>?,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        searchEngine: SearchEngineDto?
    ): ResponseEntity<ActivitiesDto> {
        val userAddresses = user.map(IdParser::parseAddress)
        val result = activitySourceSelector.getActivitiesByUser(
            type,
            userAddresses,
            blockchains,
            bidCurrencies?.map { CurrencyIdParser.parse(it) },
            from,
            to,
            continuation,
            cursor,
            size,
            sort,
            searchEngine
        )
        return ResponseEntity.ok(result)
    }

    override suspend fun getActivitiesByUsers(activitiesByUsersRequestDto: ActivitiesByUsersRequestDto): ResponseEntity<ActivitiesDto> {
        val result = activitySourceSelector.getActivitiesByUser(
            activitiesByUsersRequestDto.types,
            activitiesByUsersRequestDto.users.take(MAX_USERS_COUNT),
            activitiesByUsersRequestDto.blockchains,
            activitiesByUsersRequestDto.bidCurrencies,
            activitiesByUsersRequestDto.from,
            activitiesByUsersRequestDto.to,
            activitiesByUsersRequestDto.continuation,
            activitiesByUsersRequestDto.cursor,
            activitiesByUsersRequestDto.size,
            activitiesByUsersRequestDto.sort,
            activitiesByUsersRequestDto.searchEngine
        )
        return ResponseEntity.ok(result)
    }
}
