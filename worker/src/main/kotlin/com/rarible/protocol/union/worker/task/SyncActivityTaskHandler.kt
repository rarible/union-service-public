package com.rarible.protocol.union.worker.task

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.worker.job.sync.SyncActivityJob
import org.springframework.stereotype.Component

@Component
class SyncActivityTaskHandler(
    private val job: SyncActivityJob
) : TaskHandler<String> {

    override val type = "SYNC_ACTIVITY_TASK"

    override fun runLongTask(from: String?, param: String) = job.handle(from, param)
}
