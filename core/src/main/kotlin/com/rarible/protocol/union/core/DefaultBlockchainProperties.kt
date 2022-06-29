package com.rarible.protocol.union.core

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.protocol.union.dto.BlockchainDto

open class DefaultBlockchainProperties(
    val blockchain: BlockchainDto,
    val enabled: Boolean,
    val consumer: DefaultConsumerProperties?,
    val client: DefaultClientProperties?,
    val daemon: DaemonWorkerProperties = DaemonWorkerProperties(),
    // Consul doesn't allow to specify an array, so it will be comma-separated string
    val auctionContracts: String? = null,
    val origins: Map<String, OriginProperties> = emptyMap()
)

data class OriginProperties(
    val origin: String,
    val collections: String? // Comma-separated values
)