package com.rarible.protocol.union.enrichment.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties("meta")
data class UnionMetaProperties(
    val ipfsGateway: String,
    val mediaFetchTimeout: Int,
    val mediaFetchMaxSize: Long,
    val openSeaProxyUrl: String,
    val ktorApacheHttpClientThreadCount: Int = 8,
    var timeoutSyncLoadingMetaMs: Long = 15000,
    val maxLoadingTimeMs: Long = 30000
) {
    val timeoutSyncLoadingMeta: Duration get() = Duration.ofMillis(timeoutSyncLoadingMetaMs)
}
