package com.rarible.protocol.union.listener.config

import com.rarible.core.daemon.DaemonWorkerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("listener")
class UnionListenerProperties(
    val monitoringWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val producer: ProducerProperties,
    val consumer: BlockchainConsumerSet
)

data class ProducerProperties(
    val brokerReplicaSet: String
)

data class ConsumerProperties(
    val brokerReplicaSet: String
)

data class BlockchainConsumerSet(
    val ethereum: ConsumerProperties,
    val flow: ConsumerProperties
)