package com.rarible.protocol.union.worker.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.core.service.router.ActiveBlockchainProvider
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.worker.job.ReconciliationAuctionsJob
import org.springframework.stereotype.Component

@Component
class ReconciliationAuctionTaskHandler(
    private val job: ReconciliationAuctionsJob,
    private val activeBlockchainProvider: ActiveBlockchainProvider
) : TaskHandler<String> {

    override val type = "ENRICHMENT_RECONCILIATION_AUCTIONS_JOB"

    override fun getAutorunParams(): List<RunTask> {
        // TODO Enable when all blockchains will support auctions
        // blockchains.map { RunTask(it.name) }
        return listOf(
            RunTask(BlockchainDto.ETHEREUM.name)
        )
        return activeBlockchainProvider.blockchains.map { RunTask(it.name) }
    }

    override fun runLongTask(from: String?, param: String) = job.handle(from, param)
}
