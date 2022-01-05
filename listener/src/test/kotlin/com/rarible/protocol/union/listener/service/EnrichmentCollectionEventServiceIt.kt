package com.rarible.protocol.union.listener.service

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.NftItemsDto
import com.rarible.protocol.union.enrichment.converter.EnrichedItemConverter
import com.rarible.protocol.union.enrichment.converter.EnrichedMetaConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.service.EnrichmentMetaService
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc20
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc721
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionAsset
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Mono
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
    private lateinit var collectionEventService: EnrichmentCollectionEventService

    @Test
    @Disabled // TODO UNION enable when we can use this method without field 'owners'
    fun `on best sell collection order updated`() = runWithKafka {
        val itemId = randomEthItemId()
        val shortItem = randomShortItem(itemId)
        val ethItem = randomEthNftItemDto(itemId)
        val unionItem = EthItemConverter.convert(ethItem, itemId.blockchain)

        val ownershipId = randomEthOwnershipId(itemId)
        val ethOwnership = randomEthOwnershipDto(ownershipId)

        val collectionId = randomEthCollectionId()
        val bestSellOrder = randomEthLegacyOrderDto(
            randomEthCollectionAsset(Address.apply(collectionId.value)),
            ethOwnership.owner,
            randomEthAssetErc721()
        )
        val unionBestSell = ethOrderConverter.convert(bestSellOrder, itemId.blockchain)

        coEvery { testEthereumItemApi.getNftItemById(itemId.value) } returns ethItem.toMono()
        coEvery { testEthereumItemApi.getNftItemMetaById(itemId.value) } returns ethItem.meta!!.toMono()
        coEvery { testEthereumOwnershipApi.getNftOwnershipById(ownershipId.value, false) } returns ethOwnership.toMono()

        val nft = randomEthNftItemDto(itemId)
        coEvery {
            testEthereumItemApi.getNftItemsByCollection(eq(collectionId.value), any(), any(), any())
        } returns Mono.just(NftItemsDto(1, null, listOf(nft)))

        collectionEventService.onCollectionBestSellOrderUpdate(collectionId, unionBestSell, true)

        val expected = EnrichedItemConverter.convert(unionItem).copy(
            bestSellOrder = unionBestSell,
            meta = EnrichedMetaConverter.convert(unionItem.meta!!)
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

        val collectionId = randomEthCollectionId()
        val bestBidOrder = randomEthLegacyOrderDto(
            randomEthAssetErc20(),
            randomAddress(),
            randomEthCollectionAsset(Address.apply(collectionId.value))
        )
        val unionBestBid = ethOrderConverter.convert(bestBidOrder, itemId.blockchain)

        coEvery { testEthereumItemApi.getNftItemById(itemId.value) } returns ethItem.toMono()
        coEvery { testEthereumItemApi.getNftItemMetaById(itemId.value) } returns ethItem.meta!!.toMono()

        val nft = randomEthNftItemDto(itemId)
        coEvery {
            testEthereumItemApi.getNftItemsByCollection(eq(collectionId.value), any(), any(), any())
        } returns Mono.just(NftItemsDto(1, null, listOf(nft)))

        collectionEventService.onCollectionBestBidOrderUpdate(collectionId, unionBestBid, true)

        val expected = EnrichedItemConverter.convert(unionItem).copy(
            bestBidOrder = unionBestBid,
            meta = EnrichedMetaConverter.convert(unionItem.meta!!)
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
