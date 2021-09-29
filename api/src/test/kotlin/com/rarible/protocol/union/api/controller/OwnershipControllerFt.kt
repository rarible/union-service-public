package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.FlowNftOwnershipsDto
import com.rarible.protocol.dto.NftOwnershipsDto
import com.rarible.protocol.union.api.client.OwnershipControllerApi
import com.rarible.protocol.union.api.configuration.PageSize
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import com.rarible.protocol.union.test.data.randomEthAddress
import com.rarible.protocol.union.test.data.randomEthNftOwnershipDto
import com.rarible.protocol.union.test.data.randomEthOwnershipId
import com.rarible.protocol.union.test.data.randomFlowAddress
import com.rarible.protocol.union.test.data.randomFlowNftOwnershipDto
import com.rarible.protocol.union.test.data.randomFlowOwnershipId
import com.rarible.protocol.union.test.data.randomPolygonAddress
import com.rarible.protocol.union.test.data.randomPolygonOwnershipId
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono

@FlowPreview
@IntegrationTest
@Disabled // TODO enable after enrichment implemented
class OwnershipControllerFt : AbstractIntegrationTest() {

    private val continuation = null
    private val size = PageSize.OWNERSHIP.default

    @Autowired
    lateinit var ownershipControllerClient: OwnershipControllerApi

    @Test
    fun `get ownership by id - ethereum`() = runBlocking<Unit> {
        val ownershipIdFull = randomEthOwnershipId().fullId()
        val ownershipId = OwnershipIdParser.parseFull(ownershipIdFull)
        val ownership = randomEthNftOwnershipDto(ownershipId)

        coEvery { testEthereumOwnershipApi.getNftOwnershipById(ownershipId.value) } returns ownership.toMono()

        val unionOwnership = ownershipControllerClient.getOwnershipById(ownershipIdFull).awaitFirst()

        assertThat(unionOwnership.id.value).isEqualTo(ownershipId.value)
        assertThat(unionOwnership.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
    }

    @Test
    fun `get ownership by id - polygon`() = runBlocking<Unit> {
        val ownershipIdFull = randomPolygonOwnershipId().fullId()
        val ownershipId = OwnershipIdParser.parseFull(ownershipIdFull)
        val ownership = randomEthNftOwnershipDto(ownershipId)

        coEvery { testPolygonOwnershipApi.getNftOwnershipById(ownershipId.value) } returns ownership.toMono()

        val unionOwnership = ownershipControllerClient.getOwnershipById(ownershipIdFull).awaitFirst()

        assertThat(unionOwnership.id.value).isEqualTo(ownershipId.value)
        assertThat(unionOwnership.id.blockchain).isEqualTo(BlockchainDto.POLYGON)
    }

    @Test
    fun `get ownership by id - flow`() = runBlocking<Unit> {
        val ownershipIdFull = randomFlowOwnershipId().fullId()
        val ownershipId = OwnershipIdParser.parseFull(ownershipIdFull)
        val ownership = randomFlowNftOwnershipDto(ownershipId)

        coEvery { testFlowOwnershipApi.getNftOwnershipById(ownershipId.value) } returns ownership.toMono()

        val unionOwnership = ownershipControllerClient.getOwnershipById(ownershipIdFull).awaitFirst()

        assertThat(unionOwnership.id.value).isEqualTo(ownershipId.value)
        assertThat(unionOwnership.id.blockchain).isEqualTo(BlockchainDto.FLOW)
    }

    @Test
    fun `get ownerships by item - ethereum`() = runBlocking<Unit> {
        val ethItemId = randomEthAddress()
        val ownership = randomEthNftOwnershipDto()
        val tokenId = ownership.tokenId.toString()

        coEvery {
            testEthereumOwnershipApi.getNftOwnershipsByItem(ethItemId.value, tokenId, continuation, size)
        } returns NftOwnershipsDto(1, null, listOf(ownership)).toMono()

        val ownerships = ownershipControllerClient.getOwnershipsByItem(
            ethItemId.fullId(), tokenId, continuation, size
        ).awaitFirst()

        val ethOwnership = ownerships.ownerships[0]
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

        val ownerships = ownershipControllerClient.getOwnershipsByItem(
            polyItemId.fullId(), tokenId, continuation, size
        ).awaitFirst()

        val polyOwnership = ownerships.ownerships[0]
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

        val ownerships = ownershipControllerClient.getOwnershipsByItem(
            flowItemId.fullId(), tokenId, continuation, size
        ).awaitFirst()

        val flowOwnership = ownerships.ownerships[0]
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

        val ownerships = ownershipControllerClient.getAllOwnerships(
            blockchains, continuation, size
        ).awaitFirst()

        assertThat(ownerships.ownerships).hasSize(3)
        assertThat(ownerships.total).isEqualTo(5)
        assertThat(ownerships.continuation).isNotNull()
    }
}
