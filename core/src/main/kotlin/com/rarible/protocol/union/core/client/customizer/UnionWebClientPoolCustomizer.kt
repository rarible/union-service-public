package com.rarible.protocol.union.core.client.customizer

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.unit.DataSize
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration

// Taken from openapi clients
object UnionWebClientPoolCustomizer : WebClientCustomizer {

    private val DEFAULT_MAX_BODY_SIZE = DataSize.ofMegabytes(10).toBytes().toInt()
    private val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(30)

    override fun customize(webClientBuilder: WebClient.Builder) {
        webClientBuilder.codecs { configurer ->
            configurer.defaultCodecs().maxInMemorySize(DEFAULT_MAX_BODY_SIZE)
        }
        val provider = ConnectionProvider.builder("protocol-connection-provider")
            .maxConnections(500)
            .pendingAcquireMaxCount(-1)
            .maxIdleTime(DEFAULT_TIMEOUT)
            .maxLifeTime(DEFAULT_TIMEOUT)
            .lifo()
            .build()

        val client = HttpClient
            .create(provider)
            .responseTimeout(DEFAULT_TIMEOUT)
            .followRedirect(true)

        val connector = ReactorClientHttpConnector(client)
        webClientBuilder.clientConnector(connector)
        webClientBuilder.defaultHeader("x-rarible-client", "rarible-protocol")
    }
}
