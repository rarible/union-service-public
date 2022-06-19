package com.rarible.protocol.union.worker.task.search.order

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.core.converter.EsOrderConverter
import com.rarible.protocol.union.core.model.EsOrder
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.enrichment.service.query.order.OrderApiMergeService
import com.rarible.protocol.union.worker.config.OrderReindexProperties
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import com.rarible.protocol.union.worker.task.search.ParamFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Component

@Component
class OrderTask(
    private val properties: OrderReindexProperties,
    private val orderApiMergeService: OrderApiMergeService,
    private val repository: EsOrderRepository,
    private val paramFactory: ParamFactory,
    private val searchTaskMetricFactory: SearchTaskMetricFactory
) : TaskHandler<String> {

    override val type: String
        get() = EsOrder.ENTITY_DEFINITION.reindexTask

    override suspend fun isAbleToRun(param: String): Boolean {
        val blockchain = IdParser.parseBlockchain(param)
        return properties.enabled && properties.blockchains.single { it.blockchain == blockchain }.enabled
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val blockchain = paramFactory.parse<OrderTaskParam>(param).blockchain
        val counter = searchTaskMetricFactory.createReindexOrderCounter(blockchain)
        return if (from == "") {
            emptyFlow()
        } else {
            var continuation = from
            flow {
                do {
                    val res = orderApiMergeService.getOrdersAll(
                        listOf(blockchain),
                        continuation,
                        PageSize.ORDER.max,
                        OrderSortDto.LAST_UPDATE_DESC,
                        OrderStatusDto.values().asList()
                    )

                    if (res.orders.isNotEmpty()) {
                        repository.saveAll(
                            res.orders.map { EsOrderConverter.convert(it) }
                        )
                        counter.increment(res.orders.size)
                    }
                    emit(res.continuation.orEmpty())
                    continuation = res.continuation
                } while (continuation != null)
            }
        }
    }
}
