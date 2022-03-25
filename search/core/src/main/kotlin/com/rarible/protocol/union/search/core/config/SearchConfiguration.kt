package com.rarible.protocol.union.search.core.config

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.union.search.core.repository.ActivityEsRepository
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
//@EnableConfigurationProperties(SearchProperties::class)
class SearchConfiguration(
    //val searchProperties: SearchProperties
) {

    @Bean
    fun elasticSearchClient(
        @Value("\${elasticsearch.host}") elasticsearchHost: String,
        objectMapper: ObjectMapper
    ): ElasticsearchAsyncClient {
        val lowLevelClient = RestClient
            .builder(HttpHost.create(elasticsearchHost))
            .build()
        val transport = RestClientTransport(lowLevelClient, JacksonJsonpMapper(objectMapper))
        return ElasticsearchAsyncClient(transport)
    }

    @Bean
    fun activityEsRepository(elasticsearchAsyncClient: ElasticsearchAsyncClient) =
        ActivityEsRepository(elasticsearchAsyncClient)
}