package com.rarible.protocol.union.listener.job.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.listener.job.ImxMetaInitJob
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component

@Component
class ImxMetaInitTask(
    private val job: ImxMetaInitJob
) : TaskHandler<String> {

    override val type = "IMMUTABLEX_META_INIT_JOB"

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask(""))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        return job.execute(from)
    }
}