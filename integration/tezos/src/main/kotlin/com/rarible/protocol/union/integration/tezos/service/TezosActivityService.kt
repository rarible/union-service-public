package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.model.ItemAndOwnerActivityType
import com.rarible.protocol.union.core.model.TypedActivityId
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.continuation.ActivityContinuation
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipDupTokenActivityService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderActivityService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemActivityService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigInteger
import java.time.Instant
import java.util.regex.Pattern

// TODO UNION add tests when tezos add sorting
@CaptureSpan(type = "blockchain")
open class TezosActivityService(
    private val dipdupOrderActivityService: DipdupOrderActivityService,
    private val dipdupTokenActivityService: DipDupTokenActivityService,
    private val tzktItemActivityService: TzktItemActivityService,
    private val properties: DipDupIntegrationProperties
) : AbstractBlockchainService(BlockchainDto.TEZOS), ActivityService {

    companion object {
        private val logger by Logger()
    }

    override suspend fun getAllActivities(
        types: List<ActivityTypeDto>,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        return getDipDupAndTzktActivities(types, continuation, size, sort)
    }


    suspend fun getDipDupAndTzktActivities(
        types: List<ActivityTypeDto>,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ) = coroutineScope  {
        val orderActivitiesRequest = async {
            dipdupOrderActivityService.getAll(types, continuation, size, sort)
        }
        val itemActivitiesRequest = async {
            if (properties.useDipDupTokens) {
                dipdupTokenActivityService.getAll(types, continuation, size, sort)
            } else {
                tzktItemActivityService.getAll(types, continuation, size, sort)
            }
        }
        val activities = (orderActivitiesRequest.await().entities + itemActivitiesRequest.await().entities)

        val page = Paging(
            continuationFactory(sort),
            activities
        ).getSlice(size)

        page
    }

    override suspend fun getAllActivitiesSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): Slice<ActivityDto> = Slice.empty()

    override suspend fun getAllRevertedActivitiesSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): Slice<ActivityDto> {
        return Slice.empty()
    }

    override suspend fun getActivitiesByCollection(
        types: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        // This method isn't implemented in the new backend
        return Slice.empty()
    }

    override suspend fun getActivitiesByItem(
        types: List<ActivityTypeDto>,
        itemId: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        return getDipDupAndTzktActivitiesByItem(types, contract, tokenId, continuation, size, sort)
    }

    suspend fun getDipDupAndTzktActivitiesByItem(
        types: List<ActivityTypeDto>,
        contract: String,
        tokenId: BigInteger,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ) = coroutineScope  {
        val orderActivitiesRequest = async {
            dipdupOrderActivityService.getByItem(types, contract, tokenId, continuation, size, sort)
        }
        val itemActivitiesRequest = async {
            if (properties.useDipDupTokens) {
                dipdupTokenActivityService.getByItem(types, contract, tokenId, continuation, size, sort)
            } else {
                tzktItemActivityService.getByItem(types, contract, tokenId, continuation, size, sort)
            }
        }
        val activities = (orderActivitiesRequest.await().entities + itemActivitiesRequest.await().entities)

        Paging(
            continuationFactory(sort),
            activities
        ).getSlice(size)
    }

    override suspend fun getActivitiesByItemAndOwner(
        types: List<ItemAndOwnerActivityType>,
        itemId: String,
        owner: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<ActivityDto> {
        return Slice.empty() // TODO Not implemented
    }

    override suspend fun getActivitiesByUser(
        types: List<UserActivityTypeDto>,
        users: List<String>,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        // TODO this method isn't implemented in the new backend
        return Slice.empty()
    }

    override suspend fun getActivitiesByIds(ids: List<TypedActivityId>): List<ActivityDto> = coroutineScope {
        val itemActivitiesIds = mutableListOf<String>()
        val orderActivitiesIds = mutableListOf<String>()

        ids.forEach { (id, type) ->
            when (type) {
                ActivityTypeDto.TRANSFER, ActivityTypeDto.MINT, ActivityTypeDto.BURN -> {
                    itemActivitiesIds.add(id)
                }
                ActivityTypeDto.BID, ActivityTypeDto.LIST, ActivityTypeDto.SELL, ActivityTypeDto.CANCEL_LIST, ActivityTypeDto.CANCEL_BID -> {
                    orderActivitiesIds.add(id)
                }
            }
        }

        val dipdupOrderRequest = async {
            val ids = orderActivitiesIds.filter { isValidUUID(it) }
            if (ids.isNotEmpty()) {
                dipdupOrderActivityService.getByIds(ids)
                    .also { logger.info("Total dipdup order activities returned: ${it.size}") }
            } else {
                emptyList()
            }
        }
        val tzktItemRequest = async {
            val ids = itemActivitiesIds.filter { isValidLong(it) }
            if (properties.useDipDupTokens) {
                dipdupTokenActivityService.getByIds(ids)
                    .also { logger.info("Total dipdup item activities returned: ${it.size}") }
            } else {
                if (ids.isNotEmpty()) {
                    tzktItemActivityService.getByIds(ids, properties.tzktProperties.wrapActivityHashes)
                        .also { logger.info("Total tzkt item activities returned: ${it.size}") }
                } else {
                    emptyList()
                }
            }
        }

        val dipdupOrders = dipdupOrderRequest.await()
        val tzktItems = tzktItemRequest.await()

        dipdupOrders + tzktItems
    }

    private fun continuationFactory(sort: ActivitySortDto?) = when (sort) {
        ActivitySortDto.EARLIEST_FIRST -> ActivityContinuation.ByLastUpdatedAsc
        ActivitySortDto.LATEST_FIRST, null -> ActivityContinuation.ByLastUpdatedDesc
    }

    private fun isValidUUID(str: String?): Boolean {
        return if (str == null) {
            false
        } else UUID_REGEX_PATTERN.matcher(str).matches()
    }

    private fun isValidLong(str: String?): Boolean {
        return str?.toLongOrNull()?.let { true } ?: false
    }

    private val UUID_REGEX_PATTERN: Pattern =
        Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$")

}
