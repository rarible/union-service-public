package com.rarible.protocol.union.worker.job.sync

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.UnionAuctionOwnershipWrapper
import com.rarible.protocol.union.core.producer.UnionInternalOwnershipEventProducer
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.converter.OwnershipDtoConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.test.data.randomUnionOwnership
import com.rarible.protocol.union.worker.task.search.EsRateLimiter
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.elasticsearch.action.support.WriteRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SyncOwnershipJobTest {

    @MockK
    lateinit var ownershipServiceRouter: BlockchainRouter<OwnershipService>

    @MockK
    lateinit var ownershipService: OwnershipService

    @MockK
    lateinit var enrichmentOwnershipService: EnrichmentOwnershipService

    @MockK
    lateinit var esOwnershipRepository: EsOwnershipRepository

    @MockK
    lateinit var producer: UnionInternalOwnershipEventProducer

    @MockK
    lateinit var esRateLimiter: EsRateLimiter

    @InjectMockKs
    lateinit var job: SyncOwnershipJob

    @BeforeEach
    fun beforeEach() {
        clearMocks(ownershipService, ownershipServiceRouter, esRateLimiter, producer)
        coEvery { esRateLimiter.waitIfNecessary(any()) } returns Unit
        every { ownershipServiceRouter.getService(BlockchainDto.ETHEREUM) } returns ownershipService
        coEvery { esOwnershipRepository.bulk(any(), any(), any(), WriteRequest.RefreshPolicy.NONE) } returns Unit
        coEvery { producer.sendChangeEvents(any()) } returns Unit
    }

    @Test
    fun `ownerships synced - db and es`() = runBlocking<Unit> {
        val ownership1 = randomUnionOwnership()
        val ownership2 = randomUnionOwnership()
        val ownerships = listOf(ownership1, ownership2)

        val updated1 = ShortOwnershipConverter.convert(ownership1)
        val updated2 = ShortOwnershipConverter.convert(ownership2)

        val wrappedOwnerships = ownerships.map { UnionAuctionOwnershipWrapper(it, null) }
        val dto = ownerships.map { OwnershipDtoConverter.convert(it) }

        coEvery {
            ownershipService.getOwnershipsAll(null, any())
        } returns Slice(null, ownerships)

        coEvery { enrichmentOwnershipService.getOrEmpty(ShortOwnershipId(ownership1.id)) } returns updated1
        coEvery { enrichmentOwnershipService.getOrEmpty(ShortOwnershipId(ownership2.id)) } returns updated2

        coEvery { enrichmentOwnershipService.enrich(wrappedOwnerships) } returns dto

        val param = """{"blockchain" : "ETHEREUM", "scope" : "ES"}"""
        job.handle(null, param).toList()

        coVerify(exactly = 1) { enrichmentOwnershipService.getOrEmpty(ShortOwnershipId(ownership1.id)) }
        coVerify(exactly = 1) { enrichmentOwnershipService.getOrEmpty(ShortOwnershipId(ownership1.id)) }

        coVerify(exactly = 1) {
            esOwnershipRepository.bulk(match { batch ->
                batch.map { it.ownershipId }.toSet().containsAll(ownerships.map { it.id.fullId() })
            }, emptyList(), null, WriteRequest.RefreshPolicy.NONE)
        }
        coVerify(exactly = 0) { producer.sendChangeEvents(any()) }
    }

    @Test
    fun `collections synced - db and kafka`() = runBlocking<Unit> {
        val ownership1 = randomUnionOwnership()
        val ownership2 = randomUnionOwnership()
        val ownerships = listOf(ownership1, ownership2)
        val owner = randomString()

        val updated1 = ShortOwnershipConverter.convert(ownership1)
        val updated2 = ShortOwnershipConverter.convert(ownership2)

        coEvery {
            ownershipService.getOwnershipsByOwner(owner, null, any())
        } returns Page(0, null, ownerships)

        coEvery { enrichmentOwnershipService.getOrEmpty(ShortOwnershipId(ownership1.id)) } returns updated1
        coEvery { enrichmentOwnershipService.getOrEmpty(ShortOwnershipId(ownership2.id)) } returns updated2

        val param = """{"blockchain" : "ETHEREUM", "scope" : "EVENT", "owner" : "$owner"}"""
        job.handle(null, param).toList()

        coVerify(exactly = 1) { enrichmentOwnershipService.getOrEmpty(ShortOwnershipId(ownership1.id)) }
        coVerify(exactly = 1) { enrichmentOwnershipService.getOrEmpty(ShortOwnershipId(ownership1.id)) }

        coVerify(exactly = 1) { producer.sendChangeEvents(ownerships.map { it.id }) }
    }
}
