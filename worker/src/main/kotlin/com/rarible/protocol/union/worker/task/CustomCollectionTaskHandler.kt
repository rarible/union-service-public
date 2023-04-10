package com.rarible.protocol.union.worker.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.worker.job.collection.CustomCollectionJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Component

@Component
class CustomCollectionTaskHandler(
    private val job: CustomCollectionJob
) : TaskHandler<String> {

    override val type = "CUSTOM_COLLECTION_MIGRATION"

    override fun getAutorunParams(): List<RunTask> {
        // TODO Should be taken from configuration
        return emptyList()
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        return flow {
            var next = from
            do {
                next = job.migrate(param, from)
                if (next != null) {
                    emit(next)
                }
            } while (next != null)
        }
    }

}