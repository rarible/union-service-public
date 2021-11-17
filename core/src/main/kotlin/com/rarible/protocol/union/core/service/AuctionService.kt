package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.service.router.BlockchainService
import com.rarible.protocol.union.dto.AuctionDto

interface AuctionService : BlockchainService {

    suspend fun getAuctionsByIds(
        orderIds: List<String>
    ): List<AuctionDto>
}
