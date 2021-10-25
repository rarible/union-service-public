package com.rarible.protocol.union.api.configuration

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.rarible.protocol.union.dto.UnionModelJacksonModule
import com.rarible.protocol.union.dto.UnionPrimitivesJacksonModule
import com.rarible.protocol.union.enrichment.configuration.EnrichmentApiConfiguration
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@Configuration
@Import(EnrichmentApiConfiguration::class)
class ApiConfiguration {

    @Bean
    fun jsonCustomizer(): Jackson2ObjectMapperBuilderCustomizer? {
        return Jackson2ObjectMapperBuilderCustomizer { builder: Jackson2ObjectMapperBuilder ->
            builder.modules(
                UnionPrimitivesJacksonModule,
                UnionModelJacksonModule,
                KotlinModule(),
                JavaTimeModule()
            )
        }
    }

}