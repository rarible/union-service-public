package com.rarible.protocol.union.listener.job.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.listener.job.PlatformBestSellOrderOwnershipCleanupJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class PlatformBestSellOrderOwnershipCleanupTask(
    private val job: PlatformBestSellOrderOwnershipCleanupJob
) : TaskHandler<String> {

    override val type = "PLATFORM_BEST_SELL_ORDER_OWNERSHIP_CLEANUP_TASK"

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask(PlatformDto.X2Y2.name))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val ownershipId = from?.let { OwnershipIdParser.parseFull(it) }?.let { ShortOwnershipId(it) }
        return job.execute(PlatformDto.valueOf(param), ownershipId).map { it.toDto().fullId() }
    }
}