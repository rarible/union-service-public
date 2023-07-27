package com.rarible.protocol.union.worker.task.search.collection

import com.rarible.protocol.union.core.converter.EsCollectionConverter
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.service.query.collection.CollectionApiMergeService
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import com.rarible.protocol.union.worker.task.search.EsRateLimiter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.elasticsearch.action.support.WriteRequest
import org.springframework.stereotype.Component

@Component
@Deprecated("Replace with SyncCollectionJob")
class CollectionReindexService(
    private val collectionApiMergeService: CollectionApiMergeService,
    private val repository: EsCollectionRepository,
    private val searchTaskMetricFactory: SearchTaskMetricFactory,
    private val rateLimiter: EsRateLimiter,
) {

    fun reindex(
        blockchain: BlockchainDto,
        index: String?,
        cursor: String? = null
    ): Flow<String> {
        var lastCursor = cursor
        val counter = searchTaskMetricFactory.createReindexCollectionCounter(blockchain)
        // TODO read values from config
        val size = when (blockchain) {
            BlockchainDto.IMMUTABLEX -> 200 // Max size allowed by IMX
            else -> PageSize.COLLECTION.max
        }
        return flow {
            do {
                rateLimiter.waitIfNecessary(size)
                val res = collectionApiMergeService.getAllCollections(
                    listOf(blockchain),
                    lastCursor,
                    size
                )
                if (res.collections.isNotEmpty()) {
                    repository.saveAll(
                        res.collections.map { EsCollectionConverter.convert(it) },
                        index,
                        WriteRequest.RefreshPolicy.NONE
                    )
                    counter.increment(res.collections.size)
                }
                lastCursor = res.continuation.orEmpty()
                emit(lastCursor!!)
            } while (!lastCursor.isNullOrBlank())
        }
    }
}
