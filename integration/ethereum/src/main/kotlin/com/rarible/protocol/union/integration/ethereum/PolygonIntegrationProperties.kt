package com.rarible.protocol.union.integration.ethereum

import com.rarible.protocol.union.core.DefaultBlockchainProperties
import com.rarible.protocol.union.core.DefaultClientProperties
import com.rarible.protocol.union.core.DefaultConsumerProperties
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "integration.polygon")
class PolygonIntegrationProperties(
    enabled: Boolean,
    consumer: DefaultConsumerProperties?,
    client: DefaultClientProperties?
) : DefaultBlockchainProperties(
    BlockchainDto.POLYGON,
    enabled,
    consumer,
    client
)