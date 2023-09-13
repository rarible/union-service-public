package com.rarible.protocol.union.listener.repository

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortOrder
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.enrichment.test.data.randomUnionAddress
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrder
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import java.time.Instant

@IntegrationTest
class OwnershipRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @Test
    fun `find by platform with sell order - ok`() = runBlocking<Unit> {
        val ownershipId3 = OwnershipIdDto(BlockchainDto.ETHEREUM, "${Address.ONE().hex()}:3", randomUnionAddress())
        val ownershipId6 = OwnershipIdDto(BlockchainDto.ETHEREUM, "${Address.ONE().hex()}:6", randomUnionAddress())

        val now = nowMillis().minusSeconds(1)

        val ownership1 = randomShortOwnership(randomEthOwnershipId(), now.minusSeconds(1), PlatformDto.OPEN_SEA)
        val ownership2 = randomShortOwnership(randomEthOwnershipId(), now, PlatformDto.RARIBLE)
        val ownership3 = randomShortOwnership(ownershipId3, now, PlatformDto.OPEN_SEA)
        val ownership4 = randomShortOwnership(randomEthOwnershipId(), now, PlatformDto.RARIBLE)
        val ownership5 = randomShortOwnership(randomEthOwnershipId(), now.minusSeconds(3), PlatformDto.OPEN_SEA)
        val ownership6 = randomShortOwnership(ownershipId6, now, PlatformDto.OPEN_SEA)
        val ownership7 = randomShortOwnership(randomEthOwnershipId(), now, null)

        val openSeaItems = ownershipRepository.findByPlatformWithSell(
            platform = PlatformDto.OPEN_SEA,
            fromOwnershipId = null,
            fromLastUpdatedAt = Instant.now(),
            limit = null
        ).toList()

        assertThat(openSeaItems.map { it.id })
            .isEqualTo(listOf(ownership3.id, ownership6.id, ownership1.id, ownership5.id))

        val fromOpenSeaItems = ownershipRepository.findByPlatformWithSell(
            PlatformDto.OPEN_SEA,
            fromOwnershipId = ownership3.id,
            fromLastUpdatedAt = ownership3.lastUpdatedAt
        ).toList()

        assertThat(fromOpenSeaItems.map { it.id }).isEqualTo(listOf(ownership6.id, ownership1.id, ownership5.id))
    }

    @Test
    fun findIdsByLastUpdatedAt() = runBlocking<Unit> {
        val ownership1 = ownershipRepository.save(
            randomShortOwnership().copy(
                lastUpdatedAt = Instant.ofEpochMilli(1000),
                blockchain = BlockchainDto.ETHEREUM,
                itemId = "1"
            )
        )
        val ownership2 = ownershipRepository.save(
            randomShortOwnership().copy(
                lastUpdatedAt = Instant.ofEpochMilli(2000),
                blockchain = BlockchainDto.ETHEREUM,
                itemId = "2"
            )
        )
        val ownership3 = ownershipRepository.save(
            randomShortOwnership().copy(
                lastUpdatedAt = Instant.ofEpochMilli(3000),
                blockchain = BlockchainDto.ETHEREUM,
                itemId = "3"
            )
        )
        val ownership4 = ownershipRepository.save(
            randomShortOwnership().copy(
                lastUpdatedAt = Instant.ofEpochMilli(4000),
                blockchain = BlockchainDto.ETHEREUM,
                itemId = "4"
            )
        )
        val ownership5 = ownershipRepository.save(
            randomShortOwnership().copy(
                lastUpdatedAt = Instant.ofEpochMilli(4000),
                blockchain = BlockchainDto.ETHEREUM,
                itemId = "5"
            )
        )
        val ownership6 = ownershipRepository.save(
            randomShortOwnership().copy(
                lastUpdatedAt = Instant.ofEpochMilli(6000),
                blockchain = BlockchainDto.ETHEREUM,
                itemId = "6"
            )
        )
        ownershipRepository.save(
            randomShortOwnership().copy(
                lastUpdatedAt = Instant.ofEpochMilli(8000),
                blockchain = BlockchainDto.ETHEREUM,
                itemId = "7"
            )
        )

        var result = ownershipRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = Instant.ofEpochMilli(500),
            lastUpdatedTo = Instant.ofEpochMilli(7000),
            fromId = null,
            size = 2
        )
        assertThat(result.map { it.id }).containsExactlyInAnyOrder(ownership1.id, ownership2.id)

        result = ownershipRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = result.last().lastUpdatedAt,
            lastUpdatedTo = Instant.ofEpochMilli(7000),
            fromId = result.last().id,
            size = 2
        )
        assertThat(result.map { it.id }).containsExactlyInAnyOrder(ownership3.id, ownership4.id)

        result = ownershipRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = result.last().lastUpdatedAt,
            lastUpdatedTo = Instant.ofEpochMilli(7000),
            fromId = result.last().id,
            size = 10
        )
        assertThat(result.map { it.id }).containsExactlyInAnyOrder(ownership5.id, ownership6.id)
    }

    private suspend fun randomShortOwnership(
        ownershipId: OwnershipIdDto,
        lastUpdatedAt: Instant,
        platform: PlatformDto? = null,
    ): ShortOwnership {
        return ownershipRepository.save(
            randomShortOwnership(ownershipId).copy(
                bestSellOrder = platform?.let { randomSellOrder(platform) },
                lastUpdatedAt = lastUpdatedAt
            )
        )
    }

    private fun randomSellOrder(platform: PlatformDto): ShortOrder {
        val randomSellOrder = randomUnionSellOrder(randomEthItemId())
            .copy(platform = platform)

        return ShortOrderConverter.convert(randomSellOrder)
    }
}
