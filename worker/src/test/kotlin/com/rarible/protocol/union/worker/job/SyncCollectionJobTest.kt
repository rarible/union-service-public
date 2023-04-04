package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.enrichment.converter.EnrichmentCollectionConverter
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaService
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import com.rarible.protocol.union.enrichment.test.data.randomCollectionMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomUnionCollection
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SyncCollectionJobTest {

    private val enrichmentCollectionService: EnrichmentCollectionService = mockk()
    private val collectionMetaService: CollectionMetaService = mockk()
    private val collectionService: CollectionService = mockk()
    private val collectionServiceRouter: BlockchainRouter<CollectionService> = mockk() {
        every { getService(BlockchainDto.ETHEREUM) } returns collectionService
    }

    val job = SyncCollectionJob(
        collectionServiceRouter,
        enrichmentCollectionService,
        collectionMetaService
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(collectionService)
    }

    @Test
    fun `collections synced`() = runBlocking<Unit> {
        val collection1 = randomUnionCollection()
        val collection2 = randomUnionCollection()

        val updated1 = EnrichmentCollectionConverter.convert(collection1)
        val updated2 = EnrichmentCollectionConverter.convert(collection2).copy(
            metaEntry = randomCollectionMetaDownloadEntry()
        )

        coEvery {
            collectionService.getAllCollections(null, any())
        } returns Page(0, null, listOf(collection1, collection2))

        coEvery { enrichmentCollectionService.update(collection1, false) } returns updated1
        coEvery { enrichmentCollectionService.update(collection2, false) } returns updated2

        coEvery { collectionMetaService.schedule(collection1.id, CollectionMetaPipeline.SYNC, false) } returns Unit

        job.reconcile(null, BlockchainDto.ETHEREUM).toList()

        coVerify(exactly = 1) { enrichmentCollectionService.update(collection1, false) }
        coVerify(exactly = 1) { enrichmentCollectionService.update(collection2, false) }
        // Only collection without meta should be triggered
        coVerify(exactly = 1) { collectionMetaService.schedule(any(), any(), any()) }
    }

}