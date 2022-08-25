package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.logging.Logger
import com.rarible.protocol.tezos.api.client.NftActivityControllerApi
import com.rarible.protocol.tezos.api.client.OrderActivityControllerApi
import com.rarible.protocol.tezos.dto.NftActivitiesDto
import com.rarible.protocol.tezos.dto.NftActivityFilterAllDto
import com.rarible.protocol.tezos.dto.NftActivityFilterByCollectionDto
import com.rarible.protocol.tezos.dto.NftActivityFilterByItemDto
import com.rarible.protocol.tezos.dto.NftActivityFilterByUserDto
import com.rarible.protocol.tezos.dto.NftActivityFilterDto
import com.rarible.protocol.tezos.dto.OrderActivitiesDto
import com.rarible.protocol.tezos.dto.OrderActivityFilterAllDto
import com.rarible.protocol.tezos.dto.OrderActivityFilterByCollectionDto
import com.rarible.protocol.tezos.dto.OrderActivityFilterByItemDto
import com.rarible.protocol.tezos.dto.OrderActivityFilterByUserDto
import com.rarible.protocol.tezos.dto.OrderActivityFilterDto
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
import com.rarible.protocol.union.integration.tezos.converter.TezosActivityConverter
import com.rarible.protocol.union.integration.tezos.converter.TezosConverter
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderActivityService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemActivityService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import java.math.BigInteger
import java.time.Instant
import java.util.regex.Pattern

// TODO UNION add tests when tezos add sorting
@CaptureSpan(type = "blockchain")
open class TezosActivityService(
    private val activityItemControllerApi: NftActivityControllerApi,
    private val activityOrderControllerApi: OrderActivityControllerApi,
    private val tezosActivityConverter: TezosActivityConverter,
    private val dipdupOrderActivityService: DipdupOrderActivityService,
    private val tzktItemActivityService: TzktItemActivityService
) : AbstractBlockchainService(BlockchainDto.TEZOS), ActivityService {

    companion object {
        private val logger by Logger()

        private val EMPTY_ORDER_ACTIVITIES = OrderActivitiesDto(listOf(), null)
        private val EMPTY_ITEM_ACTIVITIES = NftActivitiesDto(null, listOf())
    }

    override suspend fun getAllActivities(
        types: List<ActivityTypeDto>,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        if (dipdupOrderActivityService.enabled()) {
            return getDipDupAndTzktActivities(types, continuation, size, sort)
        } else {
            val nftFilter = tezosActivityConverter.convertToNftTypes(types)?.let {
                NftActivityFilterAllDto(it)
            }
            val orderFilter = tezosActivityConverter.convertToOrderTypes(types)?.let {
                OrderActivityFilterAllDto(it)
            }
            return getTezosActivities(nftFilter, orderFilter, continuation, size, sort)
        }
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
            tzktItemActivityService.getAll(types, continuation, size, sort)
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

    override suspend fun getActivitiesByCollection(
        types: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val nftFilter = tezosActivityConverter.convertToNftTypes(types)?.let {
            NftActivityFilterByCollectionDto(it, collection)
        }
        val orderFilter = tezosActivityConverter.convertToOrderTypes(types)?.let {
            OrderActivityFilterByCollectionDto(it, collection)
        }
        return getTezosActivities(nftFilter, orderFilter, continuation, size, sort)
    }

    override suspend fun getActivitiesByItem(
        types: List<ActivityTypeDto>,
        itemId: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        if (dipdupOrderActivityService.enabled()) {
            return getDipDupAndTzktActivitiesByItem(types, contract, tokenId, continuation, size, sort)
        } else {
            val nftFilter = tezosActivityConverter.convertToNftTypes(types)?.let {
                NftActivityFilterByItemDto(it, contract, tokenId)
            }
            val orderFilter = tezosActivityConverter.convertToOrderTypes(types)?.let {
                OrderActivityFilterByItemDto(it, contract, tokenId)
            }
            return getTezosActivities(nftFilter, orderFilter, continuation, size, sort)
        }
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
            tzktItemActivityService.getByItem(types, contract, tokenId, continuation, size, sort)
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
        val nftFilter = tezosActivityConverter.convertToNftUserTypes(types)?.let {
            NftActivityFilterByUserDto(it, users)
        }
        val orderFilter = tezosActivityConverter.convertToOrderUserTypes(types)?.let {
            OrderActivityFilterByUserDto(it, users)
        }
        return getTezosActivities(nftFilter, orderFilter, continuation, size, sort)
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

        logger.info("Item Activities ids (total ${itemActivitiesIds.size}): $itemActivitiesIds")
        logger.info("Order Activities ids (total ${orderActivitiesIds.size}): $orderActivitiesIds")

        val dipdupOrderRequest = async {
            val ids = orderActivitiesIds.filter { isValidUUID(it) }
            if (dipdupOrderActivityService.enabled() && ids.isNotEmpty()) {
                dipdupOrderActivityService.getByIds(ids)
                    .also { logger.info("Total dipdup order activities returned: ${it.size}") }
            } else {
                emptyList()
            }
        }
        val tzktItemRequest = async {
            val ids = itemActivitiesIds.filter { isValidLong(it) }
            if (tzktItemActivityService.enabled() && ids.isNotEmpty()) {
                tzktItemActivityService.getByIds(ids)
                    .also { logger.info("Total tzkt item activities returned: ${it.size}") }
            } else {
                emptyList()
            }
        }

        val dipdupOrders = dipdupOrderRequest.await()
        val tzktItems = tzktItemRequest.await()

        dipdupOrders + tzktItems
    }

    private suspend fun getTezosActivities(
        nftFilter: NftActivityFilterDto?,
        orderFilter: OrderActivityFilterDto?,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ) = coroutineScope {

        val tezosSort = TezosConverter.convert(sort ?: ActivitySortDto.LATEST_FIRST)

        val itemRequest = async { getItemActivities(nftFilter, continuation, size, tezosSort) }
        val orderRequest = async { getOrderActivities(orderFilter, continuation, size, tezosSort) }

        val itemActivities = itemRequest.await().items.map { tezosActivityConverter.convert(it, blockchain) }
        val orderActivities = orderRequest.await().items.map { tezosActivityConverter.convert(it, blockchain) }
        val allActivities = itemActivities + orderActivities

        Paging(
            continuationFactory(sort),
            allActivities
        ).getSlice(size)
    }

    private fun continuationFactory(sort: ActivitySortDto?) = when (sort) {
        ActivitySortDto.EARLIEST_FIRST -> ActivityContinuation.ByLastUpdatedAsc
        ActivitySortDto.LATEST_FIRST, null -> ActivityContinuation.ByLastUpdatedDesc
    }

    private suspend fun getItemActivities(
        filter: NftActivityFilterDto?,
        continuation: String?,
        size: Int,
        sort: com.rarible.protocol.tezos.dto.ActivitySortDto
    ): NftActivitiesDto {
        return if (filter != null) {
            activityItemControllerApi.getNftActivities(sort, size, continuation, filter).awaitFirst()
        } else {
            EMPTY_ITEM_ACTIVITIES
        }
    }

    private suspend fun getOrderActivities(
        filter: OrderActivityFilterDto?,
        continuation: String?,
        size: Int,
        sort: com.rarible.protocol.tezos.dto.ActivitySortDto
    ): OrderActivitiesDto {
        return if (filter != null) {
            activityOrderControllerApi.getOrderActivities(sort, size, continuation, filter).awaitFirst()
        } else {
            EMPTY_ORDER_ACTIVITIES
        }
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
