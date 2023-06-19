package com.rarible.protocol.union.worker.task

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.worker.job.BestBidCleanupJob
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component

@Component
class BestBidCleanupTaskHandler(
    private val job: BestBidCleanupJob
) : TaskHandler<String> {

    override val type = "ENRICHMENT_BEST_BID_CLEANUP_JOB"

    override fun runLongTask(from: String?, param: String): Flow<String> {
        return job.reconcile(from, IdParser.parseBlockchain(param))
    }
}