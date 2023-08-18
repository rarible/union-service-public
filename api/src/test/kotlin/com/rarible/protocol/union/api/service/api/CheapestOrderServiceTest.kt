package com.rarible.protocol.union.api.service.api

import com.rarible.protocol.union.core.model.elastic.EsItem
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOrder
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.service.BestOrderService
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService
import com.rarible.protocol.union.enrichment.test.data.randomEsItem
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomShortOrder
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrder
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import randomOrderDto

@ExtendWith(MockKExtension::class)
internal class CheapestOrderServiceTest {
    @InjectMockKs
    private lateinit var cheapestOrderService: CheapestOrderService

    @MockK
    private lateinit var esItemRepository: EsItemRepository

    @MockK
    private lateinit var orderRouter: BlockchainRouter<OrderService>

    @MockK
    private lateinit var bestOrderService: BestOrderService

    @MockK
    private lateinit var itemRepository: ItemRepository

    @MockK
    private lateinit var enrichmentOrderService: EnrichmentOrderService

    @MockK
    private lateinit var orderService: OrderService

    private lateinit var collectionId: CollectionIdDto
    private lateinit var esItem1: EsItem
    private lateinit var esItem2: EsItem
    private lateinit var shortOrder1: ShortOrder
    private lateinit var shortOrder2: ShortOrder
    private lateinit var shortItem1: ShortItem
    private lateinit var shortItem2: ShortItem

    @BeforeEach
    fun before() {
        collectionId = randomEthCollectionId()
        esItem1 = randomEsItem()
        esItem2 = randomEsItem()
        shortOrder1 = randomShortOrder()
        shortOrder2 = randomShortOrder()
        shortItem1 = randomShortItem(id = IdParser.parseItemId(esItem1.itemId)).copy(
            bestSellOrder = shortOrder1,
        )
        shortItem2 = randomShortItem(id = IdParser.parseItemId(esItem2.itemId)).copy(
            bestSellOrder = shortOrder2,
        )
    }

    @Test
    fun `no items found in es`() = runBlocking<Unit> {
        coEvery { esItemRepository.getCheapestItems(collectionId.fullId()) } returns emptyList()

        assertThat(cheapestOrderService.getCheapestOrder(collectionId = collectionId)).isNull()
    }

    @Test
    fun `no items found in mongo`() = runBlocking<Unit> {
        coEvery { esItemRepository.getCheapestItems(collectionId.fullId()) } returns listOf(esItem1, esItem2)
        coEvery {
            itemRepository.getAll(
                listOf(
                    ShortItemId.of(esItem1.itemId),
                    ShortItemId.of(esItem2.itemId)
                )
            )
        } returns emptyList()

        assertThat(cheapestOrderService.getCheapestOrder(collectionId = collectionId)).isNull()
    }

    @Test
    fun `no best sell order found`() = runBlocking<Unit> {
        coEvery { esItemRepository.getCheapestItems(collectionId.fullId()) } returns listOf(esItem1, esItem2)
        coEvery {
            itemRepository.getAll(
                listOf(
                    ShortItemId.of(esItem1.itemId),
                    ShortItemId.of(esItem2.itemId)
                )
            )
        } returns listOf(shortItem1, shortItem2)

        coEvery {
            bestOrderService.getBestBidOrderInUsd(
                mapOf(
                    esItem1.bestSellCurrency!! to shortOrder1,
                    esItem2.bestSellCurrency!! to shortOrder2,
                )
            )
        } returns null

        assertThat(cheapestOrderService.getCheapestOrder(collectionId = collectionId)).isNull()
    }

    @Test
    fun `best order found`() = runBlocking<Unit> {
        val unionOrder = randomUnionSellOrder()
        val order = randomOrderDto()
        coEvery { esItemRepository.getCheapestItems(collectionId.fullId()) } returns listOf(esItem1, esItem2)
        coEvery {
            itemRepository.getAll(
                listOf(
                    ShortItemId.of(esItem1.itemId),
                    ShortItemId.of(esItem2.itemId)
                )
            )
        } returns listOf(shortItem1, shortItem2)

        coEvery {
            bestOrderService.getBestBidOrderInUsd(
                mapOf(
                    esItem1.bestSellCurrency!! to shortOrder1,
                    esItem2.bestSellCurrency!! to shortOrder2,
                )
            )
        } returns shortOrder1
        coEvery { orderRouter.getService(shortOrder1.blockchain) } returns orderService
        coEvery { orderService.getOrderById(shortOrder1.id) } returns unionOrder
        coEvery { enrichmentOrderService.enrich(unionOrder) } returns order

        assertThat(cheapestOrderService.getCheapestOrder(collectionId = collectionId)).isEqualTo(order)
    }
}
