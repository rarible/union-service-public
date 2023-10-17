package com.rarible.protocol.union.worker.task.search.activity

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.core.service.router.ActiveBlockchainProvider
import com.rarible.protocol.union.core.task.RemoveRevertedActivityTaskParam
import com.rarible.protocol.union.worker.config.ActivityReindexProperties
import com.rarible.protocol.union.worker.task.search.ParamFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.stereotype.Component

@Component
@Deprecated("Replace with SyncActivityJob")
class RemoveRevertedActivityTask(
    private val properties: ActivityReindexProperties,
    private val paramFactory: ParamFactory,
    private val activityReindexService: ActivityReindexService,
    private val activeBlockchainProvider: ActiveBlockchainProvider,
) : TaskHandler<String> {

    override val type: String
        get() = "ES_REMOVE_REVERTED_ACTIVITY"

    override suspend fun isAbleToRun(param: String): Boolean {
        val blockchain = paramFactory.parse<RemoveRevertedActivityTaskParam>(param).blockchain
        return properties.enabled && activeBlockchainProvider.isActive(blockchain)
    }

    /**
     * from - cursor
     * param is json-serialized ActivityTaskParam
     */
    override fun runLongTask(from: String?, param: String): Flow<String> {
        return if (from == "") {
            emptyFlow()
        } else {
            val taskParam = paramFactory.parse<RemoveRevertedActivityTaskParam>(param)
            return activityReindexService
                .removeReverted(
                    blockchain = taskParam.blockchain,
                    type = taskParam.type,
                    cursor = from,
                )
        }
    }
}
