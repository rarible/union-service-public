package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexCollection
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexPage
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody


class CollectionsApi(
    private val webClient: WebClient
) {

    suspend fun getAll(
        continuation: String?,
        size: Int
    ): ImmutablexPage<ImmutablexCollection> {
        val query = StringBuilder("?order_by=name&direction=desc")
        query.append("&page_size=$size")
        if (!continuation.isNullOrEmpty()) {
            query.append("&cursor=$continuation")
        }
        return webClient
            .get()

            .uri("/collections$query")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .awaitBody()
    }

    suspend fun getById(collectionAddress: String): ImmutablexCollection {
        return webClient.get().uri("/collections/${collectionAddress}")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .awaitBody()
    }
}