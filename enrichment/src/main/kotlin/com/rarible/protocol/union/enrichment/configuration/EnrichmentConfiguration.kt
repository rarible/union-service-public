package com.rarible.protocol.union.enrichment.configuration

import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.enrichment.converter.EnrichmentConverterPackage
import com.rarible.protocol.union.enrichment.custom.EnrichmentCustomComponentsPackage
import com.rarible.protocol.union.enrichment.repository.EnrichmentRepositoryPackage
import com.rarible.protocol.union.enrichment.service.EnrichmentServicePackage
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource

@EnableRaribleMongo
@Import(value = [CoreConfiguration::class, EnrichmentProducerConfiguration::class])
@ComponentScan(
    basePackageClasses = [
        EnrichmentServicePackage::class,
        EnrichmentRepositoryPackage::class,
        EnrichmentConverterPackage::class,
        EnrichmentCustomComponentsPackage::class,
    ]
)
@PropertySource(
    name = "enrichment",
    value = [
        "classpath:enrichment.yaml",
        "classpath:enrichment-\${application.environment}.yaml"
    ],
    factory = YamlPropertySourceFactory::class,
    ignoreResourceNotFound = true
)
@EnableConfigurationProperties(EnrichmentProperties::class)
class EnrichmentConfiguration(
    private val properties: EnrichmentProperties
) {

    @Bean
    fun enrichmentProducerProperties(): ProducerProperties = properties.producer

    @Bean
    fun enrichmentCommonMetaProperties(): CommonMetaProperties = properties.meta.common

    @Bean
    fun enrichmentItemMetaProperties(): EnrichmentItemMetaProperties = properties.meta.item

    @Bean
    fun enrichmentCollectionProperties(): EnrichmentCollectionProperties = properties.collection

    @Bean
    fun enrichmentCurrencyProperties(): EnrichmentCurrenciesProperties = properties.currencies

    @Bean
    fun enrichmentMattelMetaCustomizerProperties(): EnrichmentMattelMetaCustomizerProperties =
        properties.meta.item.customizers.mattel
}
