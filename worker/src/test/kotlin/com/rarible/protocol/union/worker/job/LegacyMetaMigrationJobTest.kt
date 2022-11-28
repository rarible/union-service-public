package com.rarible.protocol.union.worker.job

import com.rarible.core.common.nowMillis
import com.rarible.core.loader.internal.common.LoadTask
import com.rarible.core.loader.internal.common.LoadTaskRepository
import com.rarible.core.test.data.randomInt
import com.rarible.loader.cache.internal.CacheRepository
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloader
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.worker.IntegrationTest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger

@IntegrationTest
class LegacyMetaMigrationJobIt {

    @Autowired
    lateinit var job: LegacyMetaMigrationJob

    @Autowired
    lateinit var taskRepository: LoadTaskRepository

    @Autowired
    lateinit var legacyRepository: CacheRepository

    @Autowired
    lateinit var itemRepository: ItemRepository

    private val now = nowMillis();
    private val ago1m = now.minusSeconds(60)
    private val ago2m = ago1m.minusSeconds(60)
    private val ago3m = ago2m.minusSeconds(60)

    private val loadType = "test"

    @Test
    fun execute() = runBlocking<Unit> {
        val scheduled = LoadTask.Status.Scheduled(ago3m)
        val scheduledItemId = randomEthItemId()
        taskRepository.save(LoadTask("636e13bf1e067c64c83b8950", loadType, scheduledItemId.fullId(), scheduled))

        val retry = LoadTask.Status.WaitsForRetry(ago3m, randomInt(), ago2m, ago1m, "", false)
        val retryItemId = randomEthItemId()
        taskRepository.save(LoadTask("636e13bf888b825c98f7f91c", loadType, retryItemId.fullId(), retry))
        val retryItem = itemRepository.save(randomShortItem(retryItemId).copy(totalStock = BigInteger.TEN))

        // Item should be updated with failed meta
        val failed = LoadTask.Status.Failed(ago3m, randomInt(), ago2m, "")
        val failedItemId = randomEthItemId()
        taskRepository.save(LoadTask("636e13bf888b825c98f7f91e", loadType, failedItemId.fullId(), failed))
        val failedItem = itemRepository.save(randomShortItem(failedItemId).copy(sellers = 2))

        // Item should be created with migrated meta
        val loaded = LoadTask.Status.Loaded(ago3m, randomInt(), ago2m)
        val loadedItemId = randomEthItemId()
        taskRepository.save(LoadTask("636e13bf888b825c98f7f920", loadType, loadedItemId.fullId(), loaded))
        val loadedMeta = randomUnionMeta()
        legacyRepository.save(ItemMetaDownloader.TYPE, loadedItemId.fullId(), loadedMeta, now)

        val updated = job.migrate("636e13bf1e067c64c83b894f", 2).toList()
        val migratedLoadedItem = itemRepository.get(ShortItemId(loadedItemId))!!
        val migratedFailedItem = itemRepository.get(failedItem.id)!!
        val migratedRetryItem = itemRepository.get(retryItem.id)!!

        assertThat(updated).hasSize(2)
        assertThat(migratedLoadedItem.metaEntry!!.data).isEqualTo(loadedMeta)
        assertThat(migratedFailedItem.metaEntry!!.status).isEqualTo(DownloadStatus.FAILED)
        assertThat(migratedRetryItem.metaEntry!!.status).isEqualTo(DownloadStatus.RETRY)
        assertThat(migratedRetryItem.metaEntry!!.retries).isEqualTo(retry.retryAttempts)

        val migratedFailedMetaToCompare = migratedFailedItem.copy(
            lastUpdatedAt = failedItem.lastUpdatedAt,
            version = failedItem.version,
            metaEntry = null
        )

        assertThat(migratedFailedMetaToCompare).isEqualTo(failedItem)

        // Just to ensure sorting works
        val updatedAgain = job.migrate(updated.last(), 1).toList()
        assertThat(updatedAgain).isEmpty()
    }

}