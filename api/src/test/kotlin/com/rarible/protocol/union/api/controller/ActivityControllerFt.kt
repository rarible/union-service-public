package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.AuctionActivitiesDto
import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.NftActivitiesDto
import com.rarible.protocol.dto.OrderActivitiesDto
import com.rarible.protocol.union.api.client.ActivityControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.AuctionStartActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderBidActivityDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionStartActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemMintActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderBidActivity
import com.rarible.protocol.union.test.data.randomFlowAddress
import com.rarible.protocol.union.test.data.randomFlowCancelListActivityDto
import com.rarible.protocol.union.test.data.randomFlowItemId
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono
import java.time.Instant
import java.time.temporal.ChronoUnit

@FlowPreview
@IntegrationTest
class ActivityControllerFt : AbstractIntegrationTest() {

    private val continuation: String? = null
    private val defaultSize = PageSize.ACTIVITY.default
    private val maxSize = PageSize.ACTIVITY.max
    private val sort: com.rarible.protocol.union.dto.ActivitySortDto? = null

    @Autowired
    lateinit var activityControllerApi: ActivityControllerApi

    @Test
    fun `get activities by collection - ethereum`() = runBlocking<Unit> {
        // Here we expect only one query - to Order activities, since there is only order-related activity type
        val types = listOf(ActivityTypeDto.SELL)
        val ethCollectionId = CollectionIdDto(BlockchainDto.ETHEREUM, randomEthAddress())
        val orderActivity = randomEthOrderBidActivity()

        coEvery {
            testEthereumActivityOrderApi.getOrderActivities(any(), isNull(), eq(defaultSize), ActivitySortDto.LATEST_FIRST)
        } returns OrderActivitiesDto(null, listOf(orderActivity)).toMono()

        val activities = activityControllerApi.getActivitiesByCollection(
            types, ethCollectionId.fullId(), continuation, null, defaultSize, sort
        ).awaitFirst()

        assertThat(activities.activities).hasSize(1)
        assertThat(activities.activities[0]).isInstanceOf(OrderBidActivityDto::class.java)
    }

    @Test
    fun `get activities by collection with cursor - ethereum`() = runBlocking<Unit> {
        // Here we expect only one query - to Order activities, since there is only order-related activity type
        val types = listOf(ActivityTypeDto.SELL)
        val ethCollectionId = CollectionIdDto(BlockchainDto.ETHEREUM, randomEthAddress())
        val orderActivity = randomEthOrderBidActivity()
        val ethContinuation = randomEthAddress()
        val cursor = CombinedContinuation(
            mapOf(
                BlockchainDto.ETHEREUM.toString() to ethContinuation
            )
        )

        coEvery {
            testEthereumActivityOrderApi.getOrderActivities(any(), eq(ethContinuation), eq(defaultSize), ActivitySortDto.LATEST_FIRST)
        } returns OrderActivitiesDto(null, listOf(orderActivity)).toMono()

        val activities = activityControllerApi.getActivitiesByCollection(
            types, ethCollectionId.fullId(), null, cursor.toString(), defaultSize, sort
        ).awaitFirst()

        assertThat(activities.activities).hasSize(1)
        assertThat(activities.activities[0]).isInstanceOf(OrderBidActivityDto::class.java)
    }

    @Test
    fun `get activities by collection - flow`() = runBlocking<Unit> {
        val types = ActivityTypeDto.values().toList()
        val flowCollectionId = randomFlowAddress()
        val activity = randomFlowCancelListActivityDto()

        coEvery {
            testFlowActivityApi.getNftOrderActivitiesByCollection(
                types.map { it.name },
                flowCollectionId.value,
                continuation,
                maxSize,
                any()
            )
        } returns FlowActivitiesDto(1, null, listOf(activity)).toMono()

        val activities = activityControllerApi.getActivitiesByCollection(
            types, flowCollectionId.fullId(), continuation, null, 100000, sort
        ).awaitFirst()

        val flowItem = activities.activities[0]
        assertThat(flowItem.id.value).isEqualTo(activity.id)
    }

    @Test
    fun `get activities by item - ethereum`() = runBlocking<Unit> {
        // All activity types specified here, so both Order and Nft indexer should be requested for activities
        val types = ActivityTypeDto.values().toList()
        val ethItemId = randomEthItemId()
        val now = nowMillis()
        val orderActivity = randomEthOrderBidActivity().copy(date = now)
        val itemActivity = randomEthItemMintActivity().copy(date = now.minusSeconds(5))
        val auctionActivity = randomEthAuctionStartActivity().copy(date = now.minusSeconds(15))

        coEvery {
            testEthereumActivityOrderApi.getOrderActivities(
                any(),
                isNull(),
                eq(maxSize),
                ActivitySortDto.LATEST_FIRST
            )
        } returns OrderActivitiesDto(null, listOf(orderActivity)).toMono()

        coEvery {
            testEthereumActivityAuctionApi.getAuctionActivities(
                any(),
                isNull(),
                eq(maxSize),
                ActivitySortDto.LATEST_FIRST
            )
        } returns AuctionActivitiesDto(null, listOf(auctionActivity)).toMono()

        coEvery {
            testEthereumActivityItemApi.getNftActivities(
                any(),
                isNull(),
                eq(maxSize),
                ActivitySortDto.LATEST_FIRST
            )
        } returns NftActivitiesDto(null, listOf(itemActivity)).toMono()

        val activities = activityControllerApi.getActivitiesByItem(
            types, ethItemId.fullId(), continuation, null, 10000000, sort
        ).awaitFirst()

        assertThat(activities.activities).hasSize(3)
        assertThat(activities.activities[0]).isInstanceOf(OrderBidActivityDto::class.java)
        assertThat(activities.activities[1]).isInstanceOf(MintActivityDto::class.java)
        assertThat(activities.activities[2]).isInstanceOf(AuctionStartActivityDto::class.java)
    }

    @Test
    fun `get activities by item with cursor - ethereum`() = runBlocking<Unit> {
        // All activity types specified here, so both Order and Nft indexer should be requested for activities
        val types = ActivityTypeDto.values().toList()
        val ethItemId = randomEthItemId()
        val now = nowMillis()
        val orderActivity = randomEthOrderBidActivity().copy(date = now)
        val itemActivity = randomEthItemMintActivity().copy(date = now.minusSeconds(5))
        val ethContinuation = randomEthAddress()
        val cursor = CombinedContinuation(
            mapOf(
                BlockchainDto.ETHEREUM.toString() to ethContinuation
            )
        )

        coEvery {
            testEthereumActivityOrderApi.getOrderActivities(
                any(),
                eq(ethContinuation),
                eq(1),
                ActivitySortDto.LATEST_FIRST
            )
        } returns OrderActivitiesDto(null, listOf(orderActivity)).toMono()

        coEvery {
            testEthereumActivityAuctionApi.getAuctionActivities(
                any(),
                eq(ethContinuation),
                eq(1),
                ActivitySortDto.LATEST_FIRST
            )
        } returns AuctionActivitiesDto(null, emptyList()).toMono()

        coEvery {
            testEthereumActivityItemApi.getNftActivities(
                any(),
                eq(ethContinuation),
                eq(1),
                ActivitySortDto.LATEST_FIRST
            )
        } returns NftActivitiesDto(null, listOf(itemActivity)).toMono()

        val activities = activityControllerApi.getActivitiesByItem(
            types, ethItemId.fullId(), null, cursor.toString(), 1, sort
        ).awaitFirst()

        assertThat(activities.activities).hasSize(1)
        assertThat(activities.continuation).isNotNull
        assertThat(activities.cursor).isEqualTo("${BlockchainDto.ETHEREUM}:${activities.continuation}")
    }

    @Test
    fun `get activities by item - flow`() = runBlocking<Unit> {
        val types = ActivityTypeDto.values().toList()
        val flowItemId = randomFlowItemId()
        val (contract, tokenId) = CompositeItemIdParser.split(flowItemId.value)
        val activity = randomFlowCancelListActivityDto()

        coEvery {
            testFlowActivityApi.getNftOrderActivitiesByItem(
                types.map { it.name },
                contract,
                tokenId.toLong(),
                continuation, defaultSize, any()
            )
        } returns FlowActivitiesDto(1, null, listOf(activity)).toMono()

        val activities = activityControllerApi.getActivitiesByItem(
            types, flowItemId.fullId(), continuation, null, defaultSize, sort
        ).awaitFirst()

        val flowItem = activities.activities[0]
        assertThat(flowItem.id.value).isEqualTo(activity.id)
    }

    @Test
    fun `get all activities - asc`() = runBlocking<Unit> {
        val types = ActivityTypeDto.values().toList()
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.POLYGON, BlockchainDto.FLOW)
        val size = 3
        val now = nowMillis()

        // From this list of activities we expect only the oldest 3 in response ordered as:
        // polygonItemActivity1, ethOrderActivity3 and flowActivity
        val ethOrderActivity1 = randomEthOrderBidActivity().copy(date = now)
        val ethOrderActivity2 = randomEthOrderBidActivity().copy(date = now.minusSeconds(5))
        val ethOrderActivity3 = randomEthOrderBidActivity().copy(date = now.minusSeconds(10))
        val ethItemActivity1 = randomEthItemMintActivity().copy(date = now.minusSeconds(4))
        val ethItemActivity2 = randomEthItemMintActivity().copy(date = now.minusSeconds(7))
        val ethItemActivity3 = randomEthItemMintActivity().copy(date = now.minusSeconds(8))
        val polygonOrderActivity1 = randomEthOrderBidActivity().copy(date = now.minusSeconds(1))
        val polygonOrderActivity2 = randomEthOrderBidActivity().copy(date = now.minusSeconds(3))
        val polygonItemActivity1 = randomEthItemMintActivity().copy(date = now.minusSeconds(12))
        val polygonItemActivity2 = randomEthItemMintActivity().copy(date = now.minusSeconds(2))
        val flowActivity = randomFlowCancelListActivityDto().copy(date = now.minusSeconds(9))

        val ethOrderActivities = listOf(ethOrderActivity1, ethOrderActivity2, ethOrderActivity3)
        val ethItemActivities = listOf(ethItemActivity1, ethItemActivity2, ethItemActivity3)
        val polygonOrderActivities = listOf(polygonOrderActivity1, polygonOrderActivity2)
        val polygonItemActivities = listOf(polygonItemActivity1, polygonItemActivity2)
        val flowActivities = listOf(flowActivity)

        // Since all activity types specified in request, all of existing clients should be requested
        coEvery {
            testEthereumActivityOrderApi.getOrderActivities(any(), isNull(), eq(size), ActivitySortDto.EARLIEST_FIRST)
        } returns OrderActivitiesDto(null, ethOrderActivities).toMono()

        coEvery {
            testEthereumActivityAuctionApi.getAuctionActivities(any(), isNull(), eq(size), ActivitySortDto.EARLIEST_FIRST)
        } returns AuctionActivitiesDto(null, emptyList()).toMono()

        coEvery {
            testEthereumActivityItemApi.getNftActivities(any(), isNull(), eq(size), ActivitySortDto.EARLIEST_FIRST)
        } returns NftActivitiesDto(null, ethItemActivities).toMono()

        coEvery {
            testPolygonActivityOrderApi.getOrderActivities(any(), isNull(), eq(size), ActivitySortDto.EARLIEST_FIRST)
        } returns OrderActivitiesDto(null, polygonOrderActivities).toMono()

        coEvery {
            testPolygonActivityAuctionApi.getAuctionActivities(any(), isNull(), eq(size), ActivitySortDto.EARLIEST_FIRST)
        } returns AuctionActivitiesDto(null, emptyList()).toMono()

        coEvery {
            testPolygonActivityItemApi.getNftActivities(any(), isNull(), eq(size), ActivitySortDto.EARLIEST_FIRST)
        } returns NftActivitiesDto(null, polygonItemActivities).toMono()

        coEvery {
            testFlowActivityApi.getNftOrderAllActivities(any(), isNull(), eq(size), any())
        } returns FlowActivitiesDto(1, null, flowActivities).toMono()

        val activities = activityControllerApi.getAllActivities(
            types, blockchains, null, null, size, com.rarible.protocol.union.dto.ActivitySortDto.EARLIEST_FIRST
        ).awaitFirst()

        assertThat(activities.activities).hasSize(3)
        assertThat(activities.continuation).isNotNull

        val oldestActivity = activities.activities[0]
        val secondActivity = activities.activities[1]
        val newestActivity = activities.activities[2]

        assertThat(oldestActivity.id.value).isEqualTo(polygonItemActivity1.id)
        assertThat(secondActivity.id.value).isEqualTo(ethOrderActivity3.id)
        assertThat(newestActivity.id.value).isEqualTo(flowActivity.id)

        assertThat(activities.cursor).isNotNull
        val cursor = CombinedContinuation.parse(activities.cursor)
        assertThat(cursor.continuations).containsKey(BlockchainDto.ETHEREUM.name)
        assertThat(cursor.continuations).containsKey(BlockchainDto.POLYGON.name)
        assertThat(cursor.continuations).containsEntry(BlockchainDto.FLOW.name, ArgSlice.COMPLETED)
    }

    @Test
    fun `get all activities - asc with cursor`() = runBlocking<Unit> {
        val types = ActivityTypeDto.values().toList()
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.POLYGON, BlockchainDto.FLOW)
        val size = 3
        val now = nowMillis()
        val ethContinuation = "${now.toEpochMilli()}_${randomString()}"
        val polyContinuation = "${now.toEpochMilli()}_${randomString()}"
        val flowContinuation = "${now.toEpochMilli()}_${randomInt()}"
        val cursorArg = CombinedContinuation(
            mapOf(
                BlockchainDto.ETHEREUM.toString() to ethContinuation,
                BlockchainDto.POLYGON.toString() to polyContinuation,
                BlockchainDto.FLOW.toString() to flowContinuation
            )
        )

        // From this list of activities we expect only the oldest 3 in response ordered as:
        // polygonItemActivity1, ethOrderActivity3 and flowActivity
        val ethOrderActivity1 = randomEthOrderBidActivity().copy(date = now)
        val ethOrderActivity2 = randomEthOrderBidActivity().copy(date = now.minusSeconds(5))
        val ethOrderActivity3 = randomEthOrderBidActivity().copy(date = now.minusSeconds(10))
        val ethItemActivity1 = randomEthItemMintActivity().copy(date = now.minusSeconds(4))
        val ethItemActivity2 = randomEthItemMintActivity().copy(date = now.minusSeconds(7))
        val ethItemActivity3 = randomEthItemMintActivity().copy(date = now.minusSeconds(8))
        val polygonOrderActivity1 = randomEthOrderBidActivity().copy(date = now.minusSeconds(1))
        val polygonOrderActivity2 = randomEthOrderBidActivity().copy(date = now.minusSeconds(3))
        val polygonItemActivity1 = randomEthItemMintActivity().copy(date = now.minusSeconds(12))
        val polygonItemActivity2 = randomEthItemMintActivity().copy(date = now.minusSeconds(2))
        val flowActivity = randomFlowCancelListActivityDto().copy(date = now.minusSeconds(9))

        val ethOrderActivities = listOf(ethOrderActivity1, ethOrderActivity2, ethOrderActivity3)
        val ethItemActivities = listOf(ethItemActivity1, ethItemActivity2, ethItemActivity3)
        val polygonOrderActivities = listOf(polygonOrderActivity1, polygonOrderActivity2)
        val polygonItemActivities = listOf(polygonItemActivity1, polygonItemActivity2)
        val flowActivities = listOf(flowActivity)

        // Since all activity types specified in request, all of existing clients should be requested
        coEvery {
            testEthereumActivityOrderApi.getOrderActivities(any(), eq(ethContinuation), eq(size), ActivitySortDto.EARLIEST_FIRST)
        } returns OrderActivitiesDto(null, ethOrderActivities).toMono()

        coEvery {
            testEthereumActivityAuctionApi.getAuctionActivities(any(), eq(ethContinuation), eq(size), ActivitySortDto.EARLIEST_FIRST)
        } returns AuctionActivitiesDto(null, emptyList()).toMono()

        coEvery {
            testEthereumActivityItemApi.getNftActivities(any(), eq(ethContinuation), eq(size), ActivitySortDto.EARLIEST_FIRST)
        } returns NftActivitiesDto(null, ethItemActivities).toMono()

        coEvery {
            testPolygonActivityOrderApi.getOrderActivities(any(), eq(polyContinuation), eq(size), ActivitySortDto.EARLIEST_FIRST)
        } returns OrderActivitiesDto(null, polygonOrderActivities).toMono()

        coEvery {
            testPolygonActivityAuctionApi.getAuctionActivities(any(), eq(polyContinuation), eq(size), ActivitySortDto.EARLIEST_FIRST)
        } returns AuctionActivitiesDto(null, emptyList()).toMono()

        coEvery {
            testPolygonActivityItemApi.getNftActivities(any(), eq(polyContinuation), eq(size), ActivitySortDto.EARLIEST_FIRST)
        } returns NftActivitiesDto(null, polygonItemActivities).toMono()

        coEvery {
            testFlowActivityApi.getNftOrderAllActivities(any(), eq(flowContinuation), eq(size), any())
        } returns FlowActivitiesDto(1, null, flowActivities).toMono()

        val activities = activityControllerApi.getAllActivities(
            types, blockchains, null, cursorArg.toString(), size, com.rarible.protocol.union.dto.ActivitySortDto.EARLIEST_FIRST
        ).awaitFirst()

        assertThat(activities.activities).hasSize(3)
        assertThat(activities.continuation).isNull()

        val oldestActivity = activities.activities[0]
        val secondActivity = activities.activities[1]
        val newestActivity = activities.activities[2]

        assertThat(oldestActivity.id.value).isEqualTo(polygonItemActivity1.id)
        assertThat(secondActivity.id.value).isEqualTo(ethOrderActivity3.id)
        assertThat(newestActivity.id.value).isEqualTo(flowActivity.id)

        assertThat(activities.cursor).isNotNull()
        val cursor = CombinedContinuation.parse(activities.cursor)
        assertThat(cursor.continuations).containsKey(BlockchainDto.ETHEREUM.name)
        assertThat(cursor.continuations).containsKey(BlockchainDto.POLYGON.name)
        assertThat(cursor.continuations).containsEntry(BlockchainDto.FLOW.name, ArgSlice.COMPLETED)
    }

    @Test
    fun `get all activities - with null size`() = runBlocking<Unit> {
        val types = listOf(ActivityTypeDto.BID)
        val blockchains = listOf(BlockchainDto.ETHEREUM)

        val ethOrderActivities = List(defaultSize * 2) { randomEthOrderBidActivity() }
        coEvery {
            testEthereumActivityOrderApi.getOrderActivities(any(), any(), any(), any())
        } returns OrderActivitiesDto(null, ethOrderActivities).toMono()

        val activities = activityControllerApi.getAllActivities(types, blockchains, null, null, null, null)
            .awaitFirst()

        assertThat(activities.activities).hasSize(defaultSize)
    }

    @Test
    fun `get activities by user`() = runBlocking<Unit> {
        // Only Item-specific activity types specified here, so only NFT-Indexer of Ethereum should be requested
        val types = listOf(
            UserActivityTypeDto.TRANSFER_FROM,
            UserActivityTypeDto.TRANSFER_TO,
            UserActivityTypeDto.MINT,
            UserActivityTypeDto.BURN
        )
        // Flow and Ethereum user specified - request should be routed only for them, Polygon omitted
        val userEth = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())
        val userFlow = randomFlowAddress()
        val size = 3

        val flowActivity = randomFlowCancelListActivityDto()
        val ethItemActivity = randomEthItemMintActivity()
        val ethItemActivity2 = randomEthItemMintActivity()
        val polygonItemActivity = randomEthItemMintActivity()

        coEvery {
            testEthereumActivityItemApi.getNftActivities(any(), isNull(), eq(size), ActivitySortDto.LATEST_FIRST)
        } returns NftActivitiesDto(null, listOf(ethItemActivity, ethItemActivity2)).toMono()

        coEvery {
            testPolygonActivityItemApi.getNftActivities(any(), isNull(), eq(size), ActivitySortDto.LATEST_FIRST)
        } returns NftActivitiesDto(null, listOf(polygonItemActivity)).toMono()

        coEvery {
            testFlowActivityApi.getNftOrderActivitiesByUser(
                types.map { it.name },
                listOf(userFlow.value),
                any(),
                any(),
                isNull(),
                eq(size),
                sort?.name
            )
        } returns FlowActivitiesDto(1, null, listOf(flowActivity)).toMono()

        val now = Instant.now()
        val oneWeekAgo = now.minus(7, ChronoUnit.DAYS)
        val activities = activityControllerApi.getActivitiesByUser(
            types, listOf(userEth.fullId(), userFlow.fullId()), null, oneWeekAgo, now, null, null, size, sort
        ).awaitFirst()

        assertThat(activities.activities).hasSize(3)
        assertThat(activities.cursor).isNotNull()
        assertThat(activities.continuation).isNotNull()
    }

    @Test
    fun `get activities by user with cursor`() = runBlocking<Unit> {
        // Only Item-specific activity types specified here, so only NFT-Indexer of Ethereum should be requested
        val types = listOf(
            UserActivityTypeDto.TRANSFER_FROM,
            UserActivityTypeDto.TRANSFER_TO,
            UserActivityTypeDto.MINT,
            UserActivityTypeDto.BURN
        )
        // Flow and Ethereum user specified - request should be routed only for them, Polygon omitted
        val userEth = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())
        val userFlow = randomFlowAddress()
        val size = 4

        val flowActivity = randomFlowCancelListActivityDto()
        val ethItemActivity = randomEthItemMintActivity()
        val polygonItemActivity = randomEthItemMintActivity()

        val now = nowMillis()
        val ethContinuation = "${now.toEpochMilli()}_${randomString()}"
        val polyContinuation = "${now.toEpochMilli()}_${randomString()}"
        val flowContinuation = "${now.toEpochMilli()}_${randomInt()}"
        val cursorArg = CombinedContinuation(
            mapOf(
                BlockchainDto.ETHEREUM.toString() to ethContinuation,
                BlockchainDto.POLYGON.toString() to polyContinuation,
                BlockchainDto.FLOW.toString() to flowContinuation
            )
        )

        coEvery {
            testEthereumActivityItemApi.getNftActivities(any(), eq(ethContinuation), eq(size), ActivitySortDto.LATEST_FIRST)
        } returns NftActivitiesDto(null, listOf(ethItemActivity)).toMono()

        coEvery {
            testPolygonActivityItemApi.getNftActivities(any(), eq(polyContinuation), eq(size), ActivitySortDto.LATEST_FIRST)
        } returns NftActivitiesDto(null, listOf(polygonItemActivity)).toMono()

        coEvery {
            testFlowActivityApi.getNftOrderActivitiesByUser(
                types.map { it.name },
                listOf(userFlow.value),
                any(),
                any(),
                eq(flowContinuation),
                eq(size),
                sort?.name
            )
        } returns FlowActivitiesDto(1, null, listOf(flowActivity)).toMono()

        val oneWeekAgo = now.minus(7, ChronoUnit.DAYS)
        val activities = activityControllerApi.getActivitiesByUser(
            types, listOf(userEth.fullId(), userFlow.fullId()), null, oneWeekAgo, now, null, cursorArg.toString(), size,
            sort
        ).awaitFirst()

        assertThat(activities.activities).hasSize(3)
        assertThat(activities.continuation).isNull()
    }

    @Test
    fun `get activities by user with an empty slice`() = runBlocking<Unit> {
        // Only Item-specific activity types specified here, so only NFT-Indexer of Ethereum should be requested
        val types = listOf(
            UserActivityTypeDto.TRANSFER_FROM,
            UserActivityTypeDto.TRANSFER_TO,
            UserActivityTypeDto.MINT,
            UserActivityTypeDto.BURN
        )
        // Flow and Ethereum user specified - request should be routed only for them, Polygon omitted
        val userEth = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())
        val userFlow = randomFlowAddress()
        val size = 1

        val ethItemActivity = randomEthItemMintActivity()
        val polygonItemActivity = randomEthItemMintActivity()

        coEvery {
            testEthereumActivityItemApi.getNftActivities(any(), isNull(), eq(size), ActivitySortDto.LATEST_FIRST)
        } returns NftActivitiesDto(null, listOf(ethItemActivity)).toMono()

        coEvery {
            testPolygonActivityItemApi.getNftActivities(any(), isNull(), eq(size), ActivitySortDto.LATEST_FIRST)
        } returns NftActivitiesDto(null, listOf(polygonItemActivity)).toMono()

        coEvery {
            testFlowActivityApi.getNftOrderActivitiesByUser(
                types.map { it.name },
                listOf(userFlow.value),
                any(),
                any(),
                isNull(),
                eq(size),
                sort?.name
            )
        } returns FlowActivitiesDto(0, null, listOf()).toMono()

        val now = Instant.now()
        val oneWeekAgo = now.minus(7, ChronoUnit.DAYS)
        val activities = activityControllerApi.getActivitiesByUser(
            types, listOf(userEth.fullId(), userFlow.fullId()), null, oneWeekAgo, now, null, null, size, sort
        ).awaitFirst()

        assertThat(activities.activities).hasSize(size)
        assertThat(activities.cursor).isNotNull()
        assertThat(activities.continuation).isNotNull()
    }

}
