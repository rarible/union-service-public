package com.rarible.protocol.union.listener.clickhouse.client

import com.clickhouse.client.ClickHouseClientBuilder
import com.clickhouse.client.ClickHouseNode
import com.clickhouse.client.ClickHouseRecord
import com.clickhouse.client.ClickHouseValues
import kotlinx.coroutines.future.await

/**
 * Simple client for working with ClickHouse.
 *
 * @property clickHouseNode          node of ClickHouse server.
 * @property clickHouseClientBuilder builder of ClickHouse clients.
 */
class DefaultClickHouseSimpleClient(
    private val clickHouseNode: ClickHouseNode,
    private val clickHouseClientBuilder: ClickHouseClientBuilder
) : ClickHouseSimpleClient {

    /**
     * Execute SQL query operation with entered arguments and map single result row to result object.
     *
     * @param query          SQL query to execute.
     * @param arguments      SQL query arguments to execute.
     * @param recordToObject function that will be applied to record returned from ClickHouse.
     * @param T              type of result.
     * @return mapped object.
     */
    override suspend fun <T> queryForObject(
        query: String,
        arguments: Map<String, Any?>,
        recordToObject: (ClickHouseRecord) -> T
    ): T? {
        val result = queryForList(query, arguments, recordToObject)

        return when {
            result.isEmpty() -> null
            result.size == 1 -> result.single()
            else -> throw IllegalStateException("Query returned more than one result")
        }
    }

    /**
     * Execute SQL query operation with entered arguments and map each row to result object.
     *
     * @param query          SQL query to execute.
     * @param arguments      SQL query arguments to execute.
     * @param recordToObject function that will be applied to record returned from ClickHouse.
     * @param T              type of result.
     * @return mapped objects.
     */
    override suspend fun <T> queryForList(
        query: String,
        arguments: Map<String, Any?>,
        recordToObject: (ClickHouseRecord) -> T
    ): List<T> {
        val nativeArguments = arguments.mapValues { ClickHouseValues.convertToSqlExpression(it.value) }

        return clickHouseClientBuilder.build()
            .use { client ->
                val request = client.connect(clickHouseNode)
                    .query(query)
                    .params(nativeArguments)

                request.execute()
                    .await()
                    .use { response ->
                        response.records().map(recordToObject)
                    }
            }
    }

    override suspend fun execute(query: String) {
        return clickHouseClientBuilder.build()
            .use { client ->
                client.connect(clickHouseNode)
                    .query(query)
                    .execute()
                    .await()
            }
    }
}
