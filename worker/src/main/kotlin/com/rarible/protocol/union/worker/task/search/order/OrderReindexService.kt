package com.rarible.protocol.union.worker.task.search.order

import com.rarible.protocol.union.core.converter.EsOrderConverter
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService
import com.rarible.protocol.union.enrichment.service.query.order.OrderApiMergeService
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import com.rarible.protocol.union.worker.task.search.EsRateLimiter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.elasticsearch.action.support.WriteRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OrderReindexService(
    private val enrichmentOrderService: EnrichmentOrderService,
    private val orderApiMergeService: OrderApiMergeService,
    private val repository: EsOrderRepository,
    private val searchTaskMetricFactory: SearchTaskMetricFactory,
    private val rateLimiter: EsRateLimiter,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun reindex(
        blockchain: BlockchainDto,
        index: String?,
        cursor: String? = null
    ): Flow<String> {
        val counter = searchTaskMetricFactory.createReindexOrderCounter(blockchain)
        var continuation = cursor
        // TODO read values from config
        val size = when (blockchain) {
            BlockchainDto.IMMUTABLEX -> 200 // Max size allowed by IMX
            else -> PageSize.ORDER.max
        }
        return flow {
            do {
                rateLimiter.waitIfNecessary(size)
                val res = orderApiMergeService.getOrdersAll(
                    listOf(blockchain),
                    continuation,
                    size,
                    OrderSortDto.LAST_UPDATE_DESC,
                    OrderStatusDto.values().asList()
                )

                if (res.entities.isNotEmpty()) {
                    repository.saveAll(
                        res.entities.map {
                            logger.info("Converting OrderDto  $it")
                            EsOrderConverter.convert(enrichmentOrderService.enrich(it))
                        },
                        refreshPolicy = WriteRequest.RefreshPolicy.NONE
                    )
                    counter.increment(res.entities.size)
                }
                emit(res.continuation.orEmpty())
                continuation = res.continuation
            } while (!continuation.isNullOrEmpty())
        }
    }
}
