package com.rarible.protocol.union.api.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties("api.es.optimization")
data class EsOptimizationProperties(
    val lastUpdatedSearchPeriod: Duration = Duration.ofMinutes(30),
)
