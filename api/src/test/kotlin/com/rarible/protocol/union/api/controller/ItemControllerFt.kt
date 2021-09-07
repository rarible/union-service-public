package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.FlowNftItemsDto
import com.rarible.protocol.dto.NftItemsDto
import com.rarible.protocol.union.api.client.ItemControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.dto.ethereum.EthItemIdProvider
import com.rarible.protocol.union.dto.flow.FlowItemIdProvider
import com.rarible.protocol.union.test.data.*
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono

@FlowPreview
@IntegrationTest
class ItemControllerFt : AbstractIntegrationTest() {

    private val DEF_CONTINUATION = null
    private val DEF_SIZE = 5
    private val DEF_META = null


    @Autowired
    lateinit var itemControllerClient: ItemControllerApi

    @Test
    fun `get item by id - ethereum`() = runBlocking<Unit> {
        val itemIdFull = randomEthItemIdFullValue()
        val itemId = EthItemIdProvider.parseFull(itemIdFull)
        val item = randomEthNftItemDto(itemId)

        coEvery { testEthereumItemApi.getNftItemById(itemId.value, true) } returns item.toMono()

        val unionItem = itemControllerClient.getItemById(itemIdFull, true).awaitFirst()
        val ethItem = unionItem as EthItemDto

        assertThat(ethItem.id.value).isEqualTo(itemId.value)
        assertThat(ethItem.id.blockchain).isEqualTo(EthBlockchainDto.ETHEREUM)
    }

    @Test
    fun `get item by id - polygon`() = runBlocking<Unit> {
        val itemIdFull = randomPolygonItemIdFullValue()
        val itemId = EthItemIdProvider.parseFull(itemIdFull)
        val item = randomEthNftItemDto(itemId)

        coEvery { testPolygonItemApi.getNftItemById(itemId.value, true) } returns item.toMono()

        val unionItem = itemControllerClient.getItemById(itemIdFull, true).awaitFirst()
        val polyItem = unionItem as EthItemDto

        assertThat(polyItem.id.value).isEqualTo(itemId.value)
        assertThat(polyItem.id.blockchain).isEqualTo(EthBlockchainDto.POLYGON)
    }

    @Test
    fun `get item by id - flow`() = runBlocking<Unit> {
        val itemIdFull = randomFlowItemIdFullValue()
        val itemId = FlowItemIdProvider.parseFull(itemIdFull)
        val item = randomFlowNftItemDto(itemId)

        coEvery { testFlowItemApi.getNftItemById(itemId.value) } returns item.toMono()

        val unionItem = itemControllerClient.getItemById(itemIdFull, false).awaitFirst()
        val flowItem = unionItem as FlowItemDto

        assertThat(flowItem.id.value).isEqualTo(itemId.value)
        assertThat(flowItem.id.blockchain).isEqualTo(FlowBlockchainDto.FLOW)
    }

    @Test
    fun `get items by collection - ethereum`() = runBlocking<Unit> {
        val ethCollectionId = randomEthAddress()
        val item = randomEthNftItemDto()

        coEvery {
            testEthereumItemApi.getNftItemsByCollection(ethCollectionId.value, DEF_CONTINUATION, DEF_SIZE, DEF_META)
        } returns NftItemsDto(1, null, listOf(item)).toMono()

        val unionItems = itemControllerClient.getItemsByCollection(
            ethCollectionId.toString(), DEF_CONTINUATION, DEF_SIZE, DEF_META
        ).awaitFirst()

        val ethItem = unionItems.items[0] as EthItemDto
        assertThat(ethItem.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by collection - polygon`() = runBlocking<Unit> {
        val polyCollectionId = randomPolygonAddress()
        val item = randomEthNftItemDto()

        coEvery {
            testPolygonItemApi.getNftItemsByCollection(polyCollectionId.value, DEF_CONTINUATION, DEF_SIZE, DEF_META)
        } returns NftItemsDto(1, null, listOf(item)).toMono()

        val unionItems = itemControllerClient.getItemsByCollection(
            polyCollectionId.toString(), DEF_CONTINUATION, DEF_SIZE, DEF_META
        ).awaitFirst()

        val polyItem = unionItems.items[0] as EthItemDto
        assertThat(polyItem.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by collection - flow`() = runBlocking<Unit> {
        val flowCollectionId = randomFlowAddress()
        val item = randomFlowNftItemDto()

        coEvery {
            testFlowItemApi.getNftItemsByCollection(flowCollectionId.value, DEF_CONTINUATION, DEF_SIZE)
        } returns FlowNftItemsDto(1, null, listOf(item)).toMono()

        val unionItems = itemControllerClient.getItemsByCollection(
            flowCollectionId.toString(), DEF_CONTINUATION, DEF_SIZE, DEF_META
        ).awaitFirst()

        val flowItem = unionItems.items[0] as FlowItemDto
        assertThat(flowItem.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by owner - ethereum`() = runBlocking<Unit> {
        val ethOwnerId = randomEthAddress()
        val item = randomEthNftItemDto()

        coEvery {
            testEthereumItemApi.getNftItemsByOwner(ethOwnerId.value, DEF_CONTINUATION, DEF_SIZE, DEF_META)
        } returns NftItemsDto(1, null, listOf(item)).toMono()

        val unionItems = itemControllerClient.getItemsByOwner(
            ethOwnerId.toString(), DEF_CONTINUATION, DEF_SIZE, DEF_META
        ).awaitFirst()

        val ethItem = unionItems.items[0] as EthItemDto
        assertThat(ethItem.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by owner - polygon`() = runBlocking<Unit> {
        val polyOwnerId = randomPolygonAddress()
        val item = randomEthNftItemDto()

        coEvery {
            testPolygonItemApi.getNftItemsByOwner(polyOwnerId.value, DEF_CONTINUATION, DEF_SIZE, DEF_META)
        } returns NftItemsDto(1, null, listOf(item)).toMono()

        val unionItems = itemControllerClient.getItemsByOwner(
            polyOwnerId.toString(), DEF_CONTINUATION, DEF_SIZE, DEF_META
        ).awaitFirst()

        val polyItem = unionItems.items[0] as EthItemDto
        assertThat(polyItem.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by owner - flow`() = runBlocking<Unit> {
        val flowOwnerId = randomFlowAddress()
        val item = randomFlowNftItemDto()

        coEvery {
            testFlowItemApi.getNftItemsByOwner(flowOwnerId.value, DEF_CONTINUATION, DEF_SIZE)
        } returns FlowNftItemsDto(1, null, listOf(item)).toMono()

        val unionItems = itemControllerClient.getItemsByOwner(
            flowOwnerId.toString(), DEF_CONTINUATION, DEF_SIZE, DEF_META
        ).awaitFirst()

        val flowItem = unionItems.items[0] as FlowItemDto
        assertThat(flowItem.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by creator - ethereum`() = runBlocking<Unit> {
        val ethCreatorId = randomEthAddress()
        val item = randomEthNftItemDto()

        coEvery {
            testEthereumItemApi.getNftItemsByCreator(ethCreatorId.value, DEF_CONTINUATION, DEF_SIZE, DEF_META)
        } returns NftItemsDto(1, null, listOf(item)).toMono()

        val unionItems = itemControllerClient.getItemsByCreator(
            ethCreatorId.toString(), DEF_CONTINUATION, DEF_SIZE, DEF_META
        ).awaitFirst()

        val ethItem = unionItems.items[0] as EthItemDto
        assertThat(ethItem.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by creator - polygon`() = runBlocking<Unit> {
        val polyCreatorId = randomPolygonAddress()
        val item = randomEthNftItemDto()

        coEvery {
            testPolygonItemApi.getNftItemsByCreator(polyCreatorId.value, DEF_CONTINUATION, DEF_SIZE, DEF_META)
        } returns NftItemsDto(1, null, listOf(item)).toMono()

        val unionItems = itemControllerClient.getItemsByCreator(
            polyCreatorId.toString(), DEF_CONTINUATION, DEF_SIZE, DEF_META
        ).awaitFirst()

        val polyItem = unionItems.items[0] as EthItemDto
        assertThat(polyItem.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by creator - flow`() = runBlocking<Unit> {
        val flowCreatorId = randomFlowAddress()
        val item = randomFlowNftItemDto()

        coEvery {
            testFlowItemApi.getNftItemsByCreator(flowCreatorId.value, DEF_CONTINUATION, DEF_SIZE)
        } returns FlowNftItemsDto(1, null, listOf(item)).toMono()

        val unionItems = itemControllerClient.getItemsByCreator(
            flowCreatorId.toString(), DEF_CONTINUATION, DEF_SIZE, DEF_META
        ).awaitFirst()

        val flowItem = unionItems.items[0] as FlowItemDto
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
        val includeMeta = true

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
                lastUpdatedTo,
                includeMeta
            )
        } returns NftItemsDto(2, null, ethItems).toMono()

        val unionItems = itemControllerClient.getAllItems(
            blockchains, continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo, includeMeta
        ).awaitFirst()

        assertThat(unionItems.items).hasSize(3)
        assertThat(unionItems.total).isEqualTo(4)
        assertThat(unionItems.continuation).isNotNull()
    }
}
