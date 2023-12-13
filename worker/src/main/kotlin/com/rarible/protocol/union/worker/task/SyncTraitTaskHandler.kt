package com.rarible.protocol.union.worker.task

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.core.task.SyncTraitJobParam
import com.rarible.protocol.union.worker.job.sync.SyncTraitJob
import org.springframework.stereotype.Component

@Component
class SyncTraitTaskHandler(
    private val job: SyncTraitJob
) : TaskHandler<String> {

    override val type = SyncTraitJobParam.TYPE

    override fun runLongTask(from: String?, param: String) = job.handle(from, param)
}
