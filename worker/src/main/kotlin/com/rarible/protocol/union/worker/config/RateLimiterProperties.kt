package com.rarible.protocol.union.worker.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "ratelimiter")
data class RateLimiterProperties(
    val period: Long = 10000,
    val maxEntities: Int = 5000,
)