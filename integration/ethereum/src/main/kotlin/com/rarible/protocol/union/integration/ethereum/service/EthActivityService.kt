package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.dto.AuctionActivitiesDto
import com.rarible.protocol.dto.AuctionActivityFilterAllDto
import com.rarible.protocol.dto.AuctionActivityFilterByCollectionDto
import com.rarible.protocol.dto.AuctionActivityFilterByItemDto
import com.rarible.protocol.dto.AuctionActivityFilterByUserDto
import com.rarible.protocol.dto.AuctionActivityFilterDto
import com.rarible.protocol.dto.NftActivitiesDto
import com.rarible.protocol.dto.NftActivityFilterAllDto
import com.rarible.protocol.dto.NftActivityFilterByCollectionDto
import com.rarible.protocol.dto.NftActivityFilterByItemDto
import com.rarible.protocol.dto.NftActivityFilterByUserDto
import com.rarible.protocol.dto.NftActivityFilterDto
import com.rarible.protocol.dto.OrderActivitiesDto
import com.rarible.protocol.dto.OrderActivityFilterAllDto
import com.rarible.protocol.dto.OrderActivityFilterByCollectionDto
import com.rarible.protocol.dto.OrderActivityFilterByItemDto
import com.rarible.protocol.dto.OrderActivityFilterByUserDto
import com.rarible.protocol.dto.OrderActivityFilterDto
import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.order.api.client.AuctionActivityControllerApi
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.continuation.ActivityContinuation
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import scalether.domain.Address
import java.time.Instant

open class EthActivityService(
    blockchain: BlockchainDto,
    private val activityItemControllerApi: NftActivityControllerApi,
    private val activityOrderControllerApi: OrderActivityControllerApi,
    private val activityAuctionControllerApi: AuctionActivityControllerApi,
    private val ethActivityConverter: EthActivityConverter
) : AbstractBlockchainService(blockchain), ActivityService {

    companion object {

        private val EMPTY_ORDER_ACTIVITIES = OrderActivitiesDto(null, listOf())
        private val EMPTY_AUCTION_ACTIVITIES = AuctionActivitiesDto(null, listOf())
        private val EMPTY_ITEM_ACTIVITIES = NftActivitiesDto(null, listOf())
    }

    override suspend fun getAllActivities(
        types: List<ActivityTypeDto>,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val nftFilter = ethActivityConverter.convertToNftAllTypes(types)?.let {
            NftActivityFilterAllDto(it)
        }
        val orderFilter = ethActivityConverter.convertToOrderAllTypes(types)?.let {
            OrderActivityFilterAllDto(it)
        }
        val auctionFilter = ethActivityConverter.convertToAuctionAllTypes(types)?.let {
            AuctionActivityFilterAllDto(it)
        }
        return getEthereumActivities(nftFilter, orderFilter, auctionFilter, continuation, size, sort)
    }

    override suspend fun getActivitiesByCollection(
        types: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val nftFilter = ethActivityConverter.convertToNftCollectionTypes(types)?.let {
            NftActivityFilterByCollectionDto(Address.apply(collection), it)
        }
        val orderFilter = ethActivityConverter.convertToOrderCollectionTypes(types)?.let {
            OrderActivityFilterByCollectionDto(Address.apply(collection), it)
        }
        val auctionFilter = ethActivityConverter.convertToAuctionCollectionTypes(types)?.let {
            AuctionActivityFilterByCollectionDto(Address.apply(collection), it)
        }
        return getEthereumActivities(nftFilter, orderFilter, auctionFilter, continuation, size, sort)
    }

    override suspend fun getActivitiesByItem(
        types: List<ActivityTypeDto>,
        itemId: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        val nftFilter = ethActivityConverter.convertToNftItemTypes(types)?.let {
            NftActivityFilterByItemDto(Address.apply(contract), tokenId, it)
        }
        val orderFilter = ethActivityConverter.convertToOrderItemTypes(types)?.let {
            OrderActivityFilterByItemDto(Address.apply(contract), tokenId, it)
        }
        val auctionFilter = ethActivityConverter.convertToAuctionItemTypes(types)?.let {
            AuctionActivityFilterByItemDto(Address.apply(contract), tokenId, it)
        }
        return getEthereumActivities(nftFilter, orderFilter, auctionFilter, continuation, size, sort)
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
        val userAddresses = users.map { EthConverter.convertToAddress(it) }
        val nftFilter = ethActivityConverter.convertToNftUserTypes(types)?.let {
            NftActivityFilterByUserDto(userAddresses, it, from?.epochSecond, to?.epochSecond)
        }
        val orderFilter = ethActivityConverter.convertToOrderUserTypes(types)?.let {
            OrderActivityFilterByUserDto(userAddresses, it, from?.epochSecond, to?.epochSecond)
        }
        val auctionFilter = ethActivityConverter.convertToAuctionUserTypes(types)?.let {
            AuctionActivityFilterByUserDto(userAddresses, it)
        }
        return getEthereumActivities(nftFilter, orderFilter, auctionFilter, continuation, size, sort)
    }

    private suspend fun getEthereumActivities(
        nftFilter: NftActivityFilterDto?,
        orderFilter: OrderActivityFilterDto?,
        auctionFilter: AuctionActivityFilterDto?,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ) = coroutineScope {
        val continuationFactory = when (sort) {
            ActivitySortDto.EARLIEST_FIRST -> ActivityContinuation.ByLastUpdatedAndIdAsc
            ActivitySortDto.LATEST_FIRST, null -> ActivityContinuation.ByLastUpdatedAndIdDesc
        }

        val ethSort = EthConverter.convert(sort)

        val itemRequest = async { getItemActivities(nftFilter, continuation, size, ethSort) }
        val orderRequest = async { getOrderActivities(orderFilter, continuation, size, ethSort) }
        val auctionRequest = async { getAuctionActivities(auctionFilter, continuation, size, ethSort) }

        val itemsPage = itemRequest.await()
        val ordersPage = orderRequest.await()
        val auctionsPage = auctionRequest.await()

        val itemActivities = itemsPage.items.map { ethActivityConverter.convert(it, blockchain) }
        val orderActivities = ordersPage.items.map { ethActivityConverter.convert(it, blockchain) }
        val auctionActivities = auctionsPage.items.map { ethActivityConverter.convert(it, blockchain) }
        val allActivities = itemActivities + orderActivities + auctionActivities

        Paging(
            continuationFactory,
            allActivities
        ).getSlice(size)
    }

    private suspend fun getItemActivities(
        filter: NftActivityFilterDto?,
        continuation: String?,
        size: Int,
        sort: com.rarible.protocol.dto.ActivitySortDto
    ): NftActivitiesDto {
        return if (filter != null) {
            activityItemControllerApi.getNftActivities(filter, continuation, size, sort).awaitFirst()
        } else {
            EMPTY_ITEM_ACTIVITIES
        }
    }

    private suspend fun getOrderActivities(
        filter: OrderActivityFilterDto?,
        continuation: String?,
        size: Int,
        sort: com.rarible.protocol.dto.ActivitySortDto
    ): OrderActivitiesDto {
        return if (filter != null) {
            activityOrderControllerApi.getOrderActivities(filter, continuation, size, sort).awaitFirst()
        } else {
            EMPTY_ORDER_ACTIVITIES
        }
    }

    private suspend fun getAuctionActivities(
        filter: AuctionActivityFilterDto?,
        continuation: String?,
        size: Int,
        sort: com.rarible.protocol.dto.ActivitySortDto
    ): AuctionActivitiesDto {
        return if (filter != null) {
            activityAuctionControllerApi.getAuctionActivities(filter, continuation, size, sort).awaitFirst()
        } else {
            EMPTY_AUCTION_ACTIVITIES
        }
    }
}

@CaptureSpan(type = "blockchain")
open class EthereumActivityService(
    activityItemControllerApi: NftActivityControllerApi,
    activityOrderControllerApi: OrderActivityControllerApi,
    activityAuctionControllerApi: AuctionActivityControllerApi,
    ethActivityConverter: EthActivityConverter
) : EthActivityService(
    BlockchainDto.ETHEREUM,
    activityItemControllerApi,
    activityOrderControllerApi,
    activityAuctionControllerApi,
    ethActivityConverter
)

@CaptureSpan(type = "blockchain")
open class PolygonActivityService(
    activityItemControllerApi: NftActivityControllerApi,
    activityOrderControllerApi: OrderActivityControllerApi,
    activityAuctionControllerApi: AuctionActivityControllerApi,
    ethActivityConverter: EthActivityConverter
) : EthActivityService(
    BlockchainDto.POLYGON,
    activityItemControllerApi,
    activityOrderControllerApi,
    activityAuctionControllerApi,
    ethActivityConverter
)
