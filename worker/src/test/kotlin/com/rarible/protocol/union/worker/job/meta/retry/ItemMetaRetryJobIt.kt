package com.rarible.protocol.union.worker.job.meta.retry

import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import com.rarible.protocol.union.enrichment.download.DownloadEntry
import com.rarible.protocol.union.enrichment.download.DownloadStatus
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaService
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.worker.IntegrationTest
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit

@IntegrationTest
class ItemMetaRetryJobIt {

    @Autowired
    lateinit var metaProperties: UnionMetaProperties

    @Autowired
    lateinit var itemRepository: ItemRepository

    @Autowired
    lateinit var blockchainRouter: BlockchainRouter<ItemService>

    private val metaService = mockk<ItemMetaService>(relaxed = true)

    @Test
    fun execute() = runBlocking {
        val handler = ItemMetaRetryJobHandler(itemRepository, metaProperties, metaService, blockchainRouter)
        val now = Instant.now()
        val idNow = randomEthItemId()
        val id2h = randomEthItemId()
        val id25h = randomEthItemId()

        val partialNow = randomEthItemId()
        val partial2h = randomEthItemId()
        val partial25h = randomEthItemId()

        itemRepository.save(
            randomShortItem(idNow).copy(
                metaEntry = DownloadEntry(
                    id = "now",
                    status = DownloadStatus.RETRY,
                    retriedAt = now
                )
            )
        )

        itemRepository.save(
            randomShortItem(id2h).copy(
                metaEntry = DownloadEntry(
                    id = "-2h",
                    retries = 0,
                    status = DownloadStatus.RETRY,
                    retriedAt = now.minus(2, ChronoUnit.HOURS)
                )
            )
        )

        itemRepository.save(
            randomShortItem(id25h).copy(
                metaEntry = DownloadEntry(
                    id = "-25h",
                    status = DownloadStatus.RETRY,
                    retries = 2,
                    retriedAt = now.minus(25, ChronoUnit.HOURS)
                )
            )
        )

        itemRepository.save(
            randomShortItem(partialNow).copy(
                metaEntry = DownloadEntry(
                    id = "now",
                    status = DownloadStatus.RETRY_PARTIAL,
                    retriedAt = now
                )
            )
        )

        itemRepository.save(
            randomShortItem(partial2h).copy(
                metaEntry = DownloadEntry(
                    id = "-2h",
                    retries = 0,
                    status = DownloadStatus.RETRY_PARTIAL,
                    retriedAt = now.minus(2, ChronoUnit.HOURS)
                )
            )
        )

        itemRepository.save(
            randomShortItem(partial25h).copy(
                metaEntry = DownloadEntry(
                    id = "-25h",
                    status = DownloadStatus.RETRY_PARTIAL,
                    retries = 2,
                    retriedAt = now.minus(25, ChronoUnit.HOURS)
                )
            )
        )

        handler.handle()

        coVerify(exactly = 0) { metaService.schedule(idNow, ItemMetaPipeline.RETRY, true) }
        coVerify(exactly = 1) { metaService.schedule(id2h, ItemMetaPipeline.RETRY, true) }
        coVerify(exactly = 0) { metaService.schedule(id25h, ItemMetaPipeline.RETRY, true) }

        coVerify(exactly = 0) { metaService.schedule(partialNow, ItemMetaPipeline.RETRY_PARTIAL, true) }
        coVerify(exactly = 1) { metaService.schedule(partial2h, ItemMetaPipeline.RETRY_PARTIAL, true) }
        coVerify(exactly = 0) { metaService.schedule(id25h, ItemMetaPipeline.RETRY_PARTIAL, true) }

        assertEquals(0, itemRepository.get(ShortItemId(idNow))?.metaEntry?.retries)
        assertEquals(1, itemRepository.get(ShortItemId(id2h))?.metaEntry?.retries)
        assertEquals(2, itemRepository.get(ShortItemId(id25h))?.metaEntry?.retries)

        assertEquals(0, itemRepository.get(ShortItemId(partialNow))?.metaEntry?.retries)
        assertEquals(1, itemRepository.get(ShortItemId(partial2h))?.metaEntry?.retries)
        assertEquals(2, itemRepository.get(ShortItemId(partial25h))?.metaEntry?.retries)
    }
}
