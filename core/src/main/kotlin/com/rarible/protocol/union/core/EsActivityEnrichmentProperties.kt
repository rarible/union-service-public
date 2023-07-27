package com.rarible.protocol.union.core

import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("common.es-entity.activity")
@ConstructorBinding
data class EsActivityEnrichmentProperties(
    val blockchainsToQueryItems: Set<BlockchainDto> = setOf(BlockchainDto.SOLANA)
)
