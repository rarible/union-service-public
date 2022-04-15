package com.rarible.protocol.union.search.reindexer.task

import com.rarible.core.common.mapAsync
import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.api.client.OrderControllerApi
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.core.converter.EsOrderConverter
import com.rarible.protocol.union.search.reindexer.config.SearchReindexerConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations

class OrderTask(
    private val config: SearchReindexerConfiguration,
    private val orderClient: OrderControllerApi,
    private val esOperations: ReactiveElasticsearchOperations,
    private val converter: EsOrderConverter
): TaskHandler<String> {

    override val type: String
        get() = ORDER_REINDEX

    override fun getAutorunParams(): List<RunTask> {
        return config.properties.orderTasks.map { RunTask(it.name) }
    }

    override suspend fun isAbleToRun(param: String): Boolean {
        return config.properties.startReindexOrder
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
                    PAGE_SIZE,
                    OrderSortDto.LAST_UPDATE_ASC,
                    emptyList()
                ).awaitSingle()

                esOperations.save(
                    res.orders.mapAsync(converter::convert)
                ).awaitSingle()

                emit(res.continuation ?: "")
            }
        }
    }

    companion object {
        private const val ORDER_REINDEX = "ORDER_REINDEX"
        const val PAGE_SIZE = 1000
    }
}
