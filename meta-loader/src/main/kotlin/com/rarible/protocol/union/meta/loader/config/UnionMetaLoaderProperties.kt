package com.rarible.protocol.union.meta.loader.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("meta-loader")
class UnionMetaLoaderProperties(
    val brokerReplicaSet: String,
    val downloader: DownloaderProperties
)

data class DownloaderProperties(
    val item: Map<String, ExecutorPipelineProperties>,
    val collection: Map<String, ExecutorPipelineProperties>
    // Add the same for the collection
)

data class ExecutorPipelineProperties(
    val workers: Int = 3,
    val batchSize: Int = 32,
    val poolSize: Int = 16
)