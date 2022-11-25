package com.rarible.protocol.union.worker.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.worker.job.ReconciliationAuctionsJob
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component

@Component
class ReconciliationAuctionTaskHandler(
    private val job: ReconciliationAuctionsJob,
    private val activeBlockchains: List<BlockchainDto>
) : TaskHandler<String> {

    override val type = "ENRICHMENT_RECONCILIATION_AUCTIONS_JOB"

    override fun getAutorunParams(): List<RunTask> {
        // TODO Enable when all blockchains will support auctions
        //blockchains.map { RunTask(it.name) }
        return listOf(
            RunTask(BlockchainDto.ETHEREUM.name)
        )
        return activeBlockchains.map { RunTask(it.name) }
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val blockchain = IdParser.parseBlockchain(param)
        return job.reconcile(from, blockchain)
    }
}
