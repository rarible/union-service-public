package com.rarible.protocol.union.search.core.syntax

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch.core.GetRequest
import co.elastic.clients.elasticsearch.core.GetResponse
import kotlinx.coroutines.future.await

suspend inline fun <reified T: Any> ElasticsearchAsyncClient.get(req: GetRequest): GetResponse<T>? {
    return this.get(req, T::class.java).await()
}