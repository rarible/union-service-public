package com.rarible.protocol.union.listener.job.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.listener.job.ReconciliationPoolOrderJob
import kotlinx.coroutines.flow.Flow
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

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val blockchain = IdParser.parseBlockchain(param)
        return job.reconcile(from, blockchain)
    }
}