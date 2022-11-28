package com.rarible.protocol.union.worker.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.worker.job.PlatformBestSellOrderItemCleanupJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class PlatformBestSellOrderItemCleanupTask(
    private val job: PlatformBestSellOrderItemCleanupJob
) : TaskHandler<String> {

    override val type = "PLATFORM_BEST_SELL_ORDER_ITEM_CLEANUP_TASK"

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask(PlatformDto.X2Y2.name))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val itemId = from?.let { IdParser.parseItemId(it) }?.let { ShortItemId(it) }
        return job.execute(PlatformDto.valueOf(param), itemId).map { it.toDto().fullId() }
    }
}