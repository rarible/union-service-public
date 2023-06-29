package com.rarible.protocol.union.worker.job.sync

import com.rarible.protocol.union.core.producer.UnionInternalCollectionEventProducer
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.enrichment.converter.CollectionDtoConverter
import com.rarible.protocol.union.enrichment.converter.EnrichmentCollectionConverter
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaService
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import com.rarible.protocol.union.enrichment.test.data.randomCollectionMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomUnionCollection
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
class SyncCollectionJobTest {

    @MockK
    lateinit var enrichmentCollectionService: EnrichmentCollectionService

    @MockK
    lateinit var collectionMetaService: CollectionMetaService

    @MockK
    lateinit var collectionService: CollectionService

    @MockK
    lateinit var collectionServiceRouter: BlockchainRouter<CollectionService>

    @MockK
    lateinit var esCollectionRepository: EsCollectionRepository

    @MockK
    lateinit var producer: UnionInternalCollectionEventProducer

    @MockK
    lateinit var esRateLimiter: EsRateLimiter

    @InjectMockKs
    lateinit var job: SyncCollectionJob

    @BeforeEach
    fun beforeEach() {
        clearMocks(collectionService, collectionServiceRouter, esRateLimiter, producer)
        coEvery { esRateLimiter.waitIfNecessary(any()) } returns Unit
        every { collectionServiceRouter.getService(BlockchainDto.ETHEREUM) } returns collectionService
        coEvery { esCollectionRepository.bulk(any(), any(), any(), WriteRequest.RefreshPolicy.NONE) } returns Unit
        coEvery { producer.sendChangeEvents(any()) } returns Unit
    }

    @Test
    fun `collections synced - db and es`() = runBlocking<Unit> {
        val collection1 = randomUnionCollection()
        val collection2 = randomUnionCollection()
        val collections = listOf(collection1, collection2)

        val updated1 = EnrichmentCollectionConverter.convert(collection1)
        val updated2 = EnrichmentCollectionConverter.convert(collection2).copy(
            metaEntry = randomCollectionMetaDownloadEntry()
        )

        val enrichmentCollections = listOf(updated1, updated2)
        val dto = enrichmentCollections.map { CollectionDtoConverter.convert(it) }

        coEvery {
            collectionService.getAllCollections(null, any())
        } returns Page(0, null, collections)

        coEvery { enrichmentCollectionService.update(collection1, false) } returns updated1
        coEvery { enrichmentCollectionService.update(collection2, false) } returns updated2
        coEvery { collectionMetaService.schedule(collection1.id, CollectionMetaPipeline.SYNC, false) } returns Unit

        coEvery { enrichmentCollectionService.enrich(enrichmentCollections, CollectionMetaPipeline.SYNC) } returns dto

        val param = """{"blockchain" : "ETHEREUM", "scope" : "ES"}"""
        job.handle(null, param).toList()

        coVerify(exactly = 1) { enrichmentCollectionService.update(collection1, false) }
        coVerify(exactly = 1) { enrichmentCollectionService.update(collection2, false) }
        // Only collection without meta should be triggered
        coVerify(exactly = 1) { collectionMetaService.schedule(any(), any(), any()) }

        coVerify(exactly = 1) {
            esCollectionRepository.bulk(match { batch ->
                batch.map { it.collectionId }.toSet().containsAll(enrichmentCollections.map { it.id.toDto().fullId() })
            }, emptyList(), null, WriteRequest.RefreshPolicy.NONE)
        }
        coVerify(exactly = 0) { producer.sendChangeEvents(any()) }
    }

    @Test
    fun `collections synced - db and kafka`() = runBlocking<Unit> {
        val collection1 = randomUnionCollection()
        val collection2 = randomUnionCollection()
        val collections = listOf(collection1, collection2)

        val updated1 = EnrichmentCollectionConverter.convert(collection1).copy(
            metaEntry = randomCollectionMetaDownloadEntry()
        )
        val updated2 = EnrichmentCollectionConverter.convert(collection2).copy(
            metaEntry = randomCollectionMetaDownloadEntry()
        )

        coEvery {
            collectionService.getAllCollections(null, any())
        } returns Page(0, null, collections)

        coEvery { enrichmentCollectionService.update(collection1, false) } returns updated1
        coEvery { enrichmentCollectionService.update(collection2, false) } returns updated2

        val param = """{"blockchain" : "ETHEREUM", "scope" : "EVENT"}"""
        job.handle(null, param).toList()

        coVerify(exactly = 1) { enrichmentCollectionService.update(collection1, false) }
        coVerify(exactly = 1) { enrichmentCollectionService.update(collection2, false) }
        coVerify(exactly = 0) { collectionMetaService.schedule(any(), any(), any()) }

        coVerify(exactly = 1) { producer.sendChangeEvents(listOf(collection1.id, collection2.id)) }
    }

}