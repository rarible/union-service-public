package com.rarible.protocol.union.integration.flow.service

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.AuctionBidDto
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionSortDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.page.Slice

class FlowAuctionService(
    blockchain: BlockchainDto
) : AbstractBlockchainService(blockchain), AuctionService {

    override suspend fun getAuctionsBidsById(
        auctionId: String,
        continuation: String?,
        size: Int?
    ): Slice<AuctionBidDto> {
        return Slice.empty()
    }

    override suspend fun getAuctionById(auctionId: String): AuctionDto {
        throw UnionNotFoundException("Auction [$auctionId] not found, ${blockchain.name} is not available")
    }

    override suspend fun getAuctionsByIds(auctionIds: List<String>): List<AuctionDto> {
        return emptyList()
    }

    override suspend fun getAuctionsAll(
        sort: AuctionSortDto?,
        status: List<AuctionStatusDto>?,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): Slice<AuctionDto> {
        return Slice.empty()
    }

    override suspend fun getAuctionsByCollection(
        contract: String,
        seller: String?,
        origin: String?,
        status: List<AuctionStatusDto>?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): Slice<AuctionDto> {
        return Slice.empty()
    }

    override suspend fun getAuctionsByItem(
        itemId: String,
        seller: String?,
        sort: AuctionSortDto?,
        origin: String?,
        status: List<AuctionStatusDto>?,
        currencyId: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): Slice<AuctionDto> {
        return Slice.empty()
    }

    override suspend fun getAuctionsBySeller(
        seller: String,
        status: List<AuctionStatusDto>?,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): Slice<AuctionDto> {
        return Slice.empty()
    }
}
