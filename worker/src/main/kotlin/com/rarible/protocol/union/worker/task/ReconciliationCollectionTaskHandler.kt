package com.rarible.protocol.union.worker.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.worker.job.ReconciliationCollectionJob
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component

@Component
class ReconciliationCollectionTaskHandler(
    private val job: ReconciliationCollectionJob,
    private val activeBlockchains: List<BlockchainDto>
) : TaskHandler<String> {

    override val type = "ENRICHMENT_RECONCILIATION_COLLECTION_JOB"

    override fun getAutorunParams(): List<RunTask> {
        return activeBlockchains.map { RunTask(it.name) }
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val blockchain = IdParser.parseBlockchain(param)
        return job.reconcile(from, blockchain)
    }
}