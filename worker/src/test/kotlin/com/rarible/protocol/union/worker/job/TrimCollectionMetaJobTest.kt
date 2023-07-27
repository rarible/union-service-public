package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.enrichment.meta.item.MetaTrimmer
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.test.data.randomCollectionMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.enrichment.test.data.randomUnionCollectionMeta
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class TrimCollectionMetaJobTest {
    private val metaTrimmer = mockk<MetaTrimmer>()
    private val collectionRepository = mockk<CollectionRepository>()

    private val job = TrimCollectionMetaJob(metaTrimmer, collectionRepository)

    @Test
    fun `trim collection meta - ok`() = runBlocking<Unit> {
        val metaShot = randomCollectionMetaDownloadEntry()
        val metaLong = randomCollectionMetaDownloadEntry()
        val trimmedMeta = randomUnionCollectionMeta()

        val collectionWithShortMeta = randomEnrichmentCollection().copy(metaEntry = metaShot)
        val collectionWithLongMeta = randomEnrichmentCollection().copy(metaEntry = metaLong)

        every { collectionRepository.findAll(null) } returns flowOf(
            collectionWithShortMeta,
            collectionWithLongMeta
        )
        every { metaTrimmer.trim(metaShot.data) } returns metaShot.data
        every { metaTrimmer.trim(metaLong.data) } returns trimmedMeta
        coEvery { collectionRepository.save(any()) } returns mockk()

        job.trim(null).toList()

        coVerify {
            collectionRepository.save(withArg {
                Assertions.assertThat(it.id).isEqualTo(collectionWithLongMeta.id)
                Assertions.assertThat(it.metaEntry?.data).isEqualTo(trimmedMeta)
            })
        }

        coVerify(exactly = 1) { collectionRepository.save(any()) }
    }
}
