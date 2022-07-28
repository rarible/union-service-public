package com.rarible.protocol.union.listener.job.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.listener.job.OpenSeaUrlMigrationJob
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component

@Component
class OpenSeaUrlMigrationTask(
    private val job: OpenSeaUrlMigrationJob
) : TaskHandler<String> {

    override val type = "OPEN_SEA_URL_MIGRATION"

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask(""))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        return job.migrate(from)
    }
}