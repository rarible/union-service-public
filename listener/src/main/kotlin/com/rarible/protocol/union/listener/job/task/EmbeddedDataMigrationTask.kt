package com.rarible.protocol.union.listener.job.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.listener.job.EmbeddedDataMigrationJob
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component

@Component
class EmbeddedDataMigrationTask(
    private val job: EmbeddedDataMigrationJob,
    private val featureFlagsProperties: FeatureFlagsProperties
) : TaskHandler<String> {

    override val type = "EMBEDDED_DATA_MIGRATION"

    override suspend fun isAbleToRun(param: String): Boolean {
        return featureFlagsProperties.enableEmbeddedContentMigrationJob
    }

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask(""))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        return job.migrate(from)
    }
}