package com.rarible.protocol.union.worker

import com.rarible.core.test.ext.ElasticsearchTest
import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.protocol.solana.api.client.autoconfigure.SolanaApiClientAutoConfiguration
import com.rarible.protocol.union.enrichment.configuration.EnrichmentApiConfiguration
import com.rarible.protocol.union.core.SearchConfiguration
import com.rarible.protocol.union.worker.config.WorkerConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@KafkaTest
@MongoTest
@MongoCleanup
@ElasticsearchTest
@EnableAutoConfiguration(
    exclude = [
        // duplicate beans
        SolanaApiClientAutoConfiguration::class
    ]
)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "application.environment = test",
        "spring.cloud.consul.config.enabled = false",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "local.server.port = 9090",
        "local.server.host = localhost"
    ]
)
@ActiveProfiles("test")
@Import(
    value = [
        TestWorkerConfiguration::class,
        EnrichmentApiConfiguration::class,
        SearchConfiguration::class,
        WorkerConfiguration::class]
)
@ComponentScan(basePackages = ["com.rarible.protocol.union.worker", "com.rarible.protocol.union.enrichment"])
annotation class IntegrationTest
