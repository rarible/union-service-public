package com.rarible.protocol.union.listener.repository

import com.rarible.protocol.union.enrichment.model.ShortCollection
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

@IntegrationTest
internal class CollectionRepositoryIt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var collectionRepository: CollectionRepository

    @Test
    fun getOrCreateWithLastUpdatedAtUpdate() = runBlocking<Unit> {
        val collectionId = ShortCollectionId(randomEthCollectionId())

        val collection = collectionRepository.getOrCreateWithLastUpdatedAtUpdate(collectionId)

        assertThat(collection).isEqualTo(
            ShortCollection.empty(collectionId).copy(lastUpdatedAt = collection.lastUpdatedAt, version = 1)
        )
        assertThat(collection.lastUpdatedAt).isAfter(Instant.now().minusSeconds(5))

        val updated = collectionRepository.getOrCreateWithLastUpdatedAtUpdate(collectionId)
        assertThat(updated).isEqualTo(
            ShortCollection.empty(collectionId).copy(lastUpdatedAt = updated.lastUpdatedAt, version = 2)
        )
        assertThat(updated.lastUpdatedAt).isAfter(collection.lastUpdatedAt)
    }
}