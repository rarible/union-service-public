package com.rarible.protocol.union.enrichment.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.union.core.es.ElasticsearchBootstrapperTestConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Lazy

@Lazy
@Configuration
@EnableAutoConfiguration
@Import(ElasticsearchBootstrapperTestConfig::class)
class TestEnrichmentConfiguration {
    @Bean
    fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
        return ApplicationEnvironmentInfo("test", "test.com")
    }

    @Bean
    fun meterRegistry(): SimpleMeterRegistry = SimpleMeterRegistry()
}
