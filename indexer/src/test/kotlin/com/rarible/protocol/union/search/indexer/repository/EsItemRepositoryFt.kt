package com.rarible.protocol.union.search.indexer.repository

import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsTrait
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomEsItem
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.index.query.BoolQueryBuilder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.test.context.ContextConfiguration
import java.time.Instant

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class EsItemRepositoryFt {

    @Autowired
    protected lateinit var repository: EsItemRepository

    @Test
    fun `should save and read`(): Unit = runBlocking {

        val esItem = EsItem(
            itemId = "0x03",
            blockchain = BlockchainDto.ETHEREUM,
            collection = "0x02",
            name = "TestItem",
            description = "description",
            traits = listOf(EsTrait("long", "10"), EsTrait("test", "eye")),
            creators = listOf("0x01"),
            owner = "0x05",
            mintedAt = Instant.now(),
            lastUpdatedAt = Instant.now()
        )

        val id = repository.save(esItem).itemId
        val found = repository.findById(id)
        assertThat(found).isEqualTo(esItem)
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
        assertThat(actual.items).hasSize(1000)
    }
}
