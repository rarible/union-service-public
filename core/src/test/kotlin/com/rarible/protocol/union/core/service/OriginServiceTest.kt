package com.rarible.protocol.union.core.service

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.Origin
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OriginServiceTest {

    private val ethOrderService: OrderService = mockk {
        every { blockchain } returns BlockchainDto.ETHEREUM
    }

    private val orderServiceRouter = BlockchainRouter(
        listOf(ethOrderService),
        listOf(BlockchainDto.ETHEREUM)
    )

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

        val origins = listOf(
            Origin(whiteLabel, listOf(collection1, collection2)),
            Origin(otherMarket, emptyList())
        )

        every { ethOrderService.getOrigins() } returns origins

        val service = OriginService(orderServiceRouter)

        // Whitelabel + global origins
        assertThat(service.getOrigins(collectionId1)).isEqualTo(listOf(otherMarket, whiteLabel))
        assertThat(service.getOrigins(collectionId2)).isEqualTo(listOf(otherMarket, whiteLabel))
        // Only global origin
        assertThat(service.getOrigins(collectionIdOtherChain)).isEqualTo(listOf(otherMarket))
        assertThat(service.getOrigins(collectionIdWithoutOrigin)).isEqualTo(listOf(otherMarket))
        assertThat(service.getOrigins(null)).isEqualTo(listOf(otherMarket))
    }
}
