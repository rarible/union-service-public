package com.rarible.protocol.union.core.service.router

import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import randomOrder

class BlockchainRouterTest {

    val service: OrderService = mockk { every { blockchain } returns BlockchainDto.ETHEREUM }

    @Test
    fun `empty router`() = runBlocking {
        val router = BlockchainRouter<TestService>(listOf(), listOf())
        val result = router.executeForAll(listOf()) { it.test() }

        assertEquals(0, result.size)
    }

    @Test
    fun `default blockchains`() = runBlocking {
        val enabledBlockchains = listOf(BlockchainDto.FLOW, BlockchainDto.ETHEREUM)
        val expectedResult = enabledBlockchains.map { it.name }
        val router = BlockchainRouter(enabledBlockchains.map { TestService(it) }, enabledBlockchains)

        val resultForEmptyList = router.executeForAll(listOf()) { it.test() }
        assertIterableEquals(expectedResult, resultForEmptyList)

        val resultForNull = router.executeForAll(listOf()) { it.test() }
        assertIterableEquals(expectedResult, resultForNull)
    }

    @Test
    fun `one of services failed`() = runBlocking {
        val enabledBlockchains = listOf(BlockchainDto.FLOW, BlockchainDto.ETHEREUM)

        val workingService = TestService(BlockchainDto.FLOW)
        val failedService: TestService = mockk()
        coEvery { failedService.blockchain } returns BlockchainDto.ETHEREUM
        coEvery { failedService.test() } throws Exception("oops")

        val router = BlockchainRouter(listOf(failedService, workingService), enabledBlockchains)
        val result = router.executeForAll(listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW)) { it.test() }

        assertEquals(1, result.size)
        assertEquals(BlockchainDto.FLOW.name, result[0])
    }

    @Test
    fun `single service`() = runBlocking<Unit> {
        val service = TestService(BlockchainDto.FLOW)

        val router = BlockchainRouter(listOf(service), listOf(BlockchainDto.FLOW))
        val result = router.executeForAll(emptyList()) { it.test() }

        assertEquals(1, result.size)
        assertEquals(BlockchainDto.FLOW.name, result[0])
    }

    @Test
    fun `single service failed`() = runBlocking<Unit> {
        val failedService: TestService = mockk()
        coEvery { failedService.blockchain } returns BlockchainDto.ETHEREUM
        coEvery { failedService.test() } throws IllegalArgumentException("oops")

        val router = BlockchainRouter(listOf(failedService), listOf(BlockchainDto.ETHEREUM))

        assertThrows<IllegalArgumentException> {
            runBlocking {
                router.executeForAll(listOf(BlockchainDto.ETHEREUM)) { it.test() }
            }
        }
    }

    @Test
    fun `unsupported blockchain`() = runBlocking<Unit> {
        val enabledBlockchains = listOf(BlockchainDto.FLOW, BlockchainDto.ETHEREUM)
        val router = BlockchainRouter(enabledBlockchains.map { TestService(it) }, enabledBlockchains)
        assertThrows<IllegalArgumentException> { router.getService(BlockchainDto.POLYGON) }
    }

    @Test
    fun `fetch all slices - stopped by continuation`() = runBlocking<Unit> {
        val size = 1
        val cont1 = "c1"
        val cont2: String? = null
        val sort = SyncSortDto.DB_UPDATE_DESC
        val order1 = randomOrder()
        val order2 = randomOrder()
        val router = BlockchainRouter(listOf(service), listOf(BlockchainDto.ETHEREUM))

        coEvery { service.getAllSync(null, size, sort) } returns Slice(cont1, listOf(order1))
        coEvery { service.getAllSync(cont1, size, sort) } returns Slice(cont2, listOf(order2))

        val result = router.fetchAllBySlices(BlockchainDto.ETHEREUM) { service, continuation ->
            service.getAllSync(continuation, size, sort)
        }

        assertThat(result).isEqualTo(listOf(order1, order2))
    }

    @Test
    fun `fetch all slices - stopped by empty slice`() = runBlocking<Unit> {
        val size = 1
        val cont1 = "c1"
        val cont2 = "c2"
        val sort = SyncSortDto.DB_UPDATE_DESC
        val order1 = randomOrder()
        val router = BlockchainRouter(listOf(service), listOf(BlockchainDto.ETHEREUM))

        coEvery { service.getAllSync(null, size, sort) } returns Slice(cont1, listOf(order1))
        coEvery { service.getAllSync(cont1, size, sort) } returns Slice(cont2, emptyList())

        val result = router.fetchAllBySlices(BlockchainDto.ETHEREUM) { service, continuation ->
            service.getAllSync(continuation, size, sort)
        }

        assertThat(result).isEqualTo(listOf(order1))
    }

    private class TestService(override val blockchain: BlockchainDto) : BlockchainService {

        fun test() = blockchain.name
    }

}
