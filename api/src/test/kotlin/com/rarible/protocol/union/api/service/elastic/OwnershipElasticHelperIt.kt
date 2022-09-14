package com.rarible.protocol.union.api.service.elastic

import com.ninjasquad.springmockk.SpykBean
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.enrichment.test.data.randomUnionAddress
import com.rarible.protocol.union.enrichment.test.data.randomUnionOwnership
import com.rarible.protocol.union.integration.flow.data.randomFlowItemId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import randomEsOwnership

@IntegrationTest
internal class OwnershipElasticHelperIt {

    @SpykBean
    private lateinit var router: BlockchainRouter<OwnershipService>

    @Autowired
    private lateinit var repository: EsOwnershipRepository

    @Autowired
    private lateinit var helper: OwnershipElasticHelper

    @Test
    fun `should get raw ownerships by owner`() = runBlocking<Unit> {
        // given
        val owner = randomUnionAddress()
        val continuation = null
        val size = 42
        val ownership = randomEsOwnership().copy(blockchain = BlockchainDto.ETHEREUM, owner = owner.fullId())
        repository.saveAll(listOf(ownership))
        val ownershipService = mockk<OwnershipService>()
        val expected = randomUnionOwnership().copy(id = OwnershipIdParser.parseFull(ownership.ownershipId))
        coEvery { ownershipService.getOwnershipsByIds(any()) } returns listOf(expected)
        every { router.getService(BlockchainDto.ETHEREUM) } returns ownershipService

        // when
        val actual = helper.getRawOwnershipsByOwner(owner, continuation, size)

        // then
        assertThat(actual.entities).isEqualTo(listOf(expected))
        verify {
            router.isBlockchainEnabled(BlockchainDto.ETHEREUM)
            router.getService(BlockchainDto.ETHEREUM)
        }
        coVerify {
            ownershipService.getOwnershipsByIds(listOf(OwnershipIdParser.parseFull(ownership.ownershipId).value))
        }
        confirmVerified(router, ownershipService)
    }

    @Test
    fun `should get raw ownerships by item`() = runBlocking<Unit> {
        // given
        val item = randomFlowItemId()
        val continuation = null
        val size = 42
        val ownership = randomEsOwnership().copy(blockchain = BlockchainDto.FLOW, itemId = item.fullId())
        repository.saveAll(listOf(ownership))
        val ownershipService = mockk<OwnershipService>()
        val expected = randomUnionOwnership().copy(id = OwnershipIdParser.parseFull(ownership.ownershipId))
        coEvery { ownershipService.getOwnershipsByIds(any()) } returns listOf(expected)
        every { router.getService(BlockchainDto.FLOW) } returns ownershipService

        // when
        val actual = helper.getRawOwnershipsByItem(item, continuation, size)

        // then
        assertThat(actual.entities).isEqualTo(listOf(expected))
        verify {
            router.isBlockchainEnabled(BlockchainDto.FLOW)
            router.getService(BlockchainDto.FLOW)
        }
        coVerify {
            ownershipService.getOwnershipsByIds(listOf(OwnershipIdParser.parseFull(ownership.ownershipId).value))
        }
        confirmVerified(router, ownershipService)
    }
}
