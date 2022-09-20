package com.rarible.protocol.union.listener.repository

import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

@IntegrationTest
internal class OwnershipRepositoryIt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @Test
    fun getOrCreateWithLastUpdatedAtUpdate() = runBlocking<Unit> {
        val ownershipId = ShortOwnershipId(randomEthOwnershipId())

        val ownership = ownershipRepository.getOrCreateWithLastUpdatedAtUpdate(ownershipId)

        assertThat(ownership).isEqualTo(
            ShortOwnership.empty(ownershipId).copy(lastUpdatedAt = ownership.lastUpdatedAt, version = 1)
        )
        assertThat(ownership.lastUpdatedAt).isAfter(Instant.now().minusSeconds(5))

        val updated = ownershipRepository.getOrCreateWithLastUpdatedAtUpdate(ownershipId)
        assertThat(updated).isEqualTo(
            ShortOwnership.empty(ownershipId).copy(lastUpdatedAt = updated.lastUpdatedAt, version = 2)
        )
        assertThat(updated.lastUpdatedAt).isAfter(ownership.lastUpdatedAt)
    }
}