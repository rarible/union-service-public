package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.model.ByContractAddressRule
import com.rarible.protocol.union.enrichment.model.ByContractTokensRule
import com.rarible.protocol.union.enrichment.model.CollectionMapping
import com.rarible.protocol.union.enrichment.model.CollectionMappings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.AddressFactory

class CollectionResolverServiceTest {
    private val substituteCollection: CollectionIdDto = CollectionIdDto(BlockchainDto.ETHEREUM, "artificial_collection")

    @Test
    fun `by contract address and tokens`() {
        val collection1 = CollectionIdDto(BlockchainDto.ETHEREUM, AddressFactory.create().toString())
        val token1 = ItemIdDto(BlockchainDto.ETHEREUM, AddressFactory.create().toString())
        val collection2 = CollectionIdDto(BlockchainDto.ETHEREUM, AddressFactory.create().toString())
        val token2 = ItemIdDto(BlockchainDto.ETHEREUM, AddressFactory.create().toString())
        val service = CollectionResolverService(
            CollectionMappings(
                listOf(
                    CollectionMapping(
                        collectionId = substituteCollection,
                        rules = listOf(
                            ByContractAddressRule(contractAddress = collection1.fullId()),
                            ByContractTokensRule(
                                contractAddress = collection2.fullId(),
                                tokens = listOf(token1.fullId())
                            ),
                        )
                    )
                )
            )
        )
        assertThat(service.collectionId(collection1, token1)).isEqualTo(substituteCollection)
        assertThat(service.collectionId(collection2, token2)).isEqualTo(collection2)
    }


    @Test
    fun `empty mapping`() {
        val collection = CollectionIdDto(BlockchainDto.ETHEREUM, AddressFactory.create().toString())
        val token = ItemIdDto(BlockchainDto.ETHEREUM, AddressFactory.create().toString())
        val service = CollectionResolverService(CollectionMappings(emptyList()))
        assertThat(service.collectionId(collection, token)).isEqualTo(collection)
    }
}