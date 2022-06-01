package com.rarible.protocol.union.integration.ethereum.mock

import com.rarible.protocol.dto.AuctionBidsPaginationDto
import com.rarible.protocol.dto.AuctionDto
import com.rarible.protocol.dto.AuctionIdsDto
import com.rarible.protocol.dto.AuctionsPaginationDto
import com.rarible.protocol.order.api.client.AuctionControllerApi
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.ItemIdDto
import io.mockk.every
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class EthAuctionControllerApiMock(
    private val auctionControllerApi: AuctionControllerApi
) {

    fun mockGetAuctionsByItem(itemId: ItemIdDto, seller: String, returnItems: List<AuctionDto>?) {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
        every {
            auctionControllerApi.getAuctionsByItem(
                contract,
                tokenId.toString(),
                seller,
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns (if (returnItems == null) Mono.empty() else Mono.just(AuctionsPaginationDto(returnItems, null)))
    }

    fun mockGetAuctionsByItem(itemId: ItemIdDto, returnItems: List<AuctionDto>?) {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
        every {
            auctionControllerApi.getAuctionsByItem(
                contract,
                tokenId.toString(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns (if (returnItems == null) Mono.empty() else Mono.just(AuctionsPaginationDto(returnItems, null)))
    }

    fun mockGetAuctionByHash(hash: String, returnItem: AuctionDto) {
        every {
            auctionControllerApi.getAuctionByHash(hash)
        } returns Mono.just(returnItem)
    }

    fun mockGetAuctionBidsByHash(hash: String, returnBids: AuctionBidsPaginationDto) {
        every {
            auctionControllerApi.getAuctionBidsByHash(hash, any(), any())
        } returns Mono.just(returnBids)
    }

    fun mockGetAuctionsByIds(vararg auctions: AuctionDto) {
        every {
            auctionControllerApi.getAuctionsByIds(AuctionIdsDto(auctions.map { it.hash }))
        } returns Flux.fromIterable(auctions.toList())
    }

    fun mockGetAllAuctions(returnItems: List<AuctionDto>) {
        every {
            auctionControllerApi.getAuctionsAll(any(), any(), any(), any(), any(), any())
        } returns Mono.just(AuctionsPaginationDto(returnItems, null))
    }

    fun mockGetAuctionsByCollection(contract: String, returnItems: List<AuctionDto>) {
        every {
            auctionControllerApi.getAuctionsByCollection(contract, any(), any(), any(), any(), any(), any())
        } returns Mono.just(AuctionsPaginationDto(returnItems, null))
    }

    fun mockGetAuctionsBySeller(address: String, returnItems: List<AuctionDto>) {
        every {
            auctionControllerApi.getAuctionsBySeller(address, any(), any(), any(), any(), any())
        } returns Mono.just(AuctionsPaginationDto(returnItems, null))
    }

}
