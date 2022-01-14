package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.DefaultBlockchainProperties
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.stereotype.Component

@Component
class AuctionContractService(
    blockchainProperties: List<DefaultBlockchainProperties>
) {

    private val auctionContracts = blockchainProperties.associateBy({ it.blockchain }, { HashSet(it.auctionContracts) })

    // TODO should we use here blockchainGroup instead of blockchain?
    suspend fun isAuctionContract(blockchain: BlockchainDto, contract: String): Boolean {
        return auctionContracts[blockchain]!!.contains(contract)
    }

}