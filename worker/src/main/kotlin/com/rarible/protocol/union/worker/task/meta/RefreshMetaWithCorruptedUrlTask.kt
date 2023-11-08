package com.rarible.protocol.union.worker.task.meta

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.enrichment.download.DownloadTaskSource
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaService
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.regex.Pattern

// Job to trigger refresh for Items with meta had been broken but fixed later (via setBaseUri method)
@Component
class RefreshMetaWithCorruptedUrlTask(
    private val job: RefreshMetaWithCorruptedUrlJob
) : TaskHandler<String> {

    override val type = "REFRESH_META_WITH_CORRUPTED_URL_TASK"

    override fun runLongTask(from: String?, param: String) = job.handle(from, param)

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask("https://rarible.mypinata.com.*"))
    }
}

@Component
class RefreshMetaWithCorruptedUrlJob(
    private val itemRepository: ItemRepository,
    private val itemMetaService: ItemMetaService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun handle(continuation: String?, param: String): Flow<String> {
        val from = continuation?.let { ShortItemId.of(it) }
        val pattern = Pattern.compile(param)
        return itemRepository.findAll(from).map {
            checkItem(it, pattern)
            it.id.toString()
        }
    }

    private suspend fun checkItem(item: ShortItem, pattern: Pattern) {
        val corruptedUrl = item.metaEntry?.data?.content?.find {
            pattern.matcher(it.url).matches()
        }?.url ?: return

        logger.info("Found Item ${item.id} with corrupted URL: $corruptedUrl, scheduling refresh")
        itemMetaService.schedule(
            itemId = item.id.toDto(),
            pipeline = ItemMetaPipeline.SYNC,
            force = true,
            source = DownloadTaskSource.INTERNAL,
            priority = 0
        )
    }
}
