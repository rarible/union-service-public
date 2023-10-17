package com.rarible.protocol.union.meta.loader.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties("meta-loader")
class UnionMetaLoaderProperties(
    val downloader: DownloaderProperties
)

data class DownloaderProperties(
    val item: Map<String, ExecutorPipelineProperties> = emptyMap(),
    val collection: Map<String, ExecutorPipelineProperties> = emptyMap(),
    val limits: List<DownloadLimit>
    // Add the same for the collection
)

data class ExecutorPipelineProperties(
    val poolSize: Int = 16
)

data class DownloadLimit(
    // If meta has been downloaded (or failed) N times, then next retry allowed only after some interval
    val iterations: Int,
    val interval: Duration
)
