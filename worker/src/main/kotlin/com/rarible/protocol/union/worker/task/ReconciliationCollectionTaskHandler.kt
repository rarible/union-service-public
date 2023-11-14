package com.rarible.protocol.union.worker.task

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.worker.job.ReconciliationCollectionJob
import org.springframework.stereotype.Component

@Component
class ReconciliationCollectionTaskHandler(
    private val job: ReconciliationCollectionJob
) : TaskHandler<String> {

    override val type = "ENRICHMENT_RECONCILIATION_COLLECTION_JOB"

    override fun runLongTask(from: String?, param: String) = job.handle(from, param)
}
