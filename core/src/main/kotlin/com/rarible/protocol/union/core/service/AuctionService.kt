package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.service.router.BlockchainService
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionSortDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.PlatformDto

interface AuctionService : BlockchainService {

    suspend fun getAuctionsByIds(
        orderIds: List<String>
    ): List<AuctionDto>

    suspend fun getAuctionsAll(
        sort: AuctionSortDto? = null,
        status: List<AuctionStatusDto>? = null,
        origin: String? = null,
        platform: PlatformDto? = null,
        continuation: String? = null,
        size: Int? = null
    ): Slice<AuctionDto>

    suspend fun getAuctionsByItem(
        contract: String,
        tokenId: String,
        seller: String? = null,
        sort: AuctionSortDto? = null,
        origin: String? = null,
        status: List<AuctionStatusDto>? = null,
        currencyId: String? = null,
        platform: PlatformDto? = null,
        continuation: String? = null,
        size: Int? = null
    ): Slice<AuctionDto>
}
