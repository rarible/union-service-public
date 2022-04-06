package com.rarible.protocol.union.search.reindexer.config

import com.rarible.core.task.EnableRaribleTask
import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.api.client.ActivityControllerApi
import com.rarible.protocol.union.api.client.UnionApiClientFactory
import com.rarible.protocol.union.search.core.config.SearchConfiguration
import com.rarible.protocol.union.search.core.converter.ElasticActivityConverter
import com.rarible.protocol.union.search.reindexer.task.ActivityTask
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations

@Configuration
@EnableRaribleTask
@Import(SearchConfiguration::class)
@EnableConfigurationProperties(SearchReindexerProperties::class)
class SearchReindexerConfiguration(
    val properties: SearchReindexerProperties
) {

    @Bean
    fun activityClient(factory: UnionApiClientFactory): ActivityControllerApi {
        return factory.createActivityApiClient()
    }

    @Bean
    fun activityTask(
        activityClient: ActivityControllerApi,
        taskRepository: TaskRepository,
        esOperations: ReactiveElasticsearchOperations,
        activityConverter: ElasticActivityConverter
    ): TaskHandler<String> {
        return ActivityTask(this, activityClient, taskRepository, esOperations, activityConverter)
    }

}