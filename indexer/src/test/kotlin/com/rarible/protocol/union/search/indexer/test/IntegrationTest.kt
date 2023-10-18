package com.rarible.protocol.union.search.indexer.test

import com.rarible.core.test.ext.ElasticsearchTest
import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.protocol.union.core.es.ElasticsearchBootstrapperTestConfig
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.search.indexer.config.IndexerConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@KafkaTest
@MongoTest
@MongoCleanup
@ElasticsearchTest
@EnableAutoConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "application.environment = test",
        "spring.cloud.consul.config.enabled = false",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "logging.logjson.enabled = false",
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
@Import(
    value = [ElasticsearchBootstrapperTestConfig::class, TestIndexerConfiguration::class, IndexerConfiguration::class, TestUnionSearchConfiguration::class]
)
@ContextConfiguration(classes = [SearchConfiguration::class])
annotation class IntegrationTest
