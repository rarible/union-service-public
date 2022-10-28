package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.api.ApiClient
import com.rarible.protocol.union.core.ProtocolWebClientCustomizer
import io.netty.handler.logging.LogLevel
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.logging.AdvancedByteBufFormat

object ImxWebClientFactory {

    fun createClient(baseUrl: String, apiKey: String?): WebClient {
        val mapper = ApiClient.createDefaultObjectMapper()
        val httpClient = HttpClient.create().wiretap(
            "reactor.netty.http.client.HttpClient",
            LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL
        )
        val strategies = ExchangeStrategies
            .builder()
            .codecs { configurer: ClientCodecConfigurer ->
                configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON))
                configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON))
            }.build()
        val webClient = WebClient.builder()
            .exchangeStrategies(strategies)
            .clientConnector(ReactorClientHttpConnector(httpClient))

        ProtocolWebClientCustomizer().customize(webClient)
        apiKey?.let {
            webClient.defaultHeaders {
                it.add("x-api-key", apiKey)
            }
        }

        return webClient.baseUrl(baseUrl).build()
    }

}