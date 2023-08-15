package com.rarible.protocol.union.api.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration
import java.time.Instant

@ConstructorBinding
@ConfigurationProperties("api.es.optimization")
data class EsOptimizationProperties(
    val lastUpdatedSearchPeriod: Duration = Duration.ofMinutes(30),
    val earliestItemByLastUpdateAt: Instant = Instant.ofEpochSecond(1461703200) // 2016-04-26T20:30:48Z
)
