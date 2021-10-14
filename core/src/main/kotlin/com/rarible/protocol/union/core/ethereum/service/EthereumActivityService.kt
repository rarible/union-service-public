package com.rarible.protocol.union.core.ethereum.service

import com.rarible.protocol.dto.ActivityFilterAllDto
import com.rarible.protocol.dto.ActivityFilterByCollectionDto
import com.rarible.protocol.dto.ActivityFilterByItemDto
import com.rarible.protocol.dto.ActivityFilterByUserDto
import com.rarible.protocol.dto.ActivityFilterDto
import com.rarible.protocol.dto.NftActivitiesDto
import com.rarible.protocol.dto.OrderActivitiesDto
import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.union.core.continuation.ActivityContinuation
import com.rarible.protocol.union.core.continuation.page.Paging
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.core.ethereum.converter.EthActivityFilterConverter
import com.rarible.protocol.union.core.ethereum.converter.EthConverter
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant

class EthereumActivityService(
    blockchain: BlockchainDto,
    private val activityItemControllerApi: NftActivityControllerApi,
    private val activityOrderControllerApi: OrderActivityControllerApi
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
            LinkedHashSet(types).map { EthActivityConverter.asGlobalActivityType(it) }
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
            Address.apply(collection),
            LinkedHashSet(types).map { EthActivityConverter.asCollectionActivityType(it) }
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
            Address.apply(contract),
            BigInteger(tokenId),
            LinkedHashSet(types).map { EthActivityConverter.asItemActivityType(it) }
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
            users.map { Address.apply(it) },
            LinkedHashSet(types).map { EthActivityConverter.asUserActivityType(it) },
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

        val itemActivities = itemsPage.items.map { EthActivityConverter.convert(it, blockchain) }
        val orderActivities = ordersPage.items.map { EthActivityConverter.convert(it, blockchain) }
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
