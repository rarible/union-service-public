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

    @Test
    fun `update metaRefreshPriority`() = runBlocking<Unit> {
        val collection1 = collectionRepository.save(randomEnrichmentCollection())
        val collection2 = collectionRepository.save(randomEnrichmentCollection())
        val collection3 = collectionRepository.save(randomEnrichmentCollection())

        collectionRepository.updatePriority(setOf(collection1.id, collection2.id), 10)

        val updatedCollection1 = collectionRepository.get(collection1.id)!!
        assertThat(updatedCollection1.metaRefreshPriority).isEqualTo(10)
        assertThat(updatedCollection1.version).isEqualTo((collection1.version ?: 0) + 1)
        assertThat(updatedCollection1.lastUpdatedAt).isAfter(collection1.lastUpdatedAt)
        val updatedCollection2 = collectionRepository.get(collection2.id)!!
        assertThat(updatedCollection2.metaRefreshPriority).isEqualTo(10)
        assertThat(updatedCollection2.version).isEqualTo((collection1.version ?: 0) + 1)
        assertThat(updatedCollection2.lastUpdatedAt).isAfter(collection1.lastUpdatedAt)
        val updatedCollection3 = collectionRepository.get(collection3.id)!!
        assertThat(updatedCollection3).isEqualTo(collection3)

        collectionRepository.updatePriority(setOf(collection1.id), null)
        val updatedCollection11 = collectionRepository.get(collection1.id)!!
        assertThat(updatedCollection11.metaRefreshPriority).isNull()
        assertThat(updatedCollection11.version).isEqualTo((updatedCollection1.version ?: 0) + 1)
        assertThat(updatedCollection11.lastUpdatedAt).isAfter(updatedCollection1.lastUpdatedAt)
    }
}
