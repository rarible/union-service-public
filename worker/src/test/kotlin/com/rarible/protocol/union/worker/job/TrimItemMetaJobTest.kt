package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.enrichment.meta.item.MetaTrimmer
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TrimItemMetaJobTest {
    private val metaTrimmer = mockk<MetaTrimmer>()
    private val itemRepository = mockk<ItemRepository>()

    private val job = TrimItemMetaJob(metaTrimmer, itemRepository)

    @Test
    fun `trim item meta - ok`() = runBlocking<Unit> {
        val metaShot = randomItemMetaDownloadEntry()
        val metaLong = randomItemMetaDownloadEntry()
        val trimmedMeta = randomUnionMeta()

        val itemWithShortMeta = randomShortItem().copy(metaEntry = metaShot)
        val itemWithLongMeta = randomShortItem().copy(metaEntry = metaLong)

        every { itemRepository.findAll(null) } returns flowOf(itemWithShortMeta, itemWithLongMeta)

        every { metaTrimmer.trim(metaShot.data) } returns metaShot.data
        every { metaTrimmer.trim(metaLong.data) } returns trimmedMeta
        coEvery { itemRepository.save(any()) } returns mockk()

        job.trim(null).toList()

        coVerify { itemRepository.save(withArg {
            assertThat(it.id).isEqualTo(itemWithLongMeta.id)
            assertThat(it.metaEntry?.data).isEqualTo(trimmedMeta)
        }) }

        coVerify(exactly = 1) { itemRepository.save(any()) }
    }
}

