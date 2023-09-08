package com.rarible.protocol.union.enrichment.repository

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.enrichment.download.DownloadTask
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import org.springframework.data.mongodb.core.remove
import org.springframework.data.mongodb.core.updateMulti
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class DownloadTaskRepository(
    private val template: ReactiveMongoTemplate
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun createIndices() = runBlocking {
        Indices.ALL.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, DownloadTask.COLLECTION)
            template.indexOps(DownloadTask.COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    suspend fun save(task: DownloadTask): DownloadTask {
        return template.save(task).awaitSingle()
    }

    suspend fun insert(tasks: List<DownloadTask>): List<DownloadTask> {
        if (tasks.isEmpty()) {
            return emptyList()
        }
        return template.insertAll(tasks).collectList().awaitSingle()
    }

    suspend fun get(id: String): DownloadTask? {
        return template.findById<DownloadTask>(id).awaitSingleOrNull()
    }

    suspend fun getByIds(ids: List<String>): List<DownloadTask> {
        val criteria = Criteria("_id").inValues(ids)
        return template.find<DownloadTask>(Query(criteria)).collectList().awaitSingle()
    }

    suspend fun deleteByIds(ids: List<String>) {
        val criteria = Criteria("_id").inValues(ids)
        template.remove<DownloadTask>(Query(criteria)).awaitSingle()
    }

    suspend fun findForExecution(type: String, pipeline: String, limit: Int): List<DownloadTask> {
        val criteria = Criteria().andOperator(
            DownloadTask::type isEqualTo type,
            DownloadTask::pipeline isEqualTo pipeline,
            DownloadTask::inProgress isEqualTo false,
        )
        val query = Query(criteria).with(
            Sort.by(Sort.Direction.DESC, DownloadTask::priority.name)
                .and(Sort.by(Sort.Direction.ASC, DownloadTask::scheduledAt.name))
        ).limit(limit)

        return template.find<DownloadTask>(query).collectList().awaitSingle()
    }

    suspend fun reactivateStuckTasks(inProgressLimit: Duration): Long {
        val maxStartedAt = nowMillis().minus(inProgressLimit)
        val criteria = Criteria().andOperator(
            DownloadTask::inProgress isEqualTo true,
            DownloadTask::startedAt lt maxStartedAt
        )

        val update = Update()
            .set(DownloadTask::inProgress.name, false)
            .set(DownloadTask::startedAt.name, null)

        return template.updateMulti<DownloadTask>(Query(criteria), update).awaitSingle().modifiedCount
    }

    suspend fun getTaskCountInPipeline(type: String, pipeline: String, limit: Int): Long {
        val criteria = Criteria().andOperator(
            DownloadTask::type isEqualTo type,
            DownloadTask::pipeline isEqualTo pipeline
        )

        val query = Query(criteria).limit(limit)
        return template.count<DownloadTask>(query).awaitSingle()
    }

    object Indices {

        private val STUCK_IN_PROGRESS: Index = Index()
            .on(DownloadTask::inProgress.name, Sort.Direction.DESC)
            .on(DownloadTask::startedAt.name, Sort.Direction.DESC)
            .background()

        private val EXECUTION: Index = Index()
            .on(DownloadTask::type.name, Sort.Direction.ASC)
            .on(DownloadTask::pipeline.name, Sort.Direction.ASC)
            .on(DownloadTask::inProgress.name, Sort.Direction.ASC)
            .on(DownloadTask::priority.name, Sort.Direction.DESC)
            .on(DownloadTask::scheduledAt.name, Sort.Direction.ASC)
            .background()

        val ALL = listOf(
            STUCK_IN_PROGRESS,
            EXECUTION
        )
    }
}
