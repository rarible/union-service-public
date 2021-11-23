package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.client.AuctionControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.continuation.page.PageSize
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.AuctionSortDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@FlowPreview
@IntegrationTest
class AuctionControllerFt : AbstractIntegrationTest() {

    private val continuation: String? = null
    private val size = PageSize.AUCTION.default
    private val platform = PlatformDto.ALL

    @Autowired
    lateinit var auctionControllerClient: AuctionControllerApi

    @Autowired
    lateinit var ethAuctionConverter: EthAuctionConverter

    @Test
    fun `get auction by hash - ethereum`() = runBlocking<Unit> {
        val auction = randomEthAuctionDto()
        val auctionId = EthConverter.convert(auction.hash)
        val orderIdFull = AuctionIdDto(BlockchainDto.ETHEREUM, auction.hash.prefixed()).fullId()

        ethereumAuctionControllerApiMock.mockGetAuctionByHash(auction.hash.prefixed(), auction)

        val unionAuction = auctionControllerClient.getAuctionById(orderIdFull).awaitFirst()

        assertThat(unionAuction.id.value).isEqualTo(auctionId)
        assertThat(unionAuction.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
    }

    @Test
    fun `get all auction - ethereum`() = runBlocking<Unit> {
        val ethereumAuction = randomEthAuctionDto()
        val polygonAuction = randomEthAuctionDto()
        val origin = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())
        val unionEthereumAuction = ethAuctionConverter.convert(ethereumAuction, BlockchainDto.ETHEREUM)
        val unionPolygonAuction = ethAuctionConverter.convert(polygonAuction, BlockchainDto.POLYGON)

        ethereumAuctionControllerApiMock.mockGetAllAuctions(listOf(ethereumAuction))
        polygonAuctionControllerApiMock.mockGetAllAuctions(listOf(polygonAuction))

        val unionAuctions = auctionControllerClient.getAuctionsAll(
            listOf(BlockchainDto.ETHEREUM),
            AuctionSortDto.LAST_UPDATE_DESC,
            listOf(AuctionStatusDto.ACTIVE),
            origin.fullId(),
            platform, continuation, size
        ).awaitFirst()

        val auctionIds = unionAuctions.auctions.map { it.auctionId }

        assertThat(unionAuctions.auctions).hasSize(2)
        assertThat(auctionIds).contains(unionEthereumAuction.auctionId, unionPolygonAuction.auctionId)
    }

    @Test
    fun `get auctions by collection - ethereum`() = runBlocking<Unit> {
        val auction = randomEthAuctionDto()
        val contract = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())
        val seller = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())
        val origin = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())
        val unionAuction = ethAuctionConverter.convert(auction, BlockchainDto.ETHEREUM)

        ethereumAuctionControllerApiMock.mockGetAuctionsByCollection(contract.value, listOf(auction))

        val unionAuctions = auctionControllerClient.getAuctionsByCollection(
            contract.fullId(),
            seller.fullId(),
            origin.fullId(),
            listOf(AuctionStatusDto.ACTIVE),
            platform, continuation, size
        ).awaitFirst()

        assertThat(unionAuctions.auctions.size).isEqualTo(1)
        assertThat(unionAuctions.auctions.first().id).isEqualTo(unionAuction.id)
    }

    @Test
    fun `get auctions by item - ethereum`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val auction = randomEthAuctionDto(ethItemId)
        val seller = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())
        val origin = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())
        val unionAuction = ethAuctionConverter.convert(auction, BlockchainDto.ETHEREUM)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(ethItemId, listOf(auction))

        val unionAuctions = auctionControllerClient.getAuctionsByItem(
            ethItemId.fullId(),
            seller.fullId(),
            AuctionSortDto.LAST_UPDATE_DESC,
            origin.fullId(),
            listOf(AuctionStatusDto.ACTIVE),
            platform, continuation, size
        ).awaitFirst()

        assertThat(unionAuctions.auctions.size).isEqualTo(1)
        assertThat(unionAuctions.auctions.first().id).isEqualTo(unionAuction.id)
    }

    @Test
    fun `get auctions by seller - ethereum`() = runBlocking<Unit> {
        val auction = randomEthAuctionDto()
        val seller = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())
        val origin = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())
        val unionAuction = ethAuctionConverter.convert(auction, BlockchainDto.ETHEREUM)

        ethereumAuctionControllerApiMock.mockGetAuctionsBySeller(seller.value, listOf(auction))
        polygonAuctionControllerApiMock.mockGetAuctionsBySeller(seller.value, listOf())

        val unionAuctions = auctionControllerClient.getAuctionsBySeller(
            seller.fullId(),
            listOf(AuctionStatusDto.ACTIVE),
            origin.fullId(),
            platform, continuation, size
        ).awaitFirst()

        assertThat(unionAuctions.auctions.size).isEqualTo(1)
        assertThat(unionAuctions.auctions.first().id).isEqualTo(unionAuction.id)
    }

}
