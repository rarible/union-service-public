package com.rarible.protocol.union.worker.task.search.collection

import com.rarible.protocol.union.core.converter.EsCollectionConverter
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.service.query.collection.CollectionApiMergeService
import com.rarible.protocol.union.worker.config.CollectionReindexProperties
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import com.rarible.protocol.union.worker.task.search.ParamFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Component

@Component
class CollectionReindexService(
    private val collectionApiMergeService: CollectionApiMergeService,
    private val repository: EsCollectionRepository,
    private val searchTaskMetricFactory: SearchTaskMetricFactory,
) {

    fun reindex(
        blockchain: BlockchainDto,
        index: String?,
        cursor: String? = null
    ): Flow<String> {
        var lastCursor = cursor
        val counter = searchTaskMetricFactory.createReindexCollectionCounter(blockchain)
        return flow {
            do {
                val res = collectionApiMergeService.getAllCollections(
                    listOf(blockchain),
                    lastCursor,
                    PageSize.COLLECTION.max
                )
                if (res.collections.isNotEmpty()) {
                    repository.saveAll(
                        res.collections.map { EsCollectionConverter.convert(it) },
                        index
                    )
                    counter.increment(res.collections.size)
                }
                lastCursor = res.continuation.orEmpty()
                emit(lastCursor!!)
            } while (!lastCursor.isNullOrBlank())
        }
    }
}