package com.rarible.protocol.union.worker.task

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.worker.job.sync.SyncCollectionJob
import org.springframework.stereotype.Component

@Component
class SyncCollectionTaskHandler(
    private val job: SyncCollectionJob,
    private val activeBlockchains: List<BlockchainDto>
) : TaskHandler<String> {

    override val type = "SYNC_COLLECTION_TASK"

    override fun runLongTask(from: String?, param: String) = job.handle(from, param)
}
