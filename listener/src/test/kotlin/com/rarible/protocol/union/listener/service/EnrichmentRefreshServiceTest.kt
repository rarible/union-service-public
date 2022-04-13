package com.rarible.protocol.union.listener.service

import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.enrichment.service.EnrichmentRefreshService
import com.rarible.protocol.union.integration.ethereum.data.*
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.clearMocks
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address

@IntegrationTest
class EnrichmentRefreshServiceTest : AbstractIntegrationTest(){

    @Autowired
    private lateinit var enrichmentRefreshService: EnrichmentRefreshService

    @BeforeEach
    fun beforeEach() {
        clearMocks(testEthereumOrderApi)
        clearMocks(testEthereumCollectionApi)
    }

    @Test
    fun `reconcile collection null best sell bid`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()

        coEvery {
            testEthereumOrderApi.getCurrenciesBySellOrdersOfItem(collectionId.value, "-1")
        } returns OrderCurrenciesDto(
            OrderCurrenciesDto.OrderType.SELL,
            listOf(EthAssetTypeDto())
        ).toMono()

        coEvery {
            testEthereumOrderApi.getCurrenciesByBidOrdersOfItem(collectionId.value, "-1")
        } returns OrderCurrenciesDto(
            OrderCurrenciesDto.OrderType.BID,
            listOf(EthAssetTypeDto())
        ).toMono()

        val sellOrder = randomEthV2OrderDto()
        val bidOrder = randomEthV2OrderDto()

        val ordersPaginationDtoSell = OrdersPaginationDto(listOf(sellOrder), "")
        val ordersPaginationDtoBid = OrdersPaginationDto(listOf(bidOrder), "")

        val ethCollectionDto = randomEthCollectionDto(Address.apply(collectionId.value))

        coEvery { testEthereumCollectionApi.getNftCollectionById(collectionId.value) } returns ethCollectionDto.toMono()

        coEvery {
            testEthereumOrderApi.getSellOrdersByItemAndByStatus(
                collectionId.value,
                "-1",
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns ordersPaginationDtoSell.toMono()

        coEvery {
            testEthereumOrderApi.getOrderBidsByItemAndByStatus(
                collectionId.value,
                "-1",
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns ordersPaginationDtoBid.toMono()


        val updateEvent = enrichmentRefreshService.reconcileCollection(collectionId)

        assertThat(updateEvent.collectionId).isEqualTo(collectionId)
        assertThat(updateEvent.javaClass.kotlin).isEqualTo(CollectionUpdateEventDto::class)
        val collectionUpdateEvent = updateEvent as CollectionUpdateEventDto

        assertThat(collectionUpdateEvent.collection.bestBidOrder).isNull()
        assertThat(collectionUpdateEvent.collection.bestSellOrder).isNull()
    }

    @Test
    fun `reconcile collection`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()

        coEvery {
            testEthereumOrderApi.getCurrenciesBySellOrdersOfItem(collectionId.value, "-1")
        } returns OrderCurrenciesDto(
            OrderCurrenciesDto.OrderType.SELL,
            listOf(EthAssetTypeDto())
        ).toMono()

        coEvery {
            testEthereumOrderApi.getCurrenciesByBidOrdersOfItem(collectionId.value, "-1")
        } returns OrderCurrenciesDto(
            OrderCurrenciesDto.OrderType.BID,
            listOf(EthAssetTypeDto())
        ).toMono()

        val sellOrder = randomEthV2OrderDto().copy(
            make = randomEthCollectionAsset(Address.apply(collectionId.value))
        )
        val bidOrder = randomEthV2OrderDto().copy(
            take = randomEthCollectionAsset(Address.apply(collectionId.value)),
            make = randomEthAssetErc20()
        )

        val ordersPaginationDtoSell = OrdersPaginationDto(listOf(sellOrder), "")
        val ordersPaginationDtoBid = OrdersPaginationDto(listOf(bidOrder), "")

        val ethCollectionDto = randomEthCollectionDto(Address.apply(collectionId.value))

        coEvery { testEthereumCollectionApi.getNftCollectionById(collectionId.value) } returns ethCollectionDto.toMono()

        coEvery {
            testEthereumOrderApi.getSellOrdersByItemAndByStatus(
                collectionId.value,
                "-1",
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns ordersPaginationDtoSell.toMono()

        coEvery {
            testEthereumOrderApi.getOrderBidsByItemAndByStatus(
                collectionId.value,
                "-1",
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns ordersPaginationDtoBid.toMono()


        val updateEvent = enrichmentRefreshService.reconcileCollection(collectionId)

        assertThat(updateEvent.collectionId).isEqualTo(collectionId)
        assertThat(updateEvent.javaClass.kotlin).isEqualTo(CollectionUpdateEventDto::class)
        val collectionUpdateEvent = updateEvent as CollectionUpdateEventDto

        assertThat(collectionUpdateEvent.collection.bestBidOrder!!.take.type.ext.isCollection).isTrue
        assertThat(collectionUpdateEvent.collection.bestBidOrder!!.take.type.ext.collectionId).isEqualTo(collectionId)
        assertThat(collectionUpdateEvent.collection.bestSellOrder!!.make.type.ext.isCollection).isTrue
        assertThat(collectionUpdateEvent.collection.bestSellOrder!!.make.type.ext.collectionId).isEqualTo(collectionId)
    }
}