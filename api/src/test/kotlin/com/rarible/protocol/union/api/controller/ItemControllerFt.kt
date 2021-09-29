package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.FlowNftItemsDto
import com.rarible.protocol.dto.NftItemsDto
import com.rarible.protocol.union.api.client.ItemControllerApi
import com.rarible.protocol.union.api.configuration.PageSize
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.ItemIdParser
import com.rarible.protocol.union.test.data.randomEthAddress
import com.rarible.protocol.union.test.data.randomEthItemId
import com.rarible.protocol.union.test.data.randomEthNftItemDto
import com.rarible.protocol.union.test.data.randomFlowAddress
import com.rarible.protocol.union.test.data.randomFlowItemIdFullValue
import com.rarible.protocol.union.test.data.randomFlowNftItemDto
import com.rarible.protocol.union.test.data.randomPolygonAddress
import com.rarible.protocol.union.test.data.randomPolygonItemId
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono

@FlowPreview
@IntegrationTest
@Disabled // TODO enable after enrichment implemented
class ItemControllerFt : AbstractIntegrationTest() {

    private val continuation: String? = null
    private val size = PageSize.ITEM.default


    @Autowired
    lateinit var itemControllerClient: ItemControllerApi

    @Test
    fun `get item by id - ethereum`() = runBlocking<Unit> {
        val itemIdFull = randomEthItemId().fullId()
        val itemId = ItemIdParser.parseFull(itemIdFull)
        val item = randomEthNftItemDto(itemId)

        coEvery { testEthereumItemApi.getNftItemById(itemId.value) } returns item.toMono()

        val unionItem = itemControllerClient.getItemById(itemIdFull).awaitFirst()

        assertThat(unionItem.id.value).isEqualTo(itemId.value)
        assertThat(unionItem.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
    }

    @Test
    fun `get item by id - polygon`() = runBlocking<Unit> {
        val itemIdFull = randomPolygonItemId().fullId()
        val itemId = ItemIdParser.parseFull(itemIdFull)
        val item = randomEthNftItemDto(itemId)

        coEvery { testPolygonItemApi.getNftItemById(itemId.value) } returns item.toMono()

        val unionItem = itemControllerClient.getItemById(itemIdFull).awaitFirst()

        assertThat(unionItem.id.value).isEqualTo(itemId.value)
        assertThat(unionItem.id.blockchain).isEqualTo(BlockchainDto.POLYGON)
    }

    @Test
    fun `get item by id - flow`() = runBlocking<Unit> {
        val itemIdFull = randomFlowItemIdFullValue()
        val itemId = ItemIdParser.parseFull(itemIdFull)
        val item = randomFlowNftItemDto(itemId)

        coEvery { testFlowItemApi.getNftItemById(itemId.value) } returns item.toMono()

        val unionItem = itemControllerClient.getItemById(itemIdFull).awaitFirst()

        assertThat(unionItem.id.value).isEqualTo(itemId.value)
        assertThat(unionItem.id.blockchain).isEqualTo(BlockchainDto.FLOW)
    }

    @Test
    fun `get items by collection - ethereum`() = runBlocking<Unit> {
        val ethCollectionId = randomEthAddress()
        val item = randomEthNftItemDto()

        coEvery {
            testEthereumItemApi.getNftItemsByCollection(ethCollectionId.value, continuation, size)
        } returns NftItemsDto(1, null, listOf(item)).toMono()

        val items = itemControllerClient.getItemsByCollection(
            ethCollectionId.fullId(), continuation, size
        ).awaitFirst()

        val ethItem = items.items[0]
        assertThat(ethItem.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by collection - polygon`() = runBlocking<Unit> {
        val polyCollectionId = randomPolygonAddress()
        val item = randomEthNftItemDto()

        coEvery {
            testPolygonItemApi.getNftItemsByCollection(polyCollectionId.value, continuation, size)
        } returns NftItemsDto(1, null, listOf(item)).toMono()

        val items = itemControllerClient.getItemsByCollection(
            polyCollectionId.fullId(), continuation, size
        ).awaitFirst()

        val polyItem = items.items[0]
        assertThat(polyItem.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by collection - flow`() = runBlocking<Unit> {
        val flowCollectionId = randomFlowAddress()
        val item = randomFlowNftItemDto()

        coEvery {
            testFlowItemApi.getNftItemsByCollection(flowCollectionId.value, continuation, size)
        } returns FlowNftItemsDto(1, null, listOf(item)).toMono()

        val items = itemControllerClient.getItemsByCollection(
            flowCollectionId.fullId(), continuation, size
        ).awaitFirst()

        val flowItem = items.items[0]
        assertThat(flowItem.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by owner - ethereum`() = runBlocking<Unit> {
        val ethOwnerId = randomEthAddress()
        val item = randomEthNftItemDto()

        coEvery {
            testEthereumItemApi.getNftItemsByOwner(ethOwnerId.value, continuation, size)
        } returns NftItemsDto(1, null, listOf(item)).toMono()

        val items = itemControllerClient.getItemsByOwner(
            ethOwnerId.fullId(), continuation, size
        ).awaitFirst()

        val ethItem = items.items[0]
        assertThat(ethItem.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by owner - polygon`() = runBlocking<Unit> {
        val polyOwnerId = randomPolygonAddress()
        val item = randomEthNftItemDto()

        coEvery {
            testPolygonItemApi.getNftItemsByOwner(polyOwnerId.value, continuation, size)
        } returns NftItemsDto(1, null, listOf(item)).toMono()

        val items = itemControllerClient.getItemsByOwner(
            polyOwnerId.fullId(), continuation, size
        ).awaitFirst()

        val polyItem = items.items[0]
        assertThat(polyItem.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by owner - flow`() = runBlocking<Unit> {
        val flowOwnerId = randomFlowAddress()
        val item = randomFlowNftItemDto()

        coEvery {
            testFlowItemApi.getNftItemsByOwner(flowOwnerId.value, continuation, size)
        } returns FlowNftItemsDto(1, null, listOf(item)).toMono()

        val items = itemControllerClient.getItemsByOwner(
            flowOwnerId.fullId(), continuation, size
        ).awaitFirst()

        val flowItem = items.items[0]
        assertThat(flowItem.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by creator - ethereum`() = runBlocking<Unit> {
        val ethCreatorId = randomEthAddress()
        val item = randomEthNftItemDto()

        coEvery {
            testEthereumItemApi.getNftItemsByCreator(ethCreatorId.value, continuation, size)
        } returns NftItemsDto(1, null, listOf(item)).toMono()

        val items = itemControllerClient.getItemsByCreator(
            ethCreatorId.fullId(), continuation, size
        ).awaitFirst()

        val ethItem = items.items[0]
        assertThat(ethItem.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by creator - polygon`() = runBlocking<Unit> {
        val polyCreatorId = randomPolygonAddress()
        val item = randomEthNftItemDto()

        coEvery {
            testPolygonItemApi.getNftItemsByCreator(polyCreatorId.value, continuation, size)
        } returns NftItemsDto(1, null, listOf(item)).toMono()

        val items = itemControllerClient.getItemsByCreator(
            polyCreatorId.fullId(), continuation, size
        ).awaitFirst()

        val polyItem = items.items[0]
        assertThat(polyItem.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by creator - flow`() = runBlocking<Unit> {
        val flowCreatorId = randomFlowAddress()
        val item = randomFlowNftItemDto()

        coEvery {
            testFlowItemApi.getNftItemsByCreator(flowCreatorId.value, continuation, size)
        } returns FlowNftItemsDto(1, null, listOf(item)).toMono()

        val items = itemControllerClient.getItemsByCreator(
            flowCreatorId.fullId(), continuation, size
        ).awaitFirst()

        val flowItem = items.items[0]
        assertThat(flowItem.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get all items`() = runBlocking<Unit> {

        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW)
        val continuation = "${nowMillis()}_${randomString()}"
        val showDeleted = true
        val size = 3
        val lastUpdatedFrom = nowMillis().minusSeconds(120).toEpochMilli()
        val lastUpdatedTo = nowMillis().plusSeconds(120).toEpochMilli()

        val flowItems = listOf(randomFlowNftItemDto(), randomFlowNftItemDto())
        val ethItems = listOf(randomEthNftItemDto(), randomEthNftItemDto())

        coEvery {
            testFlowItemApi.getNftAllItems(continuation, size, showDeleted)
        } returns FlowNftItemsDto(2, null, flowItems).toMono()

        coEvery {
            testEthereumItemApi.getNftAllItems(
                continuation,
                size,
                showDeleted,
                lastUpdatedFrom,
                lastUpdatedTo
            )
        } returns NftItemsDto(2, null, ethItems).toMono()

        val items = itemControllerClient.getAllItems(
            blockchains, continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo
        ).awaitFirst()

        assertThat(items.items).hasSize(3)
        assertThat(items.total).isEqualTo(4)
        assertThat(items.continuation).isNotNull()
    }
}
