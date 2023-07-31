package com.rarible.protocol.union.meta.loader.executor

import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.configuration.MetaTrimmingProperties
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaDownloader
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadNotifier
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.repository.CollectionMetaRepository
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentBlacklistService
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.enrichment.test.data.randomUnionCollectionMeta
import com.rarible.protocol.union.meta.loader.test.AbstractIntegrationTest
import com.rarible.protocol.union.meta.loader.test.IntegrationTest
import com.rarible.protocol.union.meta.loader.test.data.randomTask
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class CollectionDownloadExecutorIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var repository: CollectionMetaRepository

    @Autowired
    lateinit var trimmingProperties: MetaTrimmingProperties

    @Autowired
    lateinit var collectionRepository: CollectionRepository

    @Autowired
    lateinit var metrics: DownloadExecutorMetrics

    @Autowired
    lateinit var enrichmentBlacklistService: EnrichmentBlacklistService

    val downloader: CollectionMetaDownloader = mockk() { every { type } returns "collection" }
    val notifier: DownloadNotifier<UnionCollectionMeta> = mockk { coEvery { notify(any()) } returns Unit }
    val pool = DownloadPool(2, "item-meta-test")
    val maxRetries = 2

    lateinit var downloadExecutor: DownloadExecutor<UnionCollectionMeta>

    @BeforeEach
    fun beforeEach() {
        downloadExecutor = CollectionDownloadExecutor(
            enrichmentBlacklistService,
            repository,
            downloader,
            notifier,
            pool,
            metrics,
            maxRetries,
        )
    }

    @Test
    fun `initial task - success, trim`() = runBlocking<Unit> {
        val collectionId = CollectionIdDto(BlockchainDto.ETHEREUM, "0x0")
        val task = randomTask(collectionId.fullId())
        createCollection(collectionId, null)
        collectionRepository.get(EnrichmentCollectionId(collectionId))!!

        mockGetMeta(
            collectionId.fullId(),
            randomUnionCollectionMeta().copy(
                name = IntRange(0, trimmingProperties.nameLength * 2).joinToString { "x" },
                description = IntRange(0, trimmingProperties.descriptionLength * 2).joinToString { "x" }
            )
        )

        downloadExecutor.execute(listOf(task))

        val saved = repository.get(collectionId.fullId())!!

        assertThat(saved.data?.name?.length)
            .isEqualTo(trimmingProperties.nameLength + trimmingProperties.suffix.length)

        assertThat(saved.data?.description?.length)
            .isEqualTo(trimmingProperties.descriptionLength + trimmingProperties.suffix.length)
    }

    private suspend fun createCollection(collectionId: CollectionIdDto, metaEntry: DownloadEntry<UnionMeta>?): EnrichmentCollection =
        collectionRepository.save(randomEnrichmentCollection(collectionId))

    private fun mockGetMeta(collectionId: String, meta: UnionCollectionMeta) {
        coEvery { downloader.download(collectionId) } returns meta
    }
}
