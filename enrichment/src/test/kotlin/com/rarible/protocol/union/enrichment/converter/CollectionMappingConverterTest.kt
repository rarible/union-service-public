package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.CollectionMappingTestConfiguration
import com.rarible.protocol.union.enrichment.model.ByContractAddressRule
import com.rarible.protocol.union.enrichment.model.ByContractTokensRule
import com.rarible.protocol.union.enrichment.model.CollectionMapping
import com.rarible.protocol.union.enrichment.model.CollectionResolverProperties
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [CollectionMappingTestConfiguration::class, CollectionMappingConverter::class])
class CollectionMappingConverterTest {

    @Autowired
    lateinit var collectionResolverProperties: CollectionResolverProperties

    @Test
    fun `parse yaml properties`() {
        val collectionMappings = CollectionMappingConverter.parseProperties(collectionResolverProperties)
        Assertions.assertThat(collectionMappings.mappings).hasSize(2)
        Assertions.assertThat(collectionMappings.mappings).containsExactly(
            CollectionMapping(
                CollectionIdDto(BlockchainDto.ETHEREUM, "collection_1"),
                listOf(
                    ByContractAddressRule("ETHEREUM:0x123"),
                    ByContractTokensRule("ETHEREUM:0x456", listOf("ETHEREUM:0xabcf", "ETHEREUM:0xfbcda")),
                )
            ),
            CollectionMapping(
                CollectionIdDto(BlockchainDto.ETHEREUM, "collection_2"),
                listOf(
                    ByContractAddressRule("ETHEREUM:0x789"),
                    ByContractTokensRule("ETHEREUM:0x012", listOf("ETHEREUM:0xbac", "ETHEREUM:0xabc")),
                )
            ),
        )
    }
}