package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.FlowNftOwnershipsDto
import com.rarible.protocol.dto.NftOwnershipsDto
import com.rarible.protocol.union.api.client.OwnershipControllerApi
import com.rarible.protocol.union.api.configuration.PageSize
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.dto.ethereum.EthOwnershipIdProvider
import com.rarible.protocol.union.dto.flow.FlowOwnershipIdProvider
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
class OwnershipControllerFt : AbstractIntegrationTest() {

    private val continuation = null
    private val size = PageSize.OWNERSHIP.default

    @Autowired
    lateinit var ownershipControllerClient: OwnershipControllerApi

    @Test
    fun `get ownership by id - ethereum`() = runBlocking<Unit> {
        val ownershipIdFull = randomEthOwnershipIdFullValue()
        val ownershipId = EthOwnershipIdProvider.parseFull(ownershipIdFull)
        val ownership = randomEthNftOwnershipDto(ownershipId)

        coEvery { testEthereumOwnershipApi.getNftOwnershipById(ownershipId.value) } returns ownership.toMono()

        val unionOwnership = ownershipControllerClient.getOwnershipById(ownershipIdFull).awaitFirst()
        val ethOwnership = unionOwnership as EthOwnershipDto

        assertThat(ethOwnership.id.value).isEqualTo(ownershipId.value)
        assertThat(ethOwnership.id.blockchain).isEqualTo(EthBlockchainDto.ETHEREUM)
    }

    @Test
    fun `get ownership by id - polygon`() = runBlocking<Unit> {
        val ownershipIdFull = randomPolygonOwnershipIdFullValue()
        val ownershipId = EthOwnershipIdProvider.parseFull(ownershipIdFull)
        val ownership = randomEthNftOwnershipDto(ownershipId)

        coEvery { testPolygonOwnershipApi.getNftOwnershipById(ownershipId.value) } returns ownership.toMono()

        val unionOwnership = ownershipControllerClient.getOwnershipById(ownershipIdFull).awaitFirst()
        val polyOwnership = unionOwnership as EthOwnershipDto

        assertThat(polyOwnership.id.value).isEqualTo(ownershipId.value)
        assertThat(polyOwnership.id.blockchain).isEqualTo(EthBlockchainDto.POLYGON)
    }

    @Test
    fun `get ownership by id - flow`() = runBlocking<Unit> {
        val ownershipIdFull = randomFlowOwnershipIdFullValue()
        val ownershipId = FlowOwnershipIdProvider.parseFull(ownershipIdFull)
        val ownership = randomFlowNftOwnershipDto(ownershipId)

        coEvery { testFlowOwnershipApi.getNftOwnershipById(ownershipId.value) } returns ownership.toMono()

        val unionOwnership = ownershipControllerClient.getOwnershipById(ownershipIdFull).awaitFirst()
        val flowOwnership = unionOwnership as FlowOwnershipDto

        assertThat(flowOwnership.id.value).isEqualTo(ownershipId.value)
        assertThat(flowOwnership.id.blockchain).isEqualTo(FlowBlockchainDto.FLOW)
    }

    @Test
    fun `get ownerships by item - ethereum`() = runBlocking<Unit> {
        val ethItemId = randomEthAddress()
        val ownership = randomEthNftOwnershipDto()
        val tokenId = ownership.tokenId.toString()

        coEvery {
            testEthereumOwnershipApi.getNftOwnershipsByItem(ethItemId.value, tokenId, continuation, size)
        } returns NftOwnershipsDto(1, null, listOf(ownership)).toMono()

        val unionOwnerships = ownershipControllerClient.getOwnershipsByItem(
            ethItemId.toString(), tokenId, continuation, size
        ).awaitFirst()

        val ethOwnership = unionOwnerships.ownerships[0] as EthOwnershipDto
        assertThat(ethOwnership.id.value).isEqualTo(ownership.id)
    }

    @Test
    fun `get ownerships by item - polygon`() = runBlocking<Unit> {
        val polyItemId = randomPolygonAddress()
        val ownership = randomEthNftOwnershipDto()
        val tokenId = ownership.tokenId.toString()

        coEvery {
            testPolygonOwnershipApi.getNftOwnershipsByItem(polyItemId.value, tokenId, continuation, size)
        } returns NftOwnershipsDto(1, null, listOf(ownership)).toMono()

        val unionOwnerships = ownershipControllerClient.getOwnershipsByItem(
            polyItemId.toString(), tokenId, continuation, size
        ).awaitFirst()

        val polyOwnership = unionOwnerships.ownerships[0] as EthOwnershipDto
        assertThat(polyOwnership.id.value).isEqualTo(ownership.id)
    }

    @Test
    fun `get ownerships by item - flow`() = runBlocking<Unit> {
        val flowItemId = randomFlowAddress()
        val ownership = randomFlowNftOwnershipDto()
        val tokenId = ownership.tokenId.toString()

        coEvery {
            testFlowOwnershipApi.getNftOwnershipsByItem(flowItemId.value, tokenId, continuation, size)
        } returns FlowNftOwnershipsDto(1, null, listOf(ownership)).toMono()

        val unionOwnerships = ownershipControllerClient.getOwnershipsByItem(
            flowItemId.toString(), tokenId, continuation, size
        ).awaitFirst()

        val flowOwnership = unionOwnerships.ownerships[0] as FlowOwnershipDto
        assertThat(flowOwnership.id.value).isEqualTo(ownership.id)
    }

    @Test
    fun `get all ownerships`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW)
        val continuation = "${nowMillis()}_${randomString()}"
        val size = 3

        val flowOwnerships = listOf(randomFlowNftOwnershipDto(), randomFlowNftOwnershipDto())
        val ethOwnerships = listOf(randomEthNftOwnershipDto(), randomEthNftOwnershipDto(), randomEthNftOwnershipDto())

        coEvery {
            testFlowOwnershipApi.getNftAllOwnerships(continuation, size)
        } returns FlowNftOwnershipsDto(2, null, flowOwnerships).toMono()

        coEvery {
            testEthereumOwnershipApi.getNftAllOwnerships(
                continuation,
                size
            )
        } returns NftOwnershipsDto(3, null, ethOwnerships).toMono()

        val unionOwnerships = ownershipControllerClient.getAllOwnerships(
            blockchains, continuation, size
        ).awaitFirst()

        assertThat(unionOwnerships.ownerships).hasSize(3)
        assertThat(unionOwnerships.total).isEqualTo(5)
        assertThat(unionOwnerships.continuation).isNotNull()
    }
}
