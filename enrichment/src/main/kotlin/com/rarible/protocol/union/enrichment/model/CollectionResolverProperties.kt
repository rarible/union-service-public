package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.dto.CollectionIdDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "common")
class CollectionResolverProperties(collectionMappings: List<Map<String, Any>>?) {
    val collectionMappings: List<Map<String, Any>>

    init {
        this.collectionMappings = collectionMappings ?: listOf()
    }
}

data class CollectionMappings(
    val mappings: List<CollectionMapping>,
)

data class CollectionMapping(
    val collectionId: CollectionIdDto,
    val rules: List<CollectionMappingRule>,
)

interface CollectionMappingRule {
    fun type(): Type

    enum class Type {
        BY_CONTRACT_ADDRESS,
        BY_CONTRACT_TOKENS,
    }
}

data class ByContractAddressRule(
    val contractAddress: String,
) : CollectionMappingRule {
    val type = CollectionMappingRule.Type.BY_CONTRACT_ADDRESS
    override fun type(): CollectionMappingRule.Type = type
}

data class ByContractTokensRule(
    val contractAddress: String,
    val tokens: List<String>,
) : CollectionMappingRule {
    val type = CollectionMappingRule.Type.BY_CONTRACT_TOKENS
    override fun type(): CollectionMappingRule.Type = type
}