package com.rarible.protocol.union.worker.task

import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.api.client.CollectionControllerApi
import com.rarible.protocol.union.core.converter.EsCollectionConverter
import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.awaitSingle

class CollectionTask(
    activeBlockchains: List<BlockchainDto>,
    private val ableToRun: Boolean,
    private val client: CollectionControllerApi,
    private val repository: EsCollectionRepository
) : TaskHandler<String> {

    private val tasksByParam = activeBlockchains.associateBy { "${type}_${it.name}" }

    override val type: String
        get() = EsCollection.ENTITY_DEFINITION.reindexTaskName

    override fun getAutorunParams(): List<RunTask> {
        return tasksByParam.keys.map { RunTask(it) }
    }

    override suspend fun isAbleToRun(param: String): Boolean = ableToRun

    override fun runLongTask(from: String?, param: String): Flow<String> {
        tasksByParam[param]?.let { blockchain ->
            if (from?.isEmpty() == true) return emptyFlow()
            return flow {
                val res = client.getAllCollections(
                    listOf(blockchain),
                    from, PAGE_SIZE
                ).awaitSingle()

                if (res.collections.isNotEmpty()) {
                    repository.saveAll(
                        res.collections.map { EsCollectionConverter.convert(it) },
                    )
                }
                emit(res.continuation.orEmpty())
            }
        } ?: return emptyFlow()
    }

    companion object {
        private const val PAGE_SIZE = 1000
    }
}
