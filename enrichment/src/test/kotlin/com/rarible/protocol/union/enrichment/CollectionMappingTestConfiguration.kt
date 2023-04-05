package com.rarible.protocol.union.enrichment

import com.rarible.protocol.union.enrichment.configuration.YamlPropertySourceFactory
import com.rarible.protocol.union.enrichment.model.CollectionResolverProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@EnableConfigurationProperties(CollectionResolverProperties::class)
@PropertySource(value = ["classpath:enrichment-test.yaml"], factory = YamlPropertySourceFactory::class)
class CollectionMappingTestConfiguration