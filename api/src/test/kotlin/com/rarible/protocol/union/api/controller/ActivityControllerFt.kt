package com.rarible.protocol.union.api.controller

import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.dipdup.client.OrderActivityClient
import com.rarible.dipdup.client.model.DipDupActivitiesPage
import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.AuctionActivitiesDto
import com.rarible.protocol.dto.AuctionActivityDto
import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.NftActivitiesDto
import com.rarible.protocol.dto.NftActivityDto
import com.rarible.protocol.dto.OrderActivitiesDto
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.dto.SyncSortDto
import com.rarible.protocol.solana.dto.ActivitiesDto
import com.rarible.protocol.solana.dto.ActivityDto
import com.rarible.protocol.union.api.client.ActivityControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.api.data.randomDipDupListActivity
import com.rarible.protocol.union.api.data.randomTzktItemMintActivity
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.ActivitiesByUsersRequestDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.AuctionStartActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderBidActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.SearchEngineDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.enrichment.converter.ActivityDtoConverter
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionStartActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemMintActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderBidActivity
import com.rarible.protocol.union.integration.flow.converter.FlowActivityConverter
import com.rarible.protocol.union.integration.flow.data.randomFlowAddress
import com.rarible.protocol.union.integration.flow.data.randomFlowCancelListActivityDto
import com.rarible.protocol.union.integration.flow.data.randomFlowItemId
import com.rarible.protocol.union.integration.flow.data.randomFlowMintDto
import com.rarible.protocol.union.integration.flow.data.randomFlowNftOrderActivityListDto
import com.rarible.protocol.union.integration.solana.data.randomActivityOrderBid
import com.rarible.protocol.union.integration.solana.data.randomSolanaMintActivity
import com.rarible.tzkt.model.Page
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.kotlin.core.publisher.toMono
import java.time.temporal.ChronoUnit

@FlowPreview
@IntegrationTest
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "application.environment = test",
        "spring.cloud.consul.config.enabled = false",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "integration.tezos.dipdup.enabled = true" // turn on dipdup integration
    ]
)
class ActivityControllerFt : AbstractIntegrationTest() {

    private val continuation: String? = null
    private val defaultSize = PageSize.ACTIVITY.default
    private val maxSize = PageSize.ACTIVITY.max
    private val sort: com.rarible.protocol.union.dto.ActivitySortDto? = null

    @Autowired
    lateinit var activityControllerApi: ActivityControllerApi

    @Autowired
    lateinit var esActivityRepository: EsActivityRepository

    @Autowired
    lateinit var flowActivityConverter: FlowActivityConverter

    @Autowired
    lateinit var ethActivityConverter: EthActivityConverter

    @Autowired
    lateinit var esActivityConverter: EsActivityConverter

    @MockkBean
    lateinit var dipdupActivityClient: OrderActivityClient

    @Test
    fun `get sync activities - solana - order type`() = runBlocking<Unit> {
        val size = 35

        mockSolanaOrderActivitiesSync(size)

        val activities = activityControllerApi.getAllActivitiesSync(
            BlockchainDto.SOLANA,
            null,
            size,
            com.rarible.protocol.union.dto.SyncSortDto.DB_UPDATE_ASC,
            SyncTypeDto.ORDER
        ).awaitFirst()

        assertThat(activities.activities).hasSize(size)
        assertThat(activities.continuation).isNull()
        activities.activities.forEach { assertThat(it).isExactlyInstanceOf(OrderBidActivityDto::class.java) }
    }

    @Test
    fun `get sync activities - solana - nft type`() = runBlocking<Unit> {
        val size = 47

        mockSolanaNftActivitiesSync(size)

        val activities = activityControllerApi.getAllActivitiesSync(
            BlockchainDto.SOLANA,
            null,
            size,
            com.rarible.protocol.union.dto.SyncSortDto.DB_UPDATE_ASC,
            SyncTypeDto.NFT
        ).awaitFirst()

        assertThat(activities.activities).hasSize(size)
        assertThat(activities.continuation).isNull()
        activities.activities.forEach { assertThat(it).isExactlyInstanceOf(MintActivityDto::class.java) }
    }

    @Test
    fun `get sync activities - flow - order type - asc`() = runBlocking<Unit> {
        val size = 47

        mockFlowOrderActivitiesSync(size)

        val activities = activityControllerApi.getAllActivitiesSync(
            BlockchainDto.FLOW,
            null,
            size,
            com.rarible.protocol.union.dto.SyncSortDto.DB_UPDATE_ASC,
            SyncTypeDto.ORDER
        ).awaitFirst()

        assertThat(activities.activities).hasSize(size)
        activities.activities.forEach { assertThat(it).isExactlyInstanceOf(OrderListActivityDto::class.java) }
        assertThat(activities.activities).isSortedAccordingTo { o1, o2 ->
            compareValues(
                o1.lastUpdatedAt, o2.lastUpdatedAt
            )
        }
    }

    @Test
    fun `get sync activities - flow - nft type - asc`() = runBlocking<Unit> {
        val size = 64

        mockFlowNftActivitiesSync(size)

        val activities = activityControllerApi.getAllActivitiesSync(
            BlockchainDto.FLOW,
            null,
            size,
            com.rarible.protocol.union.dto.SyncSortDto.DB_UPDATE_ASC,
            SyncTypeDto.NFT
        ).awaitFirst()

        assertThat(activities.activities).hasSize(size)
        activities.activities.forEach { assertThat(it).isExactlyInstanceOf(MintActivityDto::class.java) }
        assertThat(activities.activities).isSortedAccordingTo { o1, o2 ->
            compareValues(
                o1.lastUpdatedAt, o2.lastUpdatedAt
            )
        }
    }

    @Test
    fun `get sync activities - ethereum - nft type - asc`() = runBlocking<Unit> {
        val size = 55
        val orderActivities = mutableListOf<OrderActivityDto>()
        val auctionActivities = mutableListOf<AuctionActivityDto>()
        val itemActivities = mutableListOf<NftActivityDto>()

        fillEthereumActivitiesLists(
            size = size,
            orderActivities = orderActivities,
            auctionActivities = auctionActivities,
            itemActivities = itemActivities
        )

        mockEthereumActivitiesSync(
            size = size,
            sort = SyncSortDto.DB_UPDATE_ASC,
            orderActivities = orderActivities,
            auctionActivities = auctionActivities,
            itemActivities = itemActivities
        )

        val activities = activityControllerApi.getAllActivitiesSync(
            BlockchainDto.ETHEREUM,
            null,
            size,
            com.rarible.protocol.union.dto.SyncSortDto.DB_UPDATE_ASC,
            SyncTypeDto.NFT
        ).awaitFirst()

        assertThat(activities.activities).hasSize(size)
        assertThat(activities.continuation).isNotNull
        assertThat(activities.activities).isSortedAccordingTo { o1, o2 ->
            compareValues(
                o1.lastUpdatedAt,
                o2.lastUpdatedAt
            )
        }
        activities.activities.forEach { assertThat(it).isExactlyInstanceOf(MintActivityDto::class.java) }
    }

    @Test
    fun `get sync activities - ethereum - order type - asc`() = runBlocking<Unit> {
        val size = 55
        val orderActivities = mutableListOf<OrderActivityDto>()
        val auctionActivities = mutableListOf<AuctionActivityDto>()
        val itemActivities = mutableListOf<NftActivityDto>()

        fillEthereumActivitiesLists(
            size = size,
            orderActivities = orderActivities,
            auctionActivities = auctionActivities,
            itemActivities = itemActivities
        )

        mockEthereumActivitiesSync(
            size = size,
            sort = SyncSortDto.DB_UPDATE_ASC,
            orderActivities = orderActivities,
            auctionActivities = auctionActivities,
            itemActivities = itemActivities
        )

        val activities = activityControllerApi.getAllActivitiesSync(
            BlockchainDto.ETHEREUM,
            null,
            size,
            com.rarible.protocol.union.dto.SyncSortDto.DB_UPDATE_ASC,
            SyncTypeDto.ORDER
        ).awaitFirst()

        assertThat(activities.activities).hasSize(size)
        assertThat(activities.continuation).isNotNull
        assertThat(activities.activities).isSortedAccordingTo { o1, o2 ->
            compareValues(
                o1.lastUpdatedAt,
                o2.lastUpdatedAt
            )
        }
        activities.activities.forEach { assertThat(it).isExactlyInstanceOf(OrderBidActivityDto::class.java) }
    }

    @Test
    fun `get sync activities - ethereum - asc`() = runBlocking<Unit> {
        val size = 51
        val orderActivities = mutableListOf<OrderActivityDto>()
        val auctionActivities = mutableListOf<AuctionActivityDto>()
        val itemActivities = mutableListOf<NftActivityDto>()

        fillEthereumActivitiesLists(
            size = size,
            orderActivities = orderActivities,
            auctionActivities = auctionActivities,
            itemActivities = itemActivities
        )

        mockEthereumActivitiesSync(
            size = size,
            sort = SyncSortDto.DB_UPDATE_ASC,
            orderActivities = orderActivities,
            auctionActivities = auctionActivities,
            itemActivities = itemActivities
        )

        val activities = activityControllerApi.getAllActivitiesSync(
            BlockchainDto.ETHEREUM,
            null,
            size,
            com.rarible.protocol.union.dto.SyncSortDto.DB_UPDATE_ASC,
            null
        ).awaitFirst()

        assertThat(activities.activities).hasSize(size)
        assertThat(activities.continuation).isNotNull
        assertThat(activities.activities).isSortedAccordingTo { o1, o2 ->
            compareValues(
                o1.lastUpdatedAt,
                o2.lastUpdatedAt
            )
        }
    }

    @Test
    fun `get sync activities - ethereum - desc`() = runBlocking<Unit> {
        val size = 51
        val orderActivities = mutableListOf<OrderActivityDto>()
        val auctionActivities = mutableListOf<AuctionActivityDto>()
        val itemActivities = mutableListOf<NftActivityDto>()

        fillEthereumActivitiesLists(
            size = size,
            orderActivities = orderActivities,
            auctionActivities = auctionActivities,
            itemActivities = itemActivities
        )

        mockEthereumActivitiesSync(
            size = size,
            sort = SyncSortDto.DB_UPDATE_DESC,
            orderActivities = orderActivities,
            auctionActivities = auctionActivities,
            itemActivities = itemActivities
        )

        val activities = activityControllerApi.getAllActivitiesSync(
            BlockchainDto.ETHEREUM,
            null,
            size,
            com.rarible.protocol.union.dto.SyncSortDto.DB_UPDATE_DESC,
            null
        ).awaitFirst()

        assertThat(activities.activities).hasSize(size)
        assertThat(activities.continuation).isNotNull
        assertThat(activities.activities).isSortedAccordingTo { o1, o2 ->
            compareValues(
                o2.lastUpdatedAt,
                o1.lastUpdatedAt
            )
        }
    }

    @Test
    fun `get sync activities - ethereum - continuation - null`() = runBlocking<Unit> {
        val size = 40
        val orderActivities = mutableListOf<OrderActivityDto>()
        val auctionActivities = mutableListOf<AuctionActivityDto>()
        val itemActivities = mutableListOf<NftActivityDto>()

        fillEthereumActivitiesLists(
            size = size / 5,
            orderActivities = orderActivities,
            auctionActivities = auctionActivities,
            itemActivities = itemActivities
        )

        mockEthereumActivitiesSync(
            size = size,
            sort = SyncSortDto.DB_UPDATE_DESC,
            orderActivities = orderActivities,
            auctionActivities = auctionActivities,
            itemActivities = itemActivities
        )

        val activities = activityControllerApi.getAllActivitiesSync(
            BlockchainDto.ETHEREUM,
            null,
            size,
            com.rarible.protocol.union.dto.SyncSortDto.DB_UPDATE_DESC,
            null
        ).awaitFirst()

        assertThat(activities.activities).hasSize((size * 3) / 5)
        assertThat(activities.continuation).isNull()
        assertThat(activities.activities).isSortedAccordingTo { o1, o2 ->
            compareValues(
                o2.lastUpdatedAt,
                o1.lastUpdatedAt
            )
        }
    }

    private fun getSolanaOrderActivityList(size: Int): List<ActivityDto> {
        val result: MutableList<ActivityDto> = mutableListOf()

        repeat(size) {
            result.add(randomActivityOrderBid())
        }

        return result
    }

    private fun getSolanaNftActivityList(size: Int): List<ActivityDto> {
        val result: MutableList<ActivityDto> = mutableListOf()

        repeat(size) {
            result.add(randomSolanaMintActivity())
        }

        return result
    }

    private fun mockSolanaOrderActivitiesSync(
        size: Int,
    ) {
        coEvery {
            testSolanaActivityApi.getActivitiesSync(
                com.rarible.protocol.solana.dto.SyncTypeDto.ORDER,
                null,
                size,
                any()
            )
        } returns ActivitiesDto(null, getSolanaOrderActivityList(size)).toMono()
    }

    private fun mockSolanaNftActivitiesSync(
        size: Int
    ) {
        coEvery {
            testSolanaActivityApi.getActivitiesSync(
                com.rarible.protocol.solana.dto.SyncTypeDto.NFT,
                null,
                size,
                any()
            )
        } returns ActivitiesDto(null, getSolanaNftActivityList(size)).toMono()
    }

    private fun getFlowNftActivityList(size: Int): List<FlowActivityDto> {
        val result: MutableList<FlowActivityDto> = mutableListOf()

        repeat(size) {
            result.add(randomFlowMintDto())
        }

        return result
    }

    private fun getFlowOrderActivityList(size: Int): List<FlowActivityDto> {
        val result: MutableList<FlowActivityDto> = mutableListOf()

        repeat(size) {
            result.add(randomFlowNftOrderActivityListDto())
        }

        return result
    }

    private fun mockFlowOrderActivitiesSync(
        size: Int,
    ) {
        coEvery {
            testFlowActivityApi.getNftOrderActivitiesSync(
                FlowActivityConverter.ORDER_LIST,
                null,
                size,
                any()
            )
        } returns FlowActivitiesDto(null, getFlowOrderActivityList(size)).toMono()
    }

    private fun mockFlowNftActivitiesSync(
        size: Int
    ) {
        coEvery {
            testFlowActivityApi.getNftOrderActivitiesSync(
                FlowActivityConverter.NFT_LIST,
                null,
                size,
                any()
            )
        } returns FlowActivitiesDto(null, getFlowNftActivityList(size)).toMono()
    }

    private fun fillEthereumActivitiesLists(
        size: Int, orderActivities: MutableList<OrderActivityDto>,
        auctionActivities: MutableList<AuctionActivityDto>,
        itemActivities: MutableList<NftActivityDto>
    ) {
        var startDate = nowMillis()
        repeat(size) {
            startDate = startDate.plusMillis(100)
            orderActivities.add(randomEthOrderBidActivity().copy(lastUpdatedAt = startDate.plusMillis(randomLong())))
            auctionActivities.add(
                randomEthAuctionStartActivity().copy(lastUpdatedAt = startDate.plusMillis(randomLong()))
            )
            itemActivities.add(randomEthItemMintActivity().copy(lastUpdatedAt = startDate.plusMillis(randomLong())))
        }
    }

    private fun mockEthereumActivitiesSync(
        size: Int,
        sort: SyncSortDto,
        orderActivities: List<OrderActivityDto>,
        auctionActivities: List<AuctionActivityDto>,
        itemActivities: List<NftActivityDto>
    ) {
        coEvery {
            testEthereumActivityOrderApi.getOrderActivitiesSync(
                null,
                size,
                sort
            )
        } returns OrderActivitiesDto(null, orderActivities).toMono()

        coEvery {
            testEthereumActivityAuctionApi.getAuctionActivitiesSync(
                null,
                size,
                sort
            )
        } returns AuctionActivitiesDto(null, auctionActivities).toMono()

        coEvery {
            testEthereumActivityItemApi.getNftActivitiesSync(
                false,
                null,
                size,
                sort
            )
        } returns NftActivitiesDto(null, itemActivities).toMono()
    }

    private fun randomLong(): Long {
        return (0..10).random().toLong()
    }

    @Test
    fun `get activities by collection - ethereum`() = runBlocking<Unit> {
        // Here we expect only one query - to Order activities, since there is only order-related activity type
        val types = listOf(ActivityTypeDto.SELL)
        val ethCollectionId = CollectionIdDto(BlockchainDto.ETHEREUM, randomEthAddress())
        val orderActivity = randomEthOrderBidActivity()

        coEvery {
            testEthereumActivityOrderApi.getOrderActivities(
                any(), isNull(), eq(defaultSize), ActivitySortDto.LATEST_FIRST
            )
        } returns OrderActivitiesDto(null, listOf(orderActivity)).toMono()

        val activities = activityControllerApi.getActivitiesByCollection(
            types, listOf(ethCollectionId.fullId()), continuation, null, defaultSize, sort, null,
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
            testEthereumActivityOrderApi.getOrderActivities(
                any(), eq(ethContinuation), eq(defaultSize), ActivitySortDto.LATEST_FIRST
            )
        } returns OrderActivitiesDto(null, listOf(orderActivity)).toMono()

        val activities = activityControllerApi.getActivitiesByCollection(
            types, listOf(ethCollectionId.fullId()), null, cursor.toString(), defaultSize, sort, null,
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
        } returns FlowActivitiesDto(null, listOf(activity)).toMono()

        val activities = activityControllerApi.getActivitiesByCollection(
            types, listOf(flowCollectionId.fullId()), continuation, null, 100000, sort, null,
        ).awaitFirst()

        val flowItem = activities.activities[0]
        assertThat(flowItem.id.value).isEqualTo(activity.id)
    }

    @Test
    fun `get activities by multiple collections`() = runBlocking<Unit> {
        val types = ActivityTypeDto.values().toList()
        val flowCollectionId = randomFlowAddress()
        val flowSourceActivity = randomFlowCancelListActivityDto()
        val flowActivity = flowActivityConverter.convert(flowSourceActivity)

        val ethCollectionId = CollectionIdDto(BlockchainDto.ETHEREUM, randomEthAddress())
        val ethSourceActivity = randomEthOrderBidActivity()
        val ethActivity = ethActivityConverter.convert(
            ethSourceActivity, BlockchainDto.ETHEREUM
        )

        val flowExpected = ActivityDtoConverter.convert(flowActivity)
        val ethExpected = ActivityDtoConverter.convert(ethActivity)

        esActivityRepository.saveAll(
            listOf(
                esActivityConverter.convert(flowExpected, flowCollectionId.value)!!,
                esActivityConverter.convert(ethExpected, ethCollectionId.value)!!
            )
        )

        coEvery {
            testFlowActivityApi.getNftOrderActivitiesById(any())
        } returns FlowActivitiesDto(null, listOf(flowSourceActivity)).toMono()

        coEvery {
            testEthereumActivityOrderApi.getOrderActivitiesById(any())
        } returns OrderActivitiesDto(null, listOf(ethSourceActivity)).toMono()

        val activities = activityControllerApi.getActivitiesByCollection(
            types,
            listOf(ethCollectionId.fullId(), flowCollectionId.fullId()),
            continuation, null, 100000, sort, SearchEngineDto.V1
        ).awaitFirst()

        assertThat(activities.activities).hasSize(2)
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
            types, ethItemId.fullId(), continuation, null, 10000000, sort, null,
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
            types, ethItemId.fullId(), null, cursor.toString(), 1, sort, null,
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
        } returns FlowActivitiesDto(null, listOf(activity)).toMono()

        val activities = activityControllerApi.getActivitiesByItem(
            types, flowItemId.fullId(), continuation, null, defaultSize, sort, null,
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
            testEthereumActivityAuctionApi.getAuctionActivities(
                any(), isNull(), eq(size), ActivitySortDto.EARLIEST_FIRST
            )
        } returns AuctionActivitiesDto(null, emptyList()).toMono()

        coEvery {
            testEthereumActivityItemApi.getNftActivities(any(), isNull(), eq(size), ActivitySortDto.EARLIEST_FIRST)
        } returns NftActivitiesDto(null, ethItemActivities).toMono()

        coEvery {
            testPolygonActivityOrderApi.getOrderActivities(any(), isNull(), eq(size), ActivitySortDto.EARLIEST_FIRST)
        } returns OrderActivitiesDto(null, polygonOrderActivities).toMono()

        coEvery {
            testPolygonActivityAuctionApi.getAuctionActivities(
                any(), isNull(), eq(size), ActivitySortDto.EARLIEST_FIRST
            )
        } returns AuctionActivitiesDto(null, emptyList()).toMono()

        coEvery {
            testPolygonActivityItemApi.getNftActivities(any(), isNull(), eq(size), ActivitySortDto.EARLIEST_FIRST)
        } returns NftActivitiesDto(null, polygonItemActivities).toMono()

        coEvery {
            testFlowActivityApi.getNftOrderAllActivities(any(), isNull(), eq(size), any())
        } returns FlowActivitiesDto(null, flowActivities).toMono()

        val activities = activityControllerApi.getAllActivities(
            types, blockchains, null, null, size, com.rarible.protocol.union.dto.ActivitySortDto.EARLIEST_FIRST, null,
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
            testEthereumActivityOrderApi.getOrderActivities(
                any(), eq(ethContinuation), eq(size), ActivitySortDto.EARLIEST_FIRST
            )
        } returns OrderActivitiesDto(null, ethOrderActivities).toMono()

        coEvery {
            testEthereumActivityAuctionApi.getAuctionActivities(
                any(), eq(ethContinuation), eq(size), ActivitySortDto.EARLIEST_FIRST
            )
        } returns AuctionActivitiesDto(null, emptyList()).toMono()

        coEvery {
            testEthereumActivityItemApi.getNftActivities(
                any(), eq(ethContinuation), eq(size), ActivitySortDto.EARLIEST_FIRST
            )
        } returns NftActivitiesDto(null, ethItemActivities).toMono()

        coEvery {
            testPolygonActivityOrderApi.getOrderActivities(
                any(), eq(polyContinuation), eq(size), ActivitySortDto.EARLIEST_FIRST
            )
        } returns OrderActivitiesDto(null, polygonOrderActivities).toMono()

        coEvery {
            testPolygonActivityAuctionApi.getAuctionActivities(
                any(), eq(polyContinuation), eq(size), ActivitySortDto.EARLIEST_FIRST
            )
        } returns AuctionActivitiesDto(null, emptyList()).toMono()

        coEvery {
            testPolygonActivityItemApi.getNftActivities(
                any(), eq(polyContinuation), eq(size), ActivitySortDto.EARLIEST_FIRST
            )
        } returns NftActivitiesDto(null, polygonItemActivities).toMono()

        coEvery {
            testFlowActivityApi.getNftOrderAllActivities(any(), eq(flowContinuation), eq(size), any())
        } returns FlowActivitiesDto(null, flowActivities).toMono()

        val activities = activityControllerApi.getAllActivities(
            types, blockchains, null, cursorArg.toString(), size,
            com.rarible.protocol.union.dto.ActivitySortDto.EARLIEST_FIRST, null,
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
    fun `get activities with preserved id order - tezos`() = runBlocking<Unit> {

        val nowTime = nowMillis()
        val rawActivities = listOf(
            randomDipDupListActivity("bace384c-74c2-5310-84a6-fbe753e2813b", nowTime),
            randomDipDupListActivity("9bb20f90-3c67-5332-9042-659993bcf319", nowTime),
            randomDipDupListActivity("3861c5d9-97ba-577d-ac84-b0a1c14e9bce", nowTime)
        )
        val rawTzktActivities = listOf(
            randomTzktItemMintActivity("2", nowTime.minusSeconds(100)),
            randomTzktItemMintActivity("10", nowTime.minusSeconds(100))
        )

        coEvery {
            dipdupActivityClient.getActivitiesAll(any(), any(), any(), any())
        } returns DipDupActivitiesPage(
            activities = rawActivities,
            continuation = null
        )

        coEvery {
            tzktTokenActivityClient.getActivitiesAll(any(), any(), any(), any(), any())
        } returns Page(
            items = rawTzktActivities,
            continuation = null
        )

        val activities = activityControllerApi.getAllActivities(
            listOf(ActivityTypeDto.LIST, ActivityTypeDto.MINT),
            listOf(BlockchainDto.TEZOS),
            null, null, 10,
            com.rarible.protocol.union.dto.ActivitySortDto.EARLIEST_FIRST, null,
        ).awaitFirst()

        // should have the same order as in api responses
        assertThat(activities.activities.map { it.id.value }).isEqualTo(
            rawTzktActivities.map { it.id.toString() } + rawActivities.map { it.id }
        )
    }

    @Test
    fun `get all activities - with null size`() = runBlocking<Unit> {
        val types = listOf(ActivityTypeDto.BID)
        val blockchains = listOf(BlockchainDto.ETHEREUM)

        val ethOrderActivities = List(defaultSize * 2) { randomEthOrderBidActivity() }
        coEvery {
            testEthereumActivityOrderApi.getOrderActivities(any(), any(), any(), any())
        } returns OrderActivitiesDto(null, ethOrderActivities).toMono()

        val activities = activityControllerApi.getAllActivities(types, blockchains, null, null, null, null, null)
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
        } returns FlowActivitiesDto(null, listOf(flowActivity)).toMono()

        val now = nowMillis()
        val oneWeekAgo = now.minus(7, ChronoUnit.DAYS)
        val activities = activityControllerApi.getActivitiesByUser(
            types,
            listOf(userEth.fullId(), userFlow.fullId()),
            listOf(BlockchainDto.ETHEREUM, BlockchainDto.POLYGON, BlockchainDto.FLOW),
            oneWeekAgo,
            now,
            null,
            null, size,
            sort,
            null
        ).awaitFirst()

        assertThat(activities.activities).hasSize(3)
        assertThat(activities.cursor).isNotNull()
        assertThat(activities.continuation).isNotNull()

        val usersActivities = activityControllerApi.getActivitiesByUsers(
            ActivitiesByUsersRequestDto(
                types = types,
                users = listOf(userEth, userFlow),
                blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.POLYGON, BlockchainDto.FLOW),
                from = oneWeekAgo,
                to = now,
                continuation = null,
                cursor = null,
                size = size,
                sort = sort,
                searchEngine = null
            )
        ).awaitFirst()

        assertThat(usersActivities.activities).hasSize(3)
        assertThat(usersActivities.cursor).isNotNull()
        assertThat(usersActivities.continuation).isNotNull()
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
                BlockchainDto.FLOW.toString() to flowContinuation,
                BlockchainDto.IMMUTABLEX.toString() to ArgSlice.COMPLETED
            )
        )

        coEvery {
            testEthereumActivityItemApi.getNftActivities(
                any(), eq(ethContinuation), eq(size), ActivitySortDto.LATEST_FIRST
            )
        } returns NftActivitiesDto(null, listOf(ethItemActivity)).toMono()

        coEvery {
            testPolygonActivityItemApi.getNftActivities(
                any(), eq(polyContinuation), eq(size), ActivitySortDto.LATEST_FIRST
            )
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
        } returns FlowActivitiesDto(null, listOf(flowActivity)).toMono()

        val oneWeekAgo = now.minus(7, ChronoUnit.DAYS)
        val activities = activityControllerApi.getActivitiesByUser(
            types, listOf(userEth.fullId(), userFlow.fullId()), null, oneWeekAgo, now, null, cursorArg.toString(), size,
            sort, null,
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
        } returns FlowActivitiesDto(null, listOf()).toMono()

        val now = nowMillis()
        val oneWeekAgo = now.minus(7, ChronoUnit.DAYS)
        val activities = activityControllerApi.getActivitiesByUser(
            types, listOf(userEth.fullId(), userFlow.fullId()),
            listOf(BlockchainDto.ETHEREUM, BlockchainDto.POLYGON, BlockchainDto.FLOW),
            oneWeekAgo,
            now,
            null,
            null,
            size,
            sort,
            null
        ).awaitFirst()

        assertThat(activities.activities).hasSize(size)
        assertThat(activities.cursor).isNotNull()
        assertThat(activities.continuation).isNotNull()
    }
}
