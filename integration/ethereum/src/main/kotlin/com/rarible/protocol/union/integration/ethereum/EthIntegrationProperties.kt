package com.rarible.protocol.union.integration.ethereum

import com.rarible.protocol.union.core.DefaultConsumerProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.subchains
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties
class EthIntegrationProperties(
    integration: Map<String, EthEvmIntegrationProperties> = emptyMap()
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // EVMs integrated without our indexer
    private val exclude = setOf(BlockchainDto.IMMUTABLEX)
    private val evms = BlockchainGroupDto.ETHEREUM.subchains().filter { it !in exclude }

    // TODO we need to re-design integration properties, otherwise it will gather ALL integrations
    val blockchains = evms.mapNotNull {
        val properties = integration[it.name.lowercase()] ?: return@mapNotNull null
        if (!properties.enabled) return@mapNotNull null
        it to properties
    }.associateBy({ it.first }, { it.second })

    // Just to have active blockchains in the same order as they defined in BlockchainGroup
    val active = evms.filter { blockchains.containsKey(it) }

    init {
        logger.info("Found enabled configurations of ETHEREUM EVMs: $active")
    }
}

data class EthEvmIntegrationProperties(
    val enabled: Boolean,
    val consumer: DefaultConsumerProperties?,
    val auctionContracts: String? = null,
    val origins: Map<String, OriginProperties> = emptyMap()
)

data class OriginProperties(
    val origin: String,
    val collections: String? // Comma-separated values
)
