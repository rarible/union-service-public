package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.logging.Logger
import com.rarible.protocol.dto.ActivitiesByIdRequestDto
import com.rarible.protocol.dto.AuctionActivitiesDto
import com.rarible.protocol.dto.AuctionActivityFilterAllDto
import com.rarible.protocol.dto.AuctionActivityFilterByCollectionDto
import com.rarible.protocol.dto.AuctionActivityFilterByItemDto
import com.rarible.protocol.dto.AuctionActivityFilterByUserDto
import com.rarible.protocol.dto.AuctionActivityFilterDto
import com.rarible.protocol.dto.NftActivitiesDto
import com.rarible.protocol.dto.NftActivityFilterAllDto
import com.rarible.protocol.dto.NftActivityFilterByCollectionDto
import com.rarible.protocol.dto.NftActivityFilterByItemAndOwnerDto
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
import com.rarible.protocol.union.core.model.ItemAndOwnerActivityType
import com.rarible.protocol.union.core.model.TypedActivityId
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.ActivityTypeDto.AUCTION_BID
import com.rarible.protocol.union.dto.ActivityTypeDto.AUCTION_CANCEL
import com.rarible.protocol.union.dto.ActivityTypeDto.AUCTION_CREATED
import com.rarible.protocol.union.dto.ActivityTypeDto.AUCTION_ENDED
import com.rarible.protocol.union.dto.ActivityTypeDto.AUCTION_FINISHED
import com.rarible.protocol.union.dto.ActivityTypeDto.AUCTION_STARTED
import com.rarible.protocol.union.dto.ActivityTypeDto.BID
import com.rarible.protocol.union.dto.ActivityTypeDto.BURN
import com.rarible.protocol.union.dto.ActivityTypeDto.CANCEL_BID
import com.rarible.protocol.union.dto.ActivityTypeDto.CANCEL_LIST
import com.rarible.protocol.union.dto.ActivityTypeDto.LIST
import com.rarible.protocol.union.dto.ActivityTypeDto.MINT
import com.rarible.protocol.union.dto.ActivityTypeDto.SELL
import com.rarible.protocol.union.dto.ActivityTypeDto.TRANSFER
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.continuation.ActivityContinuation
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import kotlinx.coroutines.Deferred
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
        private val logger by Logger()

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

    override suspend fun getAllActivitiesSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): Slice<ActivityDto> = coroutineScope {
        val continuationFactory = when (sort) {
            SyncSortDto.DB_UPDATE_DESC -> ActivityContinuation.ByLastUpdatedSyncAndIdDesc
            SyncSortDto.DB_UPDATE_ASC, null -> ActivityContinuation.ByLastUpdatedSyncAndIdAsc
        }

        val ethSort = EthConverter.convert(sort)

        val allActivities = when (type) {
            SyncTypeDto.NFT -> itemActivitiesAsync(continuation, size, ethSort).await()
            SyncTypeDto.ORDER -> orderActivitiesAsync(continuation, size, ethSort).await()
            SyncTypeDto.AUCTION -> auctionActivitiesAsync(continuation, size, ethSort).await()
            else -> allActivities(continuation, size, ethSort)
        }

        Paging(
            continuationFactory,
            allActivities
        ).getSlice(size)
    }

    override suspend fun getAllRevertedActivitiesSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): Slice<ActivityDto> {
        val continuationFactory = when (sort) {
            SyncSortDto.DB_UPDATE_DESC -> ActivityContinuation.ByLastUpdatedSyncAndIdDesc
            SyncSortDto.DB_UPDATE_ASC, null -> ActivityContinuation.ByLastUpdatedSyncAndIdAsc
        }
        val ethSort = EthConverter.convert(sort)

        val allActivities = when (type) {
            SyncTypeDto.ORDER -> orderRevertedActivitiesAsync(continuation, size, ethSort).await()
            SyncTypeDto.NFT -> itemRevertedActivitiesAsync(continuation, size, ethSort).await()
            SyncTypeDto.AUCTION -> auctionRevertedActivitiesAsync(continuation, size, ethSort).await()
            null -> allRevertedActivities(continuation, size, ethSort)
        }
        return Paging(
            continuationFactory,
            allActivities
        ).getSlice(size)
    }

    private suspend fun allActivities(
        continuation: String?,
        size: Int,
        ethSort: com.rarible.protocol.dto.SyncSortDto?
    ): List<ActivityDto> = coroutineScope {
        val itemActivities = itemActivitiesAsync(continuation, size, ethSort)
        val orderActivities = orderActivitiesAsync(continuation, size, ethSort)
        val auctionActivities = auctionActivitiesAsync(continuation, size, ethSort)

        itemActivities.await() + orderActivities.await() + auctionActivities.await()
    }

    private suspend fun allRevertedActivities(
        continuation: String?,
        size: Int,
        ethSort: com.rarible.protocol.dto.SyncSortDto?
    ): List<ActivityDto> = coroutineScope {
        val orderRevertedActivities = orderRevertedActivitiesAsync(continuation, size, ethSort)
        val itemRevertedActivities = itemRevertedActivitiesAsync(continuation, size, ethSort)
        val auctionRevertedActivities = auctionRevertedActivitiesAsync(continuation, size, ethSort)
        orderRevertedActivities.await() + itemRevertedActivities.await() + auctionRevertedActivities.await()
    }

    private suspend fun itemActivitiesAsync(
        continuation: String?,
        size: Int,
        ethSort: com.rarible.protocol.dto.SyncSortDto?
    ): Deferred<List<ActivityDto>> = coroutineScope {
        async {
            val itemsDto = activityItemControllerApi.getNftActivitiesSync(continuation, size, ethSort).awaitFirst()
            itemsDto.items.map {
                ethActivityConverter.convert(it, blockchain)
            }
        }
    }

    private suspend fun orderActivitiesAsync(
        continuation: String?,
        size: Int,
        ethSort: com.rarible.protocol.dto.SyncSortDto?
    ): Deferred<List<ActivityDto>> = coroutineScope {
        async {
            val itemsDto = activityOrderControllerApi.getOrderActivitiesSync(continuation, size, ethSort).awaitFirst()
            itemsDto.items.map {
                ethActivityConverter.convert(it, blockchain)
            }
        }
    }

    private suspend fun auctionActivitiesAsync(
        continuation: String?,
        size: Int,
        ethSort: com.rarible.protocol.dto.SyncSortDto?
    ): Deferred<List<ActivityDto>> = coroutineScope {
        async {
            val itemsDto =
                activityAuctionControllerApi.getAuctionActivitiesSync(continuation, size, ethSort).awaitFirst()
            itemsDto.items.map {
                ethActivityConverter.convert(it, blockchain)
            }
        }
    }

    private suspend fun orderRevertedActivitiesAsync(
        continuation: String?,
        size: Int,
        ethSort: com.rarible.protocol.dto.SyncSortDto?
    ): Deferred<List<ActivityDto>> = coroutineScope {
        async {
            val itemsDto = activityOrderControllerApi.getOrderRevertedActivitiesSync(continuation, size, ethSort).awaitFirst()
            itemsDto.items.map {
                ethActivityConverter.convert(it, blockchain)
            }
        }
    }

    private suspend fun auctionRevertedActivitiesAsync(
        continuation: String?,
        size: Int,
        ethSort: com.rarible.protocol.dto.SyncSortDto?
    ): Deferred<List<ActivityDto>> = coroutineScope {
        async { emptyList() }
    }

    private suspend fun itemRevertedActivitiesAsync(
        continuation: String?,
        size: Int,
        ethSort: com.rarible.protocol.dto.SyncSortDto?
    ): Deferred<List<ActivityDto>> = coroutineScope {
        async { emptyList() }
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

    override suspend fun getActivitiesByItemAndOwner(
        types: List<ItemAndOwnerActivityType>,
        itemId: String,
        owner: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?,
    ): Slice<ActivityDto> {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        val nftFilter = ethActivityConverter.convertToNftItemAndOwnerTypes(types)?.let {
            NftActivityFilterByItemAndOwnerDto(Address.apply(contract), tokenId, Address.apply(owner), it)
        }
        return getEthereumActivities(nftFilter, null, null, continuation, size, sort)
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

    override suspend fun getActivitiesByIds(ids: List<TypedActivityId>): List<ActivityDto> = coroutineScope {
        val itemActivitiesIds = mutableListOf<String>()
        val orderActivitiesIds = mutableListOf<String>()
        val auctionActivitiesIds = mutableListOf<String>()

        ids.forEach { (id, type) ->
            when (type) {
                TRANSFER, MINT, BURN -> {
                    itemActivitiesIds.add(id)
                }
                BID, LIST, SELL, CANCEL_LIST, CANCEL_BID -> {
                    orderActivitiesIds.add(id)
                }
                AUCTION_BID, AUCTION_CREATED, AUCTION_CANCEL, AUCTION_FINISHED, AUCTION_STARTED, AUCTION_ENDED -> {
                    auctionActivitiesIds.add(id)
                }
            }
        }

        val itemRequest = async {
            if (itemActivitiesIds.isNotEmpty()) {
                activityItemControllerApi.getNftActivitiesById(ActivitiesByIdRequestDto(itemActivitiesIds)).awaitFirst()
            } else {
                EMPTY_ITEM_ACTIVITIES
            }
        }
        val orderRequest = async {
            if (orderActivitiesIds.isNotEmpty()) {
                activityOrderControllerApi.getOrderActivitiesById(ActivitiesByIdRequestDto(orderActivitiesIds)).awaitFirst()
            } else {
                EMPTY_ORDER_ACTIVITIES
            }
        }
        val auctionRequest = async {
            if (auctionActivitiesIds.isNotEmpty()) {
                activityAuctionControllerApi.getAuctionActivitiesById(ActivitiesByIdRequestDto(auctionActivitiesIds)).awaitFirst()
            } else {
                EMPTY_AUCTION_ACTIVITIES
            }
        }

        val items = itemRequest.await()
        val orders = orderRequest.await()
        val auctions = auctionRequest.await()

        val itemActivities = items.items.map { ethActivityConverter.convert(it, blockchain) }
        val orderActivities = orders.items.map { ethActivityConverter.convert(it, blockchain) }
        val auctionActivities = auctions.items.map { ethActivityConverter.convert(it, blockchain) }

        itemActivities + orderActivities + auctionActivities
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
