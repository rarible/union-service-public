package com.rarible.protocol.union.listener.config

import com.rarible.core.daemon.DaemonWorkerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties("listener")
data class UnionListenerProperties(
    val monitoringWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val consumer: BlockchainConsumerSet,
    val reconciliation: ReconciliationProperties,
    val priceUpdate: PriceUpdateProperties
)

data class BlockchainConsumerSet(
    val ethereum: ConsumerProperties,
    val polygon: ConsumerProperties,
    val flow: ConsumerProperties,
    val tezos: ConsumerProperties
)

data class ConsumerProperties(
    val brokerReplicaSet: String,
    val ownershipWorkers: Int = 1,
    val orderWorkers: Int = 1,
    val itemWorkers: Int = 1,
    val username: String? = null,
    val password: String? = null
)

class ReconciliationProperties(
    val orderBatchSize: Int = 50,
    val threadCount: Int = 4
)

class PriceUpdateProperties(
    val rate: Duration = Duration.ofMinutes(5),
    val delay: Duration = Duration.ofMinutes(1)
)
