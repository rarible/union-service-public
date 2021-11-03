package com.rarible.protocol.union.listener.service

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.enrichment.converter.EnrichedItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.EnrichmentMetaService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc721
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionAsset
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthLegacyOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address

@FlowPreview
@IntegrationTest
class EnrichmentCollectionEventServiceIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var itemService: EnrichmentItemService

    @Autowired
    lateinit var ethOrderConverter: EthOrderConverter

    @Autowired
    lateinit var enrichmentMetaService: EnrichmentMetaService

    @Autowired
    private lateinit var ownershipService: EnrichmentOwnershipService

    @Autowired
    private lateinit var collectionEventService: EnrichmentCollectionEventService

    @Test
    fun `on best sell collection order updated`() = runWithKafka {
        val itemId = randomEthItemId()
        val shortItem = randomShortItem(itemId)
        val ethItem = randomEthNftItemDto(itemId)
        val unionItem = EthItemConverter.convert(ethItem, itemId.blockchain)
        itemService.save(shortItem)

        val ownershipId = randomEthOwnershipId(itemId)
        val ethOwnership = randomEthOwnershipDto(ownershipId)
        val unionOwnership = EthOwnershipConverter.convert(ethOwnership, itemId.blockchain)
        val shortOwnership = ShortOwnershipConverter.convert(unionOwnership)
        ownershipService.save(shortOwnership)

        val address = UnionAddress(itemId.blockchain, itemId.token.value)
        val bestSellOrder = randomEthLegacyOrderDto(
            randomEthCollectionAsset(Address.apply(address.value)),
            ethOwnership.owner,
            randomEthAssetErc721()
        )
        val unionBestSell = ethOrderConverter.convert(bestSellOrder, itemId.blockchain)

        coEvery { testEthereumItemApi.getNftItemById(itemId.value) } returns ethItem.toMono()
        coEvery { testEthereumItemApi.getNftItemMetaById(itemId.value) } returns ethItem.meta!!.toMono()
        coEvery { testEthereumOwnershipApi.getNftOwnershipById(ownershipId.value) } returns ethOwnership.toMono()

        collectionEventService.onCollectionBestSellOrderUpdate(address, unionBestSell, true)

        val expected = EnrichedItemConverter.convert(unionItem).copy(
            bestSellOrder = unionBestSell,
            meta = enrichmentMetaService.enrichMeta(unionItem.meta!!, ShortItemId(itemId))
        )

        val saved = itemService.get(shortItem.id)!!
        assertThat(saved.bestSellOrder).isEqualTo(ShortOrderConverter.convert(unionBestSell))

        Wait.waitAssert {
            val messages = findItemUpdates(itemId.value)
            assertThat(messages).hasSize(2)

            messages.map { it.value }.forEach {
                assertThat(it.itemId).isEqualTo(itemId)
                assertThat(it.item.id).isEqualTo(expected.id)
                assertThat(it.item.bestSellOrder!!.id).isEqualTo(expected.bestSellOrder!!.id)
                assertThat(it.item.bestBidOrder).isNull()
            }
        }
    }

    @Test
    fun `on best bid collection order updated`() = runWithKafka {
        val itemId = randomEthItemId()
        val shortItem = randomShortItem(itemId)
        val ethItem = randomEthNftItemDto(itemId)
        val unionItem = EthItemConverter.convert(ethItem, itemId.blockchain)
        itemService.save(shortItem)

        val address = UnionAddress(itemId.blockchain, itemId.token.value)
        val bestBidOrder = randomEthLegacyOrderDto(
            randomEthAssetErc721(),
            randomAddress(),
            randomEthCollectionAsset(Address.apply(address.value))
        )
        val unionBestBid = ethOrderConverter.convert(bestBidOrder, itemId.blockchain)

        coEvery { testEthereumItemApi.getNftItemById(itemId.value) } returns ethItem.toMono()
        coEvery { testEthereumItemApi.getNftItemMetaById(itemId.value) } returns ethItem.meta!!.toMono()

        collectionEventService.onCollectionBestBidOrderUpdate(address, unionBestBid, true)

        val expected = EnrichedItemConverter.convert(unionItem).copy(
            bestBidOrder = unionBestBid,
            meta = enrichmentMetaService.enrichMeta(unionItem.meta!!, ShortItemId(itemId))
        )

        val saved = itemService.get(shortItem.id)!!
        assertThat(saved.bestBidOrder).isEqualTo(ShortOrderConverter.convert(unionBestBid))

        Wait.waitAssert {
            val messages = findItemUpdates(itemId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.itemId).isEqualTo(itemId)
            assertThat(messages[0].value.item.id).isEqualTo(expected.id)
            assertThat(messages[0].value.item.bestSellOrder).isNull()
            assertThat(messages[0].value.item.bestBidOrder!!.id).isEqualTo(expected.bestBidOrder!!.id)
        }
    }
}
