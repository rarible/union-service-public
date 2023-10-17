package com.rarible.protocol.union.worker.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.core.service.router.ActiveBlockchainProvider
import com.rarible.protocol.union.worker.job.ReconciliationLastSaleJob
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component

@Component
class ReconciliationLastSaleTaskHandler(
    private val job: ReconciliationLastSaleJob,
    private val activeBlockchainProvider: ActiveBlockchainProvider
) : TaskHandler<String> {

    override val type = "ENRICHMENT_LAST_SALE_RECONCILIATION_JOB"

    override fun getAutorunParams(): List<RunTask> {
        return activeBlockchainProvider.blockchains.map { RunTask(it.name) }
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        return job.handle(from, param)
    }
}
