package com.rarible.protocol.union.integration.solana

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.protocol.union.core.DefaultBlockchainProperties
import com.rarible.protocol.union.core.DefaultClientProperties
import com.rarible.protocol.union.core.DefaultConsumerProperties
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "integration.solana")
class SolanaIntegrationProperties(
    enabled: Boolean,
    consumer: DefaultConsumerProperties?,
    client: DefaultClientProperties?,
    daemon: DaemonWorkerProperties = DaemonWorkerProperties(),
    auctionContracts: String? = null
) : DefaultBlockchainProperties(
    BlockchainDto.SOLANA,
    enabled,
    consumer,
    client,
    daemon,
    auctionContracts
)
