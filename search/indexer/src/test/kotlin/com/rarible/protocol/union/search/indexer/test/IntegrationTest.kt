package com.rarible.protocol.union.search.indexer.test

import com.rarible.core.test.ext.KafkaTest
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@KafkaTest
@EnableAutoConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = [
        "application.environment = test",
        "spring.cloud.consul.config.enabled = false",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "logging.logstash.tcp-socket.enabled = false"
    ]
)
@Import(value = [TestListenerConfiguration::class])
@ActiveProfiles("test")
annotation class IntegrationTest
