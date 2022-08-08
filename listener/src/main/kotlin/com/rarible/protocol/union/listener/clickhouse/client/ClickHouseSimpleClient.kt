package com.rarible.protocol.union.listener.clickhouse.client

import com.clickhouse.client.ClickHouseRecord

/**
 * Simple client for working with ClickHouse.
 */
interface ClickHouseSimpleClient {
    /**
     * Execute SQL query operation with entered arguments and map single result row to result object.
     *
     * @param query          SQL query to execute.
     * @param arguments      SQL query arguments to execute.
     * @param recordToObject function that will be applied to record returned from ClickHouse.
     * @param T              type of result.
     * @return mapped object.
     */
    suspend fun <T> queryForObject(
        query: String,
        arguments: Map<String, Any?> = emptyMap(),
        recordToObject: (ClickHouseRecord) -> T
    ): T?

    /**
     * Execute SQL query operation with entered arguments and map each row to result object.
     *
     * @param query          SQL query to execute.
     * @param arguments      SQL query arguments to execute.
     * @param recordToObject function that will be applied to record returned from ClickHouse.
     * @param T              type of result.
     * @return mapped objects.
     */
    suspend fun <T> queryForList(
        query: String,
        arguments: Map<String, Any?> = emptyMap(),
        recordToObject: (ClickHouseRecord) -> T
    ): List<T>

    suspend fun execute(query: String)
}
