package com.rarible.protocol.union.search.indexer.repository

import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.test.context.ContextConfiguration
import randomEsOwnership

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class OwnershipEsRepositoryFt {
    @Autowired
    protected lateinit var repository: EsOwnershipRepository

    @Test
    fun `should save and read`(): Unit = runBlocking {
        val expected = randomEsOwnership()

        repository.saveAll(listOf(expected)).first()
        val actual = repository.findById(expected.ownershipId)
        assertThat(actual).isEqualTo(expected)

        repository.deleteAll(listOf(expected.ownershipId))
        val deleted = repository.findById(expected.ownershipId)
        assertNull(deleted)
    }
}
