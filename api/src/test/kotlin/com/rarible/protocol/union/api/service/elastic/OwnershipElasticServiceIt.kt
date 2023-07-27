package com.rarible.protocol.union.api.service.elastic

import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomDouble
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.currency.dto.CurrenciesDto
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.converter.CurrencyConverter
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.elastic.EsOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.test.WaitAssert
import com.rarible.protocol.union.core.test.nativeTestCurrencies
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipSearchFilterDto
import com.rarible.protocol.union.dto.OwnershipSearchRequestDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentAuctionService
import com.rarible.protocol.union.enrichment.test.data.randomUnionOwnership
import com.rarible.protocol.union.integration.ethereum.data.randomAddressString
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.flow.data.randomFlowCollectionDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import com.rarible.protocol.union.test.mock.CurrencyMock.mockCurrencies
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import randomOwnershipId
import reactor.kotlin.core.publisher.toMono
import java.time.Duration
import kotlin.random.Random

@IntegrationTest
class OwnershipElasticServiceIt {

    @MockkBean
    private lateinit var router: BlockchainRouter<OwnershipService>

    @MockK
    private lateinit var ethereumService: OwnershipService

    @MockK
    private lateinit var flowService: OwnershipService

    @Autowired
    private lateinit var repository: EsOwnershipRepository

    @Autowired
    private lateinit var service: OwnershipElasticService

    @MockkBean
    private lateinit var enrichmentAuctionService: EnrichmentAuctionService

    private val ownerships = mutableListOf<EsOwnership>()

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    internal fun setUp() {
        runBlocking {
            elasticsearchTestBootstrapper.bootstrap()

            repeat(10) {
                val currency = nativeTestCurrencies().find { it.currencyId == "ethereum" }!!
                val oid = randomOwnershipId(
                    blockchain = BlockchainDto.ETHEREUM,
                    itemIdValue = "${randomAddressString()}:${randomLong()}",
                )
                ownerships.add(
                    EsOwnership(
                        ownershipId = oid.fullId(),
                        originalOwnershipId = null,
                        blockchain = BlockchainDto.ETHEREUM,
                        itemId = oid.getItemId().fullId(),
                        collection = "${randomEthCollectionId()}",
                        owner = oid.owner.fullId(),
                        date = nowMillis() + Duration.ofDays(it.toLong()),
                        auctionId = randomString(),
                        auctionOwnershipId = randomString(),
                        bestSellAmount = randomDouble(1.0, 10.0),
                        bestSellCurrency = ContractAddress(
                            blockchain = CurrencyConverter.convert(currency.blockchain),
                            value = currency.address,
                        ).fullId(),
                        bestSellMarketplace = PlatformDto.values().random().name,
                    )
                )
            }
            repeat(10) {
                val currency = nativeTestCurrencies().find { it.currencyId == "flow" }!!
                val oid = randomOwnershipId(blockchain = BlockchainDto.FLOW)
                ownerships.add(
                    EsOwnership(
                        ownershipId = oid.fullId(),
                        originalOwnershipId = null,
                        blockchain = BlockchainDto.FLOW,
                        itemId = oid.getItemId().fullId(),
                        collection = randomFlowCollectionDto().id,
                        owner = oid.owner.fullId(),
                        date = nowMillis() - Duration.ofDays(it.toLong()),
                        auctionId = randomString(),
                        auctionOwnershipId = randomString(),
                        bestSellAmount = randomDouble(1.0, 10.0),
                        bestSellCurrency = ContractAddress(
                            blockchain = CurrencyConverter.convert(currency.blockchain),
                            value = currency.address,
                        ).fullId(),
                        bestSellMarketplace = PlatformDto.values().random().name,
                    )
                )
            }

            repository.bulk(ownerships, emptyList())

            coEvery {
                flowService.getOwnershipsByIds(any())
            } answers {
                (arg(0) as List<String>).map { s ->
                    val eso = ownerships.find { it.ownershipId.contains(s) }!!
                    randomUnionOwnership().copy(
                        id = OwnershipIdDto(
                            blockchain = BlockchainDto.FLOW,
                            itemIdValue = IdParser.parseItemId(eso.itemId!!).value,
                            owner = IdParser.parseAddress(eso.owner)
                        )
                    )
                }
            }

            coEvery {
                ethereumService.getOwnershipsByIds(any())
            } answers {
                (arg(0) as List<String>).map { s ->
                    val eso = ownerships.find { it.ownershipId.contains(s) }!!
                    randomUnionOwnership().copy(
                        id = OwnershipIdDto(
                            blockchain = BlockchainDto.ETHEREUM,
                            itemIdValue = IdParser.parseItemId(eso.itemId!!).value,
                            owner = IdParser.parseAddress(eso.owner)
                        )
                    )
                }
            }

            coEvery {
                router.getService(BlockchainDto.ETHEREUM)
            } returns ethereumService

            coEvery {
                router.getService(BlockchainDto.FLOW)
            } returns flowService

            coEvery {
                router.isBlockchainEnabled(BlockchainDto.FLOW)
            } returns true
            coEvery {
                router.isBlockchainEnabled(BlockchainDto.ETHEREUM)
            } returns true

            coEvery {
                enrichmentAuctionService.findByItem(any())
            } returns emptyList()
            coEvery {
                enrichmentAuctionService.findBySeller(any())
            } returns emptyList()
            coEvery {
                enrichmentAuctionService.fetchAuctionsIfAbsent(any(), any())
            } returns emptyMap()
        }

        clearMocks(CurrencyMock.currencyControllerApiMock)
        every { CurrencyMock.currencyControllerApiMock.allCurrencies } returns CurrenciesDto(nativeTestCurrencies()).toMono()
    }

    @Test
    internal fun `search by request test`() {
        runBlocking {

            var expectedIds = takeRandomIds()
            check(
                expectedIds = expectedIds,
                filter = OwnershipSearchFilterDto(
                    items = ownerships.filter { it.ownershipId in expectedIds }
                        .map { IdParser.parseItemId(it.itemId!!) }
                ),
                failMessage = "Search by itemId failed"
            )

            expectedIds = takeRandomIds()
            check(
                expectedIds = expectedIds,
                filter = OwnershipSearchFilterDto(
                    collections = ownerships.filter { it.ownershipId in expectedIds }.mapNotNull { esOwnership ->
                        esOwnership.collection?.let { IdParser.parseCollectionId(it) }
                    }
                ),
                failMessage = "Search by collection failed"
            )

            expectedIds = takeRandomIds()
            check(
                expectedIds = expectedIds,
                filter = OwnershipSearchFilterDto(
                    owners = ownerships.filter { it.ownershipId in expectedIds }.map {
                        IdParser.parseAddress(it.owner)
                    }
                ),
                failMessage = "Search by owners failed"
            )

            expectedIds =
                ownerships.filter { it.blockchain == BlockchainDto.ETHEREUM }.map { it.ownershipId.lowercase() }
            check(
                expectedIds = expectedIds,
                filter = OwnershipSearchFilterDto(
                    blockchains = listOf(BlockchainDto.ETHEREUM)
                ),
                failMessage = "Search by blockchain failed"
            )

            expectedIds = takeRandomIds()
            check(
                expectedIds = expectedIds,
                filter = OwnershipSearchFilterDto(
                    auctions = ownerships.filter { it.ownershipId in expectedIds }.mapNotNull { esOwnership ->
                        esOwnership.auctionId?.let { IdParser.parseAuctionId(it) }
                    }
                ),
                failMessage = "Search by auctions failed"
            )

            expectedIds = takeRandomIds()
            check(
                expectedIds = expectedIds,
                filter = OwnershipSearchFilterDto(
                    auctionsOwners = ownerships.filter { it.ownershipId in expectedIds }.mapNotNull { esOwnership ->
                        esOwnership.auctionOwnershipId?.let { IdParser.parseAddress(it) }
                    }
                ),
                failMessage = "Search by auctionsOwners failed"
            )

            expectedIds = ownerships.filter { it.date < nowMillis() }.map { it.ownershipId.lowercase() }
            check(
                expectedIds = expectedIds,
                filter = OwnershipSearchFilterDto(
                    beforeDate = nowMillis()
                ),
                failMessage = "Search by beforeDate failed"
            )

            expectedIds = ownerships.filter { it.date > nowMillis() }.map { it.ownershipId.lowercase() }
            check(
                expectedIds = expectedIds,
                filter = OwnershipSearchFilterDto(
                    afterDate = nowMillis()
                ),
                failMessage = "Search by afterDate failed"
            )

            check(
                expectedIds = ownerships.map { it.ownershipId.lowercase() },
                filter = OwnershipSearchFilterDto(
                    beforeDate = ownerships.maxOf { it.date } + Duration.ofHours(1L),
                    afterDate = ownerships.minOf { it.date } - Duration.ofHours(1L)
                ),
                failMessage = "Search by range of dates failed"
            )

            expectedIds = takeRandomIds()
            check(
                expectedIds = expectedIds,
                filter = OwnershipSearchFilterDto(
                    owners = ownerships.filter { it.ownershipId in expectedIds }.map {
                        IdParser.parseAddress(it.owner)
                    },
                    items = ownerships.filter { it.ownershipId in expectedIds }.map {
                        IdParser.parseItemId(it.itemId!!)
                    }
                ),
                failMessage = "Search by owners and items failed!"
            )
        }
    }

    @Test
    @Disabled("Unstable test PT-2682")
    fun `should find by sell price`() = runBlocking<Unit> {
        // given
        val ratesPerCurrency = mockCurrencies()
        val expected: List<EsOwnership> =
            ownerships.sortedBy { it.bestSellAmount?.times(ratesPerCurrency[it.bestSellCurrency]!!) }
                .drop(ownerships.size / 4)
                .take(ownerships.size / 2)

        val priceFrom =
            when (expected.first().bestSellCurrency) {
                "ETHEREUM:0x0000000000000000000000000000000000000000" -> {
                    expected.first().bestSellAmount!!
                }

                "FLOW:A.1654653399040a61.FlowToken" -> {
                    expected.first().bestSellAmount!! * ratesPerCurrency["FLOW:A.1654653399040a61.FlowToken"]!! /
                            ratesPerCurrency["ETHEREUM:0x0000000000000000000000000000000000000000"]!!
                }

                else -> throw RuntimeException("Test must be amended")
            }

        val priceTo =
            when (expected.last().bestSellCurrency) {
                "ETHEREUM:0x0000000000000000000000000000000000000000" -> {
                    expected.last().bestSellAmount!!
                }

                "FLOW:A.1654653399040a61.FlowToken" -> {
                    expected.last().bestSellAmount!! * ratesPerCurrency["FLOW:A.1654653399040a61.FlowToken"]!! /
                            ratesPerCurrency["ETHEREUM:0x0000000000000000000000000000000000000000"]!!
                }

                else -> throw RuntimeException("Test must be amended")
            }

        // when && then
        check(
            expectedIds = expected.map { it.ownershipId.lowercase() },
            filter = OwnershipSearchFilterDto(
                sellPriceFrom = priceFrom,
                sellPriceTo = priceTo,
                sellCurrency = "ETHEREUM:0x0000000000000000000000000000000000000000"
            ),
            failMessage = "Failed to filter by sell price"
        )
    }

    @Test
    @Disabled("Unstable test PT-2682")
    fun `should find by usd sell price`() = runBlocking<Unit> {
        // given
        val ratesPerCurrency = mockCurrencies()
        val expected: List<EsOwnership> =
            ownerships.sortedBy { it.bestSellAmount?.times(ratesPerCurrency[it.bestSellCurrency]!!) }
                .drop(ownerships.size / 4)
                .take(ownerships.size / 2)

        val priceFrom = expected.first().bestSellAmount!! * ratesPerCurrency[expected.first().bestSellCurrency]!!
        val priceTo = expected.last().bestSellAmount!! * ratesPerCurrency[expected.last().bestSellCurrency]!!

        // when && then
        check(
            expectedIds = expected.map { it.ownershipId.lowercase() },
            filter = OwnershipSearchFilterDto(
                sellPriceFrom = priceFrom,
                sellPriceTo = priceTo,
                sellCurrency = null
            ),
            failMessage = "Failed to filter by usd sell price"
        )
    }

    @Test
    fun `should filter by marketplace`() = runBlocking<Unit> {
        // given
        val platforms = setOf(
            PlatformDto.RARIBLE, PlatformDto.OPEN_SEA,
            PlatformDto.LOOKSRARE, PlatformDto.CRYPTO_PUNKS, PlatformDto.IMMUTABLEX
        )
        val expected = ownerships.filter {
            platforms.contains(PlatformDto.valueOf(it.bestSellMarketplace!!))
        }

        // when && then
        check(
            expectedIds = expected.map { it.ownershipId.lowercase() },
            filter = OwnershipSearchFilterDto(
                sellPlatforms = platforms.toList()
            ),
            failMessage = "Search by best sell platforms failed"
        )
    }

    private fun takeRandomIds(): List<String> =
        ownerships.shuffled().take(Random.nextInt(3, 6)).map { it.ownershipId.lowercase() }

    private suspend fun check(expectedIds: List<String>, filter: OwnershipSearchFilterDto, failMessage: String) {
        WaitAssert.wait {
            val actual = service.search(OwnershipSearchRequestDto(filter = filter))
            assertThat(actual.ownerships.map { it.id.fullId().lowercase() })
                // .withFailMessage(failMessage)
                .containsAll(expectedIds)
        }
    }
}
