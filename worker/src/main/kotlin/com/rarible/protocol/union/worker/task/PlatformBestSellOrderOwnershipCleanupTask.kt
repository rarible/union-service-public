package com.rarible.protocol.union.worker.task

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.worker.job.PlatformBestSellOrderOwnershipCleanupJob
import org.springframework.stereotype.Component

@Component
class PlatformBestSellOrderOwnershipCleanupTask(
    private val job: PlatformBestSellOrderOwnershipCleanupJob
) : TaskHandler<String> {

    override val type = "PLATFORM_BEST_SELL_ORDER_OWNERSHIP_CLEANUP_TASK"
    override fun runLongTask(from: String?, param: String) = job.handle(from, param)
}
