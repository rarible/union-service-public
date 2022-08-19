package com.rarible.protocol.union.integration.immutablex

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.protocol.union.core.DefaultBlockchainProperties
import com.rarible.protocol.union.core.DefaultClientProperties
import com.rarible.protocol.union.core.DefaultConsumerProperties
import com.rarible.protocol.union.core.OriginProperties
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "integration.immutablex")
class ImxIntegrationProperties(
    enabled: Boolean,
    consumer: DefaultConsumerProperties?,
    client: ImxClientProperties?,
    daemon: DaemonWorkerProperties = DaemonWorkerProperties(),
    auctionContracts: String? = null,
    origins: Map<String, OriginProperties> = emptyMap(),
    val apiKey: String,
) : DefaultBlockchainProperties(
    BlockchainDto.IMMUTABLEX,
    enabled,
    consumer,
    client,
    daemon,
    auctionContracts,
    origins
)

class ImxClientProperties(
    url: String,
    val byIdsChunkSize: Int = 16
) : DefaultClientProperties(url)
