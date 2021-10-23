package com.rarible.protocol.union.core

import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("integration")
class IntegrationProperties(
    integrations: List<BlockchainProperties>
) {

    val integrations = integrations.associateBy { it.blockchain }

    fun get(blockchain: BlockchainDto) = integrations[blockchain]!!

}

data class BlockchainProperties(
    val blockchain: BlockchainDto,
    val enabled: Boolean,
    val consumer: ConsumerProperties?,
    val client: ClientProperties?
)

data class ConsumerProperties(
    val brokerReplicaSet: String,
    val workers: Map<String, Int>,
    val username: String? = null,
    val password: String? = null
)

data class ClientProperties(
    val url: String? = null
)