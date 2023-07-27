package com.rarible.protocol.union.api.configuration

import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("api")
data class DomainProperties(
    val domains: Map<BlockchainDto, List<String>> = emptyMap(),
) {
    fun findBlockchain(topDomain: String): BlockchainDto? {
        return domains.entries.find { (_, domains) -> topDomain in domains }?.key
    }
}
