package com.rarible.protocol.union.worker.task

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.worker.job.PlatformBestSellOrderItemCleanupJob
import org.springframework.stereotype.Component

@Component
class PlatformBestSellOrderItemCleanupTask(
    private val job: PlatformBestSellOrderItemCleanupJob
) : TaskHandler<String> {

    override val type = "PLATFORM_BEST_SELL_ORDER_ITEM_CLEANUP_TASK"
    override fun runLongTask(from: String?, param: String) = job.handle(from, param)
}
