package com.rarible.protocol.union.core

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration

class ProtocolWebClientCustomizer() : WebClientCustomizer {

    override fun customize(webClientBuilder: WebClient.Builder?) {
        val provider = ConnectionProvider.builder("protocol-default-connection-provider")
            .maxConnections(2000)
            .pendingAcquireMaxCount(-1)
            .maxIdleTime(Duration.ofSeconds(60))
            .maxLifeTime(Duration.ofSeconds(60))
            .lifo()
            .build()

        //val client = HttpClient.create(provider)
        val client = HttpClient.create(ConnectionProvider.newConnection())
            .responseTimeout(Duration.ofSeconds(30))

        val connector = ReactorClientHttpConnector(client)

        webClientBuilder
            ?.codecs { configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
            ?.clientConnector(connector)
            ?.defaultHeader("x-rarible-client", "rarible-protocol")
    }
}