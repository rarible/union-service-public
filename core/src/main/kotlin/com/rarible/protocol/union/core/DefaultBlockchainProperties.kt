package com.rarible.protocol.union.core

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.protocol.union.dto.BlockchainDto

open class DefaultBlockchainProperties(
    val blockchain: BlockchainDto,
    val enabled: Boolean,
    val consumer: DefaultConsumerProperties?,
    val client: DefaultClientProperties?,
    val daemon: DaemonWorkerProperties = DaemonWorkerProperties(),
    val auctionContracts: List<String> = listOf()
)