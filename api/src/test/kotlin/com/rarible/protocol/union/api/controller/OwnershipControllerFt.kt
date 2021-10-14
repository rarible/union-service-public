package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.api.client.OwnershipControllerApi
import com.rarible.protocol.union.api.configuration.PageSize
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.core.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.core.flow.converter.FlowOrderConverter
import com.rarible.protocol.union.core.flow.converter.FlowOwnershipConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.test.data.randomEthItemId
import com.rarible.protocol.union.test.data.randomEthOwnershipDto
import com.rarible.protocol.union.test.data.randomEthOwnershipId
import com.rarible.protocol.union.test.data.randomEthV2OrderDto
import com.rarible.protocol.union.test.data.randomFlowItemId
import com.rarible.protocol.union.test.data.randomFlowNftOwnershipDto
import com.rarible.protocol.union.test.data.randomFlowV1OrderDto
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@FlowPreview
@IntegrationTest
class OwnershipControllerFt : AbstractIntegrationTest() {

    private val continuation = null
    private val size = PageSize.OWNERSHIP.default

    @Autowired
    lateinit var ownershipControllerClient: OwnershipControllerApi

    @Autowired
    lateinit var enrichmentOwnershipService: EnrichmentOwnershipService

    @Autowired
    lateinit var ethOrderConverter: EthOrderConverter

    @Autowired
    lateinit var flowOrderConverter: FlowOrderConverter

    @Test
    fun `get ownership by id - ethereum, not enriched`() = runBlocking<Unit> {
        val ownershipIdFull = randomEthOwnershipId().fullId()
        val ownershipId = OwnershipIdParser.parseFull(ownershipIdFull)
        val ownership = randomEthOwnershipDto(ownershipId)

        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, ownership)

        val unionOwnership = ownershipControllerClient.getOwnershipById(ownershipIdFull).awaitFirst()

        assertThat(unionOwnership.id.value).isEqualTo(ownershipId.value)
        assertThat(unionOwnership.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
    }

    @Test
    fun `get ownership by id - flow, enriched`() = runBlocking<Unit> {
        // Enriched ownership
        val flowItemId = randomFlowItemId()
        val flowOwnership = randomFlowNftOwnershipDto(flowItemId)
        val flowUnionOwnership = FlowOwnershipConverter.convert(flowOwnership, flowItemId.blockchain)
        val flowOrder = randomFlowV1OrderDto(flowItemId)
        val flowUnionOrder = flowOrderConverter.convert(flowOrder, flowItemId.blockchain)
        val flowShortOwnership = ShortOwnershipConverter.convert(flowUnionOwnership)
            .copy(bestSellOrder = ShortOrderConverter.convert(flowUnionOrder))
        enrichmentOwnershipService.save(flowShortOwnership)

        flowOwnershipControllerApiMock.mockGetNftOwnershipById(flowUnionOwnership.id, flowOwnership)
        flowOrderControllerApiMock.mockGetById(flowOrder)

        val result = ownershipControllerClient.getOwnershipById(flowUnionOwnership.id.fullId()).awaitFirst()

        assertThat(result.id).isEqualTo(flowUnionOwnership.id)
        assertThat(result.bestSellOrder).isEqualTo(flowUnionOrder)
        assertThat(result.id.blockchain).isEqualTo(BlockchainDto.FLOW)
    }

    @Test
    fun `get ownerships by item - ethereum, partially enriched`() = runBlocking<Unit> {
        // Enriched ownership
        val ethItemId = randomEthItemId()
        val ethOwnership = randomEthOwnershipDto(ethItemId).copy(date = nowMillis())
        val ethUnionOwnership = EthOwnershipConverter.convert(ethOwnership, ethItemId.blockchain)
        val ethOrder = randomEthV2OrderDto(ethItemId)
        val ethUnionOrder = ethOrderConverter.convert(ethOrder, ethItemId.blockchain)
        val ethShortOwnership = ShortOwnershipConverter.convert(ethUnionOwnership)
            .copy(bestSellOrder = ShortOrderConverter.convert(ethUnionOrder))
        enrichmentOwnershipService.save(ethShortOwnership)

        val emptyEthOwnership = randomEthOwnershipDto(ethItemId)

        ethereumOrderControllerApiMock.mockGetByIds(ethOrder)
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipsByItem(
            ethItemId, continuation, size, emptyEthOwnership, ethOwnership
        )

        val ownerships = ownershipControllerClient.getOwnershipsByItem(
            ethItemId.token.fullId(), ethItemId.tokenId.toString(), continuation, size
        ).awaitFirst()

        val resultEmptyOwnership = ownerships.ownerships[0]
        val resultEnrichedOwnership = ownerships.ownerships[1]

        assertThat(resultEmptyOwnership.id.value).isEqualTo(emptyEthOwnership.id)
        assertThat(resultEmptyOwnership.bestSellOrder).isEqualTo(null)

        assertThat(resultEnrichedOwnership.id).isEqualTo(ethUnionOwnership.id)
        assertThat(resultEnrichedOwnership.bestSellOrder).isEqualTo(ethUnionOrder)
    }

    @Test
    fun `get ownerships by item - flow, nothing found`() = runBlocking<Unit> {
        val flowItemId = randomFlowItemId()

        flowOwnershipControllerApiMock.mockGetNftOwnershipsByItem(
            flowItemId, continuation, size
        )

        val ownerships = ownershipControllerClient.getOwnershipsByItem(
            flowItemId.token.fullId(), flowItemId.tokenId.toString(), continuation, size
        ).awaitFirst()

        assertThat(ownerships.ownerships).hasSize(0)
    }

    @Test
    fun `get all ownerships - trimmed to size`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW)
        val continuation = "${nowMillis()}_${randomString()}"
        val size = 3

        flowOwnershipControllerApiMock.mockGetNftAllOwnerships(
            continuation, size, randomFlowNftOwnershipDto(), randomFlowNftOwnershipDto()
        )
        ethereumOwnershipControllerApiMock.mockGetNftAllOwnerships(
            continuation, size, randomEthOwnershipDto(), randomEthOwnershipDto(), randomEthOwnershipDto()
        )

        val ownerships = ownershipControllerClient.getAllOwnerships(
            blockchains, continuation, size
        ).awaitFirst()

        assertThat(ownerships.ownerships).hasSize(3)
        assertThat(ownerships.total).isEqualTo(5)
        assertThat(ownerships.continuation).isNotNull()
    }

    @Test
    fun `get all ownerships - enriched`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW)
        val size = 3

        // Eth ownership
        val ethItemId = randomEthItemId()
        val ethOwnership = randomEthOwnershipDto(ethItemId).copy(date = nowMillis())
        val ethUnionOwnership = EthOwnershipConverter.convert(ethOwnership, ethItemId.blockchain)
        val ethOrder = randomEthV2OrderDto(ethItemId)
        val ethUnionOrder = ethOrderConverter.convert(ethOrder, ethItemId.blockchain)
        val ethShortOwnership = ShortOwnershipConverter.convert(ethUnionOwnership)
            .copy(bestSellOrder = ShortOrderConverter.convert(ethUnionOrder))
        enrichmentOwnershipService.save(ethShortOwnership)

        // Flow ownership
        val flowItemId = randomFlowItemId()
        val flowOwnership = randomFlowNftOwnershipDto(flowItemId).copy(createdAt = ethOwnership.date.minusSeconds(1))
        val flowUnionOwnership = FlowOwnershipConverter.convert(flowOwnership, flowItemId.blockchain)
        val flowOrder = randomFlowV1OrderDto(flowItemId)
        val flowUnionOrder = flowOrderConverter.convert(flowOrder, flowItemId.blockchain)
        val flowShortOwnership = ShortOwnershipConverter.convert(flowUnionOwnership)
            .copy(bestSellOrder = ShortOrderConverter.convert(flowUnionOrder))
        enrichmentOwnershipService.save(flowShortOwnership)

        ethereumOwnershipControllerApiMock.mockGetNftAllOwnerships(continuation, size, ethOwnership)
        flowOwnershipControllerApiMock.mockGetNftAllOwnerships(continuation, size, flowOwnership)
        ethereumOrderControllerApiMock.mockGetByIds(ethOrder)
        flowOrderControllerApiMock.mockGetById(flowOrder)
        flowOrderControllerApiMock.mockGetByIds(flowOrder)

        val ownerships = ownershipControllerClient.getAllOwnerships(
            blockchains, continuation, size
        ).awaitFirst()

        assertThat(ownerships.ownerships).hasSize(2)
        assertThat(ownerships.ownerships[0].bestSellOrder).isEqualTo(ethUnionOrder)
        assertThat(ownerships.ownerships[1].bestSellOrder).isEqualTo(flowUnionOrder)
    }
}
