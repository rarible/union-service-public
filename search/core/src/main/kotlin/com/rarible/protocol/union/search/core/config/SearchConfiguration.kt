package com.rarible.protocol.union.search.core.config

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.union.search.core.repository.ActivityEsRepository
import org.apache.http.HttpHost
import org.elasticsearch.client.Node
import org.elasticsearch.client.RestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
//@EnableConfigurationProperties(SearchProperties::class)
class SearchConfiguration(
    @Value("\${elasticsearch.api-nodes}") private val  elasticsearchHost: String
): AbstractElasticsearchConfiguration() {
    override fun elasticsearchClient(): RestHighLevelClient {
        val clientConfiguration = ClientConfiguration
            .builder()
            .connectedTo("localhost")
            .build()

        return RestClients.create(clientConfiguration).rest()
    }

}