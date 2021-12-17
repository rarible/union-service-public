package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.apm.CaptureSpan
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
import com.rarible.protocol.union.core.converter.UnionConverter
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.continuation.ActivityContinuation
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.tezos.converter.TezosActivityConverter
import com.rarible.protocol.union.integration.tezos.converter.TezosConverter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import java.time.Instant

// TODO UNION add tests when tezos add sorting
@CaptureSpan(type = "blockchain")
open class TezosActivityService(
    private val activityItemControllerApi: NftActivityControllerApi,
    private val activityOrderControllerApi: OrderActivityControllerApi,
    private val tezosActivityConverter: TezosActivityConverter
) : AbstractBlockchainService(BlockchainDto.TEZOS), ActivityService {

    companion object {

        private val EMPTY_ORDER_ACTIVITIES = OrderActivitiesDto(listOf(), null)
        private val EMPTY_ITEM_ACTIVITIES = NftActivitiesDto(null, listOf())
    }

    override suspend fun getAllActivities(
        types: List<ActivityTypeDto>,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val nftFilter = tezosActivityConverter.convertToNftTypes(types)?.let {
            NftActivityFilterAllDto(it)
        }
        val orderFilter = tezosActivityConverter.convertToOrderTypes(types)?.let {
            OrderActivityFilterAllDto(it)
        }
        return getTezosActivities(nftFilter, orderFilter, continuation, size, sort)
    }

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
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val tokenIdInt = UnionConverter.convertToBigInteger(tokenId)
        val nftFilter = tezosActivityConverter.convertToNftTypes(types)?.let {
            NftActivityFilterByItemDto(it, contract, tokenIdInt)
        }
        val orderFilter = tezosActivityConverter.convertToOrderTypes(types)?.let {
            OrderActivityFilterByItemDto(it, contract, tokenIdInt)
        }
        return getTezosActivities(nftFilter, orderFilter, continuation, size, sort)
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

    private suspend fun getTezosActivities(
        nftFilter: NftActivityFilterDto?,
        orderFilter: OrderActivityFilterDto?,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ) = coroutineScope {

        val continuationFactory = when (sort) {
            ActivitySortDto.EARLIEST_FIRST -> ActivityContinuation.ByLastUpdatedAndIdAsc
            ActivitySortDto.LATEST_FIRST, null -> ActivityContinuation.ByLastUpdatedAndIdDesc
        }

        val tezosSort = TezosConverter.convert(sort ?: ActivitySortDto.LATEST_FIRST)

        val itemRequest = async { getItemActivities(nftFilter, continuation, size, tezosSort) }
        val orderRequest = async { getOrderActivities(orderFilter, continuation, size, tezosSort) }

        val itemActivities = itemRequest.await().items.map { tezosActivityConverter.convert(it, blockchain) }
        val orderActivities = orderRequest.await().items.map { tezosActivityConverter.convert(it, blockchain) }
        val allActivities = itemActivities + orderActivities

        Paging(
            continuationFactory,
            allActivities
        ).getSlice(size)
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
}