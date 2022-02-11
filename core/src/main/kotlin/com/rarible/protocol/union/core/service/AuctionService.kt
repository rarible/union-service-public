package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.service.router.BlockchainService
import com.rarible.protocol.union.dto.AuctionBidDto
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionSortDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.page.Slice

interface AuctionService : BlockchainService {

    suspend fun getAuctionsBidsById(
        auctionId: String,
        continuation: String?,
        size: Int?
    ): Slice<AuctionBidDto>

    suspend fun getAuctionById(auctionId: String): AuctionDto

    suspend fun getAuctionsByIds(
        auctionIds: List<String>
    ): List<AuctionDto>

    suspend fun getAuctionsAll(
        sort: AuctionSortDto? = null,
        status: List<AuctionStatusDto>? = null,
        origin: String? = null,
        platform: PlatformDto? = null,
        continuation: String? = null,
        size: Int? = null
    ): Slice<AuctionDto>

    suspend fun getAuctionsByCollection(
        contract: String,
        seller: String? = null,
        origin: String? = null,
        status: List<AuctionStatusDto>? = null,
        platform: PlatformDto? = null,
        continuation: String? = null,
        size: Int? = null
    ): Slice<AuctionDto>

    suspend fun getAuctionsByItem(
        itemId: String,
        seller: String? = null,
        sort: AuctionSortDto? = null,
        origin: String? = null,
        status: List<AuctionStatusDto>? = null,
        currencyId: String? = null,
        platform: PlatformDto? = null,
        continuation: String? = null,
        size: Int? = null
    ): Slice<AuctionDto>

    suspend fun getAuctionsBySeller(
        seller: String,
        status: List<AuctionStatusDto>? = null,
        origin: String? = null,
        platform: PlatformDto? = null,
        continuation: String? = null,
        size: Int? = null
    ): Slice<AuctionDto>
}
