package com.rarible.protocol.union.worker.task.search.ownership

import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.core.model.elastic.EsOwnership
import com.rarible.protocol.union.core.service.router.ActiveBlockchainProvider
import com.rarible.protocol.union.core.task.OwnershipTaskParam
import com.rarible.protocol.union.worker.config.OwnershipReindexProperties
import com.rarible.protocol.union.worker.task.search.ParamFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component

@Component
@Deprecated("Replace with SyncOwnershipTask")
class OwnershipTask(
    private val properties: OwnershipReindexProperties,
    private val paramFactory: ParamFactory,
    private val reindexService: OwnershipReindexService,
    private val taskRepository: TaskRepository,
    private val activeBlockchainProvider: ActiveBlockchainProvider,
) : TaskHandler<String> {

    override val type: String
        get() = EsOwnership.ENTITY_DEFINITION.reindexTask

    override suspend fun isAbleToRun(param: String): Boolean {
        val blockchain = paramFactory.parse<OwnershipTaskParam>(param).blockchain
        return properties.enabled && activeBlockchainProvider.isActive(blockchain)
    }

    override fun runLongTask(from: String?, param: String): Flow<String> = when (from) {
        "" -> emptyFlow()
        else -> {
            val taskParam = paramFactory.parse<OwnershipTaskParam>(param)
            reindexService.reindex(
                blockchain = taskParam.blockchain,
                target = taskParam.target,
                index = taskParam.index,
                cursor = from,
                from = taskParam.from,
                to = taskParam.to,
            )
                .takeWhile { taskRepository.findByTypeAndParam(type, param).awaitSingleOrNull()?.running ?: false }
        }
    }
}
