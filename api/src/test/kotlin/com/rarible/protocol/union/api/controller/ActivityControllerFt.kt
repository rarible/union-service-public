package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomLong
import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.NftActivitiesDto
import com.rarible.protocol.dto.OrderActivitiesDto
import com.rarible.protocol.union.api.client.ActivityControllerApi
import com.rarible.protocol.union.api.configuration.PageSize
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.test.data.*
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono

@FlowPreview
@IntegrationTest
class ActivityControllerFt : AbstractIntegrationTest() {

    private val continuation: String? = null
    private val size = PageSize.ACTIVITY.default
    private val sort: UnionActivitySortDto? = null

    @Autowired
    lateinit var activityControllerApi: ActivityControllerApi

    @Test
    fun `get activities by collection - ethereum`() = runBlocking<Unit> {
        // Here we expect only one query - to Order activities, since there is only order-related activity type
        val types = listOf(UnionActivityTypeDto.SELL)
        val ethCollectionId = randomEthAddress()
        val orderActivity = randomEthOrderBidActivity()

        coEvery {
            testEthereumActivityOrderApi.getOrderActivities(any(), isNull(), eq(size), ActivitySortDto.LATEST_FIRST)
        } returns OrderActivitiesDto(null, listOf(orderActivity)).toMono()

        val unionActivities = activityControllerApi.getActivitiesByCollection(
            types, ethCollectionId.fullId(), continuation, size, sort
        ).awaitFirst()

        assertThat(unionActivities.activities).hasSize(1)
        assertThat(unionActivities.activities[0]).isInstanceOf(UnionOrderBidActivityDto::class.java)
    }

    @Test
    fun `get activities by collection - flow`() = runBlocking<Unit> {
        val types = UnionActivityTypeDto.values().toList()
        val flowCollectionId = randomFlowAddress()
        val activity = randomFlowCancelListActivityDto()

        coEvery {
            testFlowActivityApi.getNftOrderActivitiesByCollection(
                types.map { it.name },
                flowCollectionId.value,
                continuation,
                size
            )
        } returns FlowActivitiesDto(1, null, listOf(activity)).toMono()

        val unionActivities = activityControllerApi.getActivitiesByCollection(
            types, flowCollectionId.fullId(), continuation, null, sort
        ).awaitFirst()

        val flowItem = unionActivities.activities[0]
        assertThat(flowItem.id.value).isEqualTo(activity.id)
    }

    @Test
    fun `get activities by item - ethereum`() = runBlocking<Unit> {
        // All activity types specified here, so both Order and Nft indexer should be requested for activities
        val types = UnionActivityTypeDto.values().toList()
        val ethItemId = randomEthAddress()
        val now = nowMillis()
        val tokenId = randomBigInt().toString()
        val orderActivity = randomEthOrderBidActivity().copy(date = now)
        val itemActivity = randomEthItemMintActivity().copy(date = now.minusSeconds(5))

        coEvery {
            testEthereumActivityOrderApi.getOrderActivities(
                any(),
                isNull(),
                eq(PageSize.ACTIVITY.max),
                ActivitySortDto.LATEST_FIRST
            )
        } returns OrderActivitiesDto(null, listOf(orderActivity)).toMono()

        coEvery {
            testEthereumActivityItemApi.getNftActivities(
                any(),
                isNull(),
                eq(PageSize.ACTIVITY.max),
                ActivitySortDto.LATEST_FIRST
            )
        } returns NftActivitiesDto(null, listOf(itemActivity)).toMono()

        val unionActivities = activityControllerApi.getActivitiesByItem(
            types, ethItemId.fullId(), tokenId, continuation, 10000000, sort
        ).awaitFirst()

        assertThat(unionActivities.activities).hasSize(2)
        assertThat(unionActivities.activities[0]).isInstanceOf(UnionOrderBidActivityDto::class.java)
        assertThat(unionActivities.activities[1]).isInstanceOf(UnionMintActivityDto::class.java)
    }

    @Test
    fun `get activities by item - flow`() = runBlocking<Unit> {
        val types = UnionActivityTypeDto.values().toList()
        val flowItemId = randomFlowAddress()
        val tokenId = randomLong()
        val activity = randomFlowCancelListActivityDto()

        coEvery {
            testFlowActivityApi.getNftOrderActivitiesByItem(
                types.map { it.name },
                flowItemId.value,
                tokenId,
                continuation, size
            )
        } returns FlowActivitiesDto(1, null, listOf(activity)).toMono()

        val unionActivities = activityControllerApi.getActivitiesByItem(
            types, flowItemId.fullId(), tokenId.toString(), continuation, size, sort
        ).awaitFirst()

        val flowItem = unionActivities.activities[0]
        assertThat(flowItem.id.value).isEqualTo(activity.id)
    }

    @Test
    fun `get all activities - asc`() = runBlocking<Unit> {
        val types = UnionActivityTypeDto.values().toList()
        val blockchains = listOf<BlockchainDto>()
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
            testEthereumActivityItemApi.getNftActivities(any(), isNull(), eq(size), ActivitySortDto.EARLIEST_FIRST)
        } returns NftActivitiesDto(null, ethItemActivities).toMono()

        coEvery {
            testPolygonActivityOrderApi.getOrderActivities(any(), isNull(), eq(size), ActivitySortDto.EARLIEST_FIRST)
        } returns OrderActivitiesDto(null, polygonOrderActivities).toMono()

        coEvery {
            testPolygonActivityItemApi.getNftActivities(any(), isNull(), eq(size), ActivitySortDto.EARLIEST_FIRST)
        } returns NftActivitiesDto(null, polygonItemActivities).toMono()

        coEvery {
            testFlowActivityApi.getNftOrderAllActivities(any(), isNull(), eq(size))
        } returns FlowActivitiesDto(1, null, flowActivities).toMono()

        val unionActivities = activityControllerApi.getAllActivities(
            types, blockchains, null, size, UnionActivitySortDto.EARLIEST_FIRST
        ).awaitFirst()

        assertThat(unionActivities.activities).hasSize(3)
        assertThat(unionActivities.continuation).isNotNull()

        val oldestActivity = unionActivities.activities[0]
        val secondActivity = unionActivities.activities[1]
        val newestActivity = unionActivities.activities[2]

        assertThat(oldestActivity.id.value).isEqualTo(polygonItemActivity1.id)
        assertThat(secondActivity.id.value).isEqualTo(ethOrderActivity3.id)
        assertThat(newestActivity.id.value).isEqualTo(flowActivity.id)
    }

    @Test
    fun `get activities by user`() = runBlocking<Unit> {
        // Only Item-specific activity types specified here, so only NFT-Indexer of Ethereum should be requested
        val types = listOf(
            UnionUserActivityTypeDto.TRANSFER_FROM,
            UnionUserActivityTypeDto.TRANSFER_TO,
            UnionUserActivityTypeDto.MINT,
            UnionUserActivityTypeDto.BURN
        )
        // Flow and Ethereum user specified - request should be routed only for them, Polygon omitted
        val userEth = randomEthAddress()
        val userFlow = randomFlowAddress()
        val size = 3

        val flowActivity = randomFlowCancelListActivityDto()
        val ethItemActivity = randomEthItemMintActivity()

        coEvery {
            testEthereumActivityItemApi.getNftActivities(any(), isNull(), eq(size), ActivitySortDto.LATEST_FIRST)
        } returns NftActivitiesDto(null, listOf(ethItemActivity)).toMono()

        coEvery {
            testFlowActivityApi.getNftOrderActivitiesByUser(
                types.map { it.name },
                listOf(userFlow.value),
                isNull(),
                eq(size),
                sort?.name
            )
        } returns FlowActivitiesDto(1, null, listOf(flowActivity)).toMono()

        val unionActivities = activityControllerApi.getActivitiesByUser(
            types, listOf(userEth.fullId(), userFlow.fullId()), null, size, sort
        ).awaitFirst()

        assertThat(unionActivities.activities).hasSize(2)
    }

}
