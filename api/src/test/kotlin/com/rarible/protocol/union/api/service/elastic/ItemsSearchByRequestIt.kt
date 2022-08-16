package com.rarible.protocol.union.api.service.elastic

import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.converter.EsItemConverter.toEsItem
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemsSearchFilterDto
import com.rarible.protocol.union.dto.ItemsSearchRequestDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.TraitPropertyDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomItemDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthSellOrderDto
import com.rarible.protocol.union.integration.flow.data.randomFlowItemId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import randomOrder
import java.time.Duration
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
                        bestSellOrder = randomOrder(),
                        bestBidOrder = randomOrder(),
                    )
                esItems.add(item.toEsItem())
            }
            repeat(10) {
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
                        bestSellOrder = randomOrder(),
                        bestBidOrder = randomOrder(),
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
}
