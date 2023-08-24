package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.api.metrics.ElasticMetricsFactory
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.TypedActivityId
import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionEthErc721AssetType
import com.rarible.protocol.union.core.model.UnionEthEthereumAssetType
import com.rarible.protocol.union.core.model.UnionFlowAssetTypeFt
import com.rarible.protocol.union.core.model.UnionFlowAssetTypeNft
import com.rarible.protocol.union.core.model.UnionSolanaFtAssetType
import com.rarible.protocol.union.core.model.UnionSolanaNftAssetType
import com.rarible.protocol.union.core.model.elastic.EsActivity
import com.rarible.protocol.union.core.model.elastic.EsActivityCursor
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.FlowAssetTypeFtDto
import com.rarible.protocol.union.dto.FlowAssetTypeNftDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.converter.ActivityDtoConverter
import com.rarible.protocol.union.enrichment.converter.EnrichmentActivityDtoConverter
import com.rarible.protocol.union.enrichment.model.EnrichmentActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentAuctionStartActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentMintActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentOrderListActivity
import com.rarible.protocol.union.enrichment.repository.ActivityRepository
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityService
import com.rarible.protocol.union.enrichment.test.data.randomEsActivity
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityMint
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityOrderList
import com.rarible.protocol.union.enrichment.test.data.randomUnionAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import randomAuction
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

@IntegrationTest
internal class ActivityElasticServiceIntegrationTest {

    private val ethereumService: ActivityService = mockk { every { blockchain } returns BlockchainDto.ETHEREUM }
    private val flowService: ActivityService = mockk { every { blockchain } returns BlockchainDto.FLOW }
    private val solanaService: ActivityService = mockk { every { blockchain } returns BlockchainDto.SOLANA }
    private val router = BlockchainRouter(
        listOf(ethereumService, flowService, solanaService),
        listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW, BlockchainDto.SOLANA)
    )

    @Autowired
    private lateinit var filterConverter: ActivityFilterConverter

    @Autowired
    private lateinit var repository: EsActivityRepository

    @Autowired
    private lateinit var metricsFactory: ElasticMetricsFactory

    @Autowired
    private lateinit var enrichmentActivityService: EnrichmentActivityService

    @Autowired
    private lateinit var activityRepository: ActivityRepository

    @Autowired
    private lateinit var esActivityOptimizedSearchService: EsActivityOptimizedSearchService

    private lateinit var service: ActivityElasticService

    private lateinit var one: EsActivity
    private lateinit var two: EsActivity
    private lateinit var three: EsActivity
    private lateinit var four: EsActivity
    private lateinit var five: EsActivity
    private lateinit var six: EsActivity
    private lateinit var seven: EsActivity

    private lateinit var activity1: EnrichmentActivity
    private lateinit var activity2: EnrichmentActivity
    private lateinit var activity3: EnrichmentActivity
    private lateinit var activity4: EnrichmentActivity
    private lateinit var activity5: EnrichmentActivity
    private lateinit var activity6: EnrichmentActivity
    private lateinit var activity7: EnrichmentActivity

    private lateinit var missingBefore: Map<BlockchainDto, Double>

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        missingBefore = metricsFactory.missingActivitiesCounters.map { (k, v) -> k to v.count() }.toMap()

        elasticsearchTestBootstrapper.bootstrap()
        // save some elastic activities
        activity1 = activityRepository.save(
            EnrichmentMintActivity(
                blockchain = BlockchainDto.ETHEREUM,
                activityId = "1",
                date = Instant.ofEpochMilli(5_000),
                owner = randomUnionAddress(),
                value = BigInteger.ONE,
                transactionHash = randomString(),
                itemId = ItemIdDto(BlockchainDto.ETHEREUM, Address.ONE().toString(), BigInteger.ONE).fullId()
            )
        )
        one = randomEsActivity().copy(
            activityId = "ETHEREUM:1",
            type = ActivityTypeDto.MINT,
            blockchain = BlockchainDto.ETHEREUM,
            date = Instant.ofEpochMilli(5_000),
            userFrom = "0x112233",
        )
        activity2 = activityRepository.save(
            EnrichmentOrderListActivity(
                blockchain = BlockchainDto.ETHEREUM,
                activityId = "2",
                date = Instant.ofEpochMilli(4_900),
                make = UnionAsset(
                    UnionEthErc721AssetType(
                        contract = ContractAddress(
                            blockchain = BlockchainDto.ETHEREUM,
                            value = "123",
                        ),
                        tokenId = BigInteger.ONE,
                    ),
                    BigDecimal.ONE,
                ),
                maker = randomUnionAddress(),
                hash = randomString(),
                price = BigDecimal.ONE,
                take = UnionAsset(
                    UnionEthEthereumAssetType(blockchain = BlockchainDto.ETHEREUM),
                    BigDecimal.ONE,
                ),
                itemId = ItemIdDto(BlockchainDto.ETHEREUM, "123", BigInteger.ONE).fullId(),
            )
        )
        two = randomEsActivity().copy(
            activityId = "ETHEREUM:2",
            type = ActivityTypeDto.LIST,
            blockchain = BlockchainDto.ETHEREUM,
            date = Instant.ofEpochMilli(4_900),
            collection = "123",
        )
        activity3 = activityRepository.save(
            EnrichmentMintActivity(
                blockchain = BlockchainDto.FLOW,
                activityId = "3",
                date = Instant.ofEpochMilli(4_800),
                owner = randomUnionAddress(),
                value = BigInteger.ONE,
                transactionHash = randomString(),
                itemId = ItemIdDto(BlockchainDto.FLOW, "123", BigInteger.ONE).fullId(),
            )
        )
        three = randomEsActivity().copy(
            activityId = "FLOW:3",
            type = ActivityTypeDto.MINT,
            blockchain = BlockchainDto.FLOW,
            date = Instant.ofEpochMilli(4_800),
        )
        activity4 = activityRepository.save(
            EnrichmentOrderListActivity(
                blockchain = BlockchainDto.FLOW,
                activityId = "4",
                date = Instant.ofEpochMilli(4_700),
                make = UnionAsset(
                    UnionFlowAssetTypeNft(
                        contract = ContractAddress(
                            blockchain = BlockchainDto.FLOW,
                            value = "123",
                        ),
                        tokenId = BigInteger.ONE,
                    ),
                    BigDecimal.ONE,
                ),
                maker = randomUnionAddress(),
                hash = randomString(),
                price = BigDecimal.ONE,
                take = UnionAsset(
                    UnionFlowAssetTypeFt(contract = ContractAddress(blockchain = BlockchainDto.FLOW, value = "123")),
                    BigDecimal.ONE,
                ),
                itemId = ItemIdDto(BlockchainDto.FLOW, "123", BigInteger.ONE).fullId(),
            )
        )
        four = randomEsActivity().copy(
            activityId = "FLOW:4",
            type = ActivityTypeDto.LIST,
            blockchain = BlockchainDto.FLOW,
            date = Instant.ofEpochMilli(4_700),
        )
        activity5 = activityRepository.save(
            EnrichmentAuctionStartActivity(
                blockchain = BlockchainDto.FLOW,
                activityId = "5",
                date = Instant.ofEpochMilli(5_700),
                auction = randomAuction(
                    id = AuctionIdDto(BlockchainDto.FLOW, randomString().lowercase()),
                    contract = ContractAddress(BlockchainDto.FLOW, randomString().lowercase()),
                    minimalStep = BigDecimal.ONE,
                    buyPrice = BigDecimal.ONE,
                    buyPriceUsd = BigDecimal.ONE,
                    minimalPrice = BigDecimal.ONE,
                    seller = UnionAddress(BlockchainGroupDto.FLOW, randomString().lowercase()),
                    sell = AssetDto(
                        FlowAssetTypeFtDto(
                            contract = ContractAddress(BlockchainDto.FLOW, randomString().lowercase()),
                        ),
                        BigDecimal.ONE
                    ),
                    buy = FlowAssetTypeNftDto(
                        contract = ContractAddress(BlockchainDto.FLOW, randomString().lowercase()),
                        tokenId = randomBigInt(),
                    )
                ),
                itemId = ItemIdDto(BlockchainDto.FLOW, "123", BigInteger.ONE).fullId(),
            )
        )
        five = randomEsActivity().copy(
            activityId = "FLOW:5",
            type = ActivityTypeDto.AUCTION_STARTED,
            blockchain = BlockchainDto.FLOW,
            date = Instant.ofEpochMilli(5_700),
        )
        activity6 = activityRepository.save(
            EnrichmentOrderListActivity(
                blockchain = BlockchainDto.SOLANA,
                activityId = "6",
                date = Instant.ofEpochMilli(6_700),
                make = UnionAsset(
                    UnionSolanaNftAssetType(
                        contract = ContractAddress(
                            blockchain = BlockchainDto.SOLANA,
                            value = "123",
                        ),
                        itemId = ItemIdDto(blockchain = BlockchainDto.SOLANA, value = "itemId")
                    ),
                    BigDecimal.ONE,
                ),
                maker = randomUnionAddress(),
                hash = randomString(),
                price = BigDecimal.ONE,
                take = UnionAsset(
                    UnionSolanaFtAssetType(
                        address = ContractAddress(
                            blockchain = BlockchainDto.SOLANA,
                            value = "321"
                        )
                    ),
                    BigDecimal.ONE,
                ),
                itemId = ItemIdDto(BlockchainDto.SOLANA, "itemId").fullId(),
            )
        )
        six = randomEsActivity().copy(
            activityId = "SOLANA:6",
            type = ActivityTypeDto.LIST,
            blockchain = BlockchainDto.SOLANA,
            date = Instant.ofEpochMilli(6_700),
        )
        activity7 = activityRepository.save(
            EnrichmentAuctionStartActivity(
                blockchain = BlockchainDto.ETHEREUM,
                activityId = "7",
                date = Instant.ofEpochMilli(4_700),
                auction = randomAuction(
                    id = AuctionIdDto(BlockchainDto.ETHEREUM, randomString().lowercase()),
                    minimalStep = BigDecimal.TEN,
                    contract = ContractAddress(BlockchainDto.ETHEREUM, randomString().lowercase()),
                    buyPrice = BigDecimal.TEN,
                    minimalPrice = BigDecimal.TEN,
                    sell = AssetDto(EthEthereumAssetTypeDto(blockchain = BlockchainDto.ETHEREUM), BigDecimal.TEN),
                    seller = UnionAddress(BlockchainGroupDto.ETHEREUM, randomString().lowercase()),
                    buyPriceUsd = BigDecimal.TEN,
                    buy = FlowAssetTypeNftDto(
                        contract = ContractAddress(BlockchainDto.ETHEREUM, randomString().lowercase()),
                        tokenId = randomBigInt(),
                    )
                ),
                itemId = ItemIdDto(BlockchainDto.ETHEREUM, "123", BigInteger.ONE).fullId(),
            )
        )
        seven = randomEsActivity().copy(
            activityId = "ETHEREUM:7",
            type = ActivityTypeDto.AUCTION_STARTED,
            blockchain = BlockchainDto.ETHEREUM,
            date = Instant.ofEpochMilli(4_700),
            item = "222:333",
            userFrom = "0",
            userTo = "0x223344",
        )
        repository.saveAll(listOf(one, two, three, four, five, six, seven).shuffled())
    }

    @Nested
    inner class GetAllActivitiesTest {

        @BeforeEach
        fun before() {
            service = ActivityElasticService(
                filterConverter,
                repository,
                enrichmentActivityService,
                router,
                activityRepository,
                FeatureFlagsProperties(
                    enableMongoActivityRead = false,
                ),
                esActivityOptimizedSearchService,
                metricsFactory,
            )
        }

        @Test
        fun `should getAllActivities - happy path`() = runBlocking<Unit> {
            // given
            val types = listOf(ActivityTypeDto.MINT, ActivityTypeDto.LIST)
            val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW)
            val size = 3
            val sort = ActivitySortDto.LATEST_FIRST
            val cursor1 = buildCursor(one)
            val cursor2 = buildCursor(two)
            val cursor3 = buildCursor(three)

            val eth1 = randomUnionActivityMint(randomEthItemId()).copy(
                id = ActivityIdDto(BlockchainDto.ETHEREUM, "1"),
                cursor = cursor1,
            )
            val eth2 = randomUnionActivityOrderList(BlockchainDto.ETHEREUM).copy(
                id = ActivityIdDto(BlockchainDto.ETHEREUM, "2"),
                cursor = cursor2,
            )
            val flow1 = randomUnionActivityMint(randomEthItemId()).copy(
                id = ActivityIdDto(BlockchainDto.FLOW, "3"),
                cursor = cursor3,
            )

            coEvery {
                ethereumService.getActivitiesByIds(
                    listOf(
                        TypedActivityId("1", ActivityTypeDto.MINT),
                        TypedActivityId("2", ActivityTypeDto.LIST),
                    )
                )
            } returns listOf(eth1, eth2)
            coEvery {
                flowService.getActivitiesByIds(
                    listOf(
                        TypedActivityId("3", ActivityTypeDto.MINT),
                    )
                )
            } returns listOf(flow1)

            // when
            val actual = service.getAllActivities(types, blockchains, null, null, null, size, sort)

            // then
            assertThat(actual.activities).containsExactly(
                ActivityDtoConverter.convert(eth1),
                ActivityDtoConverter.convert(eth2),
                ActivityDtoConverter.convert(flow1)
            )
            assertThat(actual.cursor).startsWith("4800_")
            assertCounterChanges()
        }

        @Test
        fun `get all activities, some are missing in blockchains - counters inc`() = runBlocking<Unit> {
            // given
            val types = listOf(ActivityTypeDto.MINT, ActivityTypeDto.LIST)
            val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW)
            val size = 3
            val sort = ActivitySortDto.LATEST_FIRST
            val cursor2 = buildCursor(two)

            val eth2 = randomUnionActivityOrderList(BlockchainDto.ETHEREUM).copy(
                id = ActivityIdDto(BlockchainDto.ETHEREUM, "2"),
                cursor = cursor2,
            )

            coEvery {
                ethereumService.getActivitiesByIds(
                    listOf(
                        TypedActivityId("1", ActivityTypeDto.MINT),
                        TypedActivityId("2", ActivityTypeDto.LIST),
                    )
                )
            } returns listOf(eth2)
            coEvery {
                flowService.getActivitiesByIds(
                    listOf(
                        TypedActivityId("3", ActivityTypeDto.MINT),
                    )
                )
            } returns emptyList()

            // when
            val actual = service.getAllActivities(types, blockchains, null, null, null, size, sort)

            // then
            assertThat(actual.activities)
                .containsExactly(ActivityDtoConverter.convert(eth2))

            assertCounterChanges(
                BlockchainDto.ETHEREUM to 1,
                BlockchainDto.FLOW to 1,
            )
        }
    }

    @Nested
    inner class GetAllActivitiesMongoSourceTest {

        @BeforeEach
        fun before() {
            service = ActivityElasticService(
                filterConverter,
                repository,
                enrichmentActivityService,
                router,
                activityRepository,
                FeatureFlagsProperties(
                    enableMongoActivityRead = true,
                ),
                esActivityOptimizedSearchService,
                metricsFactory,
            )
        }

        @Test
        fun `should getAllActivities - happy path`() = runBlocking<Unit> {
            // given
            val types = listOf(ActivityTypeDto.MINT, ActivityTypeDto.LIST)
            val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW)
            val size = 3
            val sort = ActivitySortDto.LATEST_FIRST
            val cursor1 = buildCursor(one)
            val cursor2 = buildCursor(two)
            val cursor3 = buildCursor(three)

            // when
            val actual = service.getAllActivities(types, blockchains, null, null, null, size, sort)

            // then
            assertThat(actual.activities).containsExactly(
                EnrichmentActivityDtoConverter.convert(activity1, cursor1),
                EnrichmentActivityDtoConverter.convert(activity2, cursor2),
                EnrichmentActivityDtoConverter.convert(activity3, cursor3),
            )
            assertThat(actual.cursor).startsWith("4800_")
            assertCounterChanges()
        }
    }

    @Nested
    inner class GetActivitiesByCollectionTest {

        @BeforeEach
        fun before() {
            service = ActivityElasticService(
                filterConverter,
                repository,
                enrichmentActivityService,
                router,
                activityRepository,
                FeatureFlagsProperties(
                    enableMongoActivityRead = false,
                ),
                esActivityOptimizedSearchService,
                metricsFactory,
            )
        }

        @Test
        fun `should getActivitiesByCollection - happy path`() = runBlocking<Unit> {
            // given
            val types = listOf(ActivityTypeDto.MINT, ActivityTypeDto.LIST)
            val collection = IdParser.parseCollectionId("ETHEREUM:123")
            val size = 3
            val sort = ActivitySortDto.LATEST_FIRST
            val cursor2 = buildCursor(two)
            val eth2 = randomUnionActivityOrderList(BlockchainDto.ETHEREUM).copy(
                id = ActivityIdDto(BlockchainDto.ETHEREUM, "2"),
                cursor = cursor2,
            )
            coEvery {
                ethereumService.getActivitiesByIds(
                    listOf(
                        TypedActivityId("2", ActivityTypeDto.LIST),
                    )
                )
            } returns listOf(eth2)
            // when
            val actual = service.getActivitiesByCollection(types, listOf(collection), null, null, null, size, sort)

            // then
            assertThat(actual.activities)
                .containsExactly(ActivityDtoConverter.convert(eth2))

            assertThat(actual.cursor).startsWith("4900_")
            assertCounterChanges()
        }
    }

    @Nested
    inner class GetActivitiesByCollectionMongoSourceTest {

        @BeforeEach
        fun before() {
            service = ActivityElasticService(
                filterConverter,
                repository,
                enrichmentActivityService,
                router,
                activityRepository,
                FeatureFlagsProperties(
                    enableMongoActivityRead = true,
                ),
                esActivityOptimizedSearchService,
                metricsFactory,
            )
        }

        @Test
        fun `should getActivitiesByCollection - happy path`() = runBlocking<Unit> {
            // given
            val types = listOf(ActivityTypeDto.MINT, ActivityTypeDto.LIST)
            val collection = IdParser.parseCollectionId("ETHEREUM:123")
            val size = 3
            val sort = ActivitySortDto.LATEST_FIRST
            val cursor2 = buildCursor(two)
            // when
            val actual = service.getActivitiesByCollection(types, listOf(collection), null, null, null, size, sort)

            // then
            assertThat(actual.activities)
                .containsExactly(EnrichmentActivityDtoConverter.convert(activity2, cursor2))

            assertThat(actual.cursor).startsWith("4900_")
            assertCounterChanges()
        }
    }

    @Nested
    inner class GetActivitiesByItemTest {

        @BeforeEach
        fun before() {
            service = ActivityElasticService(
                filterConverter,
                repository,
                enrichmentActivityService,
                router,
                activityRepository,
                FeatureFlagsProperties(
                    enableMongoActivityRead = false,
                ),
                esActivityOptimizedSearchService,
                metricsFactory,
            )
        }

        @Test
        fun `should getActivitiesByItem - happy path`() = runBlocking<Unit> {
            // given
            val types = listOf(ActivityTypeDto.MINT, ActivityTypeDto.LIST, ActivityTypeDto.AUCTION_STARTED)
            val item = IdParser.parseItemId("ETHEREUM:222:333")
            val size = 3
            val sort = ActivitySortDto.LATEST_FIRST
            val cursor7 = buildCursor(seven)
            val eth7 = randomUnionActivityOrderList(BlockchainDto.ETHEREUM).copy(
                id = ActivityIdDto(BlockchainDto.ETHEREUM, "7"),
                cursor = cursor7,
            )
            coEvery {
                ethereumService.getActivitiesByIds(
                    listOf(
                        TypedActivityId("7", ActivityTypeDto.AUCTION_STARTED),
                    )
                )
            } returns listOf(eth7)

            // when
            val actual = service.getActivitiesByItem(types, item, null, null, null, size, sort)

            // then
            assertThat(actual.activities)
                .containsExactly(ActivityDtoConverter.convert(eth7))

            assertThat(actual.cursor).startsWith("4700_")
            assertCounterChanges()
        }
    }

    @Nested
    inner class GetActivitiesByItemMongoSourceTest {

        @BeforeEach
        fun before() {
            service = ActivityElasticService(
                filterConverter,
                repository,
                enrichmentActivityService,
                router,
                activityRepository,
                FeatureFlagsProperties(
                    enableMongoActivityRead = true,
                ),
                esActivityOptimizedSearchService,
                metricsFactory,
            )
        }

        @Test
        fun `should getActivitiesByItem - happy path`() = runBlocking<Unit> {
            // given
            val types = listOf(ActivityTypeDto.MINT, ActivityTypeDto.LIST, ActivityTypeDto.AUCTION_STARTED)
            val item = IdParser.parseItemId("ETHEREUM:222:333")
            val size = 3
            val sort = ActivitySortDto.LATEST_FIRST
            val cursor7 = buildCursor(seven)

            // when
            val actual = service.getActivitiesByItem(types, item, null, null, null, size, sort)

            // then
            assertThat(actual.activities).hasSize(1)
            assertThat(actual.activities[0]).isEqualTo(EnrichmentActivityDtoConverter.convert(activity7, cursor7))

            assertThat(actual.cursor).startsWith("4700_")
            assertCounterChanges()
        }
    }

    @Nested
    inner class GetActivitiesByUserTest {

        @BeforeEach
        fun before() {
            service = ActivityElasticService(
                filterConverter,
                repository,
                enrichmentActivityService,
                router,
                activityRepository,
                FeatureFlagsProperties(
                    enableMongoActivityRead = false,
                ),
                esActivityOptimizedSearchService,
                metricsFactory,
            )
        }

        @Test
        fun `should getActivitiesByUser - happy path`() = runBlocking<Unit> {
            // given
            val types = listOf(UserActivityTypeDto.MINT, UserActivityTypeDto.AUCTION_STARTED)
            val blockchains = listOf(BlockchainDto.ETHEREUM)
            val size = 3
            val sort = ActivitySortDto.LATEST_FIRST
            val users = listOf(
                IdParser.parseAddress("ETHEREUM:0x112233"),
                IdParser.parseAddress("ETHEREUM:0x223344")
            )
            val cursor1 = buildCursor(one)
            val cursor7 = buildCursor(seven)
            val eth1 = randomUnionActivityOrderList(BlockchainDto.ETHEREUM).copy(
                id = ActivityIdDto(BlockchainDto.ETHEREUM, "1"),
                cursor = cursor1,
            )
            val eth7 = randomUnionActivityOrderList(BlockchainDto.ETHEREUM).copy(
                id = ActivityIdDto(BlockchainDto.ETHEREUM, "7"),
                cursor = cursor7
            )
            coEvery {
                ethereumService.getActivitiesByIds(
                    listOf(
                        TypedActivityId("1", ActivityTypeDto.MINT),
                        TypedActivityId("7", ActivityTypeDto.AUCTION_STARTED),
                    )
                )
            } returns listOf(eth1, eth7)

            // when
            val actual = service.getActivitiesByUser(
                types,
                blockchains = blockchains,
                bidCurrencies = null,
                continuation = null,
                cursor = null,
                size = size,
                sort = sort,
                user = users,
                from = Instant.ofEpochMilli(4_700),
                to = Instant.ofEpochMilli(5_150)
            )

            // then
            assertThat(actual.activities).containsExactly(
                ActivityDtoConverter.convert(eth1),
                ActivityDtoConverter.convert(eth7)
            )
            assertThat(actual.cursor).startsWith("4700_")
            assertCounterChanges()
        }
    }

    @Nested
    inner class GetActivitiesByUserMongoSourceTest {

        @BeforeEach
        fun before() {
            service = ActivityElasticService(
                filterConverter,
                repository,
                enrichmentActivityService,
                router,
                activityRepository,
                FeatureFlagsProperties(
                    enableMongoActivityRead = true,
                ),
                esActivityOptimizedSearchService,
                metricsFactory,
            )
        }

        @Test
        fun `should getActivitiesByUser - happy path`() = runBlocking<Unit> {
            // given
            val types = listOf(UserActivityTypeDto.MINT, UserActivityTypeDto.AUCTION_STARTED)
            val blockchains = listOf(BlockchainDto.ETHEREUM)
            val size = 3
            val sort = ActivitySortDto.LATEST_FIRST
            val users = listOf(
                IdParser.parseAddress("ETHEREUM:0x112233"),
                IdParser.parseAddress("ETHEREUM:0x223344")
            )
            val cursor1 = buildCursor(one)
            val cursor7 = buildCursor(seven)

            // when
            val actual = service.getActivitiesByUser(
                types,
                blockchains = blockchains,
                bidCurrencies = null,
                continuation = null,
                cursor = null,
                size = size,
                sort = sort,
                user = users,
                from = Instant.ofEpochMilli(4_700),
                to = Instant.ofEpochMilli(5_150)
            )

            // then
            assertThat(actual.activities).hasSize(2)
            assertThat(actual.activities[0]).isEqualTo(EnrichmentActivityDtoConverter.convert(activity1, cursor1))
            assertThat(actual.activities[1]).isEqualTo(EnrichmentActivityDtoConverter.convert(activity7, cursor7))
            assertThat(actual.cursor).startsWith("4700_")
            assertCounterChanges()
        }
    }

    private fun buildCursor(esActivity: EsActivity): String {
        return EsActivityCursor(
            esActivity.date, esActivity.blockNumber!!, esActivity.logIndex!!, esActivity.salt
        ).toString()
    }

    private fun assertCounterChanges(vararg change: Pair<BlockchainDto, Int>) {
        missingBefore.forEach { (blockchain, before) ->
            val after = metricsFactory.missingActivitiesCounters[blockchain]!!.count()
            val diff = change.find { it.first == blockchain }?.second ?: 0
            assertThat(diff).isEqualTo((after - before).toInt())
        }
    }
}
