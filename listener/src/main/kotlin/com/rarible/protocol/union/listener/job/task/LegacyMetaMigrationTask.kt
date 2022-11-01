package com.rarible.protocol.union.listener.job.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.listener.job.LegacyMetaMigrationJob
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component

@Component
class LegacyMetaMigrationTask(
    private val job: LegacyMetaMigrationJob
) : TaskHandler<String> {

    override val type = "LEGACY_META_MIGRATION_TASK"

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask(""))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        return job.migrate(from, 32)
    }
}