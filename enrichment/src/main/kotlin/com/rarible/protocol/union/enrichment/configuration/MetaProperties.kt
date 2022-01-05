package com.rarible.protocol.union.enrichment.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties("meta")
data class MetaProperties(
    val ipfsGateway: String,
    val mediaFetchTimeout: Int,
    val mediaFetchMaxSize: Long,
    val openSeaProxyUrl: String,
    val timeoutSyncLoadingMetaMs: Long = 3000
) {
    val timeoutSyncLoadingMeta: Duration get() = Duration.ofMillis(timeoutSyncLoadingMetaMs)
}
