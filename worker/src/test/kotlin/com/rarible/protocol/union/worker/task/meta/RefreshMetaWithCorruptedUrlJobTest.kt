package com.rarible.protocol.union.worker.task.meta

import com.rarible.protocol.union.enrichment.download.DownloadTaskSource
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaService
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionContent
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class RefreshMetaWithCorruptedUrlJobTest {

    @MockK
    lateinit var itemRepository: ItemRepository

    @MockK
    lateinit var itemMetaService: ItemMetaService

    @InjectMockKs
    lateinit var job: RefreshMetaWithCorruptedUrlJob

    @BeforeEach
    fun beforeEach() {
        coEvery {
            itemMetaService.schedule(
                itemId = any(),
                pipeline = ItemMetaPipeline.SYNC,
                force = true,
                source = DownloadTaskSource.INTERNAL,
                priority = 0
            )
        } returns Unit
    }

    @Test
    fun execute() = runBlocking<Unit> {
        val pattern = "https://rarible.mypinata.com.*"

        val corrupted = randomUnionMeta(
            content = listOf(randomUnionContent().copy(url = "https://rarible.mypinata.com/abc"))
        )

        val valid = randomUnionMeta(
            content = listOf(randomUnionContent())
        )

        // No content
        val item1 = randomShortItem().copy(metaEntry = randomItemMetaDownloadEntry())
        // No meta entry
        val item2 = randomShortItem()
        // Corrupted
        val item3 = randomShortItem().copy(metaEntry = randomItemMetaDownloadEntry(data = corrupted))
        // Valid url
        val item4 = randomShortItem().copy(metaEntry = randomItemMetaDownloadEntry(data = valid))

        every { itemRepository.findAll(null) } returns listOf(
            item1, item2, item3, item4
        ).asFlow()

        job.handle(null, pattern).collect()

        coVerify(exactly = 1) {
            itemMetaService.schedule(
                itemId = item3.id.toDto(),
                pipeline = any(),
                force = any(),
                source = any(),
                priority = any()
            )
        }
        coVerify(exactly = 1) {
            itemMetaService.schedule(
                itemId = any(),
                pipeline = any(),
                force = any(),
                source = any(),
                priority = any()
            )
        }
    }
}
