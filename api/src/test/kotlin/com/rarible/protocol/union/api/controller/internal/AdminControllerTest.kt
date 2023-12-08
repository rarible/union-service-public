package com.rarible.protocol.union.api.controller.internal

import com.rarible.protocol.union.api.service.api.CheapestOrderService
import com.rarible.protocol.union.api.service.api.CollectionApiService
import com.rarible.protocol.union.api.service.task.TaskSchedulingService
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.task.Tasks
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService
import com.rarible.protocol.union.enrichment.service.TraitService
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import randomOrderDto
import randomOrderId
import randomUnionOrder

@ExtendWith(MockKExtension::class)
class AdminControllerTest {
    @InjectMockKs
    private lateinit var controller: AdminController

    @MockK
    private lateinit var service: OrderService

    @MockK
    private lateinit var router: BlockchainRouter<OrderService>

    @MockK
    private lateinit var enrichmentOrderService: EnrichmentOrderService

    @MockK
    private lateinit var cheapestOrderService: CheapestOrderService

    @MockK
    private lateinit var collectionApiService: CollectionApiService

    @MockK
    private lateinit var traitService: TraitService

    @MockK
    private lateinit var taskSchedulingService: TaskSchedulingService

    @Test
    fun `cancel order - ok`() = runBlocking<Unit> {
        val orderId = randomOrderId(blockchain = BlockchainDto.TEZOS)
        val canceledUnionOrder = randomUnionOrder()
        val canceledDtoOrder = randomOrderDto()

        every { router.getService(orderId.blockchain) } returns service
        coEvery { service.cancelOrder(orderId.value) } returns canceledUnionOrder
        coEvery { enrichmentOrderService.enrich(canceledUnionOrder) } returns canceledDtoOrder

        val result = controller.cancelOrder(orderId.fullId())
        assertThat(result.body).isEqualTo(canceledDtoOrder)
    }

    @Test
    fun `update hasTraits no changes`() = runBlocking<Unit> {
        val collection = randomEnrichmentCollection()
        coEvery { collectionApiService.updateHasTraits(id = collection.id, hasTraits = true) } returns false

        val result = controller.updateHasTraits(collectionId = collection.id.toString(), hasTraits = true).statusCode

        assertThat(result).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `update hasTraits to true`() = runBlocking<Unit> {
        val collection = randomEnrichmentCollection()
        coEvery { collectionApiService.updateHasTraits(id = collection.id, hasTraits = true) } returns true
        coEvery {
            taskSchedulingService.schedule(
                type = Tasks.REFRESH_TRAITS_TASK,
                param = collection.id.toString()
            )
        } returns Unit

        val result = controller.updateHasTraits(collectionId = collection.id.toString(), hasTraits = true).statusCode

        assertThat(result).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `update hasTraits to false`() = runBlocking<Unit> {
        val collection = randomEnrichmentCollection()
        coEvery { collectionApiService.updateHasTraits(id = collection.id, hasTraits = false) } returns true
        coEvery { traitService.deleteAll(collection.id) } returns Unit

        val result = controller.updateHasTraits(collectionId = collection.id.toString(), hasTraits = false).statusCode

        assertThat(result).isEqualTo(HttpStatus.OK)
    }
}
