package com.rarible.protocol.union.enrichment.configuration

import com.rarible.protocol.union.enrichment.converter.CollectionMappingConverter
import com.rarible.protocol.union.enrichment.model.CollectionResolverProperties
import com.rarible.protocol.union.enrichment.model.CollectionMappings
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource

@Configuration
@EnableConfigurationProperties(CollectionResolverProperties::class)
class CollectionResolverConfiguration {

    @Configuration
    @Profile("prod")
    @PropertySource(name = "classpath:enrichment-prod.yaml", factory = YamlPropertySourceFactory::class)
    class Prod

    @Configuration
    @Profile("testnet")
    @PropertySource(name = "classpath:enrichment-dev.yaml", factory = YamlPropertySourceFactory::class)
    class Dev

    @Configuration
    @Profile("dev")
    @PropertySource(name = "classpath:enrichment-testnet.yaml", factory = YamlPropertySourceFactory::class)
    class Testnet

    @Bean
    fun collectionMappings(collectionResolverProperties: CollectionResolverProperties): CollectionMappings =
        CollectionMappingConverter.parseProperties(collectionResolverProperties)
}