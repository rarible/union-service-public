package com.rarible.protocol.union.listener.config

import com.rarible.core.daemon.DaemonWorkerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties("listener")
data class UnionListenerProperties(
    val monitoringWorker: DaemonWorkerProperties = DaemonWorkerProperties(),
    val reconciliation: ReconciliationProperties,
    val priceUpdate: PriceUpdateProperties
)

class ReconciliationProperties(
    val orderBatchSize: Int = 50,
    val threadCount: Int = 4,
    val notificationEnabled: Boolean = true
)

class PriceUpdateProperties(
    val rate: Duration = Duration.ofMinutes(5),
    val delay: Duration = Duration.ofMinutes(1)
)
