package com.rarible.protocol.union.search.indexer.test

import com.rarible.core.test.ext.ElasticsearchTest
import com.rarible.core.test.ext.KafkaTest
import com.rarible.protocol.union.core.EsIndexProvider
import com.rarible.protocol.union.search.indexer.config.UnionIndexerConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

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
        "logging.logstash.tcp-socket.enabled = false"
    ]
)
@ActiveProfiles("test")
@Import(value = [TestIndexerConfiguration::class, UnionIndexerConfiguration::class, EsIndexProvider::class])
annotation class IntegrationTest
