package com.rarible.protocol.union.listener.test

import com.rarible.core.test.ext.ElasticsearchTest
import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.core.test.ext.RedisTest
import com.rarible.protocol.union.core.es.ElasticsearchBootstrapperTestConfig
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@KafkaTest
@MongoTest
@RedisTest
@ElasticsearchTest
@MongoCleanup
@EnableAutoConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = [
            "application.environment = test",
            "spring.cloud.consul.config.enabled = false",
            "spring.cloud.service-registry.auto-registration.enabled = false",
            "spring.cloud.discovery.enabled = false",
            "logging.logstash.tcp-socket.enabled = false",
            "logging.logjson.enabled = false",
    ]
)
@Import(value = [TestListenerConfiguration::class, ElasticsearchBootstrapperTestConfig::class])
@ActiveProfiles("test")
annotation class IntegrationTest