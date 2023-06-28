package com.rarible.protocol.union.worker.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.worker.job.AbstractTrimJob
import com.rarible.protocol.union.worker.job.TrimCollectionMetaJob
import com.rarible.protocol.union.worker.job.TrimItemMetaJob
import org.springframework.stereotype.Component

abstract class AbstractTrimMetaTaskHandler<Entity>(
    private val job: AbstractTrimJob<Entity>,
) : TaskHandler<String> {

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask(""))
    }

    override fun runLongTask(from: String?, param: String) = job.trim(from)
}

@Component
class ItemTrimMetaTaskHandler(
    job: TrimItemMetaJob
) : AbstractTrimMetaTaskHandler<ShortItem>(job) {

    override val type = "TRIM_ITEM_META_TASK"
}

@Component
class CollectionTrimMetaTaskHandler(
    job: TrimCollectionMetaJob
) : AbstractTrimMetaTaskHandler<EnrichmentCollection>(job) {

    override val type = "TRIM_COLLECTION_META_TASK"
}
