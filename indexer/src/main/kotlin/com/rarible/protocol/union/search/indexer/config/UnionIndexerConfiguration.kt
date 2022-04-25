package com.rarible.protocol.union.search.indexer.config

import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.IndexMetadataService
import com.rarible.protocol.union.core.elasticsearch.NoopReindexSchedulingService
import com.rarible.protocol.union.core.elasticsearch.bootstrap.ElasticsearchBootstraper
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@EnableConfigurationProperties(value = [KafkaProperties::class])
@ComponentScan(basePackageClasses = [EsActivityRepository::class])
@Import(
    value = [
        SearchConfiguration::class,
    ]
)
class UnionIndexerConfiguration {
    @Bean(initMethod = "bootstrap")
    fun elasticsearchBootstrap(
        elasticsearchHighLevelClient: RestHighLevelClient,
        esNameResolver: EsNameResolver,
        indexMetadataService: IndexMetadataService
    ): ElasticsearchBootstraper {

        return ElasticsearchBootstraper(
            esNameResolver = esNameResolver,
            client = elasticsearchHighLevelClient,
            entityDefinitions = EsEntitiesConfig.createEsEntities(),
            reindexSchedulingService = NoopReindexSchedulingService(indexMetadataService),
            forceUpdate = emptySet()
        )
    }
}