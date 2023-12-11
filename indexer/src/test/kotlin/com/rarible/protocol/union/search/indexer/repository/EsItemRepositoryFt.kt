package com.rarible.protocol.union.search.indexer.repository

import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.elastic.EsItem
import com.rarible.protocol.union.core.model.elastic.EsItemGenericFilter
import com.rarible.protocol.union.core.model.elastic.EsItemSort
import com.rarible.protocol.union.core.model.elastic.EsItemTrait
import com.rarible.protocol.union.core.model.elastic.Range
import com.rarible.protocol.union.core.model.elastic.TraitFilter
import com.rarible.protocol.union.core.model.elastic.TraitRangeFilter
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.core.test.WaitAssert
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.index.query.BoolQueryBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.test.context.ContextConfiguration
import java.time.temporal.ChronoUnit

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class EsItemRepositoryFt {

    @MockkBean
    private lateinit var currencyService: CurrencyService

    @Autowired
    protected lateinit var repository: EsItemRepository

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        elasticsearchTestBootstrapper.bootstrap()
        coEvery { currencyService.getAllCurrencyRates() } returns emptyList()
    }

    @Test
    fun `should save and read`(): Unit = runBlocking {

        val now = nowMillis().truncatedTo(ChronoUnit.SECONDS)
        val esItem = EsItem(
            id = "0xFF",
            itemId = "0x03",
            token = "0x02",
            tokenId = "0x03",
            blockchain = BlockchainDto.ETHEREUM,
            collection = "0x02",
            name = "TestItem",
            description = "description",
            traits = listOf(EsItemTrait("long", "10"), EsItemTrait("test", "eye")),
            creators = listOf("0x01"),
            mintedAt = now,
            lastUpdatedAt = now
        )

        val id = repository.save(esItem).id
        val found = repository.findById(id)
        assertThat(found).isEqualTo(esItem)
    }

    @Test
    fun `shouldn't fail to save 0 items`() = runBlocking<Unit> {
        repository.saveAll(emptyList())
    }

    @Test
    fun `should be able to search up to 1000 items`(): Unit = runBlocking {
        // given
        val items = List(1000) { randomEsItem() }
        repository.saveAll(items)

        // when
        val query = NativeSearchQuery(BoolQueryBuilder())
        query.maxResults = 1000
        val actual = repository.search(query)

        // then
        assertThat(actual).hasSize(1000)
    }

    @Test
    fun `should search by collection`(): Unit = runBlocking {

        val esItem = randomEsItem()
        repository.save(esItem)
        val result =
            repository.search(EsItemGenericFilter(collections = setOf(esItem.collection!!)), EsItemSort.DEFAULT, 10)
                .entities

        assertThat(result.size).isEqualTo(1)

        assertThat(result[0].itemId).isEqualTo(esItem.itemId)
    }

    @Test
    fun `should search by owners`(): Unit = runBlocking {

        val esItem = randomEsItem()
        repository.save(esItem)
        val result = repository.search(
            EsItemGenericFilter(
//                    owners = setOf(esItem.owner!!)
            ), EsItemSort.DEFAULT, 10
        ).entities

        assertThat(result.size).isEqualTo(1)

        assertThat(result[0].itemId).isEqualTo(esItem.itemId)
    }

    @Test
    fun `should search by creators`(): Unit = runBlocking {

        val esItem = randomEsItem()
        repository.save(esItem)
        val result =
            repository.search(EsItemGenericFilter(creators = setOf(esItem.creators.first())), EsItemSort.DEFAULT, 10)
                .entities

        assertThat(result.size).isEqualTo(1)

        assertThat(result[0].itemId).isEqualTo(esItem.itemId)
    }

    @Test
    fun `should search by blockchains`(): Unit = runBlocking {

        val esItem = randomEsItem()
        repository.saveAll(listOf(esItem))
        val result = repository.search(
            EsItemGenericFilter(blockchains = setOf(esItem.blockchain.toString())), EsItemSort.DEFAULT, 10
        ).entities

        assertThat(result.size).isEqualTo(1)

        assertThat(result[0].itemId).isEqualTo(esItem.itemId)
    }

    @Test
    fun `should search by traits`(): Unit = runBlocking {
        val key = randomString()
        val key2 = randomString()
        val value = randomString()
        val value2 = randomString()
        val esItem1 = randomEsItem().copy(
            traits = listOf(
                EsItemTrait(key, value),
                EsItemTrait(key2, value2),
                EsItemTrait(randomString(), randomString()),
            ),
        )
        val esItem2 = randomEsItem().copy(
            traits = listOf(
                EsItemTrait(key, randomString()),
                EsItemTrait(key2, value2),
                EsItemTrait(randomString(), randomString()),
            ),
        )
        val esItem3 = randomEsItem().copy(
            traits = listOf(
                EsItemTrait(key, value),
                EsItemTrait(randomString(), value2),
                EsItemTrait(randomString(), randomString()),
            ),
        )
        repository.saveAll(listOf(esItem1, esItem2, esItem3))
        val result = repository.search(
            filter = EsItemGenericFilter(traits = listOf(TraitFilter(key, value), TraitFilter(key2, value2))),
            sort = EsItemSort.DEFAULT,
            limit = 10
        ).entities

        assertThat(result.size).isEqualTo(1)

        assertThat(result[0].itemId).isEqualTo(esItem1.itemId)
    }

    @Test
    fun `should search by trait ranges`(): Unit = runBlocking {
        val key = randomString()
        val key2 = randomString()
        val esItem1 = randomEsItem().copy(
            traits = listOf(
                EsItemTrait(key, "1"),
                EsItemTrait(key2, "2"),
            ),
        )
        val esItem2 = randomEsItem().copy(
            traits = listOf(
                EsItemTrait(key, "2"),
                EsItemTrait(key2, "3"),
            ),
        )
        val esItem3 = randomEsItem().copy(
            traits = listOf(
                EsItemTrait(key, "3"),
                EsItemTrait(key2, "4"),
            ),
        )
        repository.saveAll(listOf(esItem1, esItem2, esItem3))
        val result = repository.search(
            filter = EsItemGenericFilter(
                traitRanges = listOf(
                    TraitRangeFilter(
                        key = key,
                        valueRange = Range(from = 0.5f, to = 2.5f),
                    ),
                )
            ),
            sort = EsItemSort.DEFAULT,
            limit = 10
        ).entities

        assertThat(result.map { it.itemId }).containsExactlyInAnyOrder(esItem1.itemId, esItem2.itemId)
    }

    @Test
    fun `should search by itemIds`(): Unit = runBlocking {

        val esItem = randomEsItem()
        repository.save(esItem)
        val result = repository.search(
            EsItemGenericFilter(itemIds = setOf(esItem.itemId)), EsItemSort.DEFAULT, 10
        ).entities

        assertThat(result.size).isEqualTo(1)

        assertThat(result[0].itemId).isEqualTo(esItem.itemId)
    }

    @Test
    fun `should search by mintedAt range`(): Unit = runBlocking {

        val now = nowMillis()

        (1..100).forEach {
            val esItem = randomEsItem().copy(mintedAt = now.plusSeconds(it.toLong()))
            repository.save(esItem)
        }

        val result = repository.search(
            EsItemGenericFilter(mintedFrom = now), EsItemSort.DEFAULT, 200
        ).entities

        assertThat(result.size).isEqualTo(100)

        val result1 = repository.search(
            EsItemGenericFilter(mintedFrom = now, mintedTo = now.plusSeconds(50)), EsItemSort.DEFAULT, 200
        ).entities

        assertThat(result1.size).isEqualTo(50)

        val result2 = repository.search(
            EsItemGenericFilter(mintedFrom = now.plusSeconds(21), mintedTo = now.plusSeconds(50)),
            EsItemSort.DEFAULT,
            200
        ).entities

        assertThat(result2.size).isEqualTo(30)
    }

    @Test
    @Disabled("Text search temporary disabled. Will be fixed in PT-4066")
    fun `should search by name`(): Unit = runBlocking {

        val esItems = (1..50).map { randomEsItem() }
        repository.saveAll(esItems)

        val result = repository.search(
            EsItemGenericFilter(text = esItems[13].name), EsItemSort.DEFAULT, 10
        ).entities

        assertThat(result.size).isEqualTo(1)
        assertThat(result[0].itemId).isEqualTo(esItems[13].itemId)
    }

    @TestFactory
    fun `should search by prefix name key`() = runBlocking<Unit> {
        // Let's make strings a bit longer to avoid collisions with default traits
        val key1 = randomString(12)
        val key2 = randomString(12)
        val key3 = randomString(12)

        val esItem = randomEsItem().copy(name = "$key1 $key2 $key3")
        val esItems = (1..50).map { randomEsItem() } + esItem
        repository.bulk(esItems + esItem)

        assertItemFoundByText(esItem, key1)
        assertItemFoundByText(esItem, key2)
        assertItemFoundByText(esItem, key3)
        assertItemFoundByText(esItem, key1.uppercase())
        assertItemFoundByText(esItem, key2.lowercase())
        assertItemFoundByText(esItem, key3.uppercase())
    }

    private suspend fun assertItemFoundByText(esItem: EsItem, text: String) {
        val result = repository.search(
            EsItemGenericFilter(text = text),
            EsItemSort.DEFAULT,
            10
        ).entities

        assertThat(result.size).isEqualTo(1)
        assertThat(result[0].itemId).isEqualTo(esItem.itemId)
    }

    @Test
    @Disabled("Text search temporary disabled. Will be fixed in PT-4066")
    fun `should search by trait string`(): Unit = runBlocking {

        val esItems = (1..50).map { randomEsItem() }
        repository.saveAll(esItems)

        val result = repository.search(
            EsItemGenericFilter(text = esItems[13].traits[1].value), EsItemSort.DEFAULT, 10
        ).entities

        assertThat(result.size).isEqualTo(1)
        assertThat(result[0].itemId).isEqualTo(esItems[13].itemId)
    }

    @Test
    @Disabled("Text search temporary disabled. Will be fixed in PT-4066")
    fun `should search by trait long`(): Unit = runBlocking {

        val esItems = (1..50).map { randomEsItem() }
        repository.saveAll(esItems)

        val result = repository.search(
            EsItemGenericFilter(text = esItems[13].traits[0].value), EsItemSort.DEFAULT, 10
        ).entities

        assertThat(result.size).isEqualTo(1)
        assertThat(result[0].itemId).isEqualTo(esItems[13].itemId)
    }

    @Test
    fun `should search by trait date`(): Unit = runBlocking {

        val esItem = randomEsItem()
        repository.save(esItem)
        val result = repository.search(
            EsItemGenericFilter(text = esItem.traits[2].value), EsItemSort.DEFAULT, 10
        ).entities

        assertThat(result.size).isEqualTo(1)
        assertThat(result[0].itemId).isEqualTo(esItem.itemId)
    }

    @Test
    fun `get random items from collection`() = runBlocking<Unit> {
        val collectionId = CollectionIdDto(BlockchainDto.ETHEREUM, randomAddress().toString()).fullId()
        val otherCollectionId = CollectionIdDto(BlockchainDto.ETHEREUM, randomAddress().toString()).fullId()
        val esItem1 = randomEsItem(collectionId = collectionId)
        val esItem2 = randomEsItem(collectionId = collectionId)
        val esItem3 = randomEsItem(collectionId = collectionId)
        val esItem4 = randomEsItem(collectionId = otherCollectionId)

        repository.bulk(entitiesToSave = listOf(esItem1, esItem2, esItem3, esItem4))

        val result1 = repository.getRandomItemsFromCollection(collectionId = collectionId, size = 3)
        WaitAssert.wait {
            val result2 = repository.getRandomItemsFromCollection(collectionId = collectionId, size = 3)
            assertThat(result1).containsExactlyInAnyOrder(esItem1, esItem2, esItem3)
            assertThat(result1).containsExactlyInAnyOrder(*result2.toTypedArray())
            // Sometimes it might match
            assertThat(result1).isNotEqualTo(result2)
        }
    }

    @Test
    fun getCheapestItems() = runBlocking<Unit> {
        val collectionId = CollectionIdDto(BlockchainDto.ETHEREUM, randomAddress().toString()).fullId()
        val otherCollectionId = CollectionIdDto(BlockchainDto.ETHEREUM, randomAddress().toString()).fullId()
        val currency1 = CurrencyIdDto(BlockchainDto.ETHEREUM, randomAddress().toString(), null).fullId()
        val currency2 = CurrencyIdDto(BlockchainDto.ETHEREUM, randomAddress().toString(), null).fullId()
        val esItemNotOnSale = randomEsItem(collectionId = collectionId)
        val esItem1OnSaleCurrency1 = randomEsItem(collectionId = collectionId).copy(
            bestSellCurrency = currency1,
            bestSellAmount = 10.0
        )
        val esItem2OnSaleCurrency1 = randomEsItem(collectionId = collectionId).copy(
            bestSellCurrency = currency1,
            bestSellAmount = 1.0
        )
        val esItemOnSaleCurrency2 = randomEsItem(collectionId = collectionId).copy(
            bestSellCurrency = currency2,
            bestSellAmount = 2.0
        )
        val esItemOnSaleOtherCollection = randomEsItem(collectionId = otherCollectionId).copy(
            bestSellCurrency = currency2,
            bestSellAmount = 1.5
        )

        repository.bulk(
            entitiesToSave = listOf(
                esItemNotOnSale,
                esItem1OnSaleCurrency1,
                esItem2OnSaleCurrency1,
                esItemOnSaleCurrency2,
                esItemOnSaleOtherCollection
            )
        )

        val result = repository.getCheapestItems(collectionId)

        assertThat(result).containsExactlyInAnyOrder(esItem2OnSaleCurrency1, esItemOnSaleCurrency2)
    }

    fun randomEsItem(collectionId: String = randomAddress().toString()): EsItem {
        val blockchain = BlockchainDto.ETHEREUM
        val itemId = ItemIdDto(blockchain, collectionId, randomBigInt())
        return EsItem(
            id = randomString(),
            itemId = itemId.fullId(),
            blockchain = blockchain,
            collection = collectionId,
            token = collectionId,
            tokenId = itemId.value.split(":")[1],
            name = randomString(),
            description = randomString(),
            traits = listOf(
                EsItemTrait("long", randomLong().toString()),
                EsItemTrait("testString", randomString()),
                EsItemTrait("testDate", "2022-05-" + (1..30).random())
            ),
            creators = listOf(randomAddress().toString()),
            mintedAt = nowMillis(),
            lastUpdatedAt = nowMillis()
        )
    }
}
