package com.rarible.protocol.union.listener.job.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.parser.ItemIdParser
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.listener.job.OpenSeaOrderCleanupJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class OpenSeaOrderCleanupTask(
    private val job: OpenSeaOrderCleanupJob
) : TaskHandler<String> {

    override val type = "OPEN_SEA_ORDER_CLEANUP_TASK"

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask(""))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val itemId = from?.let { ItemIdParser.parseFull(it) }?.let { ShortItemId(it) }
        return job.execute(itemId).map { it.toDto().fullId() }
    }
}