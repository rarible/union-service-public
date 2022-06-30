package com.rarible.protocol.union.search.indexer.config

import com.rarible.core.daemon.DaemonWorkerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "indexer.consumer")
class KafkaProperties(
    val brokerReplicaSet: String,
    val workerCount: Int = 3,
    val daemon: DaemonWorkerProperties = DaemonWorkerProperties(),
)