package com.rarible.protocol.union.search.indexer.repository

import com.rarible.protocol.union.core.converter.EsOrderConverter
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.EsAllOrderFilter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
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
import randomOrder

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

        val order = randomOrder()
        val esOrder = EsOrderConverter.convert(order)

        val id = repository.save(esOrder).orderId
        val found = repository.findById(id)
        assertThat(found).isEqualTo(esOrder)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `should be able to search up to 1000 items`(): Unit = runBlocking {
        // given
        val orders = List(1000) { randomOrder() }.map { EsOrderConverter.convert(it) }
        repository.saveAll(orders)

        // when
        val query = NativeSearchQuery(BoolQueryBuilder())
        query.maxResults = 1000
        val actual = repository.findByFilter(
            EsAllOrderFilter(
                blockchains = BlockchainDto.values().asList(),
                size = 1000,
                continuation = null,
                sort = OrderSortDto.LAST_UPDATE_DESC,
                status = OrderStatusDto.values().asList()
            )
        )

        // then
        assertThat(actual).hasSize(1000)
    }
}
