package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.select.OwnershipSourceSelectService
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.enrichment.model.ShortDateIdOwnership
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.enrichment.test.data.randomOwnershipDto
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant

class OwnershipReconciliationControllerTest {
    private val ownershipRepository = mockk<OwnershipRepository>()
    private val ownershipSourceSelectService = mockk<OwnershipSourceSelectService>()
    private val controller = OwnershipReconciliationController(ownershipSourceSelectService, ownershipRepository)

    @Test
    fun `get ownerships - with continuation`() = runBlocking<Unit> {
        val from = Instant.ofEpochSecond(1)
        val to = Instant.ofEpochSecond(10)

        val id1 = ShortDateIdOwnership(randomShortOwnership().id, Instant.ofEpochSecond(2))
        val id2 = ShortDateIdOwnership(randomShortOwnership().id, Instant.ofEpochSecond(3))

        coEvery { ownershipRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = from,
            lastUpdatedTo = to,
            fromId = null,
            size = 2
        ) } returns listOf(id1, id2)

        coEvery {
            ownershipSourceSelectService.getOwnershipsByIds(listOf(id1.id.toDto(), id2.id.toDto()))
        } returns listOf(randomOwnershipDto(randomEthOwnershipId()), randomOwnershipDto(randomEthOwnershipId()))

        val items = controller.getOwnerships(
            lastUpdatedFrom = from,
            lastUpdatedTo = to,
            continuation = null,
            size = 2,
        )
        Assertions.assertThat(items.ownerships).hasSize(2)
        Assertions.assertThat(items.continuation).isEqualTo(DateIdContinuation(id2.lastUpdatedAt, id2.id.toDto().fullId()).toString())
    }

    @Test
    fun `get ownerships - no continuation`() = runBlocking<Unit> {
        val from = Instant.ofEpochSecond(1)
        val to = Instant.ofEpochSecond(10)

        val actualFrom = Instant.ofEpochSecond(5)
        val actualFromId = randomShortOwnership()

        val id1 = ShortDateIdOwnership(randomShortOwnership().id, Instant.ofEpochSecond(2))
        val id2 = ShortDateIdOwnership(randomShortOwnership().id, Instant.ofEpochSecond(3))

        coEvery { ownershipRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = actualFrom,
            lastUpdatedTo = to,
            fromId = actualFromId.id,
            size = 10
        ) } returns listOf(id1, id2)

        coEvery {
            ownershipSourceSelectService.getOwnershipsByIds(listOf(id1.id.toDto(), id2.id.toDto()))
        } returns listOf(randomOwnershipDto(randomEthOwnershipId()), randomOwnershipDto(randomEthOwnershipId()))

        val items = controller.getOwnerships(
            lastUpdatedFrom = from,
            lastUpdatedTo = to,
            continuation = DateIdContinuation(actualFrom, actualFromId.id.toDto().fullId()).toString(),
            size = 10,
        )
        Assertions.assertThat(items.ownerships).hasSize(2)
        Assertions.assertThat(items.continuation).isNull()
    }
}