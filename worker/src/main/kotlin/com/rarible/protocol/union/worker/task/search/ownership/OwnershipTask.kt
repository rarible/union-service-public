package com.rarible.protocol.union.worker.task.search.ownership

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.worker.config.OwnershipReindexProperties
import com.rarible.protocol.union.worker.task.search.ParamFactory
import com.rarible.protocol.union.worker.task.search.order.OrderTaskParam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.stereotype.Component

@Component
class OwnershipTask(
    private val properties: OwnershipReindexProperties,
    private val paramFactory: ParamFactory,
    private val reindexService: OwnershipReindexService,
    private val repository: EsOwnershipRepository,
    private val indexService: IndexService,
) : TaskHandler<String> {

    override val type: String
        get() = EsOwnership.ENTITY_DEFINITION.reindexTask

    override suspend fun isAbleToRun(param: String): Boolean {
        val blockchain = paramFactory.parse<OwnershipTaskParam>(param).blockchain
        return properties.isBlockchainActive(blockchain)
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
            )
        }
    }
}
