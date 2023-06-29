package com.rarible.protocol.union.worker.task

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.worker.job.sync.SyncActivityJob
import org.springframework.stereotype.Component

@Component
class SyncActivityTaskHandler(
    private val job: SyncActivityJob,
    private val activeBlockchains: List<BlockchainDto>
) : TaskHandler<String> {

    override val type = "SYNC_ACTIVITY_TASK"

    /*
    override fun getAutorunParams(): List<RunTask> {
        return activeBlockchains.map { RunTask(it.name) }
    }
    */

    override fun runLongTask(from: String?, param: String) = job.sync(param, from)
}