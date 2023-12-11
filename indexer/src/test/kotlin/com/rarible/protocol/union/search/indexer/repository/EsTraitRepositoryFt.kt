package com.rarible.protocol.union.search.indexer.repository

import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.elastic.EsTrait
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsTraitRepository
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.test.context.ContextConfiguration

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class EsTraitRepositoryFt {

    @Autowired
    protected lateinit var repository: EsTraitRepository

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        elasticsearchTestBootstrapper.bootstrap()
    }

    @Test
    fun `should save and read`(): Unit = runBlocking {
        val esItem = createEsTrait()

        val id = repository.save(esItem).id
        val found = repository.findById(id)
        assertThat(found).isEqualTo(esItem)
    }

    fun createEsTrait(): EsTrait {
        return EsTrait(
            id = "0xFF",
            blockchain = BlockchainDto.ETHEREUM,
            collection = "0x02",
            key = "test",
            value = "value",
            itemsCount = 2,
            listedItemsCount = 1,
            version = 1
        )
    }
}
