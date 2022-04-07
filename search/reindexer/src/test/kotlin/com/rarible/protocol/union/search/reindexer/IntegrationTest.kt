package com.rarible.protocol.union.search.reindexer

import com.rarible.core.test.ext.ElasticsearchTest
import com.rarible.core.test.ext.KafkaTest
import com.rarible.protocol.union.search.core.config.SearchConfiguration
import com.rarible.protocol.union.search.reindexer.config.SearchReindexerConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles


//@KafkaTest
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
@Import(value = [SearchConfiguration::class, SearchReindexerConfiguration::class])
annotation class IntegrationTest
