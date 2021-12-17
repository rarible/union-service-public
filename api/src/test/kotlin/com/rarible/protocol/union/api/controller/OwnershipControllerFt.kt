package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.api.client.OwnershipControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import com.rarible.protocol.union.integration.ethereum.data.randomEthV2OrderDto
import com.rarible.protocol.union.integration.flow.converter.FlowOrderConverter
import com.rarible.protocol.union.integration.flow.converter.FlowOwnershipConverter
import com.rarible.protocol.union.integration.tezos.data.randomTezosItemId
import com.rarible.protocol.union.integration.tezos.data.randomTezosOwnershipDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosOwnershipId
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
    fun `get ownership by id - tezos, not enriched`() = runBlocking<Unit> {
        val ownershipIdFull = randomTezosOwnershipId().fullId()
        val ownershipId = OwnershipIdParser.parseFull(ownershipIdFull)
        val ownership = randomTezosOwnershipDto(ownershipId)

        tezosOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, ownership)

        val unionOwnership = ownershipControllerClient.getOwnershipById(ownershipIdFull).awaitFirst()

        assertThat(unionOwnership.id.value).isEqualTo(ownershipId.value)
        assertThat(unionOwnership.id.blockchain).isEqualTo(BlockchainDto.TEZOS)
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
        assertThat(result.bestSellOrder!!.id).isEqualTo(flowUnionOrder.id)
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
            ethItemId.fullId(), null, null, continuation, size
        ).awaitFirst()

        val resultEmptyOwnership = ownerships.ownerships[0]
        val resultEnrichedOwnership = ownerships.ownerships[1]

        assertThat(resultEmptyOwnership.id.value).isEqualTo(emptyEthOwnership.id)
        assertThat(resultEmptyOwnership.bestSellOrder).isEqualTo(null)

        assertThat(resultEnrichedOwnership.id).isEqualTo(ethUnionOwnership.id)
        assertThat(resultEnrichedOwnership.bestSellOrder!!.id).isEqualTo(ethUnionOrder.id)
    }

    @Test
    fun `get ownerships by item - flow, nothing found`() = runBlocking<Unit> {
        val flowItemId = randomFlowItemId()

        flowOwnershipControllerApiMock.mockGetNftOwnershipsByItem(
            flowItemId, continuation, size
        )

        val ownerships = ownershipControllerClient.getOwnershipsByItem(
            flowItemId.fullId(), null, null, continuation, size
        ).awaitFirst()

        assertThat(ownerships.ownerships).hasSize(0)
    }

    @Test
    fun `get ownerships by item - tezos, nothing enriched`() = runBlocking<Unit> {
        val itemId = randomTezosItemId()
        val ownership = randomTezosOwnershipDto(itemId)

        tezosOwnershipControllerApiMock.mockGetNftOwnershipsByItem(
            itemId, continuation, size, ownership
        )

        val ownerships = ownershipControllerClient.getOwnershipsByItem(
            itemId.fullId(), null, null, continuation, size
        ).awaitFirst()

        assertThat(ownerships.ownerships).hasSize(1)
    }

    @Test
    fun `get all ownerships - trimmed to size`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW, BlockchainDto.TEZOS)
        val now = nowMillis()

        val ethList = listOf(
            randomEthOwnershipDto().copy(date = now),
            randomEthOwnershipDto().copy(date = now.minusSeconds(10)),
            randomEthOwnershipDto().copy(date = now.minusSeconds(15))
        )

        val flowList = listOf(
            randomFlowNftOwnershipDto().copy(createdAt = now),
            randomFlowNftOwnershipDto().copy(createdAt = now.minusSeconds(10))
        )

        val ethContinuation = "${now.toEpochMilli()}_${ethList.first().id}"
        val flowContinuation = "${now.toEpochMilli()}_${flowList.first().id}"
        val cursorArg = CombinedContinuation(
            mapOf(
                BlockchainDto.ETHEREUM.toString() to ethContinuation,
                BlockchainDto.FLOW.toString() to flowContinuation,
                BlockchainDto.TEZOS.toString() to ArgSlice.COMPLETED,
            )
        )
        val size = 3

        flowOwnershipControllerApiMock.mockGetNftAllOwnerships(
            flowContinuation, size, *flowList.toTypedArray()
        )
        ethereumOwnershipControllerApiMock.mockGetNftAllOwnerships(
            ethContinuation, size, *ethList.toTypedArray()
        )

        val ownerships = ownershipControllerClient.getAllOwnerships(
            blockchains, cursorArg.toString(), size
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
        assertThat(ownerships.ownerships[0].bestSellOrder!!.id).isEqualTo(ethUnionOrder.id)
        assertThat(ownerships.ownerships[1].bestSellOrder!!.id).isEqualTo(flowUnionOrder.id)
    }
}
