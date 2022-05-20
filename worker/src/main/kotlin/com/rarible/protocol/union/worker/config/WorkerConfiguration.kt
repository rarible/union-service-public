package com.rarible.protocol.union.worker.config

import com.rarible.core.task.EnableRaribleTask
import com.rarible.protocol.union.api.client.ActivityControllerApi
import com.rarible.protocol.union.api.client.CollectionControllerApi
import com.rarible.protocol.union.api.client.UnionApiClientFactory
import com.rarible.protocol.union.api.client.autoconfigure.UnionApiClientAutoConfiguration
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.elasticsearch.bootstrap.ElasticsearchBootstrapper
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.worker.task.search.ReindexService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations

@Configuration
@ComponentScan(basePackageClasses = [EsActivityRepository::class])
@Import(
    value = [
        SearchConfiguration::class
    ]
)
@EnableRaribleTask
@EnableConfigurationProperties(WorkerProperties::class)
@EnableAutoConfiguration(exclude = [UnionApiClientAutoConfiguration::class])
class WorkerConfiguration(
    val properties: WorkerProperties
) {
    @Bean
    fun activityReindexProperties(): ActivityReindexProperties {
        return properties.searchReindex.activity
    }

    @Bean
    fun collectionReindexProperties(): CollectionReindexProperties {
        return properties.searchReindex.collection
    }

    @Bean
    fun orderReindexProperties(): OrderReindexProperties {
        return properties.searchReindex.order
    }

    @Bean
    fun activityClient(factory: UnionApiClientFactory): ActivityControllerApi {
        return factory.createActivityApiClient()
    }

    @Bean
    fun collectionClient(factory: UnionApiClientFactory): CollectionControllerApi {
        return factory.createCollectionApiClient()
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    @Bean(initMethod = "bootstrap")
    fun elasticsearchBootstrap(
        reactiveElasticSearchOperations: ReactiveElasticsearchOperations,
        esNameResolver: EsNameResolver,
        reindexerService: ReindexService,
        indexService: IndexService
    ): ElasticsearchBootstrapper {

        return ElasticsearchBootstrapper(
            esNameResolver = esNameResolver,
            esOperations = reactiveElasticSearchOperations,
            entityDefinitions = EsEntitiesConfig.prodEsEntities(),
            reindexSchedulingService = reindexerService,
            forceUpdate = emptySet(),
            indexService = indexService
        )
    }
}