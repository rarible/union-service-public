package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.immutablex.client.ImxOrderClient
import io.mockk.clearMocks
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import randomItemId

class ImxOrderServiceTest {

    private val orderClient: ImxOrderClient = mockk()

    private val service = ImxOrderService(orderClient)

    @BeforeEach
    fun beforeEach() {
        clearMocks(orderClient)
    }

    @Test
    fun `getOrderBidsByMaker - with origin`() = runBlocking<Unit> {
        val origin = randomString()
        val slice = service.getOrderBidsByMaker(
            null,
            listOf(randomString()),
            origin,
            null,
            null,
            null,
            null,
            50
        )
        assertThat(slice.entities).isEmpty()
    }

    @Test
    fun `getOrderBidsByItem - with origin`() = runBlocking<Unit> {
        val origin = randomString()
        val slice = service.getOrderBidsByItem(
            null,
            randomItemId(BlockchainDto.IMMUTABLEX).value,
            null,
            origin,
            null,
            null,
            null,
            randomString(),
            null,
            50
        )
        assertThat(slice.entities).isEmpty()
    }

    @Test
    fun `getSellOrders - with origin`() = runBlocking<Unit> {
        val origin = randomString()
        val slice = service.getSellOrders(
            null,
            origin,
            null,
            50
        )
        assertThat(slice.entities).isEmpty()
    }

    @Test
    fun `getSellOrdersByCollection - with origin`() = runBlocking<Unit> {
        val origin = randomString()
        val slice = service.getSellOrdersByCollection(
            null,
            randomString(),
            origin,
            null,
            50
        )
        assertThat(slice.entities).isEmpty()
    }

    @Test
    fun `getSellOrdersByItem - with origin`() = runBlocking<Unit> {
        val origin = randomString()
        val slice = service.getSellOrdersByItem(
            null,
            randomItemId(BlockchainDto.IMMUTABLEX).value,
            null,
            origin,
            null,
            randomString(),
            null,
            50
        )
        assertThat(slice.entities).isEmpty()
    }

    @Test
    fun `getSellOrdersByMaker - with origin`() = runBlocking<Unit> {
        val origin = randomString()
        val slice = service.getSellOrdersByMaker(
            null,
            listOf(randomString()),
            origin,
            null,
            null,
            50
        )
        assertThat(slice.entities).isEmpty()
    }
}