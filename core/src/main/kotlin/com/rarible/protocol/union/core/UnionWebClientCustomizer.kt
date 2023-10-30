package com.rarible.protocol.union.core

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.util.unit.DataSize
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration

@Component
class UnionWebClientCustomizer(
    private val ff: FeatureFlagsProperties
) : WebClientCustomizer {

    private val maxBodySize = DataSize.ofMegabytes(10).toBytes().toInt()
    private val timeout: Duration = Duration.ofSeconds(30)

    override fun customize(webClientBuilder: WebClient.Builder) {
        webClientBuilder.codecs { configurer ->
            configurer.defaultCodecs().maxInMemorySize(maxBodySize)
        }

        val provider = if (ff.enableWebClientConnectionPool) {
            ConnectionProvider.builder("protocol-connection-provider")
                .maxConnections(1024)
                .pendingAcquireMaxCount(-1)
                .maxIdleTime(timeout)
                .maxLifeTime(timeout)
                .lifo()
                .build()
        } else {
            ConnectionProvider.newConnection()
        }

        val client = HttpClient
            .create(provider)
            .responseTimeout(timeout)
            .followRedirect(true)

        val connector = ReactorClientHttpConnector(client)
        webClientBuilder.clientConnector(connector)
        webClientBuilder.defaultHeader("x-rarible-client", "rarible-protocol")
    }
}
