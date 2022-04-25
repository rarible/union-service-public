package com.rarible.protocol.union.worker.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.api.client.OwnershipControllerApi
import com.rarible.protocol.union.core.converter.EsOwnershipConverter
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.worker.config.SearchReindexerConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitFirst

class OwnershipTask(
    private val config: SearchReindexerConfiguration,
    private val ownershipClient: OwnershipControllerApi,
    private val ownershipRepository: EsOwnershipRepository,
    private val converter: EsOwnershipConverter,
) : TaskHandler<String> {

    override val type: String
        get() = OWNERSHIP_REINDEX

    override fun getAutorunParams(): List<RunTask> =
        config.properties.ownershipTasks.map { RunTask(it.taskParam()) }

    override suspend fun isAbleToRun(param: String): Boolean =
        config.properties.startReindexOwnership

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val task = tasks[param] ?: return emptyFlow()
        return if (from == "") {
            emptyFlow()
        } else {
            flow {
                val res = ownershipClient.getAllOwnerships(
                    listOf(task.blockchainDto), from, PAGE_SIZE
                ).awaitFirst()
                ownershipRepository.saveAll(res.ownerships.map { converter.convert(it) })
                emit(res.continuation ?: "")
            }
        }
    }

    private val tasks = config.properties.ownershipTasks.associateBy { it.taskParam() }

    companion object {
        private const val OWNERSHIP_REINDEX = "OWNERSHIP_REINDEX"
        const val PAGE_SIZE = 1000
    }
}
