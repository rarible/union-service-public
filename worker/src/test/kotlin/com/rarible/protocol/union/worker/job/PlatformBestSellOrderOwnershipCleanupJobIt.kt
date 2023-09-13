package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.core.producer.UnionInternalOwnershipEventProducer
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.enrichment.test.data.randomUnionOwnership
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrder
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.worker.IntegrationTest
import com.rarible.protocol.union.worker.config.PlatformBestSellCleanUpProperties
import com.rarible.protocol.union.worker.config.WorkerProperties
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class PlatformBestSellOrderOwnershipCleanupJobIt {

    @Autowired
    lateinit var ownershipRepository: OwnershipRepository

    val internalOwnershipEventProducer: UnionInternalOwnershipEventProducer = mockk()
    val properties = mockk<WorkerProperties>() {
        every { platformBestSellCleanup } returns PlatformBestSellCleanUpProperties(enabled = true)
    }

    lateinit var job: PlatformBestSellOrderOwnershipCleanupJob

    @BeforeEach
    fun beforeEach() {
        job = PlatformBestSellOrderOwnershipCleanupJob(
            ownershipRepository, internalOwnershipEventProducer, properties
        )
        coEvery { internalOwnershipEventProducer.sendChangeEvent(any()) } returns Unit
    }

    @Test
    fun `cleanup openSea best sells`() = runBlocking<Unit> {
        val bestSellOs = ShortOrderConverter.convert(randomUnionSellOrder().copy(platform = PlatformDto.OPEN_SEA))
        val withOpenSea = ShortOwnershipConverter.convert(randomUnionOwnership(randomEthItemId())).copy(
            bestSellOrder = bestSellOs,
            bestSellOrders = mapOf("123" to bestSellOs)
        )

        val bestSell = ShortOrderConverter.convert(randomUnionSellOrder())
        val withoutOpenSea = ShortOwnershipConverter.convert(randomUnionOwnership(randomEthItemId())).copy(
            bestSellOrder = bestSell,
            bestSellOrders = mapOf("123" to bestSell)
        )

        ownershipRepository.save(withOpenSea)
        ownershipRepository.save(withoutOpenSea)

        job.handle(null, PlatformDto.OPEN_SEA.name).collect()

        val updatedOpenSea = ownershipRepository.get(withOpenSea.id)

        assertThat(updatedOpenSea?.bestSellOrder).isNull()
        assertThat(updatedOpenSea?.bestSellOrders).isEmpty()

        val skipped = ownershipRepository.get(withoutOpenSea.id)!!

        assertThat(skipped.copy(version = null)).isEqualTo(withoutOpenSea)
    }
}
