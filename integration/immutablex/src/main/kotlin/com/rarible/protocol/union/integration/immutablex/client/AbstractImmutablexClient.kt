package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.core.common.mapAsync
import kotlinx.coroutines.coroutineScope
import org.springframework.web.reactive.function.client.WebClient

abstract class AbstractImmutablexClient(
    protected val webClient: WebClient
) {

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

}