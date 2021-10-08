package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.api.client.ItemControllerApi
import com.rarible.protocol.union.api.configuration.PageSize
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.core.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.ItemIdParser
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.test.data.randomEthAddress
import com.rarible.protocol.union.test.data.randomEthItemId
import com.rarible.protocol.union.test.data.randomEthNftItemDto
import com.rarible.protocol.union.test.data.randomEthV2OrderDto
import com.rarible.protocol.union.test.data.randomFlowAddress
import com.rarible.protocol.union.test.data.randomFlowItemIdFullValue
import com.rarible.protocol.union.test.data.randomFlowNftItemDto
import io.mockk.coEvery
import io.mockk.verify
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Mono
import java.math.BigInteger

@FlowPreview
@IntegrationTest
class ItemControllerFt : AbstractIntegrationTest() {

    private val continuation: String? = null
    private val size = PageSize.ITEM.default

    @Autowired
    lateinit var itemControllerClient: ItemControllerApi

    @Autowired
    lateinit var enrichmentItemService: EnrichmentItemService

    @Autowired
    lateinit var ethOrderConverter: EthOrderConverter

    @Test
    fun `get item by id - ethereum, enriched`() = runBlocking<Unit> {
        // Enriched item
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId)
        val ethUnionItem = EthItemConverter.convert(ethItem, ethItemId.blockchain)
        val ethOrder = randomEthV2OrderDto(ethItemId)
        val ethUnionOrder = ethOrderConverter.convert(ethOrder, ethItemId.blockchain)
        val ethShortItem = ShortItemConverter.convert(ethUnionItem)
            .copy(bestSellOrder = ShortOrderConverter.convert(ethUnionOrder))
        enrichmentItemService.save(ethShortItem)

        ethereumOrderControllerApiMock.mockGetById(ethOrder)
        ethereumItemControllerApiMock.mockGetNftItemById(ethItemId, ethItem)

        val result = itemControllerClient.getItemById(ethItemId.fullId()).awaitFirst()

        assertThat(result.id).isEqualTo(ethItemId)
        assertThat(result.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
        assertThat(result.bestSellOrder).isEqualTo(ethUnionOrder)
    }

    @Test
    fun `get item by id - flow, not enriched`() = runBlocking<Unit> {
        val itemIdFull = randomFlowItemIdFullValue()
        val itemId = ItemIdParser.parseFull(itemIdFull)
        val item = randomFlowNftItemDto(itemId)

        flowItemControllerApiMock.mockGetNftItemById(itemId, item)

        val result = itemControllerClient.getItemById(itemIdFull).awaitFirst()

        assertThat(result.id.value).isEqualTo(itemId.value)
        assertThat(result.id.blockchain).isEqualTo(BlockchainDto.FLOW)
    }

    @Test
    fun `reset item meta by id - ethereum`() = runBlocking<Unit> {
        val itemIdFull = randomEthItemId().fullId()
        val itemId = ItemIdParser.parseFull(itemIdFull)

        coEvery { testEthereumItemApi.resetNftItemMetaById(itemId.value) } returns Mono.first()

        itemControllerClient.resetItemMeta(itemIdFull).awaitFirstOrNull()

        verify(exactly = 1) { testEthereumItemApi.resetNftItemMetaById(itemId.value) }
    }

    /*
    @Test
    // TODO uncomment when Flow implement it
    fun `reset item meta by id - flow`() = runBlocking<Unit> {
        val itemIdFull = randomFlowItemIdFullValue()
        val itemId = ItemIdParser.parseFull(itemIdFull)

        coEvery { testFlowItemApi.resetNftItemMetaById(itemId.value) } returns item.toMono()

        val unionItem = itemControllerClient.getItemById(itemIdFull).awaitFirst()

        verify(exactly = 1) { testFlowItemApi.resetNftItemMetaById(itemId.value) }
    }
    */

    @Test
    fun `get items by collection - ethereum, all enriched`() = runBlocking<Unit> {
        // Enriched item
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId)
        val ethUnionItem = EthItemConverter.convert(ethItem, ethItemId.blockchain)
        val ethOrder = randomEthV2OrderDto(ethItemId)
        val ethUnionOrder = ethOrderConverter.convert(ethOrder, ethItemId.blockchain)
        val ethShortItem = ShortItemConverter.convert(ethUnionItem)
            .copy(bestBidOrder = ShortOrderConverter.convert(ethUnionOrder))
        enrichmentItemService.save(ethShortItem)

        val ethCollectionId = randomEthAddress()

        ethereumOrderControllerApiMock.mockGetByIds(ethOrder)
        ethereumItemControllerApiMock.mockGetNftOrderItemsByCollection(
            ethCollectionId.value, continuation, size, ethItem
        )

        val items = itemControllerClient.getItemsByCollection(
            ethCollectionId.fullId(), continuation, size
        ).awaitFirst()

        assertThat(items.items).hasSize(1)
        val result = items.items[0]

        assertThat(result.id).isEqualTo(ethItemId)
        assertThat(result.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
        assertThat(result.bestBidOrder).isEqualTo(ethUnionOrder)
    }

    @Test
    fun `get items by collection - flow, nothing enriched`() = runBlocking<Unit> {
        val flowCollectionId = randomFlowAddress()
        val item = randomFlowNftItemDto()

        flowItemControllerApiMock.mockGetNftOrderItemsByCollection(flowCollectionId.value, continuation, size, item)

        val items = itemControllerClient.getItemsByCollection(
            flowCollectionId.fullId(), continuation, size
        ).awaitFirst()

        val result = items.items[0]
        assertThat(result.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by owner - ethereum, enriched partially`() = runBlocking<Unit> {
        // Enriched item
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId).copy(date = nowMillis())
        val ethUnionItem = EthItemConverter.convert(ethItem, ethItemId.blockchain)
        val ethShortItem = ShortItemConverter.convert(ethUnionItem)
            .copy(totalStock = 10.toBigInteger(), sellers = 2)
        enrichmentItemService.save(ethShortItem)

        val ethOwnerId = randomEthAddress()
        val emptyEthItem = randomEthNftItemDto().copy(date = ethItem.date!!.minusSeconds(1))

        ethereumItemControllerApiMock.mockGetNftOrderItemsByOwner(
            ethOwnerId.value, continuation, size, ethItem, emptyEthItem
        )

        val items = itemControllerClient.getItemsByOwner(
            ethOwnerId.fullId(), continuation, size
        ).awaitFirst()

        assertThat(items.items).hasSize(2)
        val enrichedResult = items.items[0]
        val emptyResult = items.items[1]

        assertThat(enrichedResult.id).isEqualTo(ethItemId)
        assertThat(enrichedResult.sellers).isEqualTo(ethShortItem.sellers)
        assertThat(enrichedResult.totalStock).isEqualTo(ethShortItem.totalStock)

        assertThat(emptyResult.id.value).isEqualTo(emptyEthItem.id)
        assertThat(emptyResult.sellers).isEqualTo(0)
        assertThat(emptyResult.totalStock).isEqualTo(BigInteger.ZERO)
    }

    @Test
    fun `get items by owner - flow, nothing enriched`() = runBlocking<Unit> {
        val flowOwnerId = randomFlowAddress()
        val flowItem = randomFlowNftItemDto()

        flowItemControllerApiMock.mockGetNftOrderItemsByOwner(
            flowOwnerId.value, continuation, size, flowItem
        )

        val items = itemControllerClient.getItemsByOwner(
            flowOwnerId.fullId(), continuation, size
        ).awaitFirst()

        val result = items.items[0]
        assertThat(result.id.value).isEqualTo(flowItem.id)
    }

    @Test
    fun `get items by creator - ethereum, nothing found`() = runBlocking<Unit> {
        val ethCreatorId = randomEthAddress()

        ethereumItemControllerApiMock.mockGetNftOrderItemsByCreator(
            ethCreatorId.value, continuation, size
        )

        val items = itemControllerClient.getItemsByCreator(
            ethCreatorId.fullId(), continuation, size
        ).awaitFirst()

        assertThat(items.items).hasSize(0)
    }

    @Test
    fun `get items by creator - flow, nothing enriched`() = runBlocking<Unit> {
        val flowCreatorId = randomFlowAddress()
        val flowItem = randomFlowNftItemDto()

        flowItemControllerApiMock.mockGetNftOrderItemsByCreator(
            flowCreatorId.value, continuation, size, flowItem
        )

        val items = itemControllerClient.getItemsByCreator(
            flowCreatorId.fullId(), continuation, size
        ).awaitFirst()

        val result = items.items[0]
        assertThat(result.id.value).isEqualTo(flowItem.id)
    }

    @Test
    fun `get all items - trimmed to size`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW)
        val continuation = "${nowMillis()}_${randomString()}"
        val showDeleted = true
        val size = 3
        val lastUpdatedFrom = nowMillis().minusSeconds(120).toEpochMilli()
        val lastUpdatedTo = nowMillis().plusSeconds(120).toEpochMilli()

        flowItemControllerApiMock.mockGetNftAllItems(
            continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo,
            randomFlowNftItemDto(), randomFlowNftItemDto()
        )

        ethereumItemControllerApiMock.mockGetNftAllItems(
            continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo,
            randomEthNftItemDto(), randomEthNftItemDto()
        )

        val items = itemControllerClient.getAllItems(
            blockchains, continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo
        ).awaitFirst()

        assertThat(items.items).hasSize(3)
        assertThat(items.total).isEqualTo(4)
        assertThat(items.continuation).isNotNull()
    }
}
