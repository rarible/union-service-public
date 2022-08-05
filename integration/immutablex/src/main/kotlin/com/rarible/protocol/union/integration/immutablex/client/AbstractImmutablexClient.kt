package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.core.common.mapAsync
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

abstract class AbstractImmutablexClient(
    protected val webClient: WebClient
) {

    protected suspend inline fun <reified T> getByUri(uri: String): T {
        return webClient.get().uri(uri)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(T::class.java)
            .awaitSingle().body!!
    }

    protected suspend inline fun <T> getByUri(uri: String, bodyTypeReference: ParameterizedTypeReference<T>): T {
        return webClient.get().uri(uri)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(bodyTypeReference)
            .awaitSingle().body!!
    }

    protected suspend fun <K, V> getChunked(
        chuckSize: Int,
        keys: Collection<K>,
        call: suspend (K) -> V?
    ): List<V> {
        return coroutineScope {
            keys.chunked(chuckSize).map { chunk ->
                chunk.mapAsync { call(it) }
            }
        }.flatten().filterNotNull()
    }

    suspend fun <T> ignore404(call: suspend () -> T?): T? {
        return try {
            call()
        } catch (e: WebClientResponseException.NotFound) {
            null
        }
    }

}