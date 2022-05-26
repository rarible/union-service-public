package com.rarible.protocol.union.worker.task.search.order

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.api.client.OrderControllerApi
import com.rarible.protocol.union.core.converter.EsOrderConverter
import com.rarible.protocol.union.core.model.EsOrder
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.worker.config.OrderReindexProperties
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.awaitSingle

//TODO enable when orders are ready
//@Component
class OrderTask(
    private val properties: OrderReindexProperties,
    private val orderClient: OrderControllerApi,
    private val repository: EsOrderRepository
) : TaskHandler<String> {

    override val type: String
        get() = EsOrder.ENTITY_DEFINITION.reindexTask

    override suspend fun isAbleToRun(param: String): Boolean {
        val blockchain = IdParser.parseBlockchain(param)
        return properties.enabled && properties.blockchains.single { it.blockchain == blockchain }.enabled
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val blockchain = IdParser.parseBlockchain(param)
        return if (from == "") {
            emptyFlow()
        } else {
            flow {
                val res = orderClient.getOrdersAll(
                    listOf(blockchain),
                    from,
                    PageSize.ORDER.max,
                    OrderSortDto.LAST_UPDATE_ASC,
                    emptyList()
                ).awaitSingle()

                if (res.orders.isNotEmpty()) {
                    repository.saveAll(
                        res.orders.map { EsOrderConverter.convert(it) },
                    )
                }
                emit(res.continuation.orEmpty())
            }
        }
    }
}
