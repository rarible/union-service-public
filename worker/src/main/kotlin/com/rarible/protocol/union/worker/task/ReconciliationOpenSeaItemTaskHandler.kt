package com.rarible.protocol.union.worker.task

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.worker.job.ReconciliationItemJob
import org.springframework.stereotype.Component

@Component
class ReconciliationOpenSeaItemTaskHandler(
    private val job: ReconciliationItemJob,
) : TaskHandler<String> {

    override val type = "RECONCILE_ITEMS_WITH_OPEN_SEA_ORDER"
    override fun runLongTask(from: String?, param: String) = job.handle(from, param)
}
