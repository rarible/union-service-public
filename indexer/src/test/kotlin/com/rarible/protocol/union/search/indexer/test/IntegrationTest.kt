package com.rarible.protocol.union.search.indexer.test

import com.rarible.core.test.ext.ElasticsearchTest
import com.rarible.core.test.ext.KafkaTest
import com.rarible.protocol.union.core.es.ElasticsearchBootstraperTestConfig
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.search.indexer.config.KafkaConsumerConfiguration
import com.rarible.protocol.union.search.indexer.config.UnionIndexerConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@KafkaTest
@ElasticsearchTest
@EnableAutoConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "application.environment = test",
        "spring.cloud.consul.config.enabled = false",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "listener.consumer.workerCount = 1",
        "handler.activity.enabled = true",
        "handler.order.enabled = true",
        "handler.collection.enabled = true",
        "handler.ownership.enabled = true",
        "handler.item.enabled = true",
    ]
)
@ActiveProfiles("test")
@Import(value = [ElasticsearchBootstraperTestConfig::class, TestIndexerConfiguration::class, UnionIndexerConfiguration::class, TestUnionSearchConfiguration::class])
@ContextConfiguration(classes = [SearchConfiguration::class, KafkaConsumerConfiguration::class])
annotation class IntegrationTest
