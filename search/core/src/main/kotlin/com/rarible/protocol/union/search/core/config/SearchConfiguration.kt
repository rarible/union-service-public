package com.rarible.protocol.union.search.core.config

import org.elasticsearch.client.RestHighLevelClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.RestClients
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories


@Configuration
@AutoConfigurationPackage
@EnableReactiveElasticsearchRepositories
class SearchConfiguration(
    @Value("\${elasticsearch.api-nodes}") private val elasticsearchHost: String
): AbstractElasticsearchConfiguration() {
    override fun elasticsearchClient(): RestHighLevelClient {
        val clientConfiguration = ClientConfiguration
            .builder()
            .connectedTo(elasticsearchHost)
            .build()

        return RestClients.create(clientConfiguration).rest()
    }

}