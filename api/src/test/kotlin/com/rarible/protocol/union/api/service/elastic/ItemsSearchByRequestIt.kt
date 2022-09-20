package com.rarible.protocol.union.api.service.elastic

import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomDouble
import com.rarible.core.test.data.randomString
import com.rarible.protocol.currency.dto.CurrenciesDto
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.converter.CurrencyConverter
import com.rarible.protocol.union.core.converter.EsItemConverter.toEsItem
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.test.nativeTestCurrencies
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemsSearchFilterDto
import com.rarible.protocol.union.dto.ItemsSearchRequestDto
import com.rarible.protocol.union.dto.ItemsSearchSortDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.TraitPropertyDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomItemDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.flow.data.randomFlowItemId
import com.rarible.protocol.union.test.mock.CurrencyMock.currencyControllerApiMock
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import randomOrder
import reactor.kotlin.core.publisher.toMono
import java.time.Duration
import kotlin.math.pow
import kotlin.random.Random

@IntegrationTest
class ItemsSearchByRequestIt {

    @Autowired
    private lateinit var repository: EsItemRepository

    @MockkBean
    private lateinit var router: BlockchainRouter<ItemService>

    @MockK
    private lateinit var ethereumService: ItemService

    @MockK
    private lateinit var flowService: ItemService


    @Autowired
    private lateinit var itemElasticService: ItemElasticService

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    private val esItems = mutableListOf<EsItem>()

    @BeforeEach
    internal fun setUp() {
        runBlocking {
            elasticsearchTestBootstrapper.bootstrap()

            repeat(10) {
                val currency = nativeTestCurrencies().find { it.currencyId == "ethereum" }!!
                val item = randomItemDto(randomEthItemId())
                    .copy(
                        mintedAt = nowMillis() + Duration.ofHours(it.toLong()),
                        lastUpdatedAt = nowMillis() + Duration.ofHours(it.toLong()),
                        meta = MetaDto(
                            name = randomString(),
                            attributes = listOf(
                                MetaAttributeDto(
                                    key = randomString(),
                                    value = randomString()
                                ),
                                MetaAttributeDto(
                                    key = randomString(),
                                    value = randomString()
                                )
                            ),
                            content = emptyList()
                        ),
                        bestSellOrder = randomOrder(
                            take = AssetDto(
                                EthErc20AssetTypeDto(
                                    contract = ContractAddress(
                                        blockchain = CurrencyConverter.convert(currency.blockchain),
                                        value = currency.address,
                                    )
                                ),
                                randomDouble(1.0, 10.0).toBigDecimal(),
                            )
                        ),
                        bestBidOrder = randomOrder(
                            make = AssetDto(
                                EthErc20AssetTypeDto(
                                    contract = ContractAddress(
                                        blockchain = CurrencyConverter.convert(currency.blockchain),
                                        value = currency.address,
                                    )
                                ),
                                randomDouble(1.0, 10.0).toBigDecimal(),
                            )
                        ),
                    )
                esItems.add(item.toEsItem())
            }
            repeat(10) {
                val currency = nativeTestCurrencies().find { it.currencyId == "flow" }!!
                val item = randomItemDto(randomFlowItemId())
                    .copy(
                        mintedAt = nowMillis() - Duration.ofHours(it.toLong()),
                        lastUpdatedAt = nowMillis() - Duration.ofHours(it.toLong()),
                        meta = MetaDto(
                            name = randomString(),
                            attributes = listOf(
                                MetaAttributeDto(
                                    key = randomString(),
                                    value = randomString()
                                ),
                                MetaAttributeDto(
                                    key = randomString(),
                                    value = randomString()
                                )
                            ),
                            content = emptyList()
                        ),
                        bestSellOrder = randomOrder(
                            take = AssetDto(
                                EthErc20AssetTypeDto(
                                    contract = ContractAddress(
                                        blockchain = CurrencyConverter.convert(currency.blockchain),
                                        value = currency.address,
                                    )
                                ),
                                randomDouble(1.0, 10.0).toBigDecimal(),
                            )
                        ),
                        bestBidOrder = randomOrder(
                            make = AssetDto(
                                EthErc20AssetTypeDto(
                                    contract = ContractAddress(
                                        blockchain = CurrencyConverter.convert(currency.blockchain),
                                        value = currency.address,
                                    )
                                ),
                                randomDouble(1.0, 10.0).toBigDecimal(),
                            )
                        ),
                    )
                esItems.add(item.toEsItem())
            }

            repository.saveAll(esItems)

            every {
                router.getService(BlockchainDto.ETHEREUM)
            } returns ethereumService

            every {
                router.getService(BlockchainDto.FLOW)
            } returns flowService

            every {
                router.isBlockchainEnabled(BlockchainDto.ETHEREUM)
            } returns true

            every {
                router.isBlockchainEnabled(BlockchainDto.FLOW)
            } returns true

            coEvery {
                ethereumService.getItemsByIds(any())
            } answers {
                (arg(0) as List<String>).map {
                    randomUnionItem(ItemIdDto(blockchain = BlockchainDto.ETHEREUM, value = it))
                }
            }

            coEvery {
                flowService.getItemsByIds(any())
            } answers {
                (arg(0) as List<String>).map {
                    randomUnionItem(ItemIdDto(blockchain = BlockchainDto.FLOW, value = it))
                }
            }

            clearMocks(currencyControllerApiMock)
            every { currencyControllerApiMock.allCurrencies } returns CurrenciesDto(nativeTestCurrencies()).toMono()
        }
    }


    @Test
    internal fun `search items by request`() {
        runBlocking {

            checkResult(
                filter = ItemsSearchFilterDto(
                    blockchains = listOf(BlockchainDto.FLOW, BlockchainDto.ETHEREUM)
                ),
                expected = esItems,
                failMessage = "Search by blockchains failed!"
            )

            var expected = takeRandomItems()
            checkResult(
                filter = ItemsSearchFilterDto(
                    collections = expected.map { IdParser.parseCollectionId(it.collection!!) }
                ),
                expected = expected,
                failMessage = "Search by collections failed!"
            )

            expected = takeRandomItems()
            checkResult(
                filter = ItemsSearchFilterDto(
                    names = expected.mapNotNull { it.name },
                ),
                expected = expected,
                failMessage = "Search by names failed!"
            )

            expected = takeRandomItems()
            checkResult(
                filter = ItemsSearchFilterDto(
                    descriptions = expected.mapNotNull { it.description }
                ),
                expected = expected,
                failMessage = "Search by descriptions failed!"
            )

            expected = takeRandomItems()
            checkResult(
                filter = ItemsSearchFilterDto(
                    creators = expected.flatMap { it.creators }.map { IdParser.parseAddress(it) }
                ),
                expected = expected,
                failMessage = "Search by creators failed!"
            )

            expected = takeRandomItems()
            checkResult(
                filter = ItemsSearchFilterDto(
                    traits = expected.flatMap { esItem ->
                        esItem.traits.map {
                            TraitPropertyDto(
                                key = it.key,
                                value = it.value.orEmpty()
                            )
                        }
                    },
                ),
                expected = expected,
                failMessage = "Search by traits failed"
            )
            expected = esItems.filter { it.mintedAt < nowMillis() }
            checkResult(
                filter = ItemsSearchFilterDto(
                    mintedAtTo = nowMillis()
                ),
                expected = expected,
                failMessage = "Search by mintedAtTo failed!"
            )
            checkResult(
                filter = ItemsSearchFilterDto(
                    mintedAtTo = nowMillis(),
                    mintedAtFrom = expected.minOf { it.mintedAt }
                ),
                expected = expected,
                failMessage = "Search by mintedAt from/to range is failed!"
            )

            expected = esItems.filter { it.lastUpdatedAt > nowMillis() }
            checkResult(
                filter = ItemsSearchFilterDto(
                    lastUpdatedAtFrom = nowMillis()
                ),
                expected = expected,
                failMessage = "Search by lastUpdatedAtFrom failed!"
            )
            checkResult(
                filter = ItemsSearchFilterDto(
                    lastUpdatedAtFrom = nowMillis(),
                    lastUpdatedAtTo = expected.maxOf { it.lastUpdatedAt }
                ),
                expected = expected,
                failMessage = "Search by lastUpdatedAt from/to range is failed!"
            )

            expected = esItems.filter {
                setOf(PlatformDto.LOOKSRARE.name, PlatformDto.CRYPTO_PUNKS.name, PlatformDto.IMMUTABLEX.name)
                    .contains(it.bestSellMarketplace)
            }
            checkResult(
                filter = ItemsSearchFilterDto(
                    sellPlatforms = listOf(PlatformDto.LOOKSRARE, PlatformDto.CRYPTO_PUNKS, PlatformDto.IMMUTABLEX),
                ),
                expected = expected,
                failMessage = "Search by best sell platforms failed!"
            )

            expected = esItems.filter {
                setOf(PlatformDto.RARIBLE.name, PlatformDto.OPEN_SEA.name, PlatformDto.X2Y2.name)
                    .contains(it.bestBidMarketplace)
            }
            checkResult(
                filter = ItemsSearchFilterDto(
                    bidPlatforms = listOf(PlatformDto.RARIBLE, PlatformDto.OPEN_SEA, PlatformDto.X2Y2),
                ),
                expected = expected,
                failMessage = "Search by best bid platforms failed!"
            )


            expected = takeRandomItems()
            checkResult(
                filter = ItemsSearchFilterDto(
                    names = expected.mapNotNull { it.name },
                    creators = expected.flatMap { esItem -> esItem.creators.map { IdParser.parseAddress(it) } }
                ),
                expected = expected,
                failMessage = "Search by name and creators failed!"
            )
        }
    }

    @Test
    fun `should find by sell price`() = runBlocking<Unit> {
        // given
        val ratesPerCurrency = mockCurrencies()

        val expected: List<EsItem> =
            esItems.sortedBy { it.bestSellAmount?.times(ratesPerCurrency[it.bestSellCurrency]!!) }
                .drop(esItems.size / 4)
                .take(esItems.size / 2)

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
        checkResult(
            filter = ItemsSearchFilterDto(
                sellPriceFrom = priceFrom,
                sellPriceTo = priceTo,
                sellCurrency = "ETHEREUM:0x0000000000000000000000000000000000000000"
            ),
            expected = expected,
            failMessage = "Failed to filter by sell price"
        )
    }

    @Test
    fun `should find by bid price`() = runBlocking<Unit> {
        // given
        val ratesPerCurrency = mockCurrencies()

        val expected: List<EsItem> =
            esItems.sortedBy { it.bestBidAmount?.times(ratesPerCurrency[it.bestBidCurrency]!!) }
                .drop(esItems.size / 4)
                .take(esItems.size / 2)

        val priceFrom =
            when (expected.first().bestBidCurrency) {
                "ETHEREUM:0x0000000000000000000000000000000000000000" -> {
                    expected.first().bestBidAmount!!
                }
                "FLOW:A.1654653399040a61.FlowToken" -> {
                    expected.first().bestBidAmount!! * ratesPerCurrency["FLOW:A.1654653399040a61.FlowToken"]!! /
                            ratesPerCurrency["ETHEREUM:0x0000000000000000000000000000000000000000"]!!
                }
                else -> throw RuntimeException("Test must be amended")
            }

        val priceTo =
            when (expected.last().bestBidCurrency) {
                "ETHEREUM:0x0000000000000000000000000000000000000000" -> {
                    expected.last().bestBidAmount!!
                }
                "FLOW:A.1654653399040a61.FlowToken" -> {
                    expected.last().bestBidAmount!! * ratesPerCurrency["FLOW:A.1654653399040a61.FlowToken"]!! /
                            ratesPerCurrency["ETHEREUM:0x0000000000000000000000000000000000000000"]!!
                }
                else -> throw RuntimeException("Test must be amended")
            }

        // when && then
        checkResult(
            filter = ItemsSearchFilterDto(
                bidPriceFrom = priceFrom,
                bidPriceTo = priceTo,
                bidCurrency = "ETHEREUM:0x0000000000000000000000000000000000000000"
            ),
            expected = expected,
            failMessage = "Failed to filter by bid price"
        )
    }

    @Test
    fun `should find by usd sell price`() = runBlocking<Unit> {
        // given
        val ratesPerCurrency = mockCurrencies()
        val expected: List<EsItem> =
            esItems.sortedBy { it.bestSellAmount?.times(ratesPerCurrency[it.bestSellCurrency]!!) }
                .drop(esItems.size / 4)
                .take(esItems.size / 2)

        val priceFrom = expected.first().bestSellAmount!! * ratesPerCurrency[expected.first().bestSellCurrency]!!
        val priceTo = expected.last().bestSellAmount!! * ratesPerCurrency[expected.last().bestSellCurrency]!!

        // when && then
        checkResult(
            filter = ItemsSearchFilterDto(
                sellPriceFrom = priceFrom,
                sellPriceTo = priceTo,
                sellCurrency = null
            ),
            expected = expected,
            failMessage = "Failed to filter by sell price in usd"
        )
    }

    @Test
    fun `should find by usd bid price`() = runBlocking<Unit> {
        // given
        val ratesPerCurrency = mockCurrencies()
        val expected: List<EsItem> =
            esItems.sortedBy { it.bestBidAmount?.times(ratesPerCurrency[it.bestBidCurrency]!!) }
                .drop(esItems.size / 4)
                .take(esItems.size / 2)

        val priceFrom = expected.first().bestBidAmount!! * ratesPerCurrency[expected.first().bestBidCurrency]!!
        val priceTo = expected.last().bestBidAmount!! * ratesPerCurrency[expected.last().bestBidCurrency]!!

        // when && then
        checkResult(
            filter = ItemsSearchFilterDto(
                bidPriceFrom = priceFrom,
                bidPriceTo = priceTo,
                bidCurrency = null
            ),
            expected = expected,
            failMessage = "Failed to filter by bid price in usd"
        )
    }

    @Test
    fun `should find by usd sell price, with cursor`() = runBlocking<Unit> {
        // given
        val ratesPerCurrency = mockCurrencies()
        val expected: List<EsItem> =
            esItems.sortedBy { it.bestSellAmount?.times(ratesPerCurrency[it.bestSellCurrency]!!) }
                .drop(esItems.size / 4)
                .take(esItems.size / 2)

        val priceFrom = expected.first().bestSellAmount!! * ratesPerCurrency[expected.first().bestSellCurrency]!!
        val priceTo = expected.last().bestSellAmount!! * ratesPerCurrency[expected.last().bestSellCurrency]!!
        var request = ItemsSearchRequestDto(
            size = 2,
            continuation = null,
            filter = ItemsSearchFilterDto(
                sellPriceFrom = priceFrom,
                sellPriceTo = priceTo,
                sellCurrency = null,
                ),
            sort = ItemsSearchSortDto.LOWEST_SELL
        )
        // when
        val actual = mutableListOf<ItemDto>()
        do {
            val result = itemElasticService.searchItems(request)
            actual.addAll(result.items)
            request = request.copy(continuation = result.continuation)
        } while (!result.continuation.isNullOrEmpty())

        // then
        assertThat(actual.map { it.id.fullId().lowercase() })
            .isEqualTo(expected.map { it.itemId.lowercase() })
    }

    private fun takeRandomItems(): List<EsItem> {
        return esItems.shuffled().take(Random.nextInt(10, 40))
    }

    private suspend fun checkResult(filter: ItemsSearchFilterDto, expected: List<EsItem>, failMessage: String) {
        assertThat(expected).withFailMessage(failMessage).isNotEmpty
        val items = itemElasticService.searchItems(
            ItemsSearchRequestDto(
                filter = filter
            )
        )
        assertThat(items.items).withFailMessage(failMessage).isNotEmpty
        assertThat(items.items.map { it.id.fullId().lowercase() }).withFailMessage(failMessage)
            .containsAll(expected.map { it.itemId.lowercase() })
    }

    private suspend fun mockCurrencies(): Map<String, Double> {
        val ratesPerCurrency = mutableMapOf<String, Double>()

        nativeTestCurrencies().forEachIndexed { index, currency ->
            val rate = 1.0 + 2.0.pow(index.toDouble())
            ratesPerCurrency["${currency.blockchain}:${currency.address}"] = rate
            every { currencyControllerApiMock.getCurrencyRate(currency.blockchain, currency.address, any()) } returns
                    CurrencyRateDto(
                        fromCurrencyId = currency.currencyId,
                        toCurrencyId = "",
                        rate = rate.toBigDecimal(),
                        date = nowMillis(),
                    ).toMono()
        }

        return  ratesPerCurrency
    }
}
