package com.rarible.protocol.union.listener.job

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.BlockchainDto
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component

@Component
class ReconciliationTaskHandler(
    private val job: ReconciliationJob,
    private val blockchains: List<BlockchainDto>
) : TaskHandler<String> {

    override val type = "ENRICHMENT_RECONCILIATION_JOB"

    override fun getAutorunParams(): List<RunTask> {
        // TODO Enable before release
        //blockchains.map { RunTask(it.name) }
        return listOf(
            RunTask(BlockchainDto.ETHEREUM.name)
        )
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val blockchain = BlockchainDto.valueOf(param)
        return job.reconcile(from, blockchain)
    }
}