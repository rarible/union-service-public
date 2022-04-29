package com.rarible.protocol.union.core.es

import com.rarible.protocol.union.core.elasticsearch.EsMetadataRepository
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.elasticsearch.NoopReindexSchedulingService
import com.rarible.protocol.union.core.elasticsearch.bootstrap.ElasticsearchBootstraper
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations

@Import(EsMetadataRepository::class)
class ElasticsearchBootstraperTestConfig {

    @Bean(initMethod = "bootstrap")
    fun elasticsearchBootstrap(
        reactiveElasticSearchOperations: ReactiveElasticsearchOperations,
        esNameResolver: EsNameResolver,
        indexService: IndexService
    ): ElasticsearchBootstraper {

        return ElasticsearchBootstraper(
            esNameResolver = esNameResolver,
            esOperations = reactiveElasticSearchOperations,
            entityDefinitions = EsEntitiesConfig.createEsEntities(),
            reindexSchedulingService = NoopReindexSchedulingService(indexService),
            forceUpdate = emptySet(),
            indexService = indexService
        )
    }
}