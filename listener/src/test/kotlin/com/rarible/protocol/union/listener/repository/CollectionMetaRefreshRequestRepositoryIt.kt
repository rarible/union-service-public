package com.rarible.protocol.union.listener.repository

import com.rarible.protocol.union.enrichment.model.CollectionMetaRefreshRequest
import com.rarible.protocol.union.enrichment.repository.CollectionMetaRefreshRequestRepository
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

@IntegrationTest
class CollectionMetaRefreshRequestRepositoryIt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var collectionMetaRefreshRequestRepository: CollectionMetaRefreshRequestRepository

    @Test
    fun crud() = runBlocking<Unit> {
        val collectionId1 = randomEthCollectionId().fullId()
        val collectionId2 = randomEthCollectionId().fullId()
        collectionMetaRefreshRequestRepository.save(
            CollectionMetaRefreshRequest(
                collectionId = collectionId1,
                scheduled = true,
            )
        )
        collectionMetaRefreshRequestRepository.save(
            CollectionMetaRefreshRequest(
                collectionId = collectionId1,
                scheduled = false,
            )
        )
        collectionMetaRefreshRequestRepository.save(
            CollectionMetaRefreshRequest(
                collectionId = collectionId2,
                scheduled = true,
            )
        )
        collectionMetaRefreshRequestRepository.save(
            CollectionMetaRefreshRequest(
                collectionId = collectionId1,
                createdAt = Instant.now().plusSeconds(60),
                scheduled = true,
            )
        )

        assertThat(collectionMetaRefreshRequestRepository.countForCollectionId(collectionId1)).isEqualTo(3)
        assertThat(collectionMetaRefreshRequestRepository.countForCollectionId(collectionId2)).isEqualTo(1)
        assertThat(collectionMetaRefreshRequestRepository.countNotScheduled()).isEqualTo(1)
        assertThat(collectionMetaRefreshRequestRepository.countNotScheduledForCollectionId(collectionId1)).isEqualTo(1)
        assertThat(collectionMetaRefreshRequestRepository.countNotScheduledForCollectionId(collectionId2)).isEqualTo(0)

        collectionMetaRefreshRequestRepository.deleteCreatedBefore(Instant.now().plusSeconds(1))

        assertThat(collectionMetaRefreshRequestRepository.countForCollectionId(collectionId1)).isEqualTo(2)
        assertThat(collectionMetaRefreshRequestRepository.countForCollectionId(collectionId2)).isEqualTo(0)

        collectionMetaRefreshRequestRepository.deleteAll()

        assertThat(collectionMetaRefreshRequestRepository.countForCollectionId(collectionId1)).isEqualTo(0)
    }

    @Test
    fun findToScheduleAndUpdate() = runBlocking<Unit> {
        val collectionId1 = randomEthCollectionId().fullId()
        val collectionId2 = randomEthCollectionId().fullId()
        val collectionId3 = randomEthCollectionId().fullId()
        val collectionId4 = randomEthCollectionId().fullId()
        val collectionId5 = randomEthCollectionId().fullId()
        collectionMetaRefreshRequestRepository.save(
            CollectionMetaRefreshRequest(
                collectionId = collectionId1,
                scheduled = true,
            )
        )
        collectionMetaRefreshRequestRepository.save(
            CollectionMetaRefreshRequest(
                collectionId = collectionId2,
                scheduledAt = Instant.now().plusSeconds(60)
            )
        )
        collectionMetaRefreshRequestRepository.save(
            CollectionMetaRefreshRequest(
                collectionId = collectionId3
            )
        )
        collectionMetaRefreshRequestRepository.save(
            CollectionMetaRefreshRequest(
                collectionId = collectionId4
            )
        )
        collectionMetaRefreshRequestRepository.save(
            CollectionMetaRefreshRequest(
                collectionId = collectionId5
            )
        )

        val result = collectionMetaRefreshRequestRepository.findToScheduleAndUpdate(2)
        assertThat(result.map { it.collectionId }).containsExactly(collectionId3, collectionId4)
    }
}