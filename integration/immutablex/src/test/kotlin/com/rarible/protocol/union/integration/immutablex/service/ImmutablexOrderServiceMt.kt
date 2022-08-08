package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.test.ManualTest
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.integration.ImmutablexManualTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address

@ManualTest
class ImmutablexOrderServiceMt : ImmutablexManualTest() {

    private val service = ImmutablexOrderService(orderClient)

    @Test
    fun getById() = runBlocking<Unit> {
        val orderId = "143955"
        val order = service.getOrderById(orderId)

        println(order)
    }

    @Test
    fun getByIds() = runBlocking<Unit> {
        val orderIds = listOf(
            "143878", // exists
            "287364789" // doesn't exists
        )

        val result = service.getOrdersByIds(orderIds)

        println(result)
        assertThat(result).hasSize(1)
        assertThat(result[0].id.value).isEqualTo(orderIds[0])
    }

    @Test
    fun getAll() = runBlocking<Unit> {
        val page1 = service.getOrdersAll(null, 2, OrderSortDto.LAST_UPDATE_ASC, null)

        println(page1)
        assertThat(page1.entities).hasSize(2)

        val page2 = service.getOrdersAll(page1.continuation, 2, OrderSortDto.LAST_UPDATE_ASC, null)

        println(page2)
        assertThat(page2.entities).hasSize(2)

        val bigPage = service.getOrdersAll(null, 4, OrderSortDto.LAST_UPDATE_ASC, null)

        println(bigPage)
        assertThat(bigPage.entities).hasSize(4)
        assertThat(bigPage.entities).isEqualTo(page1.entities + page2.entities)
    }

    @Test
    fun getSellOrders() = runBlocking<Unit> {
        val page1 = service.getSellOrders(null, null, null, 2)

        println(page1)
        assertThat(page1.entities).hasSize(2)

        val page2 = service.getSellOrders(null, null, page1.continuation, 2)

        println(page2)
        assertThat(page2.entities).hasSize(2)

        val bigPage = service.getSellOrders(null, null, null, 4)

        println(bigPage)
        assertThat(bigPage.entities).hasSize(4)
        assertThat(bigPage.entities).isEqualTo(page1.entities + page2.entities)
    }

    @Test
    fun getSellOrdersByCollection() = runBlocking<Unit> {
        val collection = "0xc6185055ea9891d5d9020c927ff65229baebdef2"
        val result = service.getSellOrdersByCollection(
            null,
            collection,
            null,
            null,
            100
        )

        result.entities.forEach {
            val itemId = it.make.type.ext.itemId!!.value
            assertThat(itemId).startsWith(collection)
        }
    }

    @Test
    fun getSellOrderByMaker() = runBlocking<Unit> {
        val user1 = "0xa8f8bf253e5755542e86753806f2318d5a05a926"
        val user2 = "0x2d20f0637dccb04823784f5e834ed3a9991349a4"
        val users = listOf(user1, user2)
        val resultAll = service.getSellOrdersByMaker(
            null,
            users,
            null,
            listOf(OrderStatusDto.FILLED, OrderStatusDto.CANCELLED),
            null,
            100
        )

        assertThat(resultAll.entities).hasSize(68)

        val resultFilledForUser1 = service.getSellOrdersByMaker(
            null,
            listOf(user1),
            null,
            listOf(OrderStatusDto.FILLED),
            null,
            100
        )
        assertThat(resultFilledForUser1.entities).hasSize(5)
    }

    @Test
    fun getSellByItem() = runBlocking<Unit> {
        val itemId = "0x6b47952e5efe41d99ae7e75dc5a1e3cf0cd0fb6d:2983"
        val result = service.getSellOrdersByItem(
            null,
            itemId,
            null,
            null,
            null,
            Address.ZERO().prefixed(),
            null,
            10
        ).entities

        println(result)
        assertThat(result).hasSize(3)
    }

    // TODO doesn't work (bug at IMX API)
    //@Test
    fun `getSellByItem - with continuation`() = runBlocking<Unit> {
        val itemId = "0xe8d5c55ee54eaa8e073c8bb19fc552e9fe9fd0ff:937"
        val page1 = service.getSellOrdersByItem(
            null,
            itemId,
            null,
            null,
            null,
            Address.ZERO().prefixed(),
            null,
            2
        )

        println(page1)
        assertThat(page1.entities).hasSize(2)

        val page2 = service.getSellOrdersByItem(
            null,
            itemId,
            null,
            null,
            null,
            Address.ZERO().prefixed(),
            page1.continuation,
            2
        )

        println(page2)
        assertThat(page2.entities).hasSize(2)

        val bigPage = service.getSellOrdersByItem(
            null,
            itemId,
            null,
            null,
            null,
            Address.ZERO().prefixed(),
            null,
            4
        ).entities


        println(bigPage)
        assertThat(bigPage).hasSize(4)
        assertThat(bigPage).isEqualTo(page1.entities + page2.entities)
    }

    @Test
    fun getOrderBidsByMaker() = runBlocking<Unit> {
        val user = "0xe91e379e30640b352f179c56071becf086ee473f"
        val page1 = service.getOrderBidsByMaker(
            null,
            listOf(user),
            null,
            listOf(OrderStatusDto.CANCELLED, OrderStatusDto.FILLED),
            null,
            null,
            null,
            2
        )

        assertThat(page1.entities).hasSize(2)

        val page2 = service.getOrderBidsByMaker(
            null,
            listOf(user),
            null,
            listOf(OrderStatusDto.CANCELLED, OrderStatusDto.FILLED),
            null,
            null,
            page1.continuation,
            2
        )

        println(page2)
        assertThat(page2.entities).hasSize(2)

        val bigPage = service.getOrderBidsByMaker(
            null,
            listOf(user),
            null,
            listOf(OrderStatusDto.CANCELLED, OrderStatusDto.FILLED),
            null,
            null,
            null,
            4
        ).entities


        println(bigPage)
        assertThat(bigPage).hasSize(4)
        assertThat(bigPage).isEqualTo(page1.entities + page2.entities)
    }

    @Test
    fun getBidsByItem() = runBlocking<Unit> {
        val itemId = "0xefdd9c545f29ef2df128105b288c220fe916ee7b:321"
        val result = service.getOrderBidsByItem(
            null,
            itemId,
            null,
            null,
            null,
            null,
            null,
            Address.ZERO().prefixed(),
            null,
            10
        ).entities

        println(result)
        assertThat(result).hasSize(1)
        assertThat(result[0].status).isEqualTo(OrderStatusDto.FILLED)
    }

    @Test
    fun `getBidsByItem - filtered by status`() = runBlocking<Unit> {
        val itemId = "0xefdd9c545f29ef2df128105b288c220fe916ee7b:321"
        val result = service.getOrderBidsByItem(
            null,
            itemId,
            null,
            null,
            listOf(OrderStatusDto.CANCELLED),
            null,
            null,
            Address.ZERO().prefixed(),
            null,
            10
        ).entities

        println(result)
        assertThat(result).hasSize(0)
    }
}