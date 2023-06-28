package com.rarible.protocol.union.search.indexer.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.search.indexer.config.IndexerProperties
import com.rarible.protocol.union.search.indexer.handler.ActivityEventHandler
import com.rarible.protocol.union.search.indexer.handler.CollectionEventHandler
import com.rarible.protocol.union.search.indexer.handler.ItemEventHandler
import com.rarible.protocol.union.search.indexer.handler.OrderEventHandler
import com.rarible.protocol.union.search.indexer.handler.OwnershipEventHandler
import com.rarible.protocol.union.search.indexer.metrics.IndexerMetricFactory
import com.rarible.protocol.union.search.indexer.metrics.MetricConsumerBatchEventHandlerFactory
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.mockk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary

@Lazy
@Configuration
class TestIndexerConfiguration(
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    private val env = applicationEnvironmentInfo.name

    @Bean
    fun meterRegistry(): MeterRegistry {
        return SimpleMeterRegistry()
    }

    @Bean
    fun indexerMetricFactory(meterRegistry: MeterRegistry): IndexerMetricFactory {
        return IndexerMetricFactory(meterRegistry, IndexerProperties())
    }

    @Bean
    fun metricConsumerFactory(indexerMetricFactory: IndexerMetricFactory): MetricConsumerBatchEventHandlerFactory {
        return MetricConsumerBatchEventHandlerFactory(indexerMetricFactory)
    }

    @Bean
    fun activityHandler(
        featureFlagsProperties: FeatureFlagsProperties,
        repository: EsActivityRepository,
        converter: EsActivityConverter,
        indexerMetricFactory: IndexerMetricFactory
    ): ActivityEventHandler {
        return ActivityEventHandler(featureFlagsProperties, repository, converter, indexerMetricFactory)
    }

    @Bean
    fun orderHandler(repository: EsOrderRepository, metricFactory: IndexerMetricFactory): OrderEventHandler {
        return OrderEventHandler(
            FeatureFlagsProperties(enableOrderSaveImmediateToElasticSearch = true),
            repository,
            metricFactory
        )
    }

    @Bean
    fun collectionHandler(
        repository: EsCollectionRepository,
        indexerMetricFactory: IndexerMetricFactory
    ): CollectionEventHandler {
        return CollectionEventHandler(FeatureFlagsProperties(), repository, indexerMetricFactory)
    }

    @Bean
    fun ownershipHandler(
        repository: EsOwnershipRepository,
        metricFactory: IndexerMetricFactory
    ): OwnershipEventHandler {
        return OwnershipEventHandler(FeatureFlagsProperties(), repository, metricFactory)
    }

    @Bean
    fun itemHandler(repository: EsItemRepository, metricFactory: IndexerMetricFactory): ItemEventHandler {
        return ItemEventHandler(FeatureFlagsProperties(), repository, metricFactory)
    }

    // --- APIs ---

    @Bean
    @Primary
    fun testFlowItemApi(): FlowNftItemControllerApi = mockk()

    @Bean("ethereum.item.api")
    @Primary
    fun testEthereumItemApi(): NftItemControllerApi = mockk()
}
