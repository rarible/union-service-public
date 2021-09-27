package com.rarible.protocol.union.core.ethereum.service

import com.rarible.protocol.dto.*
import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.union.core.continuation.ContinuationPaging
import com.rarible.protocol.union.core.ethereum.converter.EthActivityFilterConverter
import com.rarible.protocol.union.core.ethereum.converter.EthConverter
import com.rarible.protocol.union.core.ethereum.converter.EthUnionActivityConverter
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.dto.continuation.UnionActivityContinuation
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import scalether.domain.Address
import java.math.BigInteger

class EthereumActivityService(
    blockchain: BlockchainDto,
    private val activityItemControllerApi: NftActivityControllerApi,
    private val activityOrderControllerApi: OrderActivityControllerApi
) : AbstractEthereumService(blockchain), ActivityService {

    companion object {
        private val EMPTY_ORDER_ACTIVITIES = OrderActivitiesDto(null, listOf())
        private val EMPTY_ITEM_ACTIVITIES = NftActivitiesDto(null, listOf())
    }

    override suspend fun getAllActivities(
        types: List<UnionActivityTypeDto>,
        continuation: String?,
        size: Int,
        sort: UnionActivitySortDto?
    ): UnionActivitiesDto {
        val filter = ActivityFilterAllDto(
            LinkedHashSet(types).map { EthUnionActivityConverter.asGlobalActivityType(it) }
        )
        return getEthereumActivities(filter, continuation, size, sort)
    }

    override suspend fun getActivitiesByCollection(
        types: List<UnionActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int,
        sort: UnionActivitySortDto?
    ): UnionActivitiesDto {
        val filter = ActivityFilterByCollectionDto(
            Address.apply(collection),
            LinkedHashSet(types).map { EthUnionActivityConverter.asCollectionActivityType(it) }
        )
        return getEthereumActivities(filter, continuation, size, sort)
    }

    override suspend fun getActivitiesByItem(
        types: List<UnionActivityTypeDto>,
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int,
        sort: UnionActivitySortDto?
    ): UnionActivitiesDto {
        val filter = ActivityFilterByItemDto(
            Address.apply(contract),
            BigInteger(tokenId),
            LinkedHashSet(types).map { EthUnionActivityConverter.asItemActivityType(it) }
        )
        return getEthereumActivities(filter, continuation, size, sort)
    }

    override suspend fun getActivitiesByUser(
        types: List<UnionUserActivityTypeDto>,
        users: List<String>,
        continuation: String?,
        size: Int,
        sort: UnionActivitySortDto?
    ): UnionActivitiesDto {
        val filter = ActivityFilterByUserDto(
            users.map { Address.apply(it) },
            LinkedHashSet(types).map { EthUnionActivityConverter.asUserActivityType(it) }
        )
        return getEthereumActivities(filter, continuation, size, sort)
    }

    private suspend fun getEthereumActivities(
        filter: ActivityFilterDto,
        continuation: String?,
        size: Int,
        sort: UnionActivitySortDto?
    ) = coroutineScope {

        val continuationFactory = when (sort) {
            UnionActivitySortDto.EARLIEST_FIRST -> UnionActivityContinuation.ByLastUpdatedAndIdAsc
            UnionActivitySortDto.LATEST_FIRST, null -> UnionActivityContinuation.ByLastUpdatedAndIdDesc
        }

        val ethSort = EthConverter.convert(sort)

        val itemRequest = async { getItemActivities(filter, continuation, size, ethSort) }
        val orderRequest = async { getOrderActivities(filter, continuation, size, ethSort) }

        val itemsPage = itemRequest.await()
        val ordersPage = orderRequest.await()

        val itemUnionActivities = itemsPage.items.map { EthUnionActivityConverter.convert(it, blockchain) }
        val orderUnionActivities = ordersPage.items.map { EthUnionActivityConverter.convert(it, blockchain) }
        val allActivities = itemUnionActivities + orderUnionActivities

        val combinedPage = ContinuationPaging(
            continuationFactory,
            allActivities
        ).getPage(size)

            UnionActivitiesDto(combinedPage.printContinuation(), combinedPage.entities)
        }

    private suspend fun getItemActivities(
        filter: ActivityFilterDto,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto
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
        sort: ActivitySortDto
    ): OrderActivitiesDto {
        val orderFilterDto = EthActivityFilterConverter.asOrderActivityFilter(filter)
        return if (orderFilterDto != null) {
            activityOrderControllerApi.getOrderActivities(orderFilterDto, continuation, size, sort).awaitFirst()
        } else {
            EMPTY_ORDER_ACTIVITIES
        }
    }

}