package com.rarible.protocol.union.integration.ethereum

import com.rarible.protocol.union.core.DefaultConsumerProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.subchains
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("integration")
class EthIntegrationProperties(
    eth: Map<String, EthEvmIntegrationProperties> = emptyMap()
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // EVMs integrated without our indexer
    private val exclude = setOf(BlockchainDto.IMMUTABLEX)
    private val evms = BlockchainGroupDto.ETHEREUM.subchains().filter { it !in exclude }

    val blockchains = this.evms.mapNotNull {
        val properties = eth[it.name.lowercase()] ?: return@mapNotNull null
        if (!properties.enabled) return@mapNotNull null
        it to properties
    }.associateBy({ it.first }, { it.second })

    // Just to have active blockchains in the same order as they defined in BlockchainGroup
    val active = this.evms.filter { blockchains.containsKey(it) }

    init {
        logger.info("Found enabled configurations of ETHEREUM EVMs: $active")
    }
}

data class EthEvmIntegrationProperties(
    val enabled: Boolean,
    val consumer: DefaultConsumerProperties?,
    val auctionContracts: List<String> = emptyList(),
    val origins: Map<String, OriginProperties> = emptyMap()
)

data class OriginProperties(
    val origin: String,
    val collections: List<String> = emptyList()
)
