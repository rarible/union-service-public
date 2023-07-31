package com.rarible.protocol.union.search.indexer.repository

import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.converter.EsOrderConverter
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.elastic.EsAllOrderFilter
import com.rarible.protocol.union.core.model.elastic.EsOrder
import com.rarible.protocol.union.core.model.elastic.EsOrderBidOrdersByItem
import com.rarible.protocol.union.core.model.elastic.EsOrderSellOrders
import com.rarible.protocol.union.core.model.elastic.EsOrderSellOrdersByItem
import com.rarible.protocol.union.core.model.elastic.EsOrderSort
import com.rarible.protocol.union.core.model.elastic.EsOrdersByMakers
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.EthCollectionAssetTypeDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.index.query.BoolQueryBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.test.context.ContextConfiguration
import randomAssetTypeDto
import randomAssetTypeErc20Dto
import randomOrderDto
import randomOrderId
import randomUnionAddress
import java.time.Instant
import java.time.temporal.ChronoUnit

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class EsOrderRepositoryFt {

    @Autowired
    protected lateinit var repository: EsOrderRepository

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        elasticsearchTestBootstrapper.bootstrap()
    }

    @Test
    fun `should save and read`(): Unit = runBlocking {

        val order = randomOrderDto()
        val esOrder = EsOrderConverter.convert(order)

        val id = repository.save(esOrder).orderId
        val found = repository.findById(id)
        assertThat(found?.orderId).isEqualTo(esOrder.orderId)
    }

    @Test
    fun `EsAllOrderFilter should be able to search up to 1000 orders LAST_UPDATE_ASC`(): Unit = runBlocking {

        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        // given
        val orders = List(95) {
            randomOrderDto().copy(
                id = randomOrderId(blockchain = BlockchainDto.ETHEREUM),
                lastUpdatedAt = now.plusMillis(it.toLong())
            )
        }
            .map { EsOrderConverter.convert(it) }
        repository.saveAll(orders)
        val blockchains = listOf(BlockchainDto.ETHEREUM)
        var cursor: DateIdContinuation? = null

        val pageSize = 10
        // when

        for (i in 0..8) {

            val actual = repository.findByFilter(
                EsAllOrderFilter(
                    blockchains = blockchains,
                    cursor = cursor,
                    size = pageSize,
                    status = null,
                    sort = EsOrderSort.LAST_UPDATE_ASC
                )
            )
            val last = actual.last()

            cursor = DateIdContinuation(
                id = last.orderId,
                date = last.lastUpdatedAt
            )

            val ids = actual.map { it.orderId }

            // then
            assertThat(actual).hasSize(pageSize)

            for (j in 0 until pageSize) {
                assertThat(ids).contains(orders[i * pageSize + j].orderId)
            }
        }

        val actual = repository.findByFilter(
            EsAllOrderFilter(
                blockchains = blockchains,
                cursor = cursor,
                size = pageSize,
                status = null,
                sort = EsOrderSort.LAST_UPDATE_ASC
            )
        )
        val ids = actual.map { it.orderId }

        // then
        assertThat(actual).hasSize(5)

        for (j in 0 until 5) {
            assertThat(ids).contains(orders[9 * pageSize + j].orderId)
        }
    }

    @Test
    fun `EsAllOrderFilter should be able to search up to 1000 orders LAST_UPDATE_DESC`(): Unit = runBlocking {

        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        // given
        val orders = List(95) {
            randomOrderDto().copy(
                id = randomOrderId(blockchain = BlockchainDto.ETHEREUM),
                lastUpdatedAt = now.plusMillis(it.toLong())
            )
        }
            .map { EsOrderConverter.convert(it) }
        repository.saveAll(orders)
        val blockchains = listOf(BlockchainDto.ETHEREUM)
        var cursor: DateIdContinuation? = null

        val pageSize = 10
        // when

        for (i in 0..8) {

            val actual = repository.findByFilter(
                EsAllOrderFilter(
                    blockchains = blockchains,
                    cursor = cursor,
                    size = pageSize,
                    status = null,
                    sort = EsOrderSort.LAST_UPDATE_DESC
                )
            )
            val last = actual.last()

            cursor = DateIdContinuation(
                id = last.orderId,
                date = last.lastUpdatedAt
            )

            val ids = actual.map { it.orderId }

            // then
            assertThat(actual).hasSize(pageSize)

            for (j in 0 until pageSize) {
                assertThat(ids).contains(orders[94 - (i * pageSize + j)].orderId)
            }
        }

        val actual = repository.findByFilter(
            EsAllOrderFilter(
                blockchains = blockchains,
                cursor = cursor,
                size = pageSize,
                status = null,
                sort = EsOrderSort.LAST_UPDATE_DESC
            )
        )
        val ids = actual.map { it.orderId }

        // then
        assertThat(actual).hasSize(5)

        for (j in 4 downTo 0) {
            assertThat(ids).contains(orders[j].orderId)
        }
    }

    @Test
//    @Disabled("Test fails after recent changes, to be fixed under PT-1216")
    fun `EsOrderSellOrdersByItem filter`(): Unit = runBlocking {
        // given
        val orders = List(100) { randomOrderDto() }.map { EsOrderConverter.convert(it) }
        repository.saveAll(orders)

        // when
        val query = NativeSearchQuery(BoolQueryBuilder())
        query.maxResults = 1000

        val exampleOrder = orders.first { it.make.isNft }
        val exampleFilter = EsOrderSellOrdersByItem(
            itemId = exampleOrder.make.token + ":" + exampleOrder.make.tokenId,
            platform = null,
            maker = null,
            origin = null,
            size = 1000,
            continuation = null,
            status = OrderStatusDto.values().asList()
        )
        val filters = listOf(
            exampleFilter,
            exampleFilter.copy(platform = exampleOrder.platform),
            exampleFilter.copy(platform = exampleOrder.platform, maker = exampleOrder.maker)
        )

        // then
        filters.forEach { filter ->
            val orderId = repository.findByFilter(filter).first().orderId
            assertThat(orderId).isEqualTo(exampleOrder.orderId)
        }
    }

    @Test
    fun `EsOrderBidOrdersByItem filter`(): Unit = runBlocking {
        // given
        val id1 = OrderIdDto(BlockchainDto.ETHEREUM, "1")
        val currency1 = randomAssetTypeErc20Dto(BlockchainDto.ETHEREUM)
        val contract1 = ContractAddress(BlockchainDto.ETHEREUM, randomString())
        val item1 = EthErc721AssetTypeDto(
            contract = contract1,
            tokenId = randomBigInt(),
        )
        val order1 = EsOrderConverter.convert(
            randomOrderDto(
                id = id1,
                make = AssetDto(currency1, randomBigDecimal()),
                take = AssetDto(
                    type = item1,
                    value = randomBigDecimal()
                ),
                maker = randomUnionAddress(BlockchainDto.ETHEREUM),
                platform = PlatformDto.RARIBLE,
            )
        )
        val id2 = OrderIdDto(BlockchainDto.ETHEREUM, "2")
        val collection = EthCollectionAssetTypeDto(
            contract = ContractAddress(BlockchainDto.ETHEREUM, contract1.value),
        )
        val order2 = EsOrderConverter.convert(
            randomOrderDto(
                id = id2,
                make = AssetDto(currency1, randomBigDecimal()),
                take = AssetDto(
                    type = collection,
                    randomBigDecimal()
                ),
                maker = randomUnionAddress(BlockchainDto.ETHEREUM),
                platform = PlatformDto.OPEN_SEA,
            )
        )
        val id3 = OrderIdDto(BlockchainDto.ETHEREUM, "3")
        val item3 = randomAssetTypeDto(BlockchainDto.ETHEREUM)
        val order3 = EsOrderConverter.convert(
            randomOrderDto(
                id = id3,
                make = AssetDto(currency1, randomBigDecimal()),
                take = AssetDto(
                    type = item3,
                    value = randomBigDecimal()
                ),
                maker = randomUnionAddress(BlockchainDto.ETHEREUM),
                platform = PlatformDto.RARIBLE,
            )
        )
        val id4 = OrderIdDto(BlockchainDto.ETHEREUM, "4")
        val currency2 = randomAssetTypeErc20Dto(BlockchainDto.ETHEREUM)
        val order4 = EsOrderConverter.convert(
            randomOrderDto(
                id = id4,
                make = AssetDto(currency2, randomBigDecimal()),
                take = AssetDto(
                    type = item1,
                    value = randomBigDecimal()
                ),
                maker = randomUnionAddress(BlockchainDto.ETHEREUM),
                platform = PlatformDto.LOOKSRARE,
            )
        )
        repository.saveAll(listOf(order1, order2, order3, order4))

        // when
        val query = NativeSearchQuery(BoolQueryBuilder())
        query.maxResults = 1000

        val filter = EsOrderBidOrdersByItem(
            itemId = order1.take.token + ":" + order1.take.tokenId,
            platform = null,
            maker = null,
            origin = null,
            size = 1000,
            continuation = null,
            status = OrderStatusDto.values().asList(),
            currencies = null,
        )

        val esOrders1 = repository.findByFilter(filter)
        assertThat(esOrders1.map { it.orderId }).containsExactlyInAnyOrder(
            order1.orderId,
            order2.orderId,
            order4.orderId
        )

        val esOrders2 = repository.findByFilter(filter.copy(platform = PlatformDto.RARIBLE))
        assertThat(esOrders2.map { it.orderId }).containsExactlyInAnyOrder(order1.orderId)

        val esOrders3 = repository.findByFilter(filter.copy(maker = listOf(order2.maker)))
        assertThat(esOrders3.map { it.orderId }).containsExactlyInAnyOrder(order2.orderId)

        val esOrders4 = repository.findByFilter(
            filter.copy(
                currencies = listOf(
                    CurrencyIdDto(
                        blockchain = BlockchainDto.ETHEREUM,
                        contract = currency1.contract.value,
                        tokenId = null,
                    )
                )
            )
        )
        assertThat(esOrders4.map { it.orderId }).containsExactlyInAnyOrder(order1.orderId, order2.orderId)
    }

    @Test
    fun `EsOrdersByMakers filter`(): Unit = runBlocking {
        // given
        val id1 = OrderIdDto(BlockchainDto.ETHEREUM, "1")
        val currency1 = randomAssetTypeErc20Dto(BlockchainDto.ETHEREUM)
        val maker1 = randomUnionAddress(BlockchainDto.ETHEREUM)
        val order1 = EsOrderConverter.convert(
            randomOrderDto(
                id = id1,
                take = AssetDto(currency1, randomBigDecimal()),
                make = AssetDto(randomAssetTypeDto(BlockchainDto.ETHEREUM), randomBigDecimal()),
                maker = maker1,
                platform = PlatformDto.RARIBLE,
            )
        )
        val id2 = OrderIdDto(BlockchainDto.ETHEREUM, "2")
        val currency2 = randomAssetTypeErc20Dto(BlockchainDto.ETHEREUM)
        val order2 = EsOrderConverter.convert(
            randomOrderDto(
                id = id2,
                take = AssetDto(currency2, randomBigDecimal()),
                make = AssetDto(randomAssetTypeDto(BlockchainDto.ETHEREUM), randomBigDecimal()),
                maker = maker1,
                platform = PlatformDto.RARIBLE,
            )
        )
        val id3 = OrderIdDto(BlockchainDto.ETHEREUM, "3")
        val maker2 = randomUnionAddress(BlockchainDto.ETHEREUM)
        val order3 = EsOrderConverter.convert(
            randomOrderDto(
                id = id3,
                take = AssetDto(currency1, randomBigDecimal()),
                make = AssetDto(randomAssetTypeDto(BlockchainDto.ETHEREUM), randomBigDecimal()),
                maker = maker2,
                platform = PlatformDto.RARIBLE,
            )
        )
        val id4 = OrderIdDto(BlockchainDto.ETHEREUM, "4")
        val order4 = EsOrderConverter.convert(
            randomOrderDto(
                id = id4,
                make = AssetDto(currency1, randomBigDecimal()),
                take = AssetDto(randomAssetTypeDto(BlockchainDto.ETHEREUM), randomBigDecimal()),
                maker = maker1,
                platform = PlatformDto.RARIBLE,
            )
        )
        val id5 = OrderIdDto(BlockchainDto.ETHEREUM, "5")
        val order5 = EsOrderConverter.convert(
            randomOrderDto(
                id = id5,
                make = AssetDto(currency2, randomBigDecimal()),
                take = AssetDto(randomAssetTypeDto(BlockchainDto.ETHEREUM), randomBigDecimal()),
                maker = maker1,
                platform = PlatformDto.RARIBLE,
            )
        )
        val id6 = OrderIdDto(BlockchainDto.ETHEREUM, "6")
        val order6 = EsOrderConverter.convert(
            randomOrderDto(
                id = id6,
                make = AssetDto(currency1, randomBigDecimal()),
                take = AssetDto(randomAssetTypeDto(BlockchainDto.ETHEREUM), randomBigDecimal()),
                maker = maker2,
                platform = PlatformDto.RARIBLE,
            )
        )
        repository.saveAll(listOf(order1, order2, order3, order4, order5, order6))

        // when
        val query = NativeSearchQuery(BoolQueryBuilder())
        query.maxResults = 1000

        val exampleFilter = EsOrdersByMakers(
            blockchains = null,
            platform = null,
            maker = listOf(maker1.fullId()),
            origin = null,
            size = 1000,
            continuation = null,
            sort = EsOrderSort.LAST_UPDATE_DESC,
            status = OrderStatusDto.values().asList(),
            type = EsOrder.Type.SELL,
            currencies = null,
        )

        // then
        val esOrders1 = repository.findByFilter(exampleFilter)
        assertThat(esOrders1.map { it.orderId }).containsExactlyInAnyOrder(order1.orderId, order2.orderId)

        val esOrders2 = repository.findByFilter(
            exampleFilter.copy(
                currencies = listOf(
                    CurrencyIdDto(
                        blockchain = BlockchainDto.ETHEREUM,
                        contract = currency1.contract.value,
                        tokenId = null,
                    )
                )
            )
        )
        assertThat(esOrders2.map { it.orderId }).containsExactlyInAnyOrder(order1.orderId)

        val esOrders3 = repository.findByFilter(exampleFilter.copy(type = EsOrder.Type.BID))
        assertThat(esOrders3.map { it.orderId }).containsExactlyInAnyOrder(order4.orderId, order5.orderId)

        val esOrders4 = repository.findByFilter(
            exampleFilter.copy(
                type = EsOrder.Type.BID,
                currencies = listOf(
                    CurrencyIdDto(
                        blockchain = BlockchainDto.ETHEREUM,
                        contract = currency1.contract.value,
                        tokenId = null,
                    )
                )
            )
        )
        assertThat(esOrders4.map { it.orderId }).containsExactlyInAnyOrder(order4.orderId)
    }

    @Test
    fun `EsOrdersByMakers filter with origin`(): Unit = runBlocking {
        // given
        val orders = List(100) {
            val o = randomOrderDto()
            o.copy(take = o.make, make = o.take)
        }.map { EsOrderConverter.convert(it) }
        repository.saveAll(orders)

        // when
        val query = NativeSearchQuery(BoolQueryBuilder())
        query.maxResults = 1000

        val o1 = orders.first()
        val o2 = orders.last()
        val exampleFilter = EsOrdersByMakers(
            blockchains = null,
            platform = null,
            maker = listOf(o1.maker, o2.maker),
            origin = o1.origins.first(),
            size = 1000,
            continuation = null,
            sort = EsOrderSort.LAST_UPDATE_DESC,
            status = OrderStatusDto.values().asList(),
            type = EsOrder.Type.SELL,
            currencies = null,
        )

        // then
        val esOrders = repository.findByFilter(exampleFilter)
        assertThat(esOrders).hasSize(1)
        assertThat(esOrders.first().orderId).isEqualTo(o1.orderId)
    }

    @Test
    fun `EsOrderSellOrders filter`(): Unit = runBlocking {
        // given
        val orders = List(100) {
            val o = randomOrderDto()
            o.copy(take = o.make, make = o.take)
        }.map { EsOrderConverter.convert(it) }
        repository.saveAll(orders)

        // when
        val query = NativeSearchQuery(BoolQueryBuilder())
        query.maxResults = 1000

        val o1 = orders.first()
        val o2 = orders.last()
        val countOrders = orders.count { it.blockchain == o1.blockchain || it.blockchain == o2.blockchain }

        val exampleFilter = EsOrderSellOrders(
            blockchains = listOf(o1.blockchain, o2.blockchain),
            platform = null,
            origin = null,
            size = 1000,
            continuation = null,
            sort = EsOrderSort.LAST_UPDATE_DESC,
        )

        // then
        val esOrders = repository.findByFilter(exampleFilter)
        assertThat(esOrders).hasSize(countOrders)
    }
}
