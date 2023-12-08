package com.rarible.protocol.union.api.service.elastic

import com.apollographql.apollo3.mpp.platform
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.protocol.currency.dto.CurrenciesDto
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.api.service.api.ItemEnrichService
import com.rarible.protocol.union.core.converter.CurrencyConverter
import com.rarible.protocol.union.core.converter.EsItemConverter.toEsItem
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.elastic.EsItem
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.test.nativeTestCurrencies
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemSearchFullTextDto
import com.rarible.protocol.union.dto.ItemsSearchFilterDto
import com.rarible.protocol.union.dto.ItemsSearchRequestDto
import com.rarible.protocol.union.dto.ItemsSearchSortDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.TraitPropertyDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.enrichment.test.data.randomItemDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.flow.data.randomFlowItemId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import randomOrderDto
import reactor.kotlin.core.publisher.toMono
import java.time.Duration
import kotlin.random.Random

@IntegrationTest
class ItemsSearchByRequestIt : AbstractIntegrationTest() {

    private val ethereumService: ItemService = mockk {
        every { blockchain } returns BlockchainDto.ETHEREUM
        coEvery { getItemCollectionId(any()) } answers { it.invocation.args[0].toString().substringBefore(":") }
    }
    private val flowService: ItemService = mockk {
        every { blockchain } returns BlockchainDto.FLOW
        coEvery { getItemCollectionId(any()) } answers { it.invocation.args[0].toString().substringBefore(":") }
    }
    private val router = BlockchainRouter(
        listOf(ethereumService, flowService),
        listOf(ethereumService.blockchain, flowService.blockchain)
    )

    @Autowired
    lateinit var itemFilterConverter: ItemFilterConverter

    @Autowired
    lateinit var esItemOptimizedSearchService: EsItemOptimizedSearchService

    @Autowired
    lateinit var esOwnershipRepository: EsOwnershipRepository

    @Autowired
    lateinit var ownershipElasticHelper: OwnershipElasticHelper

    @Autowired
    lateinit var itemEnrichService: ItemEnrichService

    lateinit var itemElasticService: ItemElasticService

    @Autowired
    private lateinit var repository: EsItemRepository

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    private val esItems = mutableListOf<EsItem>()
    private val ratesPerCurrency = nativeTestCurrencies().filter { it.rate != null }.associateBy(
        { "${it.blockchain}:${it.address}" }, { it.rate!!.toDouble() }
    )
    private val flowCurrency = "FLOW:A.1654653399040a61.FlowToken"
    private val ethCurrency = "ETHEREUM:0x0000000000000000000000000000000000000000"
    private val flowEthRatio = ratesPerCurrency[flowCurrency]!! / ratesPerCurrency[ethCurrency]!!

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        every { testCurrencyApi.allCurrencies } returns CurrenciesDto(nativeTestCurrencies()).toMono()
        itemElasticService = ItemElasticService(
            itemFilterConverter,
            esItemOptimizedSearchService,
            esOwnershipRepository,
            ownershipElasticHelper,
            router,
            itemEnrichService
        )

        elasticsearchTestBootstrapper.bootstrap()

        repeat(10) {
            val currency = nativeTestCurrencies().find { it.currencyId == BlockchainDto.ETHEREUM.name.lowercase() }!!
            val item = randomItemDto(randomEthItemId()).copy(
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
                bestSellOrder = randomOrderDto(
                    take = AssetDto(
                        EthErc20AssetTypeDto(
                            contract = ContractAddress(
                                blockchain = CurrencyConverter.convert(currency.blockchain),
                                value = currency.address,
                            )
                        ),
                        (it + 1).toBigDecimal(),
                    )
                ),
                bestBidOrder = randomOrderDto(
                    platform = PlatformDto.RARIBLE,
                    make = AssetDto(
                        EthErc20AssetTypeDto(
                            contract = ContractAddress(
                                blockchain = CurrencyConverter.convert(currency.blockchain),
                                value = currency.address,
                            )
                        ),
                        (it + 1).toBigDecimal(),
                    )
                ),
            )
            esItems.add(item.toEsItem())
        }
        repeat(10) {
            val currency = nativeTestCurrencies().find { it.currencyId == "flow" }!!
            val item = randomItemDto(randomFlowItemId()).copy(
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
                bestSellOrder = randomOrderDto(
                    take = AssetDto(
                        EthErc20AssetTypeDto(
                            contract = ContractAddress(
                                blockchain = CurrencyConverter.convert(currency.blockchain),
                                value = currency.address,
                            )
                        ),
                        (it + 1).toBigDecimal(),
                    )
                ),
                bestBidOrder = randomOrderDto(
                    make = AssetDto(
                        EthErc20AssetTypeDto(
                            contract = ContractAddress(
                                blockchain = CurrencyConverter.convert(currency.blockchain),
                                value = currency.address,
                            )
                        ),
                        (it + 1).toBigDecimal(),
                    )
                ),
            )
            esItems.add(item.toEsItem())
        }

        repository.bulk(entitiesToSave = esItems)

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
    }

    @Test
    internal fun `search items by request`() = runBlocking<Unit> {
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
                creators = expected.flatMap { it.creators }.map { IdParser.parseAddress(it) }
            ),
            expected = expected,
            failMessage = "Search by creators failed!"
        )

        expected = takeRandomItems().subList(0, 1)
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

    @Test
    fun `should find by sell price`() = runBlocking<Unit> {
        val expected: List<EsItem> =
            esItems.sortedBy { it.bestSellAmount?.times(ratesPerCurrency[it.bestSellCurrency]!!) }
                .drop(esItems.size / 4)
                .take(esItems.size / 2)

        val priceFrom = expected.first().let { it.bestSellAmount!! * getRatio(it.bestSellCurrency) }
        val priceTo = expected.last().let { it.bestSellAmount!! * getRatio(it.bestSellCurrency) }

        checkResult(
            filter = ItemsSearchFilterDto(
                sellPriceFrom = priceFrom * 0.999,
                sellPriceTo = priceTo * 1.001,
                sellCurrency = ethCurrency
            ),
            expected = expected,
            failMessage = "Failed to filter by sell price"
        )
    }

    @Test
    fun `should find by bid price`() = runBlocking<Unit> {
        val expected: List<EsItem> =
            esItems.sortedBy { it.bestBidAmount?.times(ratesPerCurrency[it.bestBidCurrency]!!) }
                .drop(esItems.size / 4)
                .take(esItems.size / 2)

        val priceFrom = expected.first().let { it.bestBidAmount!! * getRatio(it.bestBidCurrency) }
        val priceTo = expected.last().let { it.bestBidAmount!! * getRatio(it.bestBidCurrency) }

        // when && then
        checkResult(
            filter = ItemsSearchFilterDto(
                bidPriceFrom = priceFrom * 0.999,
                bidPriceTo = priceTo * 1.001,
                bidCurrency = "ETHEREUM:0x0000000000000000000000000000000000000000"
            ),
            expected = expected,
            failMessage = "Failed to filter by bid price"
        )
    }

    @Test
    fun `should find by usd sell price`() = runBlocking<Unit> {
        val expected: List<EsItem> =
            esItems.sortedBy { it.bestSellAmount?.times(ratesPerCurrency[it.bestSellCurrency]!!) }
                .drop(esItems.size / 4)
                .take(esItems.size / 2)

        val priceFrom = expected.first().bestSellAmount!! * ratesPerCurrency[expected.first().bestSellCurrency]!!
        val priceTo = expected.last().bestSellAmount!! * ratesPerCurrency[expected.last().bestSellCurrency]!!

        // when && then
        checkResult(
            filter = ItemsSearchFilterDto(
                sellPriceFrom = priceFrom * 0.999,
                sellPriceTo = priceTo * 1.001,
                sellCurrency = null
            ),
            expected = expected,
            failMessage = "Failed to filter by sell price in usd"
        )
    }

    @Test
    fun `should find by usd bid price`() = runBlocking<Unit> {
        val expected: List<EsItem> =
            esItems.sortedBy { it.bestBidAmount?.times(ratesPerCurrency[it.bestBidCurrency]!!) }
                .drop(esItems.size / 4)
                .take(esItems.size / 2)

        val priceFrom = expected.first().bestBidAmount!! * ratesPerCurrency[expected.first().bestBidCurrency]!!
        val priceTo = expected.last().bestBidAmount!! * ratesPerCurrency[expected.last().bestBidCurrency]!!

        // when && then
        checkResult(
            filter = ItemsSearchFilterDto(
                bidPriceFrom = priceFrom * 0.999,
                bidPriceTo = priceTo * 1.001,
                bidCurrency = null
            ),
            expected = expected,
            failMessage = "Failed to filter by bid price in usd"
        )
    }

    @Test
    fun `should find by usd sell price, with cursor`() = runBlocking<Unit> {
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
                sellPriceFrom = priceFrom * 0.999,
                sellPriceTo = priceTo * 1.001,
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

    @Test
    fun `search items by names`() = runBlocking<Unit> {
        val expected = takeRandomItems()
        val items = itemElasticService.searchItems(
            ItemsSearchRequestDto(
                filter = ItemsSearchFilterDto(
                    names = expected.mapNotNull { it.name },
                )
            )
        )
        assertThat(items.items.map { it.id.fullId().lowercase() })
            .containsExactlyInAnyOrderElementsOf(expected.map { it.itemId.lowercase() })
    }

    @Test
    fun `search items by special character name`() = runBlocking<Unit> {
        val namePart1 = "~" + randomString(3)
        val namePart2 = randomString(3) + "!"
        val item = randomItemDto(randomEthItemId()).copy(
            meta = MetaDto(
                name = "$namePart1 $namePart2",
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
            )
        ).toEsItem()
        repository.bulk(entitiesToSave = listOf(item))
        val items1 = itemElasticService.searchItems(
            ItemsSearchRequestDto(
                filter = ItemsSearchFilterDto(
                    names = listOf(namePart1),
                )
            )
        )
        val items2 = itemElasticService.searchItems(
            ItemsSearchRequestDto(
                filter = ItemsSearchFilterDto(
                    names = listOf(namePart2),
                )
            )
        )
        val items3 = itemElasticService.searchItems(
            ItemsSearchRequestDto(
                filter = ItemsSearchFilterDto(
                    names = listOf(item.name!!),
                )
            )
        )
        assertThat(items1.items.map { it.id.fullId().lowercase() })
            .containsExactlyElementsOf(items2.items.map { it.id.fullId().lowercase() })
            .containsExactlyElementsOf(items3.items.map { it.id.fullId().lowercase() })
            .containsExactly(item.itemId.lowercase())
    }

    @Test
    fun `search items - on sale`() = runBlocking<Unit> {
        val item = randomItemDto(randomEthItemId()).toEsItem()
        repository.bulk(entitiesToSave = listOf(item))

        val onSale = itemElasticService.searchItems(
            ItemsSearchRequestDto(
                size = 100,
                filter = ItemsSearchFilterDto(
                    onSale = true
                )
            )
        ).items.map { it.id.fullId() }

        val notOnSale = itemElasticService.searchItems(
            ItemsSearchRequestDto(
                filter = ItemsSearchFilterDto(
                    onSale = false
                )
            )
        ).items.map { it.id.fullId() }

        assertThat(onSale).containsExactlyInAnyOrderElementsOf(esItems.map { it.itemId })
        assertThat(notOnSale).isEqualTo(listOf(item.itemId))
    }

    @Test
    fun `search items by names and full text`() = runBlocking<Unit> {
        val expected = takeRandomItems()[0]
        val items = itemElasticService.searchItems(
            ItemsSearchRequestDto(
                filter = ItemsSearchFilterDto(
                    names = listOf(expected.name!!),
                    fullText = ItemSearchFullTextDto(
                        text = expected.name!!,
                        fields = listOf(ItemSearchFullTextDto.Fields.NAME)
                    )
                )
            )
        )
        assertThat(items.items.map { it.id.fullId().lowercase() })
            .containsExactly(expected.itemId.lowercase())
    }

    @Test
    fun `search items full text phrase boost`() = runBlocking<Unit> {
        val phraseItem = randomItemDto(ItemIdDto(BlockchainDto.ETHEREUM, "${randomAddress()}:1")).copy(
            meta = MetaDto(name = "phrase", attributes = emptyList(), content = emptyList())
        ).toEsItem()
        val precisePhraseItem = randomItemDto(ItemIdDto(BlockchainDto.ETHEREUM, "${randomAddress()}:2")).copy(
            meta = MetaDto(name = "precise phrase", attributes = emptyList(), content = emptyList())
        ).toEsItem()
        val extendedPhraseItem = randomItemDto(ItemIdDto(BlockchainDto.ETHEREUM, "${randomAddress()}:3")).copy(
            meta = MetaDto(name = "precise phrase extended", attributes = emptyList(), content = emptyList())
        ).toEsItem()
        repository.bulk(entitiesToSave = listOf(phraseItem, precisePhraseItem, extendedPhraseItem))
        val precisePhraseSearchItems = itemElasticService.searchItems(
            ItemsSearchRequestDto(
                filter = ItemsSearchFilterDto(
                    fullText = ItemSearchFullTextDto(
                        text = "phrase",
                        fields = listOf(ItemSearchFullTextDto.Fields.NAME)
                    ),
                ),
                sort = ItemsSearchSortDto.RELEVANCE
            )
        )
        assertThat(precisePhraseSearchItems.items.map { it.id.fullId().lowercase() })
            // TODO should be containsExactly, can't align order
            .containsExactlyInAnyOrder(
                phraseItem.itemId.lowercase(),
                precisePhraseItem.itemId.lowercase(),
                extendedPhraseItem.itemId.lowercase()
            )
        val phraseSearchItems = itemElasticService.searchItems(
            ItemsSearchRequestDto(
                filter = ItemsSearchFilterDto(
                    fullText = ItemSearchFullTextDto(
                        text = "precise phrase",
                        fields = listOf(ItemSearchFullTextDto.Fields.NAME)
                    )
                ),
                sort = ItemsSearchSortDto.RELEVANCE
            )
        )
        assertThat(phraseSearchItems.items.map { it.id.fullId().lowercase() })
            // TODO should be containsExactly, can't align order
            .containsExactlyInAnyOrder(
                precisePhraseItem.itemId.lowercase(),
                extendedPhraseItem.itemId.lowercase()
            )
    }

    @Test
    fun `search items by tokenId`() = runBlocking<Unit> {
        val expected = takeRandomItems()[0]
        val items = itemElasticService.searchItems(
            ItemsSearchRequestDto(
                filter = ItemsSearchFilterDto(
                    fullText = ItemSearchFullTextDto(
                        text = expected.tokenId!!,
                        fields = listOf(ItemSearchFullTextDto.Fields.NAME)
                    )
                ),
            )
        )
        assertThat(items.items.map { it.id.fullId().lowercase() })
            .containsExactly(expected.itemId.lowercase())
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

    private fun getRatio(currency: String?): Double {
        return when (currency) {
            flowCurrency -> flowEthRatio
            else -> 1.0
        }
    }
}
