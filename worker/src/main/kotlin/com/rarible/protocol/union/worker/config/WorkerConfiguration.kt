package com.rarible.protocol.union.worker.config

import com.rarible.core.task.EnableRaribleTask
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.EsRepository
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.elasticsearch.bootstrap.ElasticsearchBootstrapper
import com.rarible.protocol.union.core.model.elastic.EsEntitiesConfig
import com.rarible.protocol.union.enrichment.configuration.EnrichmentApiConfiguration
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.MetaRefreshRequestRepository
import com.rarible.protocol.union.worker.job.BestOrderCheckJob
import com.rarible.protocol.union.worker.job.BestOrderCheckJobHandler
import com.rarible.protocol.union.worker.job.MetaRefreshRequestCleanupJob
import com.rarible.protocol.union.worker.job.ReconciliationMarkJob
import com.rarible.protocol.union.worker.job.ReconciliationMarkJobHandler
import com.rarible.protocol.union.worker.task.search.ReindexService
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations

@Configuration
@Import(
    value = [
        EnrichmentApiConfiguration::class,
        SearchConfiguration::class,
        KafkaConsumerConfiguration::class,
    ]
)
@EnableRaribleTask
@EnableConfigurationProperties(WorkerProperties::class, RateLimiterProperties::class)
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
    fun itemReindexProperties(): ItemReindexProperties {
        return properties.searchReindex.item
    }

    @Bean
    fun orderReindexProperties(): OrderReindexProperties {
        return properties.searchReindex.order
    }

    @Bean
    fun ownershipReindexProperties(): OwnershipReindexProperties {
        return properties.searchReindex.ownership
    }

    @Bean
    fun mocaXpCustomAttributesProviderProperties(): MocaXpCustomAttributesProviderProperties {
        return properties.itemMetaCustomAttributesJob.providers.mocaXp
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    @Bean(initMethod = "bootstrap")
    @Profile("!test")
    fun elasticsearchBootstrap(
        reactiveElasticSearchOperations: ReactiveElasticsearchOperations,
        esNameResolver: EsNameResolver,
        indexService: IndexService,
        esRepositories: List<EsRepository>,
        reindexService: ReindexService,
        highLevelClient: RestHighLevelClient,
    ): ElasticsearchBootstrapper {
        return ElasticsearchBootstrapper(
            esNameResolver = esNameResolver,
            esOperations = reactiveElasticSearchOperations,
            entityDefinitions = EsEntitiesConfig.prodEsEntities(),
            reindexSchedulingService = reindexService,
            indexService = indexService,
            repositories = esRepositories,
            restHighLevelClient = highLevelClient,
        )
    }

    @Bean
    @ConditionalOnProperty(name = ["worker.price-update.enabled"], havingValue = "true")
    fun bestOrderCheckJob(
        handler: BestOrderCheckJobHandler,
        properties: WorkerProperties,
        meterRegistry: MeterRegistry,
    ): BestOrderCheckJob {
        return BestOrderCheckJob(handler, properties, meterRegistry)
    }

    @Bean
    @ConditionalOnProperty(name = ["worker.reconcile-marks.enabled"], havingValue = "true")
    fun reconciliationMarkJob(
        handler: ReconciliationMarkJobHandler,
        properties: WorkerProperties,
        meterRegistry: MeterRegistry,
    ): ReconciliationMarkJob {
        return ReconciliationMarkJob(handler, properties, meterRegistry)
    }

    @Bean
    @ConditionalOnProperty(name = ["worker.collection-meta-refresh-request-cleanup.enabled"], havingValue = "true")
    fun collectionMetaRefreshRequestCleanupJob(
        metaRefreshRequestRepository: MetaRefreshRequestRepository,
        properties: WorkerProperties,
        meterRegistry: MeterRegistry,
    ): MetaRefreshRequestCleanupJob {
        return MetaRefreshRequestCleanupJob(metaRefreshRequestRepository, properties, meterRegistry)
    }
}
