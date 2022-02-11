package com.rarible.protocol.union.listener.job.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.listener.job.ReconciliationItemJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class ReconciliationOpenSeaItemTaskHandler(
    private val job: ReconciliationItemJob,
) : TaskHandler<String> {

    override val type = "RECONCILE_ITEMS_WITH_OPEN_SEA_ORDER"

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask(PlatformDto.OPEN_SEA.name))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val itemId = from?.let { IdParser.parseItemId(it) }?.let { ShortItemId(it) }
        val platform = PlatformDto.valueOf(param)
        return job.reconcileForPlatform(platform, itemId).map { it.toDto().fullId() }
    }
}