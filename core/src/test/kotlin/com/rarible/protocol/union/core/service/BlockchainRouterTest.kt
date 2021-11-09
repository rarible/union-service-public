package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.service.router.BlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BlockchainRouterTest {

    @Test
    fun `empty router`() = runBlocking {
        val router = BlockchainRouter<TestService>(listOf())
        val result = router.executeForAll(listOf()) { it.test() }

        assertEquals(0, result.size)
    }

    @Test
    fun `default blockchains`() = runBlocking {
        val expectedResult = listOf(BlockchainDto.FLOW.name, BlockchainDto.ETHEREUM.name)
        val router = BlockchainRouter(listOf(TestService(BlockchainDto.FLOW), TestService(BlockchainDto.ETHEREUM)))

        val resultForEmptyList = router.executeForAll(listOf()) { it.test() }
        assertIterableEquals(expectedResult, resultForEmptyList)

        val resultForNull = router.executeForAll(listOf()) { it.test() }
        assertIterableEquals(expectedResult, resultForNull)
    }

    @Test
    fun `one of services failed`() = runBlocking {
        val workingService = TestService(BlockchainDto.FLOW)
        val failedService: TestService = mockk()
        coEvery { failedService.blockchain } returns BlockchainDto.ETHEREUM
        coEvery { failedService.test() } throws Exception("oops")

        val router = BlockchainRouter(listOf(failedService, workingService))
        val result = router.executeForAll(listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW)) { it.test() }

        assertEquals(1, result.size)
        assertEquals(BlockchainDto.FLOW.name, result[0])
    }

    @Test
    fun `single service failed`() = runBlocking<Unit> {
        val failedService: TestService = mockk()
        coEvery { failedService.blockchain } returns BlockchainDto.ETHEREUM
        coEvery { failedService.test() } throws IllegalArgumentException("oops")

        val router = BlockchainRouter(listOf(failedService))

        assertThrows<IllegalArgumentException> {
            runBlocking {
                router.executeForAll(listOf(BlockchainDto.ETHEREUM)) { it.test() }
            }
        }
    }

    @Test
    fun `unsupported blockchain`() = runBlocking<Unit> {
        val router = BlockchainRouter(listOf(TestService(BlockchainDto.FLOW), TestService(BlockchainDto.ETHEREUM)))
        assertThrows<IllegalArgumentException> { router.getService(BlockchainDto.POLYGON) }
    }

    private class TestService(override val blockchain: BlockchainDto) : BlockchainService {
        fun test() = blockchain.name
    }

}