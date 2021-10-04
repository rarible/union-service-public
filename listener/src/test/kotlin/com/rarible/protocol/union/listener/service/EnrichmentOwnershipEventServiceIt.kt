package com.rarible.protocol.union.listener.service

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.union.core.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.core.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.enrichment.converter.EnrichedOwnershipConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.test.data.randomEthItemId
import com.rarible.protocol.union.test.data.randomEthLegacyOrderDto
import com.rarible.protocol.union.test.data.randomEthOwnershipDto
import com.rarible.protocol.union.test.data.randomEthOwnershipId
import com.rarible.protocol.union.test.data.randomUnionOwnershipDto
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono

@FlowPreview
@IntegrationTest
class EnrichmentOwnershipEventServiceIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var ownershipEventHandler: EnrichmentOwnershipEventService

    @Autowired
    private lateinit var ownershipService: EnrichmentOwnershipService

    @Test
    fun `update event - ownership doesn't exist`() = runWithKafka {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val ownershipDto = randomUnionOwnershipDto(ownershipId)

        val expected = EnrichedOwnershipConverter.convert(ownershipDto)

        ownershipEventHandler.onOwnershipUpdated(ownershipDto)

        val created = ownershipService.get(ShortOwnershipId(ownershipId))
        // Ownership should not be updated since it wasn't in DB before update
        assertThat(created).isNull()

        // But there should be single Ownership event "as is"
        Wait.waitAssert {
            assertThat(ownershipEvents).hasSize(1)
            val messages = findOwnershipUpdates(ownershipId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(itemId.fullId())
            assertThat(messages[0].id).isEqualTo(itemId.fullId())
            assertThat(messages[0].value.ownershipId).isEqualTo(ownershipId)
            assertThat(messages[0].value.ownership).isEqualTo(expected)
        }
    }

    @Test
    fun `update event - existing ownership updated`() = runWithKafka {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val ethOwnership = randomEthOwnershipDto(ownershipId)

        val bestSellOrder = randomEthLegacyOrderDto(itemId)
        val unionBestSell = EthOrderConverter.convert(bestSellOrder, itemId.blockchain)

        val unionOwnership = EthOwnershipConverter.convert(ethOwnership, itemId.blockchain)
        val shortOwnership = ShortOwnershipConverter.convert(unionOwnership).copy(
            bestSellOrder = ShortOrderConverter.convert(unionBestSell)
        )

        ownershipService.save(shortOwnership)

        coEvery { testEthereumOrderApi.getOrderByHash(unionBestSell.id.value) } returns bestSellOrder.toMono()
        coEvery { testEthereumOwnershipApi.getNftOwnershipById(itemId.value) } returns ethOwnership.toMono()

        ownershipEventHandler.onOwnershipUpdated(unionOwnership)

        val saved = ownershipService.get(shortOwnership.id)!!
        assertThat(saved.bestSellOrder).isEqualTo(shortOwnership.bestSellOrder)

        val expected = EnrichedOwnershipConverter.convert(unionOwnership)
            .copy(bestSellOrder = unionBestSell)

        // We don't have related item in enrichment DB, so expect only Ownership update
        Wait.waitAssert {
            assertThat(ownershipEvents).hasSize(1)
            val messages = findOwnershipUpdates(ownershipId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(itemId.fullId())
            assertThat(messages[0].id).isEqualTo(itemId.fullId())
            assertThat(messages[0].value.ownershipId).isEqualTo(ownershipId)
            assertThat(messages[0].value.ownership).isEqualTo(expected)
        }
    }

    @Test
    fun `on order updated - ownership exists`() = runWithKafka {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val ethOwnership = randomEthOwnershipDto(ownershipId)

        val unionOwnership = EthOwnershipConverter.convert(ethOwnership, itemId.blockchain)
        val shortOwnership = ShortOwnershipConverter.convert(unionOwnership)
        ownershipService.save(shortOwnership)

        val bestSellOrder = randomEthLegacyOrderDto(itemId)
        val unionBestSell = EthOrderConverter.convert(bestSellOrder, itemId.blockchain)

        coEvery { testEthereumOwnershipApi.getNftOwnershipById(ownershipId.value) } returns ethOwnership.toMono()

        ownershipEventHandler.onOwnershipBestSellOrderUpdated(shortOwnership.id, unionBestSell)

        val saved = ownershipService.get(shortOwnership.id)!!
        assertThat(saved.bestSellOrder).isEqualTo(ShortOrderConverter.convert(unionBestSell))

        val expected = EnrichedOwnershipConverter.convert(unionOwnership)
            .copy(bestSellOrder = unionBestSell)

        // Since Item doesn't exist in Enrichment DB, we expect only Ownership event
        Wait.waitAssert {
            assertThat(ownershipEvents).hasSize(1)
            val messages = findOwnershipUpdates(ownershipId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.ownershipId).isEqualTo(ownershipId)
            assertThat(messages[0].value.ownership).isEqualTo(expected)
        }
    }

    @Test
    fun `delete event - existing ownership deleted`() = runWithKafka {
        val itemId = randomEthItemId()
        val ownership = ownershipService.save(randomShortOwnership(itemId))
        val ownershipId = ownership.id.toDto()

        assertThat(ownershipService.get(ownership.id)).isNotNull()

        ownershipEventHandler.onOwnershipDeleted(ownershipId)

        assertThat(ownershipService.get(ownership.id)).isNull()
        Wait.waitAssert {
            val messages = findOwnershipDeletions(ownershipId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(itemId.fullId())
            assertThat(messages[0].id).isEqualTo(itemId.fullId())
            assertThat(messages[0].value.ownershipId).isEqualTo(ownershipId)
        }
    }

    @Test
    fun `delete event - ownership doesn't exist`() = runWithKafka {
        val itemId = randomEthItemId()
        val shortOwnershipId = randomShortOwnership(itemId).id
        val ownershipId = shortOwnershipId.toDto()

        ownershipEventHandler.onOwnershipDeleted(ownershipId)

        assertThat(ownershipService.get(shortOwnershipId)).isNull()

        Wait.waitAssert {
            val messages = findOwnershipDeletions(ownershipId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(itemId.fullId())
            assertThat(messages[0].id).isEqualTo(itemId.fullId())
            assertThat(messages[0].value.ownershipId).isEqualTo(ownershipId)
        }
    }
}