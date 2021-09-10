package com.rarible.protocol.union.core.service

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
        val router = TestBlockchainRouter(listOf())
        val result = router.executeForAll(listOf()) { it.test() }

        assertEquals(0, result.size)
    }

    @Test
    fun `default blockchains`() = runBlocking {
        val expectedResult = listOf(BlockchainDto.FLOW.name, BlockchainDto.ETHEREUM.name)
        val router = TestBlockchainRouter(listOf(TestService(BlockchainDto.FLOW), TestService(BlockchainDto.ETHEREUM)))

        val resultForEmptyList = router.executeForAll(listOf()) { it.test() }
        assertIterableEquals(expectedResult, resultForEmptyList)

        val resultForNull = router.executeForAll(listOf()) { it.test() }
        assertIterableEquals(expectedResult, resultForNull)
    }

    @Test
    fun `one of services failed`() = runBlocking {
        val workingService = TestService(BlockchainDto.FLOW)
        val failedService: TestService = mockk()
        coEvery { failedService.getBlockchain() } returns BlockchainDto.ETHEREUM
        coEvery { failedService.test() } throws Exception("oops")

        val router = TestBlockchainRouter(listOf(failedService, workingService))
        val result = router.executeForAll(listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW)) { it.test() }

        assertEquals(1, result.size)
        assertEquals(BlockchainDto.FLOW.name, result[0])
    }

    @Test
    fun `unsupported blockchain`() = runBlocking<Unit> {
        val router = TestBlockchainRouter(listOf(TestService(BlockchainDto.FLOW), TestService(BlockchainDto.ETHEREUM)))
        assertThrows<IllegalArgumentException> { router.getService(BlockchainDto.POLYGON) }
    }

    private class TestBlockchainRouter(services: List<TestService>) : BlockchainRouter<TestService>(services)


    private class TestService(private val blockchain: BlockchainDto) : BlockchainService {
        override fun getBlockchain() = blockchain
        fun test() = blockchain.name
    }

}