package com.rarible.protocol.union.search.indexer.repository

import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.dto.BlockchainDto
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
import java.time.Instant

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

    private fun randomEsOwnership() = EsOwnership(
        ownershipId = randomString(),
        blockchain = BlockchainDto.values().random(),
        itemId = randomString(),
        collection = randomString(),
        value = randomString(),
        date = Instant.ofEpochMilli(randomLong()),
    )
}
