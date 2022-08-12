package com.rarible.protocol.union.integration.immutablex.client

import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

class ImxCollectionClient(
    webClient: WebClient,
) : AbstractImxClient(
    webClient
) {

    private val collectionRequestChunkSize = 16

    suspend fun getById(collectionAddress: String): ImmutablexCollection {
        val uri = ImxCollectionQueryBuilder.getByIdPath(collectionAddress)
        return getByUri(uri)
    }

    suspend fun getByIds(collectionIds: List<String>): List<ImmutablexCollection> {
        return getChunked(collectionRequestChunkSize, collectionIds) {
            ignore404 { getById(it) }
        }
    }

    suspend fun getAll(
        continuation: String?,
        size: Int
    ): ImmutablexPage<ImmutablexCollection> {
        return webClient
            .get()
            .uri {
                val builder = ImxCollectionQueryBuilder(it)
                builder.continuation(continuation)
                builder.pageSize(size)
                builder.build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .awaitBody()
    }
}