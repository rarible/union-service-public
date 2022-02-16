package com.rarible.protocol.union.listener.job.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.listener.job.OpenSeaOrderOwnershipCleanupJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class OpenSeaOrderOwnershipCleanupTask(
    private val job: OpenSeaOrderOwnershipCleanupJob
) : TaskHandler<String> {

    override val type = "OPEN_SEA_ORDER_OWNERSHIP_CLEANUP_TASK"

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask(""))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val ownershipId = from?.let { OwnershipIdParser.parseFull(it) }?.let { ShortOwnershipId(it) }
        return job.execute(ownershipId).map { it.toDto().fullId() }
    }
}