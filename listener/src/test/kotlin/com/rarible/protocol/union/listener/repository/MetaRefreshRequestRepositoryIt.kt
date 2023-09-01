package com.rarible.protocol.union.listener.repository

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.enrichment.model.MetaRefreshRequest
import com.rarible.protocol.union.enrichment.repository.MetaRefreshRequestRepository
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

@IntegrationTest
class MetaRefreshRequestRepositoryIt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var metaRefreshRequestRepository: MetaRefreshRequestRepository

    @Test
    fun crud() = runBlocking<Unit> {
        val collectionId1 = randomEthCollectionId().fullId()
        val collectionId2 = randomEthCollectionId().fullId()
        metaRefreshRequestRepository.save(
            MetaRefreshRequest(
                collectionId = collectionId1,
                scheduled = true,
            )
        )
        metaRefreshRequestRepository.save(
            MetaRefreshRequest(
                collectionId = collectionId1,
                scheduled = false,
            )
        )
        metaRefreshRequestRepository.save(
            MetaRefreshRequest(
                collectionId = collectionId2,
                scheduled = true,
            )
        )
        metaRefreshRequestRepository.save(
            MetaRefreshRequest(
                collectionId = collectionId1,
                createdAt = Instant.now().plusSeconds(60),
                scheduled = true,
            )
        )

        assertThat(metaRefreshRequestRepository.countForCollectionId(collectionId1)).isEqualTo(3)
        assertThat(metaRefreshRequestRepository.countForCollectionId(collectionId2)).isEqualTo(1)
        assertThat(metaRefreshRequestRepository.countNotScheduled()).isEqualTo(1)
        assertThat(metaRefreshRequestRepository.countNotScheduledForCollectionId(collectionId1)).isEqualTo(1)
        assertThat(metaRefreshRequestRepository.countNotScheduledForCollectionId(collectionId2)).isEqualTo(0)

        metaRefreshRequestRepository.deleteCreatedBefore(Instant.now().plusSeconds(1))

        assertThat(metaRefreshRequestRepository.countForCollectionId(collectionId1)).isEqualTo(2)
        assertThat(metaRefreshRequestRepository.countForCollectionId(collectionId2)).isEqualTo(0)

        metaRefreshRequestRepository.deleteAll()

        assertThat(metaRefreshRequestRepository.countForCollectionId(collectionId1)).isEqualTo(0)
    }

    @Test
    fun findToScheduleAndUpdate() = runBlocking<Unit> {
        val collectionId1 = randomEthCollectionId().fullId()
        val collectionId2 = randomEthCollectionId().fullId()
        val collectionId3 = randomEthCollectionId().fullId()
        val collectionId4 = randomEthCollectionId().fullId()
        val collectionId5 = randomEthCollectionId().fullId()

        val now = nowMillis()

        metaRefreshRequestRepository.save(
            MetaRefreshRequest(
                collectionId = collectionId1,
                scheduled = true,
            )
        )
        metaRefreshRequestRepository.save(
            MetaRefreshRequest(
                collectionId = collectionId2,
                scheduledAt = now.plusSeconds(60)
            )
        )
        metaRefreshRequestRepository.save(
            MetaRefreshRequest(
                collectionId = collectionId3,
                scheduledAt = now.minusSeconds(1)
            )
        )
        metaRefreshRequestRepository.save(
            MetaRefreshRequest(
                collectionId = collectionId4,
                priority = 1,
                scheduledAt = now.minusSeconds(2)
            )
        )
        metaRefreshRequestRepository.save(
            MetaRefreshRequest(
                collectionId = collectionId5,
                scheduledAt = now.minusSeconds(3)
            )
        )

        val result = metaRefreshRequestRepository.findToScheduleAndUpdate(2)
        assertThat(result.map { it.collectionId }).containsExactly(collectionId4, collectionId3)
    }
}
