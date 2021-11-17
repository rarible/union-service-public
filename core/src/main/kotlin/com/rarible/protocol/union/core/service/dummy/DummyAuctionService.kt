package com.rarible.protocol.union.core.service.dummy

import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.BlockchainDto

class DummyAuctionService(
    blockchain: BlockchainDto
) : AbstractBlockchainService(blockchain), AuctionService {

    override suspend fun getAuctionsByIds(orderIds: List<String>): List<AuctionDto> {
        return emptyList()
    }

}
