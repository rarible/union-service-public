package com.rarible.protocol.union.search.indexer.config

import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.enrichment.configuration.EnrichmentApiConfiguration
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@EnableConfigurationProperties(value = [KafkaProperties::class, IndexerProperties::class])
@ComponentScan(basePackageClasses = [EsActivityRepository::class])
@Import(
    value = [
        SearchConfiguration::class,
        CoreConfiguration::class,
        EnrichmentApiConfiguration::class,
    ]
)
class UnionIndexerConfiguration