package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.dto.ActivitiesByIdRequestDto
import com.rarible.protocol.dto.AssetDto
import com.rarible.protocol.dto.AuctionActivitiesDto
import com.rarible.protocol.dto.Erc721AssetTypeDto
import com.rarible.protocol.dto.NftActivitiesDto
import com.rarible.protocol.dto.OrderActivitiesDto
import com.rarible.protocol.union.api.client.ActivityControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.ElasticActivityQueryGenericFilter
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.AuctionStartActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderBidActivityDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionStartActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemMintActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderBidActivity
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsActivitySort
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.test.data.randomEsActivity
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import reactor.kotlin.core.publisher.toMono
import java.time.Instant
import java.time.temporal.ChronoUnit

@FlowPreview
@IntegrationTest
@TestPropertySource(properties = ["common.feature-flags.enableActivityQueriesToElasticSearch=true"])
class ActivityControllerElasticFt : AbstractIntegrationTest() {

    private val continuation: String? = null
    private val defaultSize = PageSize.ACTIVITY.default
    private val sort: com.rarible.protocol.union.dto.ActivitySortDto? = null

    @Autowired
    private lateinit var activityControllerApi: ActivityControllerApi

    @Autowired
    private lateinit var repository: EsActivityRepository

    @BeforeEach
    fun setUp() = runBlocking {
        repository.deleteAll()
    }

    @Test
    fun `get all activities`() = runBlocking<Unit> {
        val types = ActivityTypeDto.values().toList()
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.POLYGON, BlockchainDto.FLOW)
        val size = 3
        val now = nowMillis()

        // From this list of activities we expect only the oldest 3 in response ordered as:
        // polygonItemActivity1, ethOrderActivity3 and ethItemActivity3
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

        val elasticEthOrderActivity1 = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${ethOrderActivity1.id}",
            type = ActivityTypeDto.BID,
            date = ethOrderActivity1.date,
            blockchain = BlockchainDto.ETHEREUM
        )
        val elasticEthOrderActivity2 = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${ethOrderActivity2.id}",
            type = ActivityTypeDto.BID,
            date = ethOrderActivity2.date,
            blockchain = BlockchainDto.ETHEREUM
        )
        val elasticEthOrderActivity3 = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${ethOrderActivity3.id}",
            type = ActivityTypeDto.BID,
            date = ethOrderActivity3.date,
            blockchain = BlockchainDto.ETHEREUM
        )
        val elasticEthItemActivity1 = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${ethItemActivity1.id}",
            type = ActivityTypeDto.MINT,
            date = ethItemActivity1.date,
            blockchain = BlockchainDto.ETHEREUM
        )
        val elasticEthItemActivity2 = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${ethItemActivity2.id}",
            type = ActivityTypeDto.MINT,
            date = ethItemActivity2.date,
            blockchain = BlockchainDto.ETHEREUM
        )
        val elasticEthItemActivity3 = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${ethItemActivity3.id}",
            type = ActivityTypeDto.MINT,
            date = ethItemActivity3.date,
            blockchain = BlockchainDto.ETHEREUM
        )
        val elasticPolygonOrderActivity1 = randomEsActivity().copy(
            activityId = "${BlockchainDto.POLYGON}:${polygonOrderActivity1.id}",
            type = ActivityTypeDto.BID,
            date = polygonOrderActivity1.date,
            blockchain = BlockchainDto.POLYGON
        )
        val elasticPolygonOrderActivity2 = randomEsActivity().copy(
            activityId = "${BlockchainDto.POLYGON}:${polygonOrderActivity2.id}",
            type = ActivityTypeDto.BID,
            date = polygonOrderActivity2.date,
            blockchain = BlockchainDto.POLYGON
        )
        val elasticPolygonItemActivity1 = randomEsActivity().copy(
            activityId = "${BlockchainDto.POLYGON}:${polygonItemActivity1.id}",
            type = ActivityTypeDto.MINT,
            date = polygonItemActivity1.date,
            blockchain = BlockchainDto.POLYGON
        )
        val elasticPolygonItemActivity2 = randomEsActivity().copy(
            activityId = "${BlockchainDto.POLYGON}:${polygonItemActivity2.id}",
            type = ActivityTypeDto.MINT,
            date = polygonItemActivity2.date,
            blockchain = BlockchainDto.POLYGON
        )

        repository.saveAll(
            listOf(
                elasticEthOrderActivity1, elasticEthOrderActivity2, elasticEthOrderActivity3,
                elasticEthItemActivity1, elasticEthItemActivity2, elasticEthItemActivity3,
                elasticPolygonOrderActivity1, elasticPolygonOrderActivity2,
                elasticPolygonItemActivity1, elasticPolygonItemActivity2,
            )
        )

        // Since all activity types specified in request, all of existing clients should be requested
        coEvery {
            testEthereumActivityOrderApi.getOrderActivitiesById(
                ActivitiesByIdRequestDto(listOf(
                    elasticEthOrderActivity3.activityId
                ))
            )
        } returns OrderActivitiesDto(null, listOf(ethOrderActivity3)).toMono()

        coEvery {
            testEthereumActivityItemApi.getNftActivitiesById(
                ActivitiesByIdRequestDto(listOf(
                    elasticEthItemActivity3.activityId
                ))
            )
        } returns NftActivitiesDto(null, listOf(ethItemActivity3)).toMono()

        coEvery {
            testPolygonActivityItemApi.getNftActivitiesById(
                ActivitiesByIdRequestDto(listOf(
                    elasticPolygonItemActivity1.activityId,
                ))
            )
        } returns NftActivitiesDto(null, listOf(polygonItemActivity1)).toMono()

        val activities = activityControllerApi.getAllActivities(
            types, blockchains, null, null, size, com.rarible.protocol.union.dto.ActivitySortDto.EARLIEST_FIRST
        ).awaitFirst()

        assertThat(activities.activities).hasSize(3)

        val oldestActivity = activities.activities[0]
        val secondActivity = activities.activities[1]
        val newestActivity = activities.activities[2]

        assertThat(oldestActivity.id.value).isEqualTo(polygonItemActivity1.id)
        assertThat(secondActivity.id.value).isEqualTo(ethOrderActivity3.id)
        assertThat(newestActivity.id.value).isEqualTo(ethItemActivity3.id)

        assertThat(activities.cursor).isNotNull
    }

    @Test
    fun `get activities by collection - ethereum`() = runBlocking<Unit> {
        // Here we expect only one query - to Order activities, since there is only order-related activity type
        val types = listOf(ActivityTypeDto.SELL)
        val ethCollectionId = CollectionIdDto(BlockchainDto.ETHEREUM, randomEthAddress())
        val orderActivity = randomEthOrderBidActivity()
        val elasticActivity = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${orderActivity.id}",
            type = types.first(),
            blockchain = ethCollectionId.blockchain,
            collection = ethCollectionId.value,
        )
        repository.save(elasticActivity)

        coEvery {
            testEthereumActivityOrderApi.getOrderActivitiesById(
                ActivitiesByIdRequestDto(ids = listOf(elasticActivity.activityId))
            )
        } returns OrderActivitiesDto(null, listOf(orderActivity)).toMono()

        val activities = activityControllerApi.getActivitiesByCollection(
            types, ethCollectionId.fullId(), continuation, null, defaultSize, sort
        ).awaitFirst()

        assertThat(activities.activities).hasSize(1)
        assertThat(activities.activities[0]).isInstanceOf(OrderBidActivityDto::class.java)
    }

    @Test
    fun `get activities by item - ethereum`() = runBlocking<Unit> {
        val types = ActivityTypeDto.values().toList()
        val now = nowMillis()
        val address = randomAddress()
        val tokenId = randomBigInt()
        val assetTypeDto = Erc721AssetTypeDto(address, tokenId)
        val ethItemId = ItemIdDto(BlockchainDto.ETHEREUM, address.toString(), tokenId)
        val orderActivity = randomEthOrderBidActivity().copy(
            date = now,
            make = AssetDto(assetTypeDto, randomBigInt(), randomBigDecimal())
        )
        val itemActivity = randomEthItemMintActivity().copy(date = now.minusSeconds(5))
        val auctionActivity = randomEthAuctionStartActivity().copy(date = now.minusSeconds(15))

        val elasticOrderActivity = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${orderActivity.id}",
            type = ActivityTypeDto.SELL,
            blockchain = BlockchainDto.ETHEREUM,
            date = orderActivity.date,
            item = "${assetTypeDto.contract}:${assetTypeDto.tokenId}"
        )
        val elasticItemActivity = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${itemActivity.id}",
            type = ActivityTypeDto.MINT,
            blockchain = BlockchainDto.ETHEREUM,
            date = itemActivity.date,
            item = "${assetTypeDto.contract}:${assetTypeDto.tokenId}"
        )
        val elasticAuctionActivity = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${auctionActivity.id}",
            type = ActivityTypeDto.AUCTION_STARTED,
            blockchain = BlockchainDto.ETHEREUM,
            date = auctionActivity.date,
            item = "${assetTypeDto.contract}:${assetTypeDto.tokenId}"
        )

        repository.saveAll(listOf(elasticOrderActivity, elasticItemActivity, elasticAuctionActivity))

        coEvery {
            testEthereumActivityOrderApi.getOrderActivitiesById(
                ActivitiesByIdRequestDto(ids = listOf(elasticOrderActivity.activityId))
            )
        } returns OrderActivitiesDto(null, listOf(orderActivity)).toMono()

        coEvery {
            testEthereumActivityAuctionApi.getAuctionActivitiesById(
                ActivitiesByIdRequestDto(ids = listOf(elasticAuctionActivity.activityId))
            )
        } returns AuctionActivitiesDto(null, listOf(auctionActivity)).toMono()

        coEvery {
            testEthereumActivityItemApi.getNftActivitiesById(
                ActivitiesByIdRequestDto(ids = listOf(elasticItemActivity.activityId))
            )
        } returns NftActivitiesDto(null, listOf(itemActivity)).toMono()

        val activities = activityControllerApi.getActivitiesByItem(
            types, ethItemId.fullId(), continuation, null, 100, sort
        ).awaitFirst()

        assertThat(activities.activities).hasSize(3)
        assertThat(activities.activities[0]).isInstanceOf(OrderBidActivityDto::class.java)
        assertThat(activities.activities[1]).isInstanceOf(MintActivityDto::class.java)
        assertThat(activities.activities[2]).isInstanceOf(AuctionStartActivityDto::class.java)
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
        val size = 3

        val ethItemActivity = randomEthItemMintActivity()
            .copy(date = Instant.now().minusSeconds(5))
        val ethItemActivity2 = randomEthItemMintActivity()
            .copy(date = Instant.now().minusSeconds(6))
        val polygonItemActivity = randomEthItemMintActivity()
            .copy(date = Instant.now().minusSeconds(7))

        val elasticEthItemActivity = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${ethItemActivity.id}",
            type = ActivityTypeDto.MINT,
            blockchain = BlockchainDto.ETHEREUM,
            date = ethItemActivity.date,
            userFrom = userEth.value,
        )
        val elasticEthItemActivity2 = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${ethItemActivity2.id}",
            type = ActivityTypeDto.MINT,
            blockchain = BlockchainDto.ETHEREUM,
            date = ethItemActivity2.date,
            userFrom = userEth.value,
        )
        val elasticPolygonItemActivity = randomEsActivity().copy(
            activityId = "${BlockchainDto.POLYGON}:${polygonItemActivity.id}",
            type = ActivityTypeDto.MINT,
            blockchain = BlockchainDto.POLYGON,
            date = polygonItemActivity.date,
            userFrom = userEth.value,
        )

        repository.saveAll(
            listOf(elasticEthItemActivity, elasticEthItemActivity2, elasticPolygonItemActivity)
        )

        coEvery {
            testEthereumActivityItemApi.getNftActivitiesById(
                ActivitiesByIdRequestDto(ids = listOf(elasticEthItemActivity.activityId, elasticEthItemActivity2.activityId))
            )
        } returns NftActivitiesDto(null, listOf(ethItemActivity, ethItemActivity2)).toMono()

        coEvery {
            testPolygonActivityItemApi.getNftActivitiesById(
                ActivitiesByIdRequestDto(ids = listOf(elasticPolygonItemActivity.activityId))
            )
        } returns NftActivitiesDto(null, listOf(polygonItemActivity)).toMono()

        val now = Instant.now()
        val oneWeekAgo = now.minus(7, ChronoUnit.DAYS)
        val activities = activityControllerApi.getActivitiesByUser(
            types, listOf(userEth.fullId()), null, oneWeekAgo, now, null, null, size, sort
        ).awaitFirst()

        assertThat(activities.activities).hasSize(3)
        assertThat(activities.cursor).isNotNull()
    }
}
