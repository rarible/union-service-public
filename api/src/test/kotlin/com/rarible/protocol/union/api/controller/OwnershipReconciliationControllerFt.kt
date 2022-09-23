package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

@FlowPreview
@IntegrationTest
internal class OwnershipReconciliationControllerFt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @Test
    fun getOwnerships() {
        val ownership1 = runBlocking {
            ownershipRepository.save(randomShortOwnership().copy(lastUpdatedAt = Instant.ofEpochMilli(2000)))
        }
        val ownership2 = runBlocking {
            ownershipRepository.save(randomShortOwnership().copy(lastUpdatedAt = Instant.ofEpochMilli(3000)))
        }

        val ownership1Dto = randomEthOwnershipDto(ownership1.id.toDto())
        val ownership2Dto = randomEthOwnershipDto(ownership2.id.toDto())

        ethereumOwnershipControllerApiMock.mockGetNftOwnershipByIds(
            listOf(ownership1Dto.id, ownership2Dto.id), listOf(ownership1Dto, ownership2Dto)
        )

        val result = testRestTemplate.getForObject(
            "$baseUri/reconciliation/ownerships?lastUpdatedFrom={from}&lastUpdatedTo={to}",
            OwnershipsDto::class.java,
            1000,
            4000
        )!!

        assertThat(result.ownerships.map { it.id.fullId() }).containsExactly(
            ownership1.id.toDto().fullId(),
            ownership2.id.toDto().fullId()
        )
    }
}