package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.api.client.OwnershipControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.group
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import com.rarible.protocol.union.integration.ethereum.data.randomEthV2OrderDto
import com.rarible.protocol.union.integration.flow.converter.FlowOrderConverter
import com.rarible.protocol.union.integration.flow.converter.FlowOwnershipConverter
import com.rarible.protocol.union.integration.flow.data.randomFlowItemId
import com.rarible.protocol.union.integration.flow.data.randomFlowNftOwnershipDto
import com.rarible.protocol.union.integration.flow.data.randomFlowV1OrderDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosItemId
import com.rarible.protocol.union.integration.tezos.data.randomTezosOwnershipDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosOwnershipId
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
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val ownership = randomEthOwnershipDto(ownershipId)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(itemId, emptyList())
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, ownership)

        val unionOwnership = ownershipControllerClient.getOwnershipById(ownershipId.fullId()).awaitFirst()

        assertThat(unionOwnership.id.value).isEqualTo(ownershipId.value)
        assertThat(unionOwnership.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
    }

    @Test
    fun `get ownership by id - ethereum, auction ownership`() = runBlocking<Unit> {
        val itemId = randomEthItemId()

        val auction = randomEthAuctionDto(itemId)
        val auctionContract = UnionAddress(itemId.blockchain.group(), EthConverter.convert(auction.contract))
        val auctionOwnershipId = randomEthOwnershipId(itemId).copy(owner = auctionContract)
        val auctionOwnership = randomEthOwnershipDto(auctionOwnershipId)

        val sellerContract = UnionAddress(itemId.blockchain.group(), EthConverter.convert(auction.seller))
        val ownershipId = randomEthOwnershipId(itemId).copy(owner = sellerContract)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(itemId, listOf(auction))
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipByIdNotFound(ownershipId)
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(auctionOwnershipId, auctionOwnership)

        val unionOwnership = ownershipControllerClient.getOwnershipById(ownershipId.fullId()).awaitFirst()

        assertThat(unionOwnership.id).isEqualTo(ownershipId)
        assertThat(unionOwnership.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
        assertThat(unionOwnership.auction!!.id.value).isEqualTo(EthConverter.convert(auction.hash))
    }

    @Test
    fun `get ownership by id - ethereum, partially auctioned ownership`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val ownership = randomEthOwnershipDto(ownershipId)

        val auction = randomEthAuctionDto(itemId)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(itemId, listOf(auction))
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, ownership)

        val unionOwnership = ownershipControllerClient.getOwnershipById(ownershipId.fullId()).awaitFirst()

        assertThat(unionOwnership.id).isEqualTo(ownershipId)
        assertThat(unionOwnership.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
        assertThat(unionOwnership.value).isEqualTo(ownership.value + auction.sell.valueDecimal!!.toBigInteger())
        assertThat(unionOwnership.auction!!.id.value).isEqualTo(EthConverter.convert(auction.hash))
    }

//    @Test
//    fun `get ownership by id - tezos, not enriched`() = runBlocking<Unit> {
//        val ownershipIdFull = randomTezosOwnershipId().fullId()
//        val ownershipId = OwnershipIdParser.parseFull(ownershipIdFull)
//        val ownership = randomTezosOwnershipDto(ownershipId)
//
//        tezosOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, ownership)
//
//        val unionOwnership = ownershipControllerClient.getOwnershipById(ownershipIdFull).awaitFirst()
//
//        assertThat(unionOwnership.id.value).isEqualTo(ownershipId.value)
//        assertThat(unionOwnership.id.blockchain).isEqualTo(BlockchainDto.TEZOS)
//    }

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
        flowOrderControllerApiMock.mockGetByIds(flowOrder)

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

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(ethItemId, listOf())
        ethereumOrderControllerApiMock.mockGetByIds(ethOrder)
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipsByItem(
            ethItemId, continuation, size, emptyEthOwnership, ethOwnership
        )

        val ownerships = ownershipControllerClient.getOwnershipsByItem(
            ethItemId.fullId(), continuation, size
        ).awaitFirst()

        val resultEmptyOwnership = ownerships.ownerships[0]
        val resultEnrichedOwnership = ownerships.ownerships[1]

        assertThat(resultEmptyOwnership.id.value).isEqualTo(emptyEthOwnership.id)
        assertThat(resultEmptyOwnership.bestSellOrder).isEqualTo(null)

        assertThat(resultEnrichedOwnership.id).isEqualTo(ethUnionOwnership.id)
        assertThat(resultEnrichedOwnership.bestSellOrder!!.id).isEqualTo(ethUnionOrder.id)
    }

    @Test
    fun `get ownerships by item - ethereum, with auctions`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()

        // Partial auction - some of user's tokens set for sale
        val auction = randomEthAuctionDto(ethItemId)

        // Full auction - all tokens set for sale
        val fullAuction = randomEthAuctionDto(ethItemId)
        val fullAuctionContract = UnionAddress(ethItemId.blockchain.group(), EthConverter.convert(fullAuction.contract))
        val fullAuctionOwnershipId = randomEthOwnershipId(ethItemId).copy(owner = fullAuctionContract)
        val fullAuctionOwnership = randomEthOwnershipDto(fullAuctionOwnershipId)

        // Free ownership of some user - no auction for it
        val ethOwnershipId = randomEthOwnershipId(ethItemId)
        val ethOwnership = randomEthOwnershipDto(ethOwnershipId)

        // Part of ownership is not participating in auction
        val ethAuctionedOwnershipId = ethItemId.toOwnership(EthConverter.convert(auction.seller))
        val ethAuctionedOwnership = randomEthOwnershipDto(ethAuctionedOwnershipId)

        // Non-existing user ownership - all items set for sale
        val ethFullyAuctionedOwnershipId = ethItemId.toOwnership(EthConverter.convert(fullAuction.seller))

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(ethItemId, listOf(auction, fullAuction))
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(fullAuctionOwnershipId, fullAuctionOwnership)
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(ethAuctionedOwnershipId, ethAuctionedOwnership)
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipByIdNotFound(ethFullyAuctionedOwnershipId)
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipsByItem(
            ethItemId, continuation, 50 + 1, ethAuctionedOwnership, ethOwnership
        )

        val ownerships = ownershipControllerClient.getOwnershipsByItem(
            ethItemId.fullId(), continuation, 50
        ).awaitFirst().ownerships

        // AS a result we expect 3 ownerships: free, partial and disguised by full auction
        assertThat(ownerships).hasSize(3)

        val freeOwnership = ownerships.find { it.id == ethOwnershipId }!!
        val auctionedOwnership = ownerships.find { it.id == ethAuctionedOwnershipId }!!
        val fullyAuctionedOwnership = ownerships.find { it.id == ethFullyAuctionedOwnershipId }!!

        assertThat(freeOwnership.auction).isNull()
        assertThat(auctionedOwnership.auction!!.id.value).isEqualTo(EthConverter.convert(auction.hash))
        assertThat(fullyAuctionedOwnership.auction!!.id.value).isEqualTo(EthConverter.convert(fullAuction.hash))

        assertThat(auctionedOwnership.value)
            .isEqualTo(ethAuctionedOwnership.value + auction.sell.valueDecimal!!.toBigInteger())
        assertThat(fullyAuctionedOwnership.value)
            .isEqualTo(fullAuction.sell.valueDecimal!!.toBigInteger())
    }

    @Test
    fun `get ownerships by item - flow, nothing found`() = runBlocking<Unit> {
        val flowItemId = randomFlowItemId()

        flowOwnershipControllerApiMock.mockGetNftOwnershipsByItem(
            flowItemId, continuation, size
        )

        val ownerships = ownershipControllerClient.getOwnershipsByItem(
            flowItemId.fullId(), continuation, size
        ).awaitFirst()

        assertThat(ownerships.ownerships).hasSize(0)
    }

//    @Test
//    fun `get ownerships by item - tezos, nothing enriched`() = runBlocking<Unit> {
//        val itemId = randomTezosItemId()
//        val ownership = randomTezosOwnershipDto(itemId)
//
//        tezosOwnershipControllerApiMock.mockGetNftOwnershipsByItem(
//            itemId, continuation, size, ownership
//        )
//
//        val ownerships = ownershipControllerClient.getOwnershipsByItem(
//            itemId.fullId(), continuation, size
//        ).awaitFirst()
//
//        assertThat(ownerships.ownerships).hasSize(1)
//    }
}
