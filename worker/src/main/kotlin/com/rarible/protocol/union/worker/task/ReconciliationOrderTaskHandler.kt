package com.rarible.protocol.union.worker.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.worker.job.ReconciliationOrderJob
import org.springframework.stereotype.Component

@Component
class ReconciliationOrderTaskHandler(
    private val job: ReconciliationOrderJob,
    private val activeBlockchains: List<BlockchainDto>
) : TaskHandler<String> {

    override val type = "ENRICHMENT_RECONCILIATION_JOB"

    override fun getAutorunParams(): List<RunTask> {
        return activeBlockchains.map { RunTask(it.name) }
    }

    override fun runLongTask(from: String?, param: String) = job.handle(from, param)
}
