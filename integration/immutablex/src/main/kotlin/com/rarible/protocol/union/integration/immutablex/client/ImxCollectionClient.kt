package com.rarible.protocol.union.integration.immutablex.client

import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

class ImxCollectionClient(
    webClient: WebClient,
    private val byIdsChunkSize: Int
) : AbstractImxClient(
    webClient
) {

    private val collectionRequestChunkSize = 16

    suspend fun getById(collectionAddress: String): ImmutablexCollection {
        val uri = ImxCollectionQueryBuilder.getByIdPath(collectionAddress)
        return getByUri(uri)
    }

    suspend fun getByIds(collectionIds: Collection<String>): List<ImmutablexCollection> {
        return getChunked(byIdsChunkSize, collectionIds) {
            ignore404 { getById(it) }
        }
    }

    suspend fun getAllWithIdSort(
        continuation: String?,
        size: Int
    ): ImmutablexPage<ImmutablexCollection> = getCollections {
        it.continuationById(continuation)
        it.pageSize(size)
    }

    suspend fun getAllWithUpdateAtSort(
        continuation: String?,
        size: Int,
        sortAsc: Boolean
    ): ImmutablexPage<ImmutablexCollection> = getCollections {
        it.continuationByUpdatedAt(continuation, sortAsc)
        it.pageSize(size)
    }

    private suspend fun getCollections(
        build: (builder: ImxCollectionQueryBuilder) -> Unit
    ): ImmutablexPage<ImmutablexCollection> {
        return webClient.get()
            .uri {
                val builder = ImxCollectionQueryBuilder(it)
                build(builder)
                builder.build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .awaitBody()
    }
}