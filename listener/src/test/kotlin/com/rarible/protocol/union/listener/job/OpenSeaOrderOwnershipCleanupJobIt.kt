package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.core.event.OutgoingOwnershipEventListener
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.converter.EnrichedOwnershipConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.test.data.randomUnionOwnership
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.listener.test.data.defaultUnionListenerProperties
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class OpenSeaOrderOwnershipCleanupJobIt {

    @Autowired
    lateinit var ownershipRepository: OwnershipRepository

    val ownershipService: EnrichmentOwnershipService = mockk()
    val listener: OutgoingOwnershipEventListener = mockk()
    val filter: OpenSeaCleanupOrderFilter = mockk()

    lateinit var job: OpenSeaOrderOwnershipCleanupJob

    @BeforeEach
    fun beforeEach() {
        job = OpenSeaOrderOwnershipCleanupJob(
            ownershipRepository, ownershipService, listOf(listener), filter, defaultUnionListenerProperties()
        )
        coEvery { listener.onEvent(any()) } returns Unit
        coEvery { filter.isOld(any(), any(), any()) } returns false
        coEvery { ownershipService.enrichOwnership(any()) } answers {
            val shortOwnership = it.invocation.args[0] as ShortOwnership
            EnrichedOwnershipConverter.convert(randomUnionOwnership(shortOwnership.id.toDto()), shortOwnership)
        }
    }

    @Test
    fun `cleanup openSea best sells`() = runBlocking<Unit> {
        val bestSellOs = ShortOrderConverter.convert(randomUnionSellOrderDto().copy(platform = PlatformDto.OPEN_SEA))
        val withOpenSea = ShortOwnershipConverter.convert(randomUnionOwnership(randomEthItemId())).copy(
            bestSellOrder = bestSellOs,
            bestSellOrders = mapOf("123" to bestSellOs)
        )

        val bestSell = ShortOrderConverter.convert(randomUnionSellOrderDto())
        val withoutOpenSea = ShortOwnershipConverter.convert(randomUnionOwnership(randomEthItemId())).copy(
            bestSellOrder = bestSell,
            bestSellOrders = mapOf("123" to bestSell)
        )

        ownershipRepository.save(withOpenSea)
        ownershipRepository.save(withoutOpenSea)

        job.execute(null).collect()

        val updatedOpenSea = ownershipRepository.get(withOpenSea.id)

        assertThat(updatedOpenSea).isNull()

        val skipped = ownershipRepository.get(withoutOpenSea.id)!!

        assertThat(skipped.copy(version = null)).isEqualTo(withoutOpenSea)
    }

}