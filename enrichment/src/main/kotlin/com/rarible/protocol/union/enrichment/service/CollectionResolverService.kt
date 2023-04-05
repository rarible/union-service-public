package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.model.ByContractAddressRule
import com.rarible.protocol.union.enrichment.model.ByContractTokensRule
import com.rarible.protocol.union.enrichment.model.CollectionMappings
import org.springframework.stereotype.Component

@Component
class CollectionResolverService(collectionMappings: CollectionMappings) {
    private val byContractAddress: Map<String, CollectionIdDto>
    private val byContractTokens: Map<Pair<String, String>, CollectionIdDto>

    init {
        byContractAddress = collectionMappings.mappings.flatMap { mapping ->
            mapping.rules.filterIsInstance<ByContractAddressRule>()
                .map { Pair(it.contractAddress, mapping.collectionId) }
        }.toMap()
        byContractTokens = collectionMappings.mappings.flatMap { mapping ->
            mapping.rules.filterIsInstance<ByContractTokensRule>()
                .flatMap { rule ->
                    rule.tokens.map { token ->
                        Pair(
                            Pair(rule.contractAddress, token),
                            mapping.collectionId
                        )
                    }
                }
        }.toMap()
    }

    fun collectionId(collectionId: CollectionIdDto, token: ItemIdDto): CollectionIdDto =
        byContractAddress[collectionId.fullId()]
            ?: byContractTokens[Pair(collectionId.fullId(), token.fullId())]
            ?: collectionId
}
