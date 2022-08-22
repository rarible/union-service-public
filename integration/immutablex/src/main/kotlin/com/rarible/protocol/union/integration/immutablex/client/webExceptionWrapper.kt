package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.core.client.WebClientResponseProxyException
import org.springframework.web.reactive.function.client.WebClientResponseException

suspend inline fun <T> wrapException(webCall: suspend () -> T): T {
    try {
        return webCall()
    } catch (e: WebClientResponseException) {
        // Our advice-controller configured for such type of exceptions
        throw WebClientResponseProxyException(original = e)
    }
}