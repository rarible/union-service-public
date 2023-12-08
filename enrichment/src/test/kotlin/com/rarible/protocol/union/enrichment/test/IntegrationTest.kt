package com.rarible.protocol.union.enrichment.test

import com.rarible.core.test.ext.ElasticsearchTest
import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.core.test.ext.RedisTest
import com.rarible.protocol.union.enrichment.configuration.EnrichmentConsumerConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.test.context.ActiveProfiles

@KafkaTest
@MongoTest
@MongoCleanup
@RedisTest
@ElasticsearchTest
@SpringBootTest(
    classes = [TestEnrichmentConfiguration::class],
    properties = [
        "application.environment = test",
        "spring.cloud.consul.config.enabled = false",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "logging.logjson.enabled = false",
        "local.server.port = 9090",
        "local.server.host = localhost"
    ]
)
@ActiveProfiles("test")
@ComponentScan(
    basePackages = ["com.rarible.protocol.union.enrichment"], excludeFilters = [
        ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = [EnrichmentConsumerConfiguration::class])
    ]
)
annotation class IntegrationTest
