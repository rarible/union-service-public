package com.rarible.protocol.union.core

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration
import java.time.Instant

@ConstructorBinding
@ConfigurationProperties("es.optimization")
data class EsOptimizationProperties(
    val lastUpdatedSearchPeriod: Duration = Duration.ofMinutes(30),
    val earliestItemByLastUpdateAt: Instant = Instant.parse("2016-04-26T20:30:48.000Z"),
    val earliestActivityByDate: Instant = Instant.parse("2016-04-26T06:08:46.000Z"),
    val activityDateSearchPeriod: Duration = Duration.ofDays(7),
)
