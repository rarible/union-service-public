package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomInt
import com.rarible.protocol.dto.AssetDto
import com.rarible.protocol.dto.Erc721AssetTypeDto
import com.rarible.protocol.union.api.client.ActivityControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.api.service.UserActivityTypeConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.core.util.truncatedToSeconds
import com.rarible.protocol.union.dto.ActivityCurrencyFilterDto
import com.rarible.protocol.union.dto.ActivitySearchFilterDto
import com.rarible.protocol.union.dto.ActivitySearchRequestDto
import com.rarible.protocol.union.dto.ActivitySearchSortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.ActivityUserFilterDto
import com.rarible.protocol.union.dto.AuctionStartActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderBidActivityDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.enrichment.converter.EnrichmentActivityConverter
import com.rarible.protocol.union.enrichment.repository.ActivityRepository
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.test.data.randomEsActivity
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionStartActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemMintActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderActivityMatch
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderBidActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderListActivity
import com.rarible.protocol.union.integration.flow.converter.FlowActivityConverter
import com.rarible.protocol.union.integration.flow.data.randomFlowBurnDto
import com.rarible.protocol.union.integration.flow.data.randomFlowCancelBidActivityDto
import com.rarible.protocol.union.integration.solana.converter.SolanaActivityConverter
import com.rarible.protocol.union.integration.solana.data.randomSolanaMintActivity
import com.rarible.protocol.union.integration.tezos.data.randomTezosItemBurnActivity
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.AddressFactory
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@IntegrationTest
class ActivityControllerElasticFt : AbstractIntegrationTest() {

    private val continuation: String? = null
    private val defaultSize = PageSize.ACTIVITY.default
    private val sort: com.rarible.protocol.union.dto.ActivitySortDto? = null

    private val logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var ethActivityConverter: EthActivityConverter

    @Autowired
    private lateinit var flowActivityConverter: FlowActivityConverter

    @Autowired
    private lateinit var solanaActivityConverter: SolanaActivityConverter

    @Autowired
    private lateinit var dipDupActivityConverter: DipDupActivityConverter

    @Autowired
    private lateinit var activityControllerApi: ActivityControllerApi

    @Autowired
    private lateinit var esActivityRepository: EsActivityRepository

    @Autowired
    private lateinit var activityRepository: ActivityRepository

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @Autowired
    private lateinit var userActivityTypeConverter: UserActivityTypeConverter

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        elasticsearchTestBootstrapper.bootstrap()
    }

    @Test
    fun `get all activities`() = runBlocking<Unit> {
        val types = ActivityTypeDto.values().toList()
        val blockchains = listOf(
            BlockchainDto.ETHEREUM, BlockchainDto.POLYGON, BlockchainDto.FLOW, BlockchainDto.SOLANA, BlockchainDto.TEZOS
        )
        val size = 5
        val now = nowMillis().truncatedToSeconds()

        // From this list of activities we expect only the oldest 5 in response ordered as:
        // flowActivity1, polygonItemActivity1, ethOrderActivity3, tezosActivity1 and solanaActivity1
        val ethOrderActivity1 = randomEthOrderBidActivity().copy(date = now)
        val ethOrderActivity2 = randomEthOrderBidActivity().copy(date = now.minusSeconds(5))
        val ethOrderActivity3 = randomEthOrderBidActivity().copy(date = now.minusSeconds(10))
        val ethItemActivity1 = randomEthItemMintActivity().copy(date = now.minusSeconds(4))
        val ethItemActivity2 = randomEthItemMintActivity().copy(date = now.minusSeconds(7))
        val ethItemActivity3 = randomEthItemMintActivity().copy(date = now.minusSeconds(7))
        val polygonOrderActivity1 = randomEthOrderBidActivity().copy(date = now.minusSeconds(1))
        val polygonOrderActivity2 = randomEthOrderBidActivity().copy(date = now.minusSeconds(3))
        val polygonItemActivity1 = randomEthItemMintActivity().copy(date = now.minusSeconds(12))
        val polygonItemActivity2 = randomEthItemMintActivity().copy(date = now.minusSeconds(2))
        val flowActivity1 = randomFlowBurnDto().copy(date = nowMillis().minusSeconds(13))
        val flowActivity2 = randomFlowCancelBidActivityDto().copy(date = now.minusSeconds(3))
        val solanaActivity1 = randomSolanaMintActivity().copy(date = now.minusSeconds(8))
        val tezosActivity1 = randomTezosItemBurnActivity().copy(
            id = randomInt().toString(),
            date = now.minusSeconds(9).atOffset(ZoneOffset.UTC)
        )

        val elasticEthOrderActivity1 = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${ethOrderActivity1.id}",
            type = ActivityTypeDto.BID,
            date = ethOrderActivity1.date,
            currency = AddressFactory.create().toString(),
            blockchain = BlockchainDto.ETHEREUM
        )
        val elasticEthOrderActivity2 = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${ethOrderActivity2.id}",
            type = ActivityTypeDto.BID,
            date = ethOrderActivity2.date,
            currency = AddressFactory.create().toString(),
            blockchain = BlockchainDto.ETHEREUM
        )
        val elasticEthOrderActivity3 = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${ethOrderActivity3.id}",
            type = ActivityTypeDto.BID,
            date = ethOrderActivity3.date,
            currency = AddressFactory.create().toString(),
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
            currency = AddressFactory.create().toString(),
            blockchain = BlockchainDto.POLYGON
        )
        val elasticPolygonOrderActivity2 = randomEsActivity().copy(
            activityId = "${BlockchainDto.POLYGON}:${polygonOrderActivity2.id}",
            type = ActivityTypeDto.BID,
            date = polygonOrderActivity2.date,
            currency = AddressFactory.create().toString(),
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
        val elasticFlowActivity1 = randomEsActivity().copy(
            activityId = "${BlockchainDto.FLOW}:${flowActivity1.id}",
            type = ActivityTypeDto.BURN,
            date = flowActivity1.date,
            blockchain = BlockchainDto.FLOW,
        )
        val elasticFlowActivity2 = randomEsActivity().copy(
            activityId = "${BlockchainDto.FLOW}:${flowActivity2.id}",
            type = ActivityTypeDto.CANCEL_BID,
            date = flowActivity2.date,
            currency = AddressFactory.create().toString(),
            blockchain = BlockchainDto.FLOW,
        )
        val elasticSolanaActivity1 = randomEsActivity().copy(
            activityId = "${BlockchainDto.SOLANA}:${solanaActivity1.id}",
            type = ActivityTypeDto.MINT,
            date = solanaActivity1.date,
            blockchain = BlockchainDto.SOLANA,
        )
        val elasticTezosActivity1 = randomEsActivity().copy(
            activityId = "${BlockchainDto.TEZOS}:${tezosActivity1.id}",
            type = ActivityTypeDto.BURN,
            date = tezosActivity1.date.toInstant(),
            blockchain = BlockchainDto.TEZOS,
        )

        esActivityRepository.bulk(
            listOf(
                elasticEthOrderActivity1, elasticEthOrderActivity2, elasticEthOrderActivity3,
                elasticEthItemActivity1, elasticEthItemActivity2, elasticEthItemActivity3,
                elasticPolygonOrderActivity1, elasticPolygonOrderActivity2,
                elasticPolygonItemActivity1, elasticPolygonItemActivity2,
                elasticFlowActivity1, elasticFlowActivity2,
                elasticSolanaActivity1,
                elasticTezosActivity1,
            )
        )

        listOf(ethOrderActivity3, ethItemActivity3).map {
            activityRepository.save(
                EnrichmentActivityConverter.convert(
                    ethActivityConverter.convert(it, BlockchainDto.ETHEREUM)
                )
            )
        }

        activityRepository.save(
            EnrichmentActivityConverter.convert(
                ethActivityConverter.convert(ethItemActivity3, BlockchainDto.ETHEREUM)
            )
        )

        activityRepository.save(
            EnrichmentActivityConverter.convert(
                ethActivityConverter.convert(polygonItemActivity1, BlockchainDto.POLYGON)
            )
        )

        activityRepository.save(
            EnrichmentActivityConverter.convert(
                flowActivityConverter.convert(flowActivity1)
            )
        )

        activityRepository.save(
            EnrichmentActivityConverter.convert(
                solanaActivityConverter.convert(solanaActivity1, BlockchainDto.SOLANA)
            )
        )

        activityRepository.save(
            EnrichmentActivityConverter.convert(
                dipDupActivityConverter.convert(tezosActivity1, BlockchainDto.TEZOS)
            )
        )

        val activitiesByEndpoint = activityControllerApi.getAllActivities(
            types,
            blockchains,
            null,
            null,
            null,
            size,
            com.rarible.protocol.union.dto.ActivitySortDto.EARLIEST_FIRST,
            null,
        ).awaitSingle()

        val activities = activityControllerApi.searchActivities(
            ActivitySearchRequestDto(
                filter = ActivitySearchFilterDto(
                    blockchains = blockchains,
                    types = types
                ),
                size = size,
                sort = ActivitySearchSortDto.EARLIEST
            )
        ).awaitSingle()

        // TODO replace later with search only
        assertThat(activitiesByEndpoint).isEqualTo(activities)

        assertThat(activities.activities).hasSize(5)

        val firstActivity = activities.activities[0]
        val secondActivity = activities.activities[1]
        val thirdActivity = activities.activities[2]
        val fourthActivity = activities.activities[3]
        val fifthActivity = activities.activities[4]

        val message = "Received activities: ${activities.activities}, but expected: " +
            "${listOf(flowActivity1, polygonItemActivity1, ethOrderActivity3, tezosActivity1, solanaActivity1)}"

        assertThat(firstActivity.id.value).withFailMessage(message).isEqualTo(flowActivity1.id)
        assertThat(secondActivity.id.value).withFailMessage(message).isEqualTo(polygonItemActivity1.id)
        assertThat(thirdActivity.id.value).withFailMessage(message).isEqualTo(ethOrderActivity3.id)
        assertThat(fourthActivity.id.value).withFailMessage(message).isEqualTo(tezosActivity1.id)
        assertThat(fifthActivity.id.value).withFailMessage(message).isEqualTo(solanaActivity1.id)

        assertThat(activities.cursor).isNotNull

        val currencyId = CurrencyIdDto(BlockchainDto.ETHEREUM, elasticEthOrderActivity3.currency!!, null)

        val activitiesByEndpoint2 = activityControllerApi.getAllActivities(
            listOf(ActivityTypeDto.BID),
            blockchains,
            listOf(currencyId.fullId()),
            null,
            null,
            size,
            com.rarible.protocol.union.dto.ActivitySortDto.EARLIEST_FIRST,
            null,
        ).awaitFirst()

        val activities2 = activityControllerApi.searchActivities(
            ActivitySearchRequestDto(
                filter = ActivitySearchFilterDto(
                    blockchains = blockchains,
                    types = listOf(ActivityTypeDto.BID),
                    currencies = ActivityCurrencyFilterDto(
                        bid = listOf(currencyId)
                    )
                ),
                size = size,
                sort = ActivitySearchSortDto.EARLIEST
            )
        ).awaitSingle()

        // TODO replace later with search only
        assertThat(activitiesByEndpoint2).isEqualTo(activities2)

        assertThat(activities2.activities.map { it.id.value }).containsExactlyInAnyOrder(ethOrderActivity3.id)
    }

    @Test
    fun `get activities by collection - ethereum`() = runBlocking<Unit> {
        // Here we expect only one query - to Order activities, since there is only order-related activity type
        val types = listOf(ActivityTypeDto.SELL, ActivityTypeDto.BID)
        val ethCollectionId = CollectionIdDto(BlockchainDto.ETHEREUM, randomEthAddress())
        val now = nowMillis().truncatedToSeconds()
        val orderActivity = randomEthOrderListActivity().copy(date = now)
        val bidActivity1 = randomEthOrderBidActivity().copy(date = now.minusSeconds(5))
        val bidActivity2 = randomEthOrderBidActivity().copy(date = now.minusSeconds(15))
        val elasticActivity = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${orderActivity.id}",
            type = ActivityTypeDto.SELL,
            blockchain = ethCollectionId.blockchain,
            collection = ethCollectionId.value,
            date = orderActivity.date,
        )
        val elasticBidActivity1 = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${bidActivity1.id}",
            type = ActivityTypeDto.BID,
            blockchain = ethCollectionId.blockchain,
            collection = ethCollectionId.value,
            currency = AddressFactory.create().toString(),
            date = bidActivity1.date,
        )
        val elasticBidActivity2 = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${bidActivity2.id}",
            type = ActivityTypeDto.BID,
            blockchain = ethCollectionId.blockchain,
            collection = ethCollectionId.value,
            currency = AddressFactory.create().toString(),
            date = bidActivity2.date,
        )

        esActivityRepository.saveAll(listOf(elasticActivity, elasticBidActivity1, elasticBidActivity2))

        listOf(orderActivity, bidActivity1, bidActivity2).map {
            activityRepository.save(
                EnrichmentActivityConverter.convert(
                    ethActivityConverter.convert(it, BlockchainDto.ETHEREUM)
                )
            )
        }

        val currencyId = CurrencyIdDto(BlockchainDto.ETHEREUM, elasticBidActivity1.currency!!, null)

        val activitiesByEndpoint = activityControllerApi.getActivitiesByCollection(
            types,
            listOf(ethCollectionId.fullId()),
            listOf(currencyId.fullId()),
            continuation,
            null,
            defaultSize,
            sort,
            null,
        ).awaitFirst()

        val activities = activityControllerApi.searchActivities(
            ActivitySearchRequestDto(
                filter = ActivitySearchFilterDto(
                    types = types,
                    collections = listOf(ethCollectionId),
                    currencies = ActivityCurrencyFilterDto(
                        bid = listOf(currencyId)
                    )
                ),
                size = defaultSize,
            )
        ).awaitSingle()

        // TODO replace later with search only
        assertThat(activitiesByEndpoint).isEqualTo(activities)

        assertThat(activities.activities.map { it.id.value }).containsExactly(orderActivity.id, bidActivity1.id)
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
        val bidActivity = randomEthOrderBidActivity().copy(
            date = now.minusSeconds(25),
            take = AssetDto(assetTypeDto, randomBigInt(), randomBigDecimal())
        )
        val bidActivity2 = randomEthOrderBidActivity().copy(
            date = now.minusSeconds(35),
            take = AssetDto(assetTypeDto, randomBigInt(), randomBigDecimal())
        )

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
        val elasticBidActivity1 = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${bidActivity.id}",
            type = ActivityTypeDto.BID,
            blockchain = BlockchainDto.ETHEREUM,
            currency = AddressFactory.create().toString(),
            date = bidActivity.date,
            item = "${assetTypeDto.contract}:${assetTypeDto.tokenId}"
        )
        val elasticBidActivity2 = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${bidActivity2.id}",
            type = ActivityTypeDto.BID,
            blockchain = BlockchainDto.ETHEREUM,
            date = bidActivity2.date,
            currency = AddressFactory.create().toString(),
            item = "${assetTypeDto.contract}:${assetTypeDto.tokenId}"
        )

        esActivityRepository.saveAll(
            listOf(
                elasticOrderActivity,
                elasticItemActivity,
                elasticAuctionActivity,
                elasticBidActivity1,
                elasticBidActivity2
            )
        )

        listOf(itemActivity, orderActivity, bidActivity, auctionActivity).map {
            activityRepository.save(
                EnrichmentActivityConverter.convert(
                    ethActivityConverter.convert(it, BlockchainDto.ETHEREUM)
                )
            )
        }

        val currencyId = CurrencyIdDto(BlockchainDto.ETHEREUM, elasticBidActivity1.currency!!, null)

        val activitiesByEndpoint = activityControllerApi.getActivitiesByItem(
            types,
            ethItemId.fullId(),
            listOf(currencyId.fullId()),
            continuation,
            null,
            100,
            sort,
            null,
        ).awaitFirst()

        val activities = activityControllerApi.searchActivities(
            ActivitySearchRequestDto(
                filter = ActivitySearchFilterDto(
                    types = types,
                    items = listOf(ethItemId),
                    currencies = ActivityCurrencyFilterDto(
                        bid = listOf(currencyId)
                    )
                ),
                size = defaultSize,
            )
        ).awaitSingle()

        // TODO replace later with search only
        assertThat(activitiesByEndpoint).isEqualTo(activities)

        assertThat(activities.activities).hasSize(4)
        assertThat(activities.activities[0]).isInstanceOf(OrderBidActivityDto::class.java)
        assertThat(activities.activities[1]).isInstanceOf(MintActivityDto::class.java)
        assertThat(activities.activities[2]).isInstanceOf(AuctionStartActivityDto::class.java)
        assertThat(activities.activities[3].id.value).isEqualTo(bidActivity.id)
    }

    @Test
    fun `get activities by user`() = runBlocking<Unit> {
        // Only Item-specific activity types specified here, so only NFT-Indexer of Ethereum should be requested
        val types = listOf(
            UserActivityTypeDto.TRANSFER_FROM,
            UserActivityTypeDto.TRANSFER_TO,
            UserActivityTypeDto.MINT,
            UserActivityTypeDto.SELL,
            UserActivityTypeDto.MAKE_BID,
            UserActivityTypeDto.GET_BID,
        )
        // Flow and Ethereum user specified - request should be routed only for them, Polygon omitted
        val userEth = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())
        val userEth2 = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())
        val size = 3

        val ethItemActivity = randomEthItemMintActivity()
            .copy(date = nowMillis().minusSeconds(5), id = "ethitemactivity")
        val ethItemActivity2 = randomEthOrderActivityMatch()
            .copy(date = nowMillis().minusSeconds(6), id = "ethitemactivity2")
        val polygonItemActivity = randomEthItemMintActivity()
            .copy(date = nowMillis().minusSeconds(7), id = "polygonitemactivity")
        val sameTypeDifferentRole = randomEthOrderActivityMatch()
            .copy(date = Instant.now().minusSeconds(8), id = "sametypedifferentrole")
        val ethBidActivity1 = randomEthOrderBidActivity()
            .copy(date = nowMillis().minusSeconds(9), id = "ethbidactivity1")
        val ethBidActivity2 = randomEthOrderBidActivity()
            .copy(date = nowMillis().minusSeconds(10), id = "ethbidactivity2")

        val elasticEthItemActivity = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${ethItemActivity.id}",
            type = ActivityTypeDto.TRANSFER,
            blockchain = BlockchainDto.ETHEREUM,
            date = ethItemActivity.date,
            userFrom = userEth.value,
            userTo = userEth2.value
        )
        val elasticEthItemActivity2 = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${ethItemActivity2.id}",
            type = ActivityTypeDto.SELL,
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

        val elasticEthItemActivity3 = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${sameTypeDifferentRole.id}",
            type = ActivityTypeDto.SELL,
            blockchain = BlockchainDto.ETHEREUM,
            date = sameTypeDifferentRole.date,
            userTo = userEth.value,
        )
        val elasticEthBidActivity1 = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${ethBidActivity1.id}",
            type = ActivityTypeDto.BID,
            blockchain = BlockchainDto.ETHEREUM,
            date = ethBidActivity1.date,
            userFrom = userEth.value,
            currency = AddressFactory.create().toString(),
        )
        val elasticEthBidActivity2 = randomEsActivity().copy(
            activityId = "${BlockchainDto.ETHEREUM}:${ethBidActivity2.id}",
            type = ActivityTypeDto.BID,
            blockchain = BlockchainDto.ETHEREUM,
            date = ethBidActivity2.date,
            userFrom = userEth.value,
            currency = AddressFactory.create().toString(),
        )

        esActivityRepository.saveAll(
            listOf(
                elasticEthItemActivity,
                elasticEthItemActivity2,
                elasticPolygonItemActivity,
                elasticEthItemActivity3,
                elasticEthBidActivity1,
                elasticEthBidActivity2
            )
        )

        listOf(ethItemActivity, ethItemActivity2, sameTypeDifferentRole, ethBidActivity1, ethBidActivity2).map {
            activityRepository.save(
                EnrichmentActivityConverter.convert(
                    ethActivityConverter.convert(it, BlockchainDto.ETHEREUM)
                )
            )
        }

        activityRepository.save(
            EnrichmentActivityConverter.convert(
                ethActivityConverter.convert(polygonItemActivity, BlockchainDto.POLYGON)
            )
        )

        val now = nowMillis()
        val oneWeekAgo = now.minus(7, ChronoUnit.DAYS)
        val currencyId = CurrencyIdDto(BlockchainDto.ETHEREUM, elasticEthBidActivity1.currency!!, null)

        val activitiesByEndpoint = activityControllerApi.getActivitiesByUser(
            types,
            listOf(userEth.fullId()),
            null,
            listOf(currencyId.fullId()),
            oneWeekAgo,
            now,
            null,
            null,
            size,
            sort,
            null,
        ).awaitFirst()

        val activities = activityControllerApi.searchActivities(
            ActivitySearchRequestDto(
                filter = ActivitySearchFilterDto(
                    types = types.map { userActivityTypeConverter.convert(it).activityTypeDto }.distinct(),
                    users = ActivityUserFilterDto(
                        any = listOf(userEth),
                    ),
                    from = oneWeekAgo,
                    to = now,
                    currencies = ActivityCurrencyFilterDto(
                        bid = listOf(currencyId)
                    )
                ),
                size = size,
            )
        ).awaitSingle()

        // TODO replace later with search only
        assertThat(activitiesByEndpoint).isEqualTo(activities)

        assertThat(activities.activities.map { it.id.value }).containsExactly(
            ethItemActivity.id,
            ethItemActivity2.id,
            polygonItemActivity.id
        )
        assertThat(activities.cursor).isNotNull

        val activitiesByEndpoint2 = activityControllerApi.getActivitiesByUser(
            types,
            listOf(userEth.fullId()),
            null,
            listOf(currencyId.fullId()),
            oneWeekAgo,
            now,
            null,
            null,
            100,
            sort,
            null,
        ).awaitFirst()

        val activities2 = activityControllerApi.searchActivities(
            ActivitySearchRequestDto(
                filter = ActivitySearchFilterDto(
                    types = types.map { userActivityTypeConverter.convert(it).activityTypeDto }.distinct(),
                    users = ActivityUserFilterDto(
                        any = listOf(userEth),
                    ),
                    from = oneWeekAgo,
                    to = now,
                    currencies = ActivityCurrencyFilterDto(
                        bid = listOf(currencyId)
                    )
                ),
                size = 100,
            )
        ).awaitSingle()

        // TODO replace later with search only
        assertThat(activitiesByEndpoint2).isEqualTo(activities2)

        assertThat(activities2.activities.map { it.id.value }).containsExactly(
            ethItemActivity.id,
            ethItemActivity2.id,
            polygonItemActivity.id,
            sameTypeDifferentRole.id,
            ethBidActivity1.id,
        )

        val fromActivitiesByEndpoint = activityControllerApi.getActivitiesByUser(
            listOf(UserActivityTypeDto.SELL),
            listOf(userEth.fullId()),
            null,
            null,
            oneWeekAgo,
            now,
            null,
            null,
            size,
            sort,
            null,
        ).awaitFirst()

        val fromActivities = activityControllerApi.searchActivities(
            ActivitySearchRequestDto(
                filter = ActivitySearchFilterDto(
                    types = listOf(ActivityTypeDto.SELL),
                    users = ActivityUserFilterDto(
                        from = listOf(userEth),
                    ),
                    from = oneWeekAgo,
                    to = now,
                ),
                size = size,
            )
        ).awaitSingle()

        // TODO replace later with search only
        assertThat(fromActivitiesByEndpoint).isEqualTo(fromActivities)

        assertThat(fromActivities.activities).hasSize(1)

        val toActivitiesByEndpoint = activityControllerApi.getActivitiesByUser(
            listOf(UserActivityTypeDto.TRANSFER_TO),
            listOf(userEth2.fullId()),
            null,
            null,
            oneWeekAgo,
            now,
            null,
            null,
            size,
            sort,
            null,
        ).awaitFirst()

        val toActivities = activityControllerApi.searchActivities(
            ActivitySearchRequestDto(
                filter = ActivitySearchFilterDto(
                    types = listOf(ActivityTypeDto.TRANSFER),
                    users = ActivityUserFilterDto(
                        to = listOf(userEth2),
                    ),
                    from = oneWeekAgo,
                    to = now,
                ),
                size = size,
            )
        ).awaitSingle()

        // TODO replace later with search only
        assertThat(toActivitiesByEndpoint).isEqualTo(toActivities)

        assertThat(toActivities.activities).hasSize(1)
    }
}
