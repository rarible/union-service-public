package com.rarible.protocol.union.core.service

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.DefaultBlockchainProperties
import com.rarible.protocol.union.core.OriginProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OriginServiceTest {

    @Test
    fun `get origins`() {
        val collection1 = randomString()
        val collection2 = randomString()
        val whiteLabel = randomString()
        val otherMarket = randomString()

        val collectionId1 = CollectionIdDto(BlockchainDto.ETHEREUM, collection1)
        val collectionId2 = CollectionIdDto(BlockchainDto.ETHEREUM, collection2)
        // Should not be found, another blockchain
        val collectionIdOtherChain = CollectionIdDto(BlockchainDto.SOLANA, collection2)
        // Should not be found, not specified in origins
        val collectionIdWithoutOrigin = CollectionIdDto(BlockchainDto.FLOW, collection2)

        val properties = createProperties(
            mapOf(
                // Trailing/leading whitespaces and empty values should be filtered
                "whitelabel" to OriginProperties(whiteLabel, " $collection1 ,$collection2, "),
                "othermarket" to OriginProperties(otherMarket, null)
            )
        )

        val service = OriginService(listOf(properties))

        // Whitelabel + global origins
        assertThat(service.getOrigins(collectionId1)).isEqualTo(listOf(otherMarket, whiteLabel))
        assertThat(service.getOrigins(collectionId2)).isEqualTo(listOf(otherMarket, whiteLabel))
        // Only global origin
        assertThat(service.getOrigins(collectionIdOtherChain)).isEqualTo(listOf(otherMarket))
        assertThat(service.getOrigins(collectionIdWithoutOrigin)).isEqualTo(listOf(otherMarket))
        assertThat(service.getOrigins(null)).isEqualTo(listOf(otherMarket))
    }

    private fun createProperties(origins: Map<String, OriginProperties>): DefaultBlockchainProperties {
        return DefaultBlockchainProperties(
            blockchain = BlockchainDto.ETHEREUM,
            enabled = true,
            consumer = null,
            client = null,
            auctionContracts = null,
            origins = origins
        )
    }
}