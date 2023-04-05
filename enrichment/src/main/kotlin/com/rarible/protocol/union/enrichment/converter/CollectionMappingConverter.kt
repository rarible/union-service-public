package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ByContractAddressRule
import com.rarible.protocol.union.enrichment.model.ByContractTokensRule
import com.rarible.protocol.union.enrichment.model.CollectionMapping
import com.rarible.protocol.union.enrichment.model.CollectionResolverProperties
import com.rarible.protocol.union.enrichment.model.CollectionMappingRule
import com.rarible.protocol.union.enrichment.model.CollectionMappings

object CollectionMappingConverter {
    fun parseProperties(collectionResolverProperties: CollectionResolverProperties): CollectionMappings =
        CollectionMappings(collectionResolverProperties.collectionMappings.map { parseMapping(it) })

    private fun parseMapping(mapping: Map<String, Any>): CollectionMapping {
        val collection = mapping[CollectionMapping::collectionId.name]?.let { it as String }
            ?: throw IllegalArgumentException("Mapping has no '${CollectionMapping::collectionId.name}' field")
        val rules = ((mapping[CollectionMapping::rules.name]
            ?: throw IllegalArgumentException("Mapping has no '${CollectionMapping::rules.name}' field")) as Map<String, Any>)
            .values
            .map { parseRule(collection, it as Map<String, Any>) }
        return CollectionMapping(IdParser.parseCollectionId(collection), rules)
    }

    private fun parseRule(collection: String, rule: Map<String, Any>): CollectionMappingRule {
        val type = rule[CollectionMappingRule::type.name]
            ?.let { it as String }
            ?.let { CollectionMappingRule.Type.valueOf(it) }
            ?: throw IllegalArgumentException("Mapping '$collection' must have one of types ${CollectionMappingRule.Type.values()}")
        return when (type) {
            CollectionMappingRule.Type.BY_CONTRACT_ADDRESS -> parseByContractAddressRule(collection, rule)
            CollectionMappingRule.Type.BY_CONTRACT_TOKENS -> parseByContractTokensRule(collection, rule)
        }
    }

    private fun parseByContractAddressRule(collection: String, rule: Map<String, Any>): ByContractAddressRule {
        val contractAddress = rule[ByContractAddressRule::contractAddress.name]
            ?: throw IllegalArgumentException("${CollectionMappingRule.Type.BY_CONTRACT_ADDRESS} mapping '$collection' must have property ${ByContractAddressRule::contractAddress.name}")
        return ByContractAddressRule(contractAddress as String)
    }

    private fun parseByContractTokensRule(collection: String, rule: Map<String, Any>): ByContractTokensRule {
        val contractAddress = rule[ByContractTokensRule::contractAddress.name]
            ?.let { it as String }
            ?: throw IllegalArgumentException("${CollectionMappingRule.Type.BY_CONTRACT_TOKENS} mapping '$collection' must have property ${ByContractTokensRule::contractAddress.name}")
        val tokens = rule[ByContractTokensRule::tokens.name]
            ?.let { (it as Map<String, String>).values.toList() }
            ?: throw IllegalArgumentException("${CollectionMappingRule.Type.BY_CONTRACT_TOKENS} mapping '$collection' must have property ${ByContractTokensRule::tokens.name}")
        return ByContractTokensRule(contractAddress, tokens)
    }
}