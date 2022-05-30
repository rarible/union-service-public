package com.rarible.protocol.union.worker.task.search.collection

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.core.converter.EsCollectionConverter
import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.service.query.collection.CollectionApiMergeService
import com.rarible.protocol.union.worker.config.CollectionReindexProperties
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Component

@Component
class CollectionTask(
    private val properties: CollectionReindexProperties,
    private val collectionApiMergeService: CollectionApiMergeService,
    private val repository: EsCollectionRepository
) : TaskHandler<String> {

    override val type: String
        get() = EsCollection.ENTITY_DEFINITION.reindexTask

    override suspend fun isAbleToRun(param: String): Boolean {
        val blockchain = IdParser.parseBlockchain(param)
        return properties.enabled && properties.blockchains.single { it.blockchain == blockchain }.enabled
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val blockchain = IdParser.parseBlockchain(param)
        return if (from == "") {
            emptyFlow()
        } else {
            var lastCursor = from
            flow {
                do {
                    val res = collectionApiMergeService.getAllCollections(
                        listOf(blockchain),
                        lastCursor,
                        PageSize.COLLECTION.max
                    )
                    if (res.collections.isNotEmpty()) {
                        repository.saveAll(
                            res.collections.map { EsCollectionConverter.convert(it) },
                        )
                    }
                    lastCursor = res.continuation.orEmpty()
                    emit(lastCursor!!)
                } while (!lastCursor.isNullOrBlank())
            }
        }
    }
}
