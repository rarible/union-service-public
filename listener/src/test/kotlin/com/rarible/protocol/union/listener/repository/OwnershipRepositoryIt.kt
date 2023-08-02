package com.rarible.protocol.union.listener.repository

import com.rarible.protocol.union.dto.BlockchainDto
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
        val ownership1 = ownershipRepository.save(randomShortOwnership().copy(
            lastUpdatedAt = Instant.ofEpochMilli(1000),
            blockchain = BlockchainDto.ETHEREUM,
            itemId = "1"
        ))
        val ownership2 = ownershipRepository.save(randomShortOwnership().copy(
            lastUpdatedAt = Instant.ofEpochMilli(2000),
            blockchain = BlockchainDto.ETHEREUM,
            itemId = "2"
        ))
        val ownership3 = ownershipRepository.save(randomShortOwnership().copy(
            lastUpdatedAt = Instant.ofEpochMilli(3000),
            blockchain = BlockchainDto.ETHEREUM,
            itemId = "3"
        ))
        val ownership4 = ownershipRepository.save(randomShortOwnership().copy(
            lastUpdatedAt = Instant.ofEpochMilli(4000),
            blockchain = BlockchainDto.ETHEREUM,
            itemId = "4"
        ))
        val ownership5 = ownershipRepository.save(randomShortOwnership().copy(
            lastUpdatedAt = Instant.ofEpochMilli(4000),
            blockchain = BlockchainDto.ETHEREUM,
            itemId = "5"
        ))
        val ownership6 = ownershipRepository.save(randomShortOwnership().copy(
            lastUpdatedAt = Instant.ofEpochMilli(6000),
            blockchain = BlockchainDto.ETHEREUM,
            itemId = "6"
        ))
        ownershipRepository.save(randomShortOwnership().copy(
            lastUpdatedAt = Instant.ofEpochMilli(8000),
            blockchain = BlockchainDto.ETHEREUM,
            itemId = "7"
        ))

        var result = ownershipRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = Instant.ofEpochMilli(500),
            lastUpdatedTo = Instant.ofEpochMilli(7000),
            fromId = null,
            size = 2
        )
        assertThat(result.map { it.id }).containsExactlyInAnyOrder(ownership1.id, ownership2.id)

        result = ownershipRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = result.last().lastUpdatedAt,
            lastUpdatedTo = Instant.ofEpochMilli(7000),
            fromId = result.last().id,
            size = 2
        )
        assertThat(result.map { it.id }).containsExactlyInAnyOrder(ownership3.id, ownership4.id)

        result = ownershipRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = result.last().lastUpdatedAt,
            lastUpdatedTo = Instant.ofEpochMilli(7000),
            fromId = result.last().id,
            size = 10
        )
        assertThat(result.map { it.id }).containsExactlyInAnyOrder(ownership5.id, ownership6.id)
    }
}
