package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.dto.ActivityFilterAllDto
import com.rarible.protocol.dto.ActivityFilterByCollectionDto
import com.rarible.protocol.dto.ActivityFilterByItemDto
import com.rarible.protocol.dto.ActivityFilterByUserDto
import com.rarible.protocol.dto.ActivityFilterDto
import com.rarible.protocol.dto.NftActivitiesDto
import com.rarible.protocol.dto.OrderActivitiesDto
import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
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
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityFilterConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import java.time.Instant

open class EthActivityService(
    blockchain: BlockchainDto,
    private val activityItemControllerApi: NftActivityControllerApi,
    private val activityOrderControllerApi: OrderActivityControllerApi,
    private val ethActivityConverter: EthActivityConverter
) : AbstractBlockchainService(blockchain), ActivityService {

    companion object {

        private val EMPTY_ORDER_ACTIVITIES = OrderActivitiesDto(null, listOf())
        private val EMPTY_ITEM_ACTIVITIES = NftActivitiesDto(null, listOf())
    }

    override suspend fun getAllActivities(
        types: List<ActivityTypeDto>,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val filter = ActivityFilterAllDto(
            LinkedHashSet(types).map { ethActivityConverter.asGlobalActivityType(it) }
        )
        return getEthereumActivities(filter, continuation, size, sort)
    }

    override suspend fun getActivitiesByCollection(
        types: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val filter = ActivityFilterByCollectionDto(
            EthConverter.convertToAddress(collection),
            LinkedHashSet(types).map { ethActivityConverter.asCollectionActivityType(it) }
        )
        return getEthereumActivities(filter, continuation, size, sort)
    }

    override suspend fun getActivitiesByItem(
        types: List<ActivityTypeDto>,
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val filter = ActivityFilterByItemDto(
            EthConverter.convertToAddress(contract),
            UnionConverter.convertToBigInteger(tokenId),
            LinkedHashSet(types).map { ethActivityConverter.asItemActivityType(it) }
        )
        return getEthereumActivities(filter, continuation, size, sort)
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
        val filter = ActivityFilterByUserDto(
            users.map { EthConverter.convertToAddress(it) },
            LinkedHashSet(types).map { ethActivityConverter.asUserActivityType(it) },
            from?.epochSecond,
            to?.epochSecond
        )
        return getEthereumActivities(filter, continuation, size, sort)
    }

    private suspend fun getEthereumActivities(
        filter: ActivityFilterDto,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ) = coroutineScope {

        val continuationFactory = when (sort) {
            ActivitySortDto.EARLIEST_FIRST -> ActivityContinuation.ByLastUpdatedAndIdAsc
            ActivitySortDto.LATEST_FIRST, null -> ActivityContinuation.ByLastUpdatedAndIdDesc
        }

        val ethSort = EthConverter.convert(sort)

        val itemRequest = async { getItemActivities(filter, continuation, size, ethSort) }
        val orderRequest = async { getOrderActivities(filter, continuation, size, ethSort) }

        val itemsPage = itemRequest.await()
        val ordersPage = orderRequest.await()

        val itemActivities = itemsPage.items.map { ethActivityConverter.convert(it, blockchain) }
        val orderActivities = ordersPage.items.map { ethActivityConverter.convert(it, blockchain) }
        val allActivities = itemActivities + orderActivities

        Paging(
            continuationFactory,
            allActivities
        ).getSlice(size)
    }

    private suspend fun getItemActivities(
        filter: ActivityFilterDto,
        continuation: String?,
        size: Int,
        sort: com.rarible.protocol.dto.ActivitySortDto
    ): NftActivitiesDto {
        val itemFilter = EthActivityFilterConverter.asItemActivityFilter(filter)
        return if (itemFilter != null) {
            activityItemControllerApi.getNftActivities(itemFilter, continuation, size, sort).awaitFirst()
        } else {
            EMPTY_ITEM_ACTIVITIES
        }
    }

    private suspend fun getOrderActivities(
        filter: ActivityFilterDto,
        continuation: String?,
        size: Int,
        sort: com.rarible.protocol.dto.ActivitySortDto
    ): OrderActivitiesDto {
        val orderFilterDto = EthActivityFilterConverter.asOrderActivityFilter(filter)
        return if (orderFilterDto != null) {
            activityOrderControllerApi.getOrderActivities(orderFilterDto, continuation, size, sort).awaitFirst()
        } else {
            EMPTY_ORDER_ACTIVITIES
        }
    }
}

@CaptureSpan(type = "blockchain")
open class EthereumActivityService(
    activityItemControllerApi: NftActivityControllerApi,
    activityOrderControllerApi: OrderActivityControllerApi,
    ethActivityConverter: EthActivityConverter
) : EthActivityService(
    BlockchainDto.ETHEREUM,
    activityItemControllerApi,
    activityOrderControllerApi,
    ethActivityConverter
)

@CaptureSpan(type = "blockchain")
open class PolygonActivityService(
    activityItemControllerApi: NftActivityControllerApi,
    activityOrderControllerApi: OrderActivityControllerApi,
    ethActivityConverter: EthActivityConverter
) : EthActivityService(
    BlockchainDto.POLYGON,
    activityItemControllerApi,
    activityOrderControllerApi,
    ethActivityConverter
)