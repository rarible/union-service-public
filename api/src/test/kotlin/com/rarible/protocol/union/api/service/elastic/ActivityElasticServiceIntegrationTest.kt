package com.rarible.protocol.union.api.service.elastic

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.api.dto.applyCursor
import com.rarible.protocol.union.api.metrics.ElasticMetricsFactory
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.TypedActivityId
import com.rarible.protocol.union.core.model.elastic.EsActivity
import com.rarible.protocol.union.core.model.elastic.EsActivityCursor
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.AuctionStartActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.FlowAssetTypeFtDto
import com.rarible.protocol.union.dto.FlowAssetTypeNftDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.SolanaFtAssetTypeDto
import com.rarible.protocol.union.dto.SolanaNftAssetTypeDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.group
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.converter.ActivityDtoConverter
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
import randomAssetTypeDto
import randomAuction
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
    private lateinit var objectMapper: ObjectMapper

    private lateinit var service: ActivityElasticService

    private lateinit var one: EsActivity
    private lateinit var two: EsActivity
    private lateinit var three: EsActivity
    private lateinit var four: EsActivity
    private lateinit var five: EsActivity
    private lateinit var six: EsActivity
    private lateinit var seven: EsActivity

    private lateinit var dto1: ActivityDto
    private lateinit var dto2: ActivityDto
    private lateinit var dto3: ActivityDto
    private lateinit var dto4: ActivityDto
    private lateinit var dto5: ActivityDto
    private lateinit var dto6: ActivityDto
    private lateinit var dto7: ActivityDto

    private lateinit var missingBefore: Map<BlockchainDto, Double>

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        missingBefore = metricsFactory.missingActivitiesCounters.map { (k, v) -> k to v.count() }.toMap()

        elasticsearchTestBootstrapper.bootstrap()
        // save some elastic activities
        dto1 = MintActivityDto(
            id = ActivityIdDto(BlockchainDto.ETHEREUM, "1"),
            date = Instant.ofEpochMilli(5_000),
            owner = randomUnionAddress(),
            value = BigInteger.ONE,
            transactionHash = randomString()
        )
        one = randomEsActivity().copy(
            activityId = "ETHEREUM:1",
            type = ActivityTypeDto.MINT,
            blockchain = BlockchainDto.ETHEREUM,
            date = Instant.ofEpochMilli(5_000),
            userFrom = "0x112233",
            activityDto = objectMapper.writeValueAsString(dto1)
        )
        dto2 = OrderListActivityDto(
            id = ActivityIdDto(BlockchainDto.ETHEREUM, "1"),
            date = Instant.ofEpochMilli(4_900),
            make = AssetDto(
                EthErc721AssetTypeDto(
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
            take = AssetDto(
                EthEthereumAssetTypeDto(blockchain = BlockchainDto.ETHEREUM),
                BigDecimal.ONE,
            )
        )
        two = randomEsActivity().copy(
            activityId = "ETHEREUM:2",
            type = ActivityTypeDto.LIST,
            blockchain = BlockchainDto.ETHEREUM,
            date = Instant.ofEpochMilli(4_900),
            collection = "123",
            activityDto = objectMapper.writeValueAsString(dto2)
        )
        dto3 = MintActivityDto(
            id = ActivityIdDto(BlockchainDto.FLOW, "3"),
            date = Instant.ofEpochMilli(4_800),
            owner = randomUnionAddress(),
            value = BigInteger.ONE,
            transactionHash = randomString()
        )
        three = randomEsActivity().copy(
            activityId = "FLOW:3",
            type = ActivityTypeDto.MINT,
            blockchain = BlockchainDto.FLOW,
            date = Instant.ofEpochMilli(4_800),
            activityDto = objectMapper.writeValueAsString(dto3)
        )
        dto4 = OrderListActivityDto(
            id = ActivityIdDto(BlockchainDto.FLOW, "4"),
            date = Instant.ofEpochMilli(4_700),
            make = AssetDto(
                FlowAssetTypeNftDto(
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
            take = AssetDto(
                FlowAssetTypeFtDto(contract = ContractAddress(blockchain = BlockchainDto.FLOW, value = "123")),
                BigDecimal.ONE,
            )
        )
        four = randomEsActivity().copy(
            activityId = "FLOW:4",
            type = ActivityTypeDto.LIST,
            blockchain = BlockchainDto.FLOW,
            date = Instant.ofEpochMilli(4_700),
            activityDto = objectMapper.writeValueAsString(dto4)
        )
        dto5 = AuctionStartActivityDto(
            id = ActivityIdDto(BlockchainDto.FLOW, "5"),
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
            )
        )
        five = randomEsActivity().copy(
            activityId = "FLOW:5",
            type = ActivityTypeDto.AUCTION_STARTED,
            blockchain = BlockchainDto.FLOW,
            date = Instant.ofEpochMilli(5_700),
            activityDto = objectMapper.writeValueAsString(dto5)
        )
        dto6 = OrderListActivityDto(
            id = ActivityIdDto(BlockchainDto.SOLANA, "6"),
            date = Instant.ofEpochMilli(6_700),
            make = AssetDto(
                SolanaNftAssetTypeDto(
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
            take = AssetDto(
                SolanaFtAssetTypeDto(
                    address = ContractAddress(
                        blockchain = BlockchainDto.SOLANA,
                        value = "321"
                    )
                ),
                BigDecimal.ONE,
            )
        )
        six = randomEsActivity().copy(
            activityId = "SOLANA:6",
            type = ActivityTypeDto.LIST,
            blockchain = BlockchainDto.SOLANA,
            date = Instant.ofEpochMilli(6_700),
            activityDto = objectMapper.writeValueAsString(dto6)
        )
        dto7 = AuctionStartActivityDto(
            id = ActivityIdDto(BlockchainDto.ETHEREUM, "7"),
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
            activityDto = objectMapper.writeValueAsString(dto7)
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
                metricsFactory,
                FeatureFlagsProperties(
                    enableEsActivitySource = false,
                ),
                objectMapper,
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
            val actual = service.getAllActivities(types, blockchains, null, null, size, sort)

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
            val actual = service.getAllActivities(types, blockchains, null, null, size, sort)

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
    inner class GetAllActivitiesEsSourceTest {

        @BeforeEach
        fun before() {
            service = ActivityElasticService(
                filterConverter,
                repository,
                enrichmentActivityService,
                router,
                metricsFactory,
                FeatureFlagsProperties(
                    enableEsActivitySource = true,
                ),
                objectMapper,
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
            val actual = service.getAllActivities(types, blockchains, null, null, size, sort)

            // then
            assertThat(actual.activities).containsExactly(
                dto1.applyCursor(cursor1),
                dto2.applyCursor(cursor2),
                dto3.applyCursor(cursor3),
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
                metricsFactory,
                FeatureFlagsProperties(
                    enableEsActivitySource = false,
                ),
                objectMapper,
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
            val actual = service.getActivitiesByCollection(types, listOf(collection), null, null, size, sort)

            // then
            assertThat(actual.activities)
                .containsExactly(ActivityDtoConverter.convert(eth2))

            assertThat(actual.cursor).startsWith("4900_")
            assertCounterChanges()
        }
    }

    @Nested
    inner class GetActivitiesByCollectionEsSourceTest {

        @BeforeEach
        fun before() {
            service = ActivityElasticService(
                filterConverter,
                repository,
                enrichmentActivityService,
                router,
                metricsFactory,
                FeatureFlagsProperties(
                    enableEsActivitySource = true,
                ),
                objectMapper,
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
            val actual = service.getActivitiesByCollection(types, listOf(collection), null, null, size, sort)

            // then
            assertThat(actual.activities)
                .containsExactly(dto2.applyCursor(cursor2))

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
                metricsFactory,
                FeatureFlagsProperties(
                    enableEsActivitySource = false,
                ),
                objectMapper,
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
            val actual = service.getActivitiesByItem(types, item, null, null, size, sort)

            // then
            assertThat(actual.activities)
                .containsExactly(ActivityDtoConverter.convert(eth7))

            assertThat(actual.cursor).startsWith("4700_")
            assertCounterChanges()
        }
    }

    @Nested
    inner class GetActivitiesByItemEsSourceTest {

        @BeforeEach
        fun before() {
            service = ActivityElasticService(
                filterConverter,
                repository,
                enrichmentActivityService,
                router,
                metricsFactory,
                FeatureFlagsProperties(
                    enableEsActivitySource = true,
                ),
                objectMapper,
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
            val actual = service.getActivitiesByItem(types, item, null, null, size, sort)

            // then
            assertThat(actual.activities).hasSize(1)
            assertThat(actual.activities[0]).isEqualTo(dto7.applyCursor(cursor7))

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
                metricsFactory,
                FeatureFlagsProperties(
                    enableEsActivitySource = false,
                ),
                objectMapper,
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
    inner class GetActivitiesByUserEsSourceTest {

        @BeforeEach
        fun before() {
            service = ActivityElasticService(
                filterConverter,
                repository,
                enrichmentActivityService,
                router,
                metricsFactory,
                FeatureFlagsProperties(
                    enableEsActivitySource = true,
                ),
                objectMapper,
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
            assertThat(actual.activities[0]).isEqualTo(dto1.applyCursor(cursor1))
            assertThat(actual.activities[1]).isEqualTo(dto7.applyCursor(cursor7))
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
