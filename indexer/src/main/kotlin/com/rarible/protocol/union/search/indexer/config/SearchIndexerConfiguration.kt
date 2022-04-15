package com.rarible.protocol.union.search.indexer.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(value = [KafkaProperties::class])
@ComponentScan(basePackages = ["com.rarible.protocol.union.search"])
class SearchIndexerConfiguration {
}