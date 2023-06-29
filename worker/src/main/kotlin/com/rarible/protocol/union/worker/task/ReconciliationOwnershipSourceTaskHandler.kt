package com.rarible.protocol.union.worker.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.worker.job.ReconciliationOwnershipSourceJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.stereotype.Component

@Component
class ReconciliationOwnershipSourceTaskHandler(
    private val job: ReconciliationOwnershipSourceJob,
    private val activeBlockchains: List<BlockchainDto>,
    private val ff: FeatureFlagsProperties
) : TaskHandler<String> {

    override val type = "ENRICHMENT_OWNERSHIP_SOURCE_RECONCILIATION_JOB"

    override fun getAutorunParams(): List<RunTask> {
        return activeBlockchains.map { RunTask(it.name) }
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        if (!ff.enableOwnershipSourceEnrichment) {
            return emptyFlow()
        }
        return job.handle(from, param)
    }
}