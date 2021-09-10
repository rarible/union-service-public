package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomLong
import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.NftActivitiesDto
import com.rarible.protocol.dto.OrderActivitiesDto
import com.rarible.protocol.union.api.client.ActivityControllerApi
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

    private val DEF_CONTINUATION = null
    private val DEF_SIZE = 5

    @Autowired
    lateinit var activityControllerApi: ActivityControllerApi

    @Test
    fun `get activities by collection - ethereum`() = runBlocking<Unit> {
        // Here we expect only one query - to Order activities, since there is only order-related activity type
        val types = listOf(UnionActivityTypeDto.SELL)
        val ethCollectionId = randomEthAddress()
        val orderActivity = randomEthOrderBidActivity()

        coEvery {
            testEthereumActivityOrderApi.getOrderActivities(any(), isNull(), eq(DEF_SIZE))
        } returns OrderActivitiesDto(null, listOf(orderActivity)).toMono()

        val unionActivities = activityControllerApi.getActivitiesByCollection(
            types, ethCollectionId.toString(), DEF_CONTINUATION, DEF_SIZE
        ).awaitFirst()

        assertThat(unionActivities.activities).hasSize(1)
        assertThat(unionActivities.activities[0]).isInstanceOf(EthOrderBidActivityDto::class.java)
    }

    @Test
    fun `get activities by collection - flow`() = runBlocking<Unit> {
        val types = UnionActivityTypeDto.values().toList()
        val flowCollectionId = randomFlowAddress()
        val activity = randomFlowCancelListActivity()

        coEvery {
            testFlowActivityApi.getNftOrderActivitiesByCollection(
                types.map { it.name },
                flowCollectionId.value,
                DEF_CONTINUATION, DEF_SIZE
            )
        } returns FlowActivitiesDto(1, null, listOf(activity)).toMono()

        val unionActivities = activityControllerApi.getActivitiesByCollection(
            types, flowCollectionId.toString(), DEF_CONTINUATION, DEF_SIZE
        ).awaitFirst()

        val flowItem = unionActivities.activities[0] as FlowActivityDto
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
            testEthereumActivityOrderApi.getOrderActivities(any(), isNull(), eq(DEF_SIZE))
        } returns OrderActivitiesDto(null, listOf(orderActivity)).toMono()

        coEvery {
            testEthereumActivityItemApi.getNftActivities(any(), isNull(), eq(DEF_SIZE))
        } returns NftActivitiesDto(null, listOf(itemActivity)).toMono()

        val unionActivities = activityControllerApi.getActivitiesByItem(
            types, ethItemId.toString(), tokenId, DEF_CONTINUATION, DEF_SIZE
        ).awaitFirst()

        assertThat(unionActivities.activities).hasSize(2)
        assertThat(unionActivities.activities[0]).isInstanceOf(EthOrderBidActivityDto::class.java)
        assertThat(unionActivities.activities[1]).isInstanceOf(EthMintActivityDto::class.java)
    }

    @Test
    fun `get activities by item - flow`() = runBlocking<Unit> {
        val types = UnionActivityTypeDto.values().toList()
        val flowItemId = randomFlowAddress()
        val tokenId = randomLong()
        val activity = randomFlowCancelListActivity()

        coEvery {
            testFlowActivityApi.getNftOrderActivitiesByItem(
                types.map { it.name },
                flowItemId.value,
                tokenId,
                DEF_CONTINUATION, DEF_SIZE
            )
        } returns FlowActivitiesDto(1, null, listOf(activity)).toMono()

        val unionActivities = activityControllerApi.getActivitiesByItem(
            types, flowItemId.toString(), tokenId.toString(), DEF_CONTINUATION, DEF_SIZE
        ).awaitFirst()

        val flowItem = unionActivities.activities[0] as FlowActivityDto
        assertThat(flowItem.id.value).isEqualTo(activity.id)
    }

    @Test
    fun `get all activities`() = runBlocking<Unit> {
        val types = UnionActivityTypeDto.values().toList()
        val blockchains = listOf<BlockchainDto>()
        val size = 3
        val now = nowMillis()

        // From this list of activities we expect only the newest 3 in response ordered as:
        // ethOrderActivity1, flowActivity and polygonItemActivity2
        val ethOrderActivity1 = randomEthOrderBidActivity().copy(date = now)
        val ethOrderActivity2 = randomEthOrderBidActivity().copy(date = now.minusSeconds(5))
        val ethOrderActivity3 = randomEthOrderBidActivity().copy(date = now.minusSeconds(10))
        val ethItemActivity1 = randomEthItemMintActivity().copy(date = now.minusSeconds(4))
        val ethItemActivity2 = randomEthItemMintActivity().copy(date = now.minusSeconds(7))
        val ethItemActivity3 = randomEthItemMintActivity().copy(date = now.minusSeconds(8))
        val polygonOrderActivity1 = randomEthOrderBidActivity().copy(date = now.minusSeconds(9))
        val polygonOrderActivity2 = randomEthOrderBidActivity().copy(date = now.minusSeconds(3))
        val polygonItemActivity1 = randomEthItemMintActivity().copy(date = now.minusSeconds(12))
        val polygonItemActivity2 = randomEthItemMintActivity().copy(date = now.minusSeconds(2))
        val flowActivity = randomFlowCancelListActivity().copy(date = now.minusSeconds(1))

        val ethOrderActivities = listOf(ethOrderActivity1, ethOrderActivity2, ethOrderActivity3)
        val ethItemActivities = listOf(ethItemActivity1, ethItemActivity2, ethItemActivity3)
        val polygonOrderActivities = listOf(polygonOrderActivity1, polygonOrderActivity2)
        val polygonItemActivities = listOf(polygonItemActivity1, polygonItemActivity2)
        val flowActivities = listOf(flowActivity)

        // Since all activity types specified in request, all of existing clients should be requested
        coEvery {
            testEthereumActivityOrderApi.getOrderActivities(any(), isNull(), eq(size))
        } returns OrderActivitiesDto(null, ethOrderActivities).toMono()

        coEvery {
            testEthereumActivityItemApi.getNftActivities(any(), isNull(), eq(size))
        } returns NftActivitiesDto(null, ethItemActivities).toMono()

        coEvery {
            testPolygonActivityOrderApi.getOrderActivities(any(), isNull(), eq(size))
        } returns OrderActivitiesDto(null, polygonOrderActivities).toMono()

        coEvery {
            testPolygonActivityItemApi.getNftActivities(any(), isNull(), eq(size))
        } returns NftActivitiesDto(null, polygonItemActivities).toMono()

        coEvery {
            testFlowActivityApi.getNftOrderAllActivities(any(), isNull(), eq(size))
        } returns FlowActivitiesDto(1, null, flowActivities).toMono()

        val unionActivities = activityControllerApi.getAllActivities(
            types, blockchains, null, size
        ).awaitFirst()

        assertThat(unionActivities.activities).hasSize(3)
        assertThat(unionActivities.continuation).isNotNull()

        val newestActivity = unionActivities.activities[0] as EthActivityDto
        val secondActivity = unionActivities.activities[1] as FlowActivityDto
        val oldestActivity = unionActivities.activities[2] as EthActivityDto

        assertThat(newestActivity.id.value).isEqualTo(ethOrderActivity1.id)
        assertThat(secondActivity.id.value).isEqualTo(flowActivity.id)
        assertThat(oldestActivity.id.value).isEqualTo(polygonItemActivity2.id)
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

        val flowActivity = randomFlowCancelListActivity()
        val ethItemActivity = randomEthItemMintActivity()

        coEvery {
            testEthereumActivityItemApi.getNftActivities(any(), isNull(), eq(size))
        } returns NftActivitiesDto(null, listOf(ethItemActivity)).toMono()

        coEvery {
            testFlowActivityApi.getNftOrderActivitiesByUser(
                types.map { it.name },
                listOf(userFlow.value),
                isNull(),
                eq(size)
            )
        } returns FlowActivitiesDto(1, null, listOf(flowActivity)).toMono()

        val unionActivities = activityControllerApi.getActivitiesByUser(
            types, listOf(userEth.toString(), userFlow.toString()), null, size
        ).awaitFirst()

        assertThat(unionActivities.activities).hasSize(2)
    }

}