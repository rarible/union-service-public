package com.rarible.protocol.union.listener.repository

import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

@IntegrationTest
class CollectionRepositoryIt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var collectionRepository: CollectionRepository

    @Test
    fun findIdsByLastUpdatedAt() = runBlocking<Unit> {
        val collection1 =
            collectionRepository.save(randomEnrichmentCollection().copy(lastUpdatedAt = Instant.ofEpochMilli(1000)))
        val collection2 =
            collectionRepository.save(randomEnrichmentCollection().copy(lastUpdatedAt = Instant.ofEpochMilli(2000)))
        val collection3 =
            collectionRepository.save(randomEnrichmentCollection().copy(lastUpdatedAt = Instant.ofEpochMilli(3000)))
        val collection4 =
            collectionRepository.save(randomEnrichmentCollection().copy(lastUpdatedAt = Instant.ofEpochMilli(4000)))
        val collection5 =
            collectionRepository.save(randomEnrichmentCollection().copy(lastUpdatedAt = Instant.ofEpochMilli(5000)))

        val result = collectionRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = Instant.ofEpochMilli(1500),
            lastUpdatedTo = Instant.ofEpochMilli(4500),
            continuation = null
        )
        assertThat(result.map { it.id }).containsExactlyInAnyOrder(collection2.id, collection3.id, collection4.id)

        val resultWithContinuation = collectionRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = Instant.ofEpochMilli(1500),
            lastUpdatedTo = Instant.ofEpochMilli(4500),
            continuation = result[0].id
        )
        assertThat(resultWithContinuation).containsExactly(result[1], result[2])

        val resultWithContinuationAndSize = collectionRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = Instant.ofEpochMilli(1500),
            lastUpdatedTo = Instant.ofEpochMilli(4500),
            continuation = result[0].id,
            size = 1
        )
        assertThat(resultWithContinuationAndSize).containsExactly(result[1])
    }
}