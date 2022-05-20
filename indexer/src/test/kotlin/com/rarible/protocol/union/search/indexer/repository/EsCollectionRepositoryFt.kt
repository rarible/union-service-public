package com.rarible.protocol.union.search.indexer.repository

import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.test.data.randomEsCollection
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.index.query.BoolQueryBuilder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.test.context.ContextConfiguration

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
class EsCollectionRepositoryFt {

    @Autowired
    protected lateinit var repository: EsCollectionRepository

    @Test
    fun `should save and read`(): Unit = runBlocking {
        val collection = EsCollection(
            collectionId = "ETHEREUM:12345",
            blockchain = BlockchainDto.ETHEREUM,
            name = "some name",
            symbol = "SMBL",
            owner = "some owner",
            meta = null,
        )

        val id = repository.saveAll(listOf(collection)).first().collectionId
        val found = repository.findById(id)
        assertThat(found).isEqualTo(collection)
    }

    @Test
    fun `should be able to search up to 1000 collections`(): Unit = runBlocking {
        // given
        val collections = List(1000) { randomEsCollection() }
        repository.saveAll(collections)

        // when
        val query = NativeSearchQuery(BoolQueryBuilder())
        query.maxResults = 1000
        val actual = repository.search(query)

        // then
        assertThat(actual).hasSize(1000)
    }
}
