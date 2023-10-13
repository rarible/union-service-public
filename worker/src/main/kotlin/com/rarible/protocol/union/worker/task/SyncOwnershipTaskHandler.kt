package com.rarible.protocol.union.worker.task

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.worker.job.sync.SyncOwnershipJob
import org.springframework.stereotype.Component

@Component
class SyncOwnershipTaskHandler(
    private val job: SyncOwnershipJob
) : TaskHandler<String> {

    override val type = "SYNC_OWNERSHIP_TASK"

    override fun runLongTask(from: String?, param: String) = job.handle(from, param)
}
