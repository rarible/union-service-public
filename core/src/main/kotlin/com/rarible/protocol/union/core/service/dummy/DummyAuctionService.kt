package com.rarible.protocol.union.core.service.dummy

import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionSortDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto

class DummyAuctionService(
    blockchain: BlockchainDto
) : AbstractBlockchainService(blockchain), AuctionService {

    override suspend fun getAuctionsByIds(orderIds: List<String>): List<AuctionDto> {
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

    override suspend fun getAuctionsByItem(
        contract: String,
        tokenId: String,
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

}
