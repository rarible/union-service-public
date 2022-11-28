package com.rarible.protocol.union.worker.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.worker.job.ReconciliationCorruptedItemJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class ReconciliationCorruptedItemTaskHandler(
    private val job: ReconciliationCorruptedItemJob,
    private val activeBlockchains: List<BlockchainDto>
) : TaskHandler<String> {

    override val type = "ENRICHMENT_RECONCILIATION_CORRUPTED_ITEMS"

    override fun getAutorunParams(): List<RunTask> {
        return activeBlockchains.map { RunTask(it.name) }
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val itemId = from?.let { IdParser.parseItemId(it) }?.let { ShortItemId(it) }
        val blockchain = IdParser.parseBlockchain(param)
        return job.reconcileCorruptedItems(itemId, blockchain).map { it.toDto().fullId() }
    }
}