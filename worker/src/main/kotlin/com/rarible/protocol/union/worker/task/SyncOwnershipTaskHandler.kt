package com.rarible.protocol.union.worker.task

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.worker.job.sync.SyncOwnershipJob
import org.springframework.stereotype.Component

@Component
class SyncOwnershipTaskHandler(
    private val job: SyncOwnershipJob,
    private val activeBlockchains: List<BlockchainDto>
) : TaskHandler<String> {

    override val type = "SYNC_OWNERSHIP_TASK"

    // TODO not needed ATM
    /*
    override fun getAutorunParams(): List<RunTask> {
        return activeBlockchains.map { RunTask(it.name) }
    }
    */

    override fun runLongTask(from: String?, param: String) = job.sync(param, from)

}