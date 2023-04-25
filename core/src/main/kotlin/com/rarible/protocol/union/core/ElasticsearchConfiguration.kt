package com.rarible.protocol.union.core

import com.rarible.common.elasticsearch.RaribleElasticsearchClient
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.RestHighLevelClientBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration

@Configuration
class ElasticsearchConfiguration(private val ff: FeatureFlagsProperties) {

    @Bean
    fun elasticsearchHighLevelClient(@Value("\${elasticsearch.api-nodes}") nodes: List<String>): RestHighLevelClient {
        val restClient = RestClient.builder(
            *nodes.map { hostAndPort -> HttpHost.create(hostAndPort) }.toTypedArray()
        ).build()
        return RestHighLevelClientBuilder(restClient).setApiCompatibilityMode(ff.enableElasticsearchCompatibilityMode)
            .build()
    }

    @Bean
    fun reactiveElasticsearchClient(@Value("\${elasticsearch.api-nodes}") nodes: List<String>): ReactiveElasticsearchClient {
        logger.info("init reactiveElasticsearchClient nodes=$nodes")

        val clientConfiguration = ClientConfiguration.builder()
            .connectedTo(*nodes.toTypedArray())
            .withWebClientConfigurer { webClient ->
                val provider = ConnectionProvider.builder("elasticsearch")
                    .maxConnections(500)
                    .pendingAcquireMaxCount(-1)
                    .maxIdleTime(Duration.ofSeconds(60))
                    .maxLifeTime(Duration.ofSeconds(60))
                    .lifo()
                    .build()
                val exchangeStrategies = ExchangeStrategies.builder()
                    .codecs { configurer ->
                        configurer.defaultCodecs()
                            .maxInMemorySize(-1)
                    }
                    .build()
                val client = HttpClient.create(provider).responseTimeout(Duration.ofSeconds(30))
                val connector = ReactorClientHttpConnector(client)
                webClient.mutate()
                    .clientConnector(connector)
                    .exchangeStrategies(exchangeStrategies)
                    .build()
            }
            .build()

        if (ff.enableElasticsearchCompatibilityMode) {
            logger.info("Will use rarible elasticsearch client")
            return RaribleElasticsearchClient(clientConfiguration, Duration.ofMinutes(5), true)
        } else {
            logger.info("Will use spring elasticsearch client")
            return ReactiveRestClients.create(clientConfiguration)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ElasticsearchConfiguration::class.java)
    }
}