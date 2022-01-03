package com.rarible.protocol.union.listener.job

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.parser.ItemIdParser
import com.rarible.protocol.union.enrichment.model.ShortItemId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component

@Component
class ReconcileItemsWithOpenSeaOrderTaskHandler(
    private val job: RefreshItemJob,
) : TaskHandler<String> {

    override val type = "RECONCILE_ITEMS_WITH_OPEN_SEA_ORDER"

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask(PlatformDto.OPEN_SEA.name))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val itemId = from?.let { ItemIdParser.parseFull(it)  }?.let { ShortItemId(it) }
        return job.refreshForPlatform(param, itemId).map { it.toDto().fullId() }
    }
}