package com.rarible.protocol.union.search.indexer.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "indexer")
data class IndexerProperties(
    val metrics: MetricsProperties = MetricsProperties()
)

data class MetricsProperties(
    val rootPath: String = "protocol.union.indexer"
)