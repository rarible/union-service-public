package com.rarible.protocol.union.worker.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.worker.job.ReconciliationPoolOrderJob
import org.springframework.stereotype.Component

@Component
class ReconciliationPoolOrderTaskHandler(
    private val job: ReconciliationPoolOrderJob,
    private val activeBlockchains: List<BlockchainDto>,
    private val ff: FeatureFlagsProperties
) : TaskHandler<String> {

    override val type = "ENRICHMENT_RECONCILIATION_POOL_ORDER_JOB"

    override suspend fun isAbleToRun(param: String): Boolean {
        return ff.enablePoolOrders
    }

    override fun getAutorunParams(): List<RunTask> {
        return activeBlockchains.map { RunTask(it.name) }
    }

    override fun runLongTask(from: String?, param: String) = job.handle(from, param)
}