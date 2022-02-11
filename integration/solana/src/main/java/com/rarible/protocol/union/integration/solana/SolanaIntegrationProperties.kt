package com.rarible.protocol.union.integration.solana

import com.rarible.protocol.union.core.DefaultBlockchainProperties
import com.rarible.protocol.union.core.DefaultClientProperties
import com.rarible.protocol.union.core.DefaultConsumerProperties
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "integration.solana")
class SolanaIntegrationProperties(
    enabled: Boolean
) : DefaultBlockchainProperties(
    BlockchainDto.SOLANA,
    enabled,
    null,
    null
)
