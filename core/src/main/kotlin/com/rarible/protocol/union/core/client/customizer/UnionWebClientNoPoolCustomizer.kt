package com.rarible.protocol.union.core.client.customizer

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.unit.DataSize
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration

object UnionWebClientNoPoolCustomizer : WebClientCustomizer {

    private val DEFAULT_MAX_BODY_SIZE = DataSize.ofMegabytes(10).toBytes().toInt()
    private val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(30)

    override fun customize(webClientBuilder: WebClient.Builder?) {
        val client = HttpClient.create(ConnectionProvider.newConnection())
            .responseTimeout(DEFAULT_TIMEOUT)
            .followRedirect(true)

        val connector = ReactorClientHttpConnector(client)

        webClientBuilder
            ?.codecs { configurer -> configurer.defaultCodecs().maxInMemorySize(DEFAULT_MAX_BODY_SIZE) }
            ?.clientConnector(connector)
            ?.defaultHeader("x-rarible-client", "rarible-protocol")
    }
}