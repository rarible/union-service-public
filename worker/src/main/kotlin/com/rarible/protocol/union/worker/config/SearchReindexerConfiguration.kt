package com.rarible.protocol.union.worker.config

import com.rarible.core.task.EnableRaribleTask
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.api.client.ActivityControllerApi
import com.rarible.protocol.union.api.client.CollectionControllerApi
import com.rarible.protocol.union.api.client.UnionApiClientFactory
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.configuration.EnrichmentApiConfiguration
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.worker.task.CollectionTask
import com.rarible.protocol.union.worker.task.search.activity.ActivityTask
import com.rarible.protocol.union.worker.task.search.activity.ActivityTaskState
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@EnableRaribleTask
@Import(value = [
    SearchConfiguration::class,
    EsActivityRepository::class,
    EnrichmentApiConfiguration::class
])
@EnableConfigurationProperties(SearchReindexerProperties::class)
class SearchReindexerConfiguration(
    val properties: SearchReindexerProperties,
) {

    @Bean
    fun activityClient(factory: UnionApiClientFactory): ActivityControllerApi {
        return factory.createActivityApiClient()
    }

    @Bean
    fun activityTask(
        activityClient: ActivityControllerApi,
        taskRepository: TaskRepository,
        esActivityRepository: EsActivityRepository,
    ): TaskHandler<ActivityTaskState> {
        return ActivityTask(this, activityClient, esActivityRepository, EsActivityConverter)
    }

    @Bean
    fun collectionClient(factory: UnionApiClientFactory): CollectionControllerApi {
        return factory.createCollectionApiClient()
    }

    @Bean
    fun collectionTask(
        collectionClient: CollectionControllerApi,
        repository: EsCollectionRepository,
        activeBlockchains: List<BlockchainDto>
    ): TaskHandler<String> {
        return CollectionTask(activeBlockchains, properties.startReindexCollection, collectionClient, repository)
    }
}
