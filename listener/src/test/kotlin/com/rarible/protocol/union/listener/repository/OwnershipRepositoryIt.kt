package com.rarible.protocol.union.listener.repository

import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

@IntegrationTest
class OwnershipRepositoryIt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @Test
    fun findIdsByLastUpdatedAt() = runBlocking<Unit> {
        val ownership1 =
            ownershipRepository.save(randomShortOwnership().copy(lastUpdatedAt = Instant.ofEpochMilli(1000)))
        val ownership2 =
            ownershipRepository.save(randomShortOwnership().copy(lastUpdatedAt = Instant.ofEpochMilli(2000)))
        val ownership3 =
            ownershipRepository.save(randomShortOwnership().copy(lastUpdatedAt = Instant.ofEpochMilli(3000)))
        val ownership4 =
            ownershipRepository.save(randomShortOwnership().copy(lastUpdatedAt = Instant.ofEpochMilli(4000)))
        val ownership5 =
            ownershipRepository.save(randomShortOwnership().copy(lastUpdatedAt = Instant.ofEpochMilli(5000)))

        val result = ownershipRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = Instant.ofEpochMilli(1500),
            lastUpdatedTo = Instant.ofEpochMilli(4500),
            continuation = null
        )
        assertThat(result).containsExactlyInAnyOrder(ownership2.id, ownership3.id, ownership4.id)

        val resultWithContinuation = ownershipRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = Instant.ofEpochMilli(1500),
            lastUpdatedTo = Instant.ofEpochMilli(4500),
            continuation = result[0]
        )
        assertThat(resultWithContinuation).containsExactly(result[1], result[2])

        val resultWithContinuationAndSize = ownershipRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = Instant.ofEpochMilli(1500),
            lastUpdatedTo = Instant.ofEpochMilli(4500),
            continuation = result[0],
            size = 1
        )
        assertThat(resultWithContinuationAndSize).containsExactly(result[1])
    }
}
