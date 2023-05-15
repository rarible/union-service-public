package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.core.client.WebClientFactory
import org.springframework.web.reactive.function.client.WebClient

object ImxWebClientFactory {

    fun configureClient(baseUrl: String, apiKey: String?): WebClient.Builder {
        val headers = apiKey?.let { mapOf("x-api-key" to it) } ?: emptyMap()
        return WebClientFactory.createClient(baseUrl, headers)
    }

}